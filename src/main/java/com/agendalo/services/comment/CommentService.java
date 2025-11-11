package com.agendalo.services.comment;

import com.agendalo.domain.Comment;
import com.agendalo.domain.Company;
import com.agendalo.domain.User;
import com.agendalo.dto.comment.CommentRequestDto;
import com.agendalo.dto.comment.CommentResponseDto;
import com.agendalo.dto.response.ApiResponse;
import com.agendalo.repository.CommentRepository;
import com.agendalo.repository.CompanyRepository;
import com.agendalo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    @Transactional
    public ResponseEntity<ApiResponse> createComment(CommentRequestDto dto, String userEmail) {
        try {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            Company company = companyRepository.findById(dto.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Compañía no encontrada"));

            Comment comment = Comment.builder()
                    .company(company)
                    .user(user)
                    .userName(user.getEmail())
                    .points(dto.getPoints())
                    .description(dto.getDescription())
                    .build();

            comment = commentRepository.save(comment);
            return ResponseEntity.ok(new ApiResponse(true, "Comentario creado exitosamente", 
                    CommentResponseDto.fromEntity(comment)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getCompanyComments(Long companyId) {
        try {
            List<Comment> comments = commentRepository.findByCompanyId(companyId);
            List<CommentResponseDto> commentDtos = comments.stream()
                    .map(CommentResponseDto::fromEntity)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(new ApiResponse(true, "Comentarios obtenidos exitosamente", commentDtos));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse(false, "Error al obtener los comentarios: " + e.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse> deleteComment(Long commentId, String userEmail) {
        try {
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new RuntimeException("Comentario no encontrado"));

            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            if (!comment.getUser().getId().equals(user.getId())) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "No tienes permiso para eliminar este comentario"));
            }

            commentRepository.delete(comment);
            return ResponseEntity.ok(new ApiResponse(true, "Comentario eliminado exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
} 