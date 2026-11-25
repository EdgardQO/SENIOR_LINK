-- Auditoría de solo lectura. Ejecutar después de verificar que el esquema corresponde a la versión desplegada.
BEGIN READ ONLY;
SELECT current_database() AS base, current_schema() AS esquema;
SELECT table_schema,table_name FROM information_schema.tables
 WHERE table_name IN ('usuarios','parentescos','reservas','reservas_jornadas','pagos','vinculaciones_familiar_adulto','transacciones')
 ORDER BY 1,2;
SELECT count(*) AS parentescos FROM parentescos;
SELECT count(*) AS vinculaciones_duplicadas FROM
 (SELECT familiar_dni,adulto_mayor_dni FROM vinculaciones_familiar_adulto GROUP BY 1,2 HAVING count(*)>1) x;
SELECT count(*) AS permisos_inconsistentes FROM vinculaciones_familiar_adulto
 WHERE estado <> 'ACEPTADO' AND (permite_reservar OR permite_pagar OR permite_gestionar);
SELECT count(*) AS reservas_con_intervalo_invalido FROM reservas WHERE fecha_hora_fin<=fecha_hora_inicio;
SELECT count(*) AS pagos_sin_reserva FROM pagos p LEFT JOIN reservas r ON r.id=p.reserva_id WHERE r.id IS NULL;
SELECT count(*) AS reservas_con_pagos_duplicados FROM (SELECT reserva_id FROM pagos GROUP BY 1 HAVING count(*)>1) x;
SELECT count(*) AS reservas_pendientes_sin_plazo FROM reservas WHERE estado_actual='PENDIENTE_PAGO' AND vence_pago_en IS NULL;
ROLLBACK;
