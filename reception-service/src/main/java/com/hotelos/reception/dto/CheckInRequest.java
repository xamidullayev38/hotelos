package com.hotelos.reception.dto;

import com.hotelos.reception.model.Proximity;
import com.hotelos.reception.model.RoomType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Boundary input. Every field is validated by Jakarta Bean Validation before
 * the controller method runs. This addresses Security task 3.2 (input
 * validation) at the only place untrusted input enters the system.
 */
public class CheckInRequest {

    @NotBlank
    public String fullName;

    @NotNull
    public RoomType roomType;

    /** Null means no floor preference. */
    public Integer floorPreference;

    public Proximity proximityPreference = Proximity.NONE;

    @Min(1)
    public int nights = 1;
}
