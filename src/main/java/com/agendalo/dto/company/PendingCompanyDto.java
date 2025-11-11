package com.agendalo.dto.company;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingCompanyDto {
    private Long id;
    private String companyName;
    private String ownerName;
    private String status;
    private LocalDateTime registrationDate;
    private String companyImage;
}
