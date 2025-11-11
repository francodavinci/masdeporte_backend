package com.agendalo.services.image;

import com.agendalo.domain.Company;
import com.agendalo.domain.Image;
import com.agendalo.domain.Provider;
import com.agendalo.domain.User;
import com.agendalo.domain.enums.ImageType;
import com.agendalo.repository.CompanyRepository;
import com.agendalo.repository.ImageRepository;
import com.agendalo.repository.ProviderRepository;
import com.agendalo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final CompanyRepository companyRepository;
    private final ImageRepository imageRepository;

    public Long uploadCompanyLogo(MultipartFile file, Long companyId) throws IOException {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        Image image = saveImage(file, company, ImageType.LOGO);
        return image.getId();
    }

    private Image saveImage(MultipartFile file, Company company, ImageType imageType) throws IOException {
        Image image = Image.builder()
                .name(file.getOriginalFilename())
                .type(file.getContentType())
                .data(file.getBytes())
                .imageType(imageType) // ¡IMPORTANTE! Establecer el imageType
                .company(company)
                .build();

        return imageRepository.save(image);
    }

    public Image getImage(Long imageId) {
        return imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Imagen no encontrada"));
    }
}