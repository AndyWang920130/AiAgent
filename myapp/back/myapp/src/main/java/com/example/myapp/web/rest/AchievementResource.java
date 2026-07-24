package com.example.myapp.web.rest;

import com.example.myapp.service.AchievementService;
import com.example.myapp.utils.SecurityUtil;
import com.example.myapp.web.rest.vm.AchievementSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AchievementResource {

    private static final Logger LOG = LoggerFactory.getLogger(AchievementResource.class);

    private final AchievementService achievementService;

    public AchievementResource(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @GetMapping("/achievements/my")
    public ResponseEntity<AchievementSummary> getMyAchievements() {
        LOG.debug("REST request to get current user's achievements");
        return ResponseEntity.ok(achievementService.getSummary(SecurityUtil.getCurrentUsername()));
    }
}
