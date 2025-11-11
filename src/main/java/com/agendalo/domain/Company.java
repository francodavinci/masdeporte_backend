package com.agendalo.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.agendalo.domain.enums.AppointmentStatus;
import com.agendalo.domain.enums.CompanyStatus;
import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "company")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"appointments"})
@EqualsAndHashCode(exclude = {"appointments"})
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String urlSlug;

    @Column(nullable = false)
    private String category;

    @Column
    private String address;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column
    private String phone;

    @Column(nullable = false)
    private Integer minAdvanceDays;

    @Column(nullable = false)
    private Integer maxAdvanceDays;

    @Column(nullable = false)
    private Boolean hasTimeBetweenTurns;

    @Column
    private Integer minutesBetweenTurns;

    @JsonIgnore
    @OneToMany(mappedBy = "company", fetch = FetchType.EAGER)
    private List<Image> galleryImages = new ArrayList<>();

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CompanyStatus status;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime registrationDate;

    @Column(nullable = false)
    private Integer cancellationHours;

    @JsonManagedReference("company-businessHours")
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<BusinessHours> businessHours = new ArrayList<>();

    @JsonManagedReference("company-services")
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<BusinessService> services = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Appointment> appointments = new ArrayList<>();

    public void addService(BusinessService service) {
        services.add(service);
        service.setCompany(this);
    }

}