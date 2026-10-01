package com.common.dto.demo_resource_service_dto;

import com.common.enums.Gender;

public record WorkerDataDto(Integer id, String firstName, String lastName, Gender gender) {}
