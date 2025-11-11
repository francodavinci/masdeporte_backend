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
@Table(name = "payment_preference")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"service", "user", "company"})
@EqualsAndHashCode(exclude = {"service", "user", "company"})
public class PaymentPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "preference_id", unique = true)
    private String preferenceId; // ID de Mercado Pago

    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    private BusinessService service;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "notes")
    private String notes;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "status")
    private String status = "pending"; // pending, approved, rejected, cancelled

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "external_reference")
    private String externalReference;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 