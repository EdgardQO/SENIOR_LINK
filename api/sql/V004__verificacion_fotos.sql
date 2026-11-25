ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS foto_perfil_ruta varchar(255);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS estado_verificacion varchar(20);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS fecha_verificacion timestamptz;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS verificado_por varchar(8);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS motivo_rechazo varchar(500);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS foto_actualizada_en timestamptz;
