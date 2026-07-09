-- Backfill de ETAPA_PRODUCTIVA.FECHA_CREACION para las etapas creadas antes de que
-- este campo existiera (columna agregada por Hibernate ddl-auto=update, nace NULL).
-- No hay forma de recuperar el timestamp real en que se registraron, así que se usa
-- FECHA_INICIO (a medianoche) como aproximación razonable, solo para no dejar el
-- campo vacío en pantallas/reportes ya existentes. Idempotente: solo toca NULLs.
UPDATE etapa_productiva
SET fecha_creacion = CAST(fecha_inicio AS TIMESTAMP)
WHERE fecha_creacion IS NULL;

COMMIT;
