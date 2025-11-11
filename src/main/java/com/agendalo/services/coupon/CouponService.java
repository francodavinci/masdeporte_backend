package com.agendalo.services.coupon;

import com.agendalo.domain.*;
import com.agendalo.dto.coupon.*;
import com.agendalo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;

    // Códigos predefinidos disponibles
    private static final String[] PREDEFINED_CODES = {
            "DESCUENTO10", "DESCUENTO15", "DESCUENTO20", "DESCUENTO25", "DESCUENTO30",
            "VERANO20", "INVIERNO15", "PRIMAVERA25", "OTOÑO10",
            "BIENVENIDA15", "FIDELIDAD20", "ESPECIAL30", "PROMO25",
            "CLUB10", "CLUB15", "CLUB20", "CLUB25", "CLUB30",
            "NUEVO15", "NUEVO20", "NUEVO25", "NUEVO30"
    };

    @Transactional
    public CouponDto createCoupon(Long companyId, CreateCouponRequest request) {
        log.info("Creando cupón para compañía: {}", companyId);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Compañía no encontrada"));

        // Validar datos
        validateCouponRequest(request);

        // Generar código único
        String code = generateUniqueCode(companyId);

        // Crear cupón
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setName(request.getName());
        coupon.setDescription(request.getDescription());
        coupon.setDiscountPercentage(request.getDiscountPercentage());
        coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
        coupon.setValidFrom(request.getValidFrom());
        coupon.setValidUntil(request.getValidUntil());
        coupon.setMaxUses(request.getMaxUses());
        coupon.setCompany(company);

        Coupon savedCoupon = couponRepository.save(coupon);
        log.info("Cupón creado exitosamente: {}", savedCoupon.getCode());

        return mapToDto(savedCoupon);
    }

    @Transactional(readOnly = true)
    public List<CouponDto> getCompanyCoupons(Long companyId) {
        log.info("Obteniendo cupones para compañía: {}", companyId);

        // Cambiar para traer TODOS los cupones, no solo los activos
        List<Coupon> coupons = couponRepository.findByCompanyId(companyId);
        return coupons.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ApplyCouponResponse applyCoupon(String couponCode, Long companyId, Double originalAmount, String userEmail) {
        log.info("Aplicando cupón: {} para compañía: {} y usuario: {}", couponCode, companyId, userEmail);

        User userFound = userRepository.findByEmail(userEmail).orElseThrow(() -> new RuntimeException("No se encontro el usuario: " + userEmail));

        // Buscar cupón válido
        Optional<Coupon> couponOpt = couponRepository.findValidCouponForCompany(
                couponCode, companyId, LocalDateTime.now());

        if (couponOpt.isEmpty()) {
            return new ApplyCouponResponse(false, "Cupón no válido o expirado", 0.0, originalAmount, null);
        }

        Coupon coupon = couponOpt.get();

        // Calcular descuento
        Double discountAmount = calculateDiscount(originalAmount, coupon.getDiscountPercentage(), coupon.getMaxDiscountAmount());
        Double finalAmount = originalAmount - discountAmount;

        if (coupon.getMaxUses() > coupon.getCurrentUses()){
            return new ApplyCouponResponse(true, "Cupón aplicado exitosamente",
                    discountAmount, finalAmount, mapToDto(coupon));

        }
        return new ApplyCouponResponse(false, "Cupón no válido o expirado", 0.0, originalAmount, null);
    }

    @Transactional
    public CouponUsageDto useCoupon(String couponCode) {
        log.info("Usando cupón: {}", couponCode);

        Coupon coupon = couponRepository.findValidCoupon(
                        couponCode, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Cupón no válido"));

        CouponUsage usage = new CouponUsage();
        usage.setCoupon(coupon);
        CouponUsage savedUsage = couponUsageRepository.save(usage);

        // Incrementar contador de usos
        coupon.incrementUsage();
        couponRepository.save(coupon);

        log.info("Cupón usado exitosamente: {}", coupon.getCode());

        return mapToUsageDto(savedUsage);
    }

    @Transactional
    public CouponUsageDto applyAndUseCoupon(String couponCode, Long companyId, Double originalAmount, String userEmail) {
        log.info("Aplicando y usando cupón: {} para compañía: {} y usuario: {}", couponCode, companyId, userEmail);

        // Validar usuario existente
        userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("No se encontro el usuario: " + userEmail));

        // Buscar cupón válido por compañía y fecha de vigencia
        Coupon coupon = couponRepository.findValidCouponForCompany(couponCode, companyId, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Cupón no válido o expirado"));

        // Validar disponibilidad de usos
        if (coupon.getMaxUses() <= coupon.getCurrentUses()) {
            throw new RuntimeException("El cupón alcanzó el máximo de usos");
        }

        // Calcular descuento y totales
        Double discountAmount = calculateDiscount(originalAmount, coupon.getDiscountPercentage(), coupon.getMaxDiscountAmount());
        Double finalAmount = originalAmount - discountAmount;

        // Registrar uso
        CouponUsage usage = new CouponUsage();
        usage.setCoupon(coupon);
        CouponUsage savedUsage = couponUsageRepository.save(usage);

        // Incrementar contador de usos del cupón
        coupon.incrementUsage();
        couponRepository.save(coupon);

        // Mapear respuesta incluyendo importes
        CouponUsageDto dto = mapToUsageDto(savedUsage);
        dto.setOriginalAmount(originalAmount);
        dto.setDiscountAmount(discountAmount);
        dto.setFinalAmount(finalAmount);
        return dto;
    }

    @Transactional
    public CouponDto updateCoupon(Long couponId, CreateCouponRequest request) {
        log.info("Actualizando cupón: {}", couponId);

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new RuntimeException("Cupón no encontrado"));

        validateCouponRequest(request);

        coupon.setName(request.getName());
        coupon.setDescription(request.getDescription());
        coupon.setDiscountPercentage(request.getDiscountPercentage());
        coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
        coupon.setValidFrom(request.getValidFrom());
        coupon.setValidUntil(request.getValidUntil());
        coupon.setMaxUses(request.getMaxUses());

        Coupon savedCoupon = couponRepository.save(coupon);
        log.info("Cupón actualizado exitosamente: {}", savedCoupon.getCode());

        return mapToDto(savedCoupon);
    }

    @Transactional
    public void deactivateCoupon(Long couponId) {
        log.info("Desactivando cupón: {}", couponId);

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new RuntimeException("Cupón no encontrado"));

        coupon.setIsActive(false);
        couponRepository.save(coupon);

        log.info("Cupón desactivado exitosamente: {}", coupon.getCode());
    }
    @Transactional
    public void activateCoupon(Long couponId) {
        log.info("Activando cupón: {}", couponId);

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new RuntimeException("Cupón no encontrado"));

        coupon.setIsActive(true);
        couponRepository.save(coupon);

        log.info("Cupón activado exitosamente: {}", coupon.getCode());
    }

    @Transactional
    public void deleteCoupon(Long couponId) {
        log.info("Eliminando cupón: {}", couponId);

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new RuntimeException("Cupón no encontrado"));

        // Verificar si el cupón ha sido usado
        List<CouponUsage> usages = couponUsageRepository.findByCouponId(coupon.getId());
        if (!usages.isEmpty()) {
            throw new RuntimeException("No se puede eliminar un cupón que ya ha sido usado");
        }

        couponRepository.delete(coupon);
        log.info("Cupón eliminado exitosamente: {}", coupon.getCode());
    }

    // Métodos privados
    private void validateCouponRequest(CreateCouponRequest request) {
        if (request.getDiscountPercentage() <= 0 || request.getDiscountPercentage() > 100) {
            throw new RuntimeException("El porcentaje de descuento debe estar entre 1 y 100");
        }
        if (request.getMaxDiscountAmount() <= 0) {
            throw new RuntimeException("El monto máximo de descuento debe ser mayor a 0");
        }
        if (request.getValidFrom().isAfter(request.getValidUntil())) {
            throw new RuntimeException("La fecha de inicio debe ser anterior a la fecha de fin");
        }
        if (request.getMaxUses() < -1) {
            throw new RuntimeException("El máximo de usos debe ser -1 (ilimitado) o mayor a 0");
        }
    }

    private String generateUniqueCode(Long companyId) {
        String baseCode = "DESCUENTO";
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        int codeLength = 6; // Longitud de la parte aleatoria
        int maxAttempts = 10; // Máximo número de intentos

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            StringBuilder randomPart = new StringBuilder();
            Random random = new Random();

            // Generar parte aleatoria
            for (int i = 0; i < codeLength; i++) {
                randomPart.append(characters.charAt(random.nextInt(characters.length())));
            }

            String uniqueCode = baseCode + randomPart.toString();

            // Verificar si el código ya existe
            if (!couponRepository.findByCodeAndCompanyId(uniqueCode, companyId).isPresent()) {
                return uniqueCode;
            }
        }

        throw new RuntimeException("No se pudo generar un código único después de varios intentos");
    }

    private Double calculateDiscount(Double originalAmount, Double discountPercentage, Double maxDiscountAmount) {
        Double discount = originalAmount * (discountPercentage / 100.0);

        if (discount > maxDiscountAmount) {
            discount = maxDiscountAmount;
        }

        return discount;
    }

    private CouponDto mapToDto(Coupon coupon) {
        CouponDto dto = new CouponDto();
        dto.setId(coupon.getId());
        dto.setCode(coupon.getCode());
        dto.setName(coupon.getName());
        dto.setDescription(coupon.getDescription());
        dto.setDiscountPercentage(coupon.getDiscountPercentage());
        dto.setMaxDiscountAmount(coupon.getMaxDiscountAmount());
        dto.setValidFrom(coupon.getValidFrom());
        dto.setValidUntil(coupon.getValidUntil());
        dto.setMaxUses(coupon.getMaxUses());
        dto.setCurrentUses(coupon.getCurrentUses());
        dto.setIsActive(coupon.getIsActive());
        dto.setCreatedAt(coupon.getCreatedAt());
        dto.setUpdatedAt(coupon.getUpdatedAt());
        return dto;
    }

    private CouponUsageDto mapToUsageDto(CouponUsage usage) {
        CouponUsageDto dto = new CouponUsageDto();
        dto.setId(usage.getId());
        dto.setCoupon(mapToDto(usage.getCoupon()));
        dto.setUsedAt(usage.getUsedAt());
        return dto;
    }
}