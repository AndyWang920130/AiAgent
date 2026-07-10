package com.example.myapp.service.mapper;

import com.example.myapp.domain.BlogConfig;
import com.example.myapp.service.dto.BlogConfigDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BlogConfigMapper extends EntityMapper<BlogConfigDTO, BlogConfig> {
}
