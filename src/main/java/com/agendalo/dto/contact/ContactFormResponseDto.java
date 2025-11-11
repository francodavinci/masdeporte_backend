package com.agendalo.dto.contact;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactFormResponseDto {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String inquiry;
    private LocalDateTime createdAt;
    private Boolean read;
    private String response;
    private LocalDateTime respondedAt;
}
