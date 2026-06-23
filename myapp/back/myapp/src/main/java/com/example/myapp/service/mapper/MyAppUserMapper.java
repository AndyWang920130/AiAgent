package com.example.myapp.service.mapper;


import com.example.myapp.domain.MyAppUser;
import com.example.myapp.service.dto.MyAppUserDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link MyAppUser} and its DTO {@link MyAppUserDTO}.
 */
@Mapper(componentModel = "spring")
public interface MyAppUserMapper extends EntityMapper<MyAppUserDTO, MyAppUser> {}
