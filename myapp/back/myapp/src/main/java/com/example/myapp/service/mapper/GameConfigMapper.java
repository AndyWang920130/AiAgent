package com.example.myapp.service.mapper;

import com.example.myapp.domain.GameConfig;
import com.example.myapp.service.dto.GameConfigDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GameConfigMapper extends EntityMapper<GameConfigDTO, GameConfig> {
}
