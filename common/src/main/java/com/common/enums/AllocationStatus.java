package com.common.enums;

public enum AllocationStatus {
    ACTIVE,      // Ресурс заброньовано, він зайнятий
    CANCELLED,   // Бронювання скасовано, ресурс знову вільний
    COMPLETED    // Бронювання успішно завершено
}
