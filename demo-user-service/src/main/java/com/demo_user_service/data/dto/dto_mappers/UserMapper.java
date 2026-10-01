package com.demo_user_service.data.dto.dto_mappers;

import org.springframework.stereotype.Component;

import com.demo_user_service.data.dto.UserDto;
import com.demo_user_service.data.models.User;

@Component
public class UserMapper {
    public User toEntity(UserDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("cant be null");
        }

        User entity = new User();
        if (dto.getLogin() != null)
            entity.setLogin(dto.getLogin());
        if (dto.getRole() != null)
            entity.setRole(dto.getRole());
        if (dto.getPassword() != null)
            entity.setHashPassword(dto.getPassword());
        if (dto.getPhoneNumber() != null)
            entity.setPhoneNumber(dto.getPhoneNumber());
        return entity;
    }

    public UserDto toDto(User entity) {
        if (entity == null) {
            throw new IllegalArgumentException("cant be null");
        }

        UserDto dto = new UserDto();
        dto.setId(entity.getId());
        dto.setLogin(entity.getLogin());
        dto.setRole(entity.getRole());
        dto.setPhoneNumber(entity.getPhoneNumber());
        dto.setRegistered(entity.getRegistered());

        return dto;
    }

    public User updateEntity(UserDto dto, User entity) {
        if (dto == null || entity == null) {
            throw new IllegalArgumentException("cant be null");
        }

        if (dto.getLogin() != null)
            entity.setLogin(dto.getLogin());
        if (dto.getRole() != null)
            entity.setRole(dto.getRole());
        if (dto.getPassword() != null)
            entity.setHashPassword(dto.getPassword());
        if (dto.getEmail() != null)
            // entity.setEmail(dto.getEmail());
        if (dto.getPhoneNumber() != null)
            entity.setPhoneNumber(dto.getPhoneNumber());
        // if (dto.getTrustLevel() != null)
        //     entity.setTrustLevel(dto.getTrustLevel());
        // if (dto.getConsecutiveCancellations() != null)
        //     entity.setConsecutiveCancellations(dto.getConsecutiveCancellations());

        return entity;
    }
}
