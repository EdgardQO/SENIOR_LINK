-- Ejecutar en PostgreSQL después de realizar una copia de seguridad.
-- Los perfiles existentes conservarán sus datos. Completar ciclo/carrera
-- antes de imponer NOT NULL en una migración posterior, si se requiere.
BEGIN;

ALTER TABLE perfiles_acompanante
    ADD COLUMN IF NOT EXISTS ciclo INTEGER,
    ADD COLUMN IF NOT EXISTS carrera VARCHAR(150);

-- La restricción admite NULL para perfiles creados antes de esta migración.
ALTER TABLE perfiles_acompanante
    DROP CONSTRAINT IF EXISTS chk_acompanante_ciclo;

ALTER TABLE perfiles_acompanante
    ADD CONSTRAINT chk_acompanante_ciclo
    CHECK (ciclo IS NULL OR ciclo BETWEEN 1 AND 20);

-- Se conserva nivel_academico temporalmente para evitar pérdida de datos.
-- Cuando se hayan migrado o archivado sus valores, puede eliminarse manualmente.
COMMIT;
