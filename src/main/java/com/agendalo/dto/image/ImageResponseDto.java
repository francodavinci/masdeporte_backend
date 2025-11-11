package com.agendalo.dto.image;

import com.agendalo.domain.Image;
import com.agendalo.domain.enums.ImageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageResponseDto {
    private Long id;
    private String name;
    private String type;
    private ImageType imageType;
    private Long companyId;

    public static ImageResponseDto fromEntity(Image image) {
        return ImageResponseDto.builder()
                .id(image.getId())
                .name(image.getName())
                .type(image.getType())
                .imageType(image.getImageType())
                .companyId(image.getCompany().getId())
                .build();
    }
} 