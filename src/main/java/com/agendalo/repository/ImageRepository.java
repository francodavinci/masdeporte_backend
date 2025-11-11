package com.agendalo.repository;

import com.agendalo.domain.Image;
import com.agendalo.domain.enums.ImageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByCompanyId(Long companyId);
    long countByCompanyId(Long companyId);

    @Query("SELECT i FROM Image i WHERE i.company.id = :companyId AND i.imageType = :imageType")
    Image findByCompanyIdAndImageType(@Param("companyId") Long companyId, @Param("imageType") ImageType imageType);

} 