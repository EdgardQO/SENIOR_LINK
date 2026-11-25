BEGIN;
CREATE TABLE IF NOT EXISTS pasarela.transacciones (
 id uuid PRIMARY KEY,
 tarjeta_id bigint NOT NULL REFERENCES pasarela.tarjetas(id),
 reserva_id bigint NOT NULL REFERENCES public.reservas(id),
 clave_idempotencia uuid NOT NULL UNIQUE,
 monto numeric(12,2) NOT NULL CHECK (monto>0),
 estado varchar(20) NOT NULL CHECK (estado IN ('APROBADO','RECHAZADO','REEMBOLSADO')),
 codigo varchar(50),
 creado_en timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_transaccion_aprobada ON pasarela.transacciones(reserva_id) WHERE estado='APROBADO';
CREATE TABLE IF NOT EXISTS public.metodos_pago (
 id uuid PRIMARY KEY,
 usuario_dni varchar(8) NOT NULL REFERENCES public.usuarios(dni),
 token uuid NOT NULL UNIQUE,
 ultimos_cuatro varchar(4) NOT NULL,
 tipo varchar(10) NOT NULL CHECK(tipo IN ('DEBITO','CREDITO')),
 activo boolean NOT NULL DEFAULT true
);
CREATE TABLE IF NOT EXISTS pasarela.tokens (
 token uuid PRIMARY KEY,
 tarjeta_id bigint NOT NULL REFERENCES pasarela.tarjetas(id),
 activo boolean NOT NULL DEFAULT true
);
COMMIT;
