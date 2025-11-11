package com.agendalo.dto.comment;

import com.agendalo.domain.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponseDto {
    private Long id;
    private Long companyId;
    private Long userId;
    private String userName;
    private Integer points;
    private String description;
    private LocalDateTime createdAt;

    public static CommentResponseDto fromEntity(Comment comment) {
        return CommentResponseDto.builder()
                .id(comment.getId())
                .companyId(comment.getCompany().getId())
                .userId(comment.getUser().getId())
                .userName(comment.getUserName())
                .points(comment.getPoints())
                .description(comment.getDescription())
                .createdAt(comment.getCreatedAt())
                .build();
    }
} 