package com.agendalo.controller.provider;

import com.agendalo.dto.provider.ProviderDto;
import com.agendalo.dto.user.UserResponse;
import com.agendalo.services.provider.ProviderService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/providers")
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class ProviderController {

    @Autowired
    ProviderService providerService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody(required = true) ProviderDto providerDto){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        log.info("Start updating user {} to provider", email);

        if( providerService.createProviderByUserEmail(providerDto)) {
            return ResponseEntity.ok("Se actualizo correctamente el usuario " + email +" a Provider");
        }

        return ResponseEntity.badRequest().build();
    }

}
