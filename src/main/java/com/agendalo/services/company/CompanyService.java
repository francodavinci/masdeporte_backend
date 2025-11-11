package com.agendalo.services.company;

import com.agendalo.domain.BusinessService;
import com.agendalo.dto.company.CompanyRegistrationDto;
import com.agendalo.dto.company.CompanyStatusUpdateDto;
import com.agendalo.dto.response.ApiResponse;
import com.agendalo.dto.service.ServiceRegistrationDto;
import com.agendalo.dto.service.ServiceUpdateDto;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface CompanyService {
    ResponseEntity<ApiResponse> create(CompanyRegistrationDto companyRegistrationDto, String ownerEmail);
    ResponseEntity<ApiResponse> update(CompanyRegistrationDto companyRegistrationDto, String ownerEmail);
    ResponseEntity<ApiResponse> findAll();
    ResponseEntity<ApiResponse> getCompanyById(Long id);
    ResponseEntity<ApiResponse> createService(ServiceRegistrationDto serviceRegistrationDto, String ownerEmail);
    ResponseEntity<ApiResponse> updateService(Long serviceId, ServiceUpdateDto serviceUpdateDto, String ownerEmail);
    ResponseEntity<ApiResponse> deleteService(Long serviceId, String ownerEmail);
    ResponseEntity<ApiResponse> getServices(String ownerEmail);
    ResponseEntity<ApiResponse> getCompanyBySlug(String urlSlug);
    ResponseEntity<ApiResponse> findUrlSlug(String ownerEmail);
    ResponseEntity<ApiResponse> searchCompanies(String query, String location);
    ResponseEntity<ApiResponse> getCompanyByOwnerEmail(String ownerEmail);
    ResponseEntity<ApiResponse> getPendingCompanies();
    ResponseEntity<ApiResponse> updateCompanyStatus(Long companyId, CompanyStatusUpdateDto statusUpdateDto, String adminEmail);
    ResponseEntity<ApiResponse> ownerUpdateStatus(Long companyId, CompanyStatusUpdateDto statusUpdateDto, String adminEmail);
}