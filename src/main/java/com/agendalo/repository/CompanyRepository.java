package com.agendalo.repository;

import com.agendalo.domain.Company;
import com.agendalo.domain.Image;
import com.agendalo.domain.enums.CompanyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    List<Company> findByOwnerId(Long ownerId);
    boolean existsByUrlSlug(String urlSlug);
    Optional<Company> findByUrlSlug(String urlSlug);

    // NUEVO: Buscar por email del propietario
    Optional<Company> findByOwner_Email(String ownerEmail);

    List<Company> findByStatus(CompanyStatus status);

    // Para búsquedas con filtro de status
    List<Company> findByNameContainingIgnoreCaseAndStatus(String name, CompanyStatus status);
    List<Company> findByAddressContainingIgnoreCaseAndStatus(String address, CompanyStatus status);
    List<Company> findByNameContainingIgnoreCaseAndAddressContainingIgnoreCaseAndStatus(
            String name, String address, CompanyStatus status);
    @Query("SELECT DISTINCT c FROM Company c " +
            "JOIN c.owner u " +
            "LEFT JOIN c.galleryImages i WITH i.imageType = 'LOGO' " +
            "WHERE c.status = :status")
    List<Company> findPendingCompaniesData(@Param("status") CompanyStatus status);
}