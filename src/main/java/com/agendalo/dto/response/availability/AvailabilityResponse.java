package com.agendalo.dto.response.availability;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityResponse {
    private List<LocalTime> availableSlots;
}
