package com.javaguy.nhxserver.model.dto;

import com.javaguy.nhxserver.model.entity.PaymentMethod;

import java.time.LocalDateTime;

public record PaymentMethodDto(
        Long id,
        String name,
        String mobileNumber,
        LocalDateTime dateAdded
) {
    public static PaymentMethodDto fromEntity(PaymentMethod paymentMethod) {
        return new PaymentMethodDto(
                paymentMethod.getId(),
                paymentMethod.getName(),
                paymentMethod.getMobileNumber(),
                paymentMethod.getDateAdded()
        );
    }
}
