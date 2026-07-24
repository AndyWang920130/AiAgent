package com.example.myapp.service.mapper;

import com.example.myapp.domain.Achievement;
import com.example.myapp.service.dto.AchievementDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Achievement} and its DTO {@link AchievementDTO}.
 */
@Mapper(componentModel = "spring")
public interface AchievementMapper extends EntityMapper<AchievementDTO, Achievement> {}
