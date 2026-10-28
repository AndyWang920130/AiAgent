package com.example.myapp.service.mapper;

import com.example.myapp.domain.RoleConfig;
import com.example.myapp.service.dto.RoleConfigDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleConfigMapper extends EntityMapper<RoleConfigDTO, RoleConfig> {

    /** {@code userCount} is counted from the user table, not carried on the entity. */
    @Override
    @Mapping(target = "userCount", ignore = true)
    RoleConfigDTO toDto(RoleConfig entity);
}
