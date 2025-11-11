package com.agendalo.controller.comment;

import com.agendalo.dto.comment.CommentRequestDto;
import com.agendalo.dto.response.ApiResponse;
import com.agendalo.services.comment.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<ApiResponse> createComment(@RequestBody CommentRequestDto commentRequestDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return commentService.createComment(commentRequestDto, email);
    }

    @GetMapping("/companies/{companyId}")
    public ResponseEntity<ApiResponse> getCompanyComments(@PathVariable Long companyId) {
        return commentService.getCompanyComments(companyId);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse> deleteComment(@PathVariable Long commentId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return commentService.deleteComment(commentId, email);
    }
} 