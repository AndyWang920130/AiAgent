package com.example.myapp.web.rest.vm;

import com.example.myapp.service.dto.AchievementDTO;

import java.util.List;

/**
 * View model summarizing a user's achievement points: the overall total plus
 * the per-type breakdown.
 */
public record AchievementSummary(long total, List<AchievementDTO> items) {}
