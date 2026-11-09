-- Ejecutar tras respaldar la base de datos.
-- Antes de retirar la tabla acompanante_institucion, migrar manualmente sus datos:
-- un acompanante solo puede conservar UNA institucion.
ALTER TABLE perfiles_acompanante
    ADD COLUMN IF NOT EXISTS institucion_ruc VARCHAR(11);

-- Ejecutar solo si la FK no existe.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_perfil_institucion'
          AND conrelid = 'perfiles_acompanante'::regclass
    ) THEN
        ALTER TABLE perfiles_acompanante
            ADD CONSTRAINT fk_perfil_institucion
            FOREIGN KEY (institucion_ruc)
            REFERENCES instituciones_aliadas(ruc);
    END IF;
END $$;

-- No se elimina automaticamente acompanante_institucion para evitar perdida de datos.
-- Tras migrar las asociaciones y verificar su integridad, retirarla manualmente.
