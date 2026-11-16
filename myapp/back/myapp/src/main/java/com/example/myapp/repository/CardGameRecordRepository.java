package com.example.myapp.repository;

import com.example.myapp.contants.enumeration.CardGameRecordOutcome;
import com.example.myapp.domain.CardGameRecord;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CardGameRecordRepository extends JpaRepository<CardGameRecord, Long> {

    /**
     * Has this particular deal already been recorded? The pair is the match's identity (a deal
     * re-stamps {@code startedDate}), so this is what makes writing a record idempotent: a
     * second attempt for the same match is a no-op instead of a duplicate row.
     */
    boolean existsByGameIdAndStartedDate(Long gameId, Instant startedDate);

    /**
     * Administrator match record listing: optional free-text match against the creator or any
     * player who held a seat, plus an optional outcome filter. A null filter means "any", which
     * keeps one query serving every combination the page offers — same shape as
     * {@link FeedbackRepository#findForAdmin}.
     *
     * <p>A plain {@code left join} rather than {@code join fetch}: a fetch join would make
     * Hibernate paginate in memory. The seats for a page are loaded separately, in one query,
     * by {@link CardGameRecordPlayerRepository#findByRecordIdInOrderBySeatAsc}.
     *
     * @param q       lower-cased term already wrapped in {@code %}, or {@code %} for all.
     * @param outcome outcome to restrict to, or null for any.
     */
    @Query("""
        select distinct r from CardGameRecord r
        left join r.players p
        where (lower(r.hostUsername) like :q
            or lower(p.username) like :q
            or lower(coalesce(r.winnerUsername, '')) like :q)
          and (:outcome is null or r.outcome = :outcome)
        """)
    Page<CardGameRecord> findForAdmin(
        @Param("q") String q,
        @Param("outcome") CardGameRecordOutcome outcome,
        Pageable pageable
    );
}
