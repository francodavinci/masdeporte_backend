package com.agendalo.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"company"})
@EqualsAndHashCode(exclude = {"company"})
public class MercadoPagoAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación con la empresa (o usuario)
    @OneToOne
    @JoinColumn(name = "company_id", unique = true)
    private Company company;

    private String accessToken;
    private String refreshToken;
    private String publicKey;
    private String userId; // ID de usuario de Mercado Pago
    private String status; // Ej: "active", "revoked", etc.

    private LocalDateTime expiresAt; // Fecha de expiración del token

    private LocalDateTime lastSync;

}