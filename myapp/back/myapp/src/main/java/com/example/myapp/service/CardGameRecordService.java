package com.example.myapp.service;

import com.example.myapp.contants.enumeration.CardGameRecordOutcome;
import com.example.myapp.domain.CardGame;
import com.example.myapp.domain.CardGamePlayer;
import com.example.myapp.domain.CardGameRecord;
import com.example.myapp.domain.CardGameRecordPlayer;
import com.example.myapp.domain.User;
import com.example.myapp.repository.CardGameRecordPlayerRepository;
import com.example.myapp.repository.CardGameRecordRepository;
import com.example.myapp.repository.UserRepository;
import com.example.myapp.service.dto.CardGameRecordDTO;
import com.example.myapp.service.dto.CardGameRecordPlayerDTO;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes and reads 70235 match records — the history behind the administrators' match record
 * list.
 *
 * <p>The history has to be written as it happens. {@link CardGame} is the room rather than the
 * match, and a rematch re-deals that row: scores go back to 0, {@code startedDate} is
 * re-stamped and the winner is cleared. So the moment a match ends is the only moment its
 * result exists anywhere, and {@link #record} is called from {@code CardGameService} at exactly
 * the two points where a dealt game ends.
 *
 * <p>Separate from {@code CardGameService} because it is a separate concern: that class plays
 * the game, this one keeps the books. {@code CardGameService} already runs past 1500 lines.
 */
@Service
@Transactional
public class CardGameRecordService {

    private static final Logger LOG = LoggerFactory.getLogger(CardGameRecordService.class);

    /** Prefix for the synthetic CPU seats on tables played before CPU players were withdrawn. */
    private static final String CPU_PREFIX = "cpu-";

    private final CardGameRecordRepository recordRepository;
    private final CardGameRecordPlayerRepository recordPlayerRepository;
    private final UserRepository userRepository;

    public CardGameRecordService(
        CardGameRecordRepository recordRepository,
        CardGameRecordPlayerRepository recordPlayerRepository,
        UserRepository userRepository
    ) {
        this.recordRepository = recordRepository;
        this.recordPlayerRepository = recordPlayerRepository;
        this.userRepository = userRepository;
    }

    /**
     * Snapshot a match that has just ended.
     *
     * <p>Idempotent: a deal is identified by {@code (gameId, startedDate)}, so a second call
     * for the same match returns without writing. That matters because the two callers are
     * reached by different routes — scoring, and a player leaving — and a room moves through
     * more terminal states than it plays matches.
     *
     * @param game     the room, with the match's result still on it. Read, never modified.
     * @param outcome  SCORED for a match that reached scoring, ABANDONED for one walked out of.
     * @param leftBy   who walked out, for ABANDONED. Null otherwise.
     * @param endedAt  when the match ended. Passed in rather than read off the clock here: a
     *                 match stopped by the round cap ended when the cap was reached, which can
     *                 be long before anything read the row and noticed.
     * @param seats    the seats that played, with their final scores.
     */
    public void record(
        CardGame game,
        CardGameRecordOutcome outcome,
        String leftBy,
        Instant endedAt,
        List<CardGamePlayer> seats
    ) {
        if (game.getId() == null || game.getStartedDate() == null) {
            // Never dealt, so there is no match to record. Reachable only by a caller mistake —
            // the two real callers both run on a game that has been dealt.
            LOG.warn("70235 match record skipped: game {} was never dealt", game.getId());
            return;
        }
        if (recordRepository.existsByGameIdAndStartedDate(game.getId(), game.getStartedDate())) {
            LOG.debug("70235 match record for game {} deal {} already written", game.getId(), game.getStartedDate());
            return;
        }

        CardGameRecord entity = new CardGameRecord();
        entity.setGameId(game.getId());
        entity.setHostUsername(game.getHostUsername());
        entity.setStartedDate(game.getStartedDate());
        entity.setEndedDate(endedAt);
        entity.setOutcome(outcome);
        // An abandoned match has no winner: scoring never ran, so nobody collected the leftover
        // points and the standings are only where play happened to stop.
        entity.setWinnerUsername(outcome == CardGameRecordOutcome.SCORED ? game.getWinnerUsername() : null);
        entity.setFirstOutUsername(game.getFirstOutUsername());
        entity.setLeftByUsername(leftBy);
        entity.setTrickCount(game.getTrickNumber());
        entity.setPlayerCount(seats.size());

        for (CardGamePlayer seat : seats) {
            CardGameRecordPlayer row = new CardGameRecordPlayer();
            row.setRecord(entity);
            row.setUsername(seat.getUsername());
            row.setSeat(seat.getSeat());
            row.setScore(seat.getScore());
            row.setCpu(seat.isCpu());
            entity.getPlayers().add(row);
        }

        recordRepository.save(entity);
        LOG.debug(
            "70235 match recorded: game {}, {} seats, outcome {}, winner {}",
            game.getId(),
            seats.size(),
            outcome,
            entity.getWinnerUsername()
        );
    }

    // ----- The administrator's list ----------------------------------------------------------

    /**
     * One page of match records for the administrator list.
     *
     * @param query   free text matched against the creator, any player's login, or the winner.
     * @param outcome restrict to one outcome, or null/blank for any.
     */
    @Transactional(readOnly = true)
    public Page<CardGameRecordDTO> listForAdmin(String query, String outcome, Pageable pageable) {
        String q = query == null || query.isBlank() ? "%" : "%" + query.trim().toLowerCase() + "%";
        Page<CardGameRecord> page = recordRepository.findForAdmin(q, parseOutcomeOrNull(outcome), pageable);
        return withSeatsAndNames(page);
    }

    /**
     * Attach each record's seats and resolve every login to a display name — two queries for
     * the whole page rather than per row, following {@code FeedbackService.withNames}.
     */
    private Page<CardGameRecordDTO> withSeatsAndNames(Page<CardGameRecord> page) {
        List<CardGameRecord> records = page.getContent();
        if (records.isEmpty()) {
            return page.map(record -> toDto(record, List.of(), Map.of()));
        }

        List<Long> ids = records.stream().map(CardGameRecord::getId).toList();
        Map<Long, List<CardGameRecordPlayer>> seatsByRecord = recordPlayerRepository
            .findByRecordIdInOrderBySeatAsc(ids)
            .stream()
            .collect(Collectors.groupingBy(seat -> seat.getRecord().getId()));

        // Every login the page mentions, in one lookup. CPU seats are not users, so they are
        // left out of the query and labelled from their own username instead.
        Set<String> logins = new HashSet<>();
        for (CardGameRecord record : records) {
            addRealLogin(logins, record.getHostUsername());
            addRealLogin(logins, record.getWinnerUsername());
            addRealLogin(logins, record.getLeftByUsername());
            for (CardGameRecordPlayer seat : seatsByRecord.getOrDefault(record.getId(), List.of())) {
                addRealLogin(logins, seat.getUsername());
            }
        }
        Map<String, User> users = logins.isEmpty()
            ? Map.of()
            : userRepository.findByLoginIn(logins).stream()
                .collect(Collectors.toMap(User::getLogin, Function.identity(), (a, b) -> a));

        return page.map(record -> toDto(record, seatsByRecord.getOrDefault(record.getId(), List.of()), users));
    }

    private void addRealLogin(Set<String> logins, String login) {
        if (login != null && !login.isBlank() && !login.startsWith(CPU_PREFIX)) {
            logins.add(login);
        }
    }

    private CardGameRecordDTO toDto(CardGameRecord record, List<CardGameRecordPlayer> seats, Map<String, User> users) {
        String winner = record.getWinnerUsername();

        // Highest score first, so the client can render the standings in order without sorting.
        // Seat order breaks a tie, which is stable and matches the order the table dealt in.
        List<CardGameRecordPlayer> standings = new ArrayList<>(seats);
        standings.sort(
            Comparator.comparingInt(CardGameRecordPlayer::getScore).reversed()
                .thenComparingInt(CardGameRecordPlayer::getSeat)
        );

        List<CardGameRecordPlayerDTO> players = standings.stream()
            .map(seat -> new CardGameRecordPlayerDTO(
                seat.getUsername(),
                nameOf(seat.getUsername(), users),
                seat.getSeat(),
                seat.getScore(),
                seat.isCpu(),
                winner != null && winner.equals(seat.getUsername())
            ))
            .toList();

        return new CardGameRecordDTO(
            record.getId(),
            record.getGameId(),
            record.getHostUsername(),
            nameOf(record.getHostUsername(), users),
            record.getStartedDate(),
            record.getEndedDate(),
            record.getOutcome().name(),
            winner,
            winner == null ? null : nameOf(winner, users),
            record.getFirstOutUsername(),
            record.getLeftByUsername(),
            record.getLeftByUsername() == null ? null : nameOf(record.getLeftByUsername(), users),
            record.getTrickCount(),
            record.getPlayerCount(),
            players
        );
    }

    /**
     * Display name for a login: the user's current name, {@code "CPU 2"} for a CPU seat, or the
     * login itself for an account that has since been removed.
     */
    private String nameOf(String login, Map<String, User> users) {
        if (login == null || login.isBlank()) {
            return null;
        }
        if (login.startsWith(CPU_PREFIX)) {
            return "CPU " + login.substring(CPU_PREFIX.length());
        }
        User user = users.get(login);
        return user == null ? login : displayName(user);
    }

    private String displayName(User user) {
        if (user.getRealName() != null && !user.getRealName().isBlank()) {
            return user.getRealName();
        }
        if (user.getNickName() != null && !user.getNickName().isBlank()) {
            return user.getNickName();
        }
        return user.getLogin();
    }

    /** Lenient on purpose: an unrecognised filter means "any" rather than an error. */
    private CardGameRecordOutcome parseOutcomeOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return CardGameRecordOutcome.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
