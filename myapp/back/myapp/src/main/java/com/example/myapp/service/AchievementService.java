package com.example.myapp.service;

import com.example.myapp.contants.enumeration.AchievementType;
import com.example.myapp.domain.Achievement;
import com.example.myapp.repository.AchievementRepository;
import com.example.myapp.service.dto.AchievementDTO;
import com.example.myapp.service.mapper.AchievementMapper;
import com.example.myapp.web.rest.vm.AchievementSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service that awards and reports achievement points earned by user actions.
 */
@Service
@Transactional
public class AchievementService {

    private static final Logger LOG = LoggerFactory.getLogger(AchievementService.class);

    private final AchievementRepository achievementRepository;
    private final AchievementMapper achievementMapper;

    public AchievementService(AchievementRepository achievementRepository, AchievementMapper achievementMapper) {
        this.achievementRepository = achievementRepository;
        this.achievementMapper = achievementMapper;
    }

    /**
     * Award the type's default number of points to the given user.
     */
    public void award(String login, AchievementType type) {
        award(login, type, type.getDefaultPoints());
    }

    /**
     * Add {@code points} to the user's bucket for {@code type}, creating the
     * bucket on first award. No-op for anonymous / system actors.
     */
    public void award(String login, AchievementType type, long points) {
        if (login == null || login.isBlank() || "system".equalsIgnoreCase(login) || points == 0) {
            return;
        }
        LOG.debug("Awarding {} {} points to {}", points, type, login);
        Achievement achievement = achievementRepository.findByLoginAndType(login, type)
            .orElseGet(() -> new Achievement().login(login).type(type).points(0L));
        achievement.setPoints((achievement.getPoints() == null ? 0L : achievement.getPoints()) + points);
        achievement.setCreatedBy(login);
        achievement.setLastModifiedBy(login);
        achievementRepository.save(achievement);
    }

    /**
     * Summarize a user's achievement points: total plus per-type breakdown.
     */
    @Transactional(readOnly = true)
    public AchievementSummary getSummary(String login) {
        List<AchievementDTO> items = achievementMapper.toDto(achievementRepository.findByLoginOrderByTypeAsc(login));
        long total = items.stream().mapToLong(item -> item.getPoints() == null ? 0L : item.getPoints()).sum();
        return new AchievementSummary(total, items);
    }
}
