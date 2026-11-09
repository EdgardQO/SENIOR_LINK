-- Senior Link: esquema para instalaciones nuevas.
-- IMPORTANTE: NO ejecutar sobre una tabla instituciones_aliadas existente con PK UUID.
-- En bases existentes, respaldar datos y migrar codigo_identificador -> ruc
-- con una correspondencia real de RUC por institución antes de cambiar las FK.
-- Esta migración no se ejecuta automáticamente.

CREATE TABLE IF NOT EXISTS instituciones_aliadas (
    ruc VARCHAR(11) PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    detalles_convenio TEXT,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_institucion_ruc_formato CHECK (ruc ~ '^[0-9]{11}$')
);

CREATE TABLE IF NOT EXISTS acompanante_institucion (
    acompanante_dni VARCHAR(8) NOT NULL,
    institucion_ruc VARCHAR(11) NOT NULL,
    CONSTRAINT pk_acompanante_institucion PRIMARY KEY (acompanante_dni, institucion_ruc),
    CONSTRAINT fk_ai_acompanante FOREIGN KEY (acompanante_dni)
        REFERENCES perfiles_acompanante(usuario_dni) ON DELETE CASCADE,
    CONSTRAINT fk_ai_institucion FOREIGN KEY (institucion_ruc)
        REFERENCES instituciones_aliadas(ruc) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ai_institucion_ruc
    ON acompanante_institucion(institucion_ruc);
