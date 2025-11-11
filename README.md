# ----------- Agendalo.com ----------
# - Backend
# TODO LIST:
-Manejar los roles con un Enum.

-Pensar como seria la logica de prestadores de servicios, deberian ser admin? O el admin lo tendriamos nosotros? En caso de no ser admin, habria que definir el nuevo rol (Seria similar a usuario).



- **Datos Prestadores de Servicios**
-- ID
-- Nombre 
-- Apellido
-- Email
-- Telefono (ver si lo dividimos por otra tabla)
-- Documento
-- created_at
-- modified_at

- **Datos Servicio por prestador**
-- ID
-- Nombre 
-- Eslogan
-- Logo (Ver por ahora capaz que nova)
-- Tipo de rubro
-- Dirección (Calle,Altura, Ciudad, Provincia, *Pais*, Piso, Planta (booleano), Entre Calles, Barrio, Observacion)
-- Moneda (Lo manejamos como enum pero por ahora siempre ARS)
-- Rango Horarios Turnos: (ID-StartTime-EndTime-ServiceID) (Solo se podrian crear dos por empresa)
  (Esto se puede normalizar y dejar las tablas Servicio y Agenda)
-- created_at
-- modified_at
- **Tipo de Servicio**
-- ID
-- Servicio_id
-- Nombre
-- Descripcion
-- Precio
-- created_at
-- modified_at

- **Imagenes** 
-- id
-- bucket 
-- tipo [Logo, ServicioEj]
-- pathImage
-- servicio_id

- **Turno**
-- ID (11, 00000000001)
-- prestador_id
-- servicio_id
-- tipo_servicio_id
-- estado ([VACIO, RESERVADO, PAGO, CANCELADO ,FINALIZADO])
-- Date (day, month, year)
-- startTime
-- endTime
-- user_id
-- created_at
-- modified_at
-- Nota (ej: necesitaria x)

// Resevaste un trno en ... a las ... el dia ... 
// Podes ver el estado de tu turno en ...

- **Transaccion**
-- id
--usuario
--endpoint
--fecha
--json request
--json response

// Ver como se implementa una pasarela de pagos (POC)