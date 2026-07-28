package com.example.myapp.service.mapper;

import com.example.myapp.domain.DataIntegration;
import com.example.myapp.service.dto.DataIntegrationDTO;
import org.mapstruct.Mapper;

/**
 * Mapper for the entity {@link DataIntegration} and its DTO {@link DataIntegrationDTO}.
 */
@Mapper(componentModel = "spring")
public interface DataIntegrationMapper extends EntityMapper<DataIntegrationDTO, DataIntegration> {
}
