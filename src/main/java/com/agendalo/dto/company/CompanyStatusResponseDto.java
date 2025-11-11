package com.agendalo.dto.company;

import com.agendalo.domain.enums.CompanyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyStatusResponseDto {
    private Long id;
    private String name;
    private String category;
    private String phone;
    private String address;
    private CompanyStatus status;
    private LocalDateTime registrationDate;
    private String ownerEmail;
}
