package com.agendalo.controller.company;

import com.agendalo.domain.BusinessService;
import com.agendalo.dto.company.CompanyRegistrationDto;
import com.agendalo.dto.company.CompanyStatusUpdateDto;
import com.agendalo.dto.response.ApiResponse;
import com.agendalo.dto.service.ServiceRegistrationDto;
import com.agendalo.dto.service.ServiceUpdateDto;
import com.agendalo.services.company.CompanyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.agendalo.dto.user.EmailRequest;

@RestController
@RequestMapping("companies")
@Slf4j
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
        log.info("CompanyController inicializado correctamente");
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createCompany(@RequestBody CompanyRegistrationDto companyRegistrationDto) {
        log.info("POST /companies - Creando compañía");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return companyService.create(companyRegistrationDto, email);
    }

    @PatchMapping(path = "/update")
    public ResponseEntity<ApiResponse> updateCompany(@RequestBody CompanyRegistrationDto companyRegistrationDto) {
        log.info("PATCH /companies - Actualizando compañía");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return companyService.update(companyRegistrationDto, email);
    }

    @PostMapping("/services")
    public ResponseEntity<ApiResponse> createService(@RequestBody ServiceRegistrationDto serviceRegistrationDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return companyService.createService(serviceRegistrationDto, email);
    }

    @PutMapping("/services/{serviceId}")
    public ResponseEntity<ApiResponse> updateService(
            @PathVariable Long serviceId,
            @RequestBody ServiceUpdateDto serviceUpdateDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return companyService.updateService(serviceId, serviceUpdateDto, email);
    }

    @DeleteMapping("/services/{serviceId}")
    public ResponseEntity<ApiResponse> deleteService(@PathVariable Long serviceId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return companyService.deleteService(serviceId, email);
    }

    @GetMapping("/services")
    public ResponseEntity<ApiResponse> getServices() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return companyService.getServices(email);
    }

    @GetMapping("/public/{urlSlug}")
    public ResponseEntity<ApiResponse> getCompanyBySlug(@PathVariable String urlSlug) {
        log.info("Solicitando información de empresa con slug: {}", urlSlug);
        return companyService.getCompanyBySlug(urlSlug);
    }

    @GetMapping("/exists/{urlSlug}")
    public ResponseEntity<ApiResponse> existsUrlSlug(@PathVariable String urlSlug) {
        log.info("Verificando si existe una compañia con urlSlug: {}", urlSlug);
        return companyService.findUrlSlug(urlSlug);
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse> getAllCompanies() {
        ResponseEntity<ApiResponse> companies = companyService.findAll();
        return companies;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse> searchCompanies(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String location) {

        return companyService.searchCompanies(query, location);
    }

    @PostMapping("/my-company")
    public ResponseEntity<ApiResponse> getMyCompany(@RequestBody EmailRequest request) {
        String userEmail = request.getUserEmail();
        log.info("Solicitando compañía para el usuario: {}", userEmail);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        log.info("Usuario autenticado: {}", email);


        if(userEmail.equals(email)){
            log.info("Email coincide, obteniendo compañía para: {}", email);
            return companyService.getCompanyByOwnerEmail(email);
        }

        log.warn("Email no coincide. Solicitado: {}, Autenticado: {}", userEmail, email);
        return ResponseEntity.badRequest().body(new ApiResponse(false, "El usuario no tiene una compañía asociada"));
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        log.info("Endpoint de prueba funcionando");
        return ResponseEntity.ok("Controlador funcionando correctamente");
    }


    @GetMapping("/pending")
    public ResponseEntity<ApiResponse> getPendingCompanies() {
        return companyService.getPendingCompanies();
    }

    @PutMapping("/{companyId}/status")
    public ResponseEntity<ApiResponse> updateCompanyStatus(
            @PathVariable Long companyId,
            @RequestBody CompanyStatusUpdateDto statusUpdateDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return companyService.updateCompanyStatus(companyId, statusUpdateDto, email);
    }
    @PutMapping("/owner/{companyId}/status")
    public ResponseEntity<ApiResponse> ownerUpdateCompanyStatus(
            @PathVariable Long companyId,
            @RequestBody CompanyStatusUpdateDto statusUpdateDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return companyService.ownerUpdateStatus(companyId, statusUpdateDto, email);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getCompanyById(@PathVariable Long id) {
        return companyService.getCompanyById(id);
    }
}
