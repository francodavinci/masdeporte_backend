package com.agendalo.dto.user;

import com.agendalo.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String email;
    private String phoneNumber;
    private String role;
    private String name;
    private LocalDateTime registrationDate;
    private String profileImageUrl;
    private int totalAppointments;
    private int totalCompanies;
    private boolean hasCompany;
    private List<CompanyBasicInfo> companies;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompanyBasicInfo {
        private Long id;
        private String name;
        private String category;
        private String urlSlug;
        private String logoUrl;
    }

    public static UserProfileResponse fromEntity(User user) {
        // Asegurarse de que las colecciones estén inicializadas
        int appointmentsCount = user.getAppointments() != null ? user.getAppointments().size() : 0;
        
        // Manejar la lista de compañías
        List<CompanyBasicInfo> companyInfos;
        int companiesCount;
        boolean hasCompany;
        
        if (user.getCompanies() != null && !user.getCompanies().isEmpty()) {
            companyInfos = user.getCompanies().stream()
                    .map(company -> CompanyBasicInfo.builder()
                            .id(company.getId())
                            .name(company.getName())
                            .category(company.getCategory())
                            .urlSlug(company.getUrlSlug())
                            .logoUrl(company.getId() != null ? "/api/images/" + company.getId() : null)
                            .build())
                    .toList();
            companiesCount = user.getCompanies().size();
            hasCompany = true;
        } else {
            companyInfos = List.of();
            companiesCount = 0;
            hasCompany = false;
        }

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .name(user.getName())
                .registrationDate(user.getRegistrationDate())
                .profileImageUrl(user.getProfileImageUrl())
                .totalAppointments(appointmentsCount)
                .totalCompanies(companiesCount)
                .hasCompany(hasCompany)
                .companies(companyInfos)
                .build();
    }
} 