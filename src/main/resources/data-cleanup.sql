-- Desactivar restricciones de clave foránea temporalmente
SET FOREIGN_KEY_CHECKS = 0;

-- Truncar todas las tablas excepto 'user'-TRUNCATE TABLE appointment;
TRUNCATE TABLE business_hours;
TRUNCATE TABLE business_service;
TRUNCATE TABLE company;
TRUNCATE TABLE images;
TRUNCATE TABLE appointment;
TRUNCATE TABLE appointment_slot;
TRUNCATE TABLE comments;
TRUNCATE TABLE company_images;
TRUNCATE TABLE mercado_pago_account;
TRUNCATE TABLE payment_preference;
TRUNCATE TABLE provider;
TRUNCATE TABLE service;
TRUNCATE TABLE service_type;
TRUNCATE TABLE user;
-- Añade aquí cualquier otra tabla que necesites vaciar

-- Reactivar restricciones de clave foránea
--SET FOREIGN_KEY_CHECKS = 1;