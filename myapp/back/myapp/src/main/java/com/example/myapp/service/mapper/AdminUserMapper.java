package com.example.myapp.service.mapper;

import com.example.myapp.domain.User;
import com.example.myapp.service.dto.AdminUserDTO;
import org.mapstruct.Mapper;

/**
 * Mapper for the entity {@link User} and its administrator-facing DTO
 * {@link AdminUserDTO}.
 *
 * <p>Only the entity-to-DTO direction exists on purpose. Writes go through explicit
 * request objects in {@code web.rest.vm} so that {@code login}, {@code password} and the
 * auditing columns can never be set from a request body by mapping a whole DTO over an
 * entity.
 */
@Mapper(componentModel = "spring")
public interface AdminUserMapper {

    AdminUserDTO toDto(User entity);
}
