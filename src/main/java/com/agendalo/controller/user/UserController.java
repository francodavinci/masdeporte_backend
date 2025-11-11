package com.agendalo.controller.user;

import com.agendalo.domain.User;
import com.agendalo.dto.google_auth.GoogleLoginRequest;
import com.agendalo.dto.user.UserRequest;
import com.agendalo.dto.user.UserResponse;
import com.agendalo.dto.user.UserProfileResponse;
import com.agendalo.services.auth.GoogleAuthService;
import com.agendalo.services.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@Slf4j
@Tag(name="Users", description = "Controller")
public class UserController {

    @Autowired
    private UserService usersManagementService;

    @Autowired
    private GoogleAuthService googleAuthService;

    @Operation(summary = "Auth - Register", description="Users Sign Up")
    @PostMapping("/auth/register")
    public ResponseEntity<UserResponse> register(@RequestBody UserRequest reg) {
        log.info("Start register user, {}", reg.toString());

        return ResponseEntity.ok(usersManagementService.register(reg));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<UserResponse> login(@RequestBody UserRequest req) {
        log.info("Start login user");
        return ResponseEntity.ok(usersManagementService.login(req));
    }
    @PostMapping("/auth/google")
    public ResponseEntity<UserResponse> googleLogin(@RequestBody GoogleLoginRequest request) {
        log.info("Start Google login");
        return ResponseEntity.ok(googleAuthService.authenticateGoogle(request.getCredential()));
    }
    @PostMapping("/auth/refresh")
    public ResponseEntity<UserResponse> refreshToken(@RequestBody UserRequest request) {
        return ResponseEntity.ok(usersManagementService.refreshToken(request));
    }

    @GetMapping("/admin/get-all-users")
    public ResponseEntity<UserResponse> getAllUsers() {
        return ResponseEntity.ok(usersManagementService.getAllUsers());
    }

    @GetMapping("/admin/get-users/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(usersManagementService.getUsersById(userId));
    }

    @PutMapping("/admin/update/{userId}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long userId, @RequestBody UserRequest userRequest) {
        return ResponseEntity.ok(usersManagementService.updateUser(userId, userRequest));
    }

    @GetMapping("/adminuser/get-profile")
    public ResponseEntity<UserResponse> getMyProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        UserResponse response = usersManagementService.getMyInfo(email);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @DeleteMapping("/admin/delete/{userId}")
    public ResponseEntity<UserResponse> deleteUser(@PathVariable Long userId) {
        return ResponseEntity.ok(usersManagementService.deleteUser(userId));
    }

    @GetMapping("/profile")
    @Operation(summary = "Get User Profile", description = "Obtiene la información detallada del perfil del usuario autenticado")
    public ResponseEntity<UserProfileResponse> getUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return ResponseEntity.ok(usersManagementService.getUserProfile(email));
    }
}