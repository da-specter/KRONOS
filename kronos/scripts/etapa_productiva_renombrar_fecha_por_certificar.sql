-- 🏷️ Renombra ETAPA_PRODUCTIVA.FECHA_POR_CERTIFICAR a FECHA_TERMINACION: el nombre original
-- venía de cuando existía el estado intermedio POR_CERTIFICAR (ver
-- etapa_productiva_renombrar_certificado_a_terminado.sql). Hoy la etapa pasa directo a
-- TERMINADO y este campo solo guarda el momento en que ocurrió ese cierre — no certifica a
-- nadie (la certificación oficial ocurre en Sofía Plus, fuera de KRONOS).
--
-- ALTER TABLE ... RENAME COLUMN preserva los datos existentes (a diferencia de dejar que
-- Hibernate con ddl-auto=update cree una columna nueva vacía).
--
-- ⚠️ Ejecutar UNA VEZ como KRONOS_DEV, ANTES de levantar la app con el código ya actualizado
-- (la entidad Java ya no conoce la columna vieja). Re-ejecutable (solo renombra si aplica).

DECLARE
    v_columna_vieja NUMBER;
    v_columna_nueva NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_columna_vieja FROM user_tab_columns
     WHERE table_name = 'ETAPA_PRODUCTIVA' AND column_name = 'FECHA_POR_CERTIFICAR';
    SELECT COUNT(*) INTO v_columna_nueva FROM user_tab_columns
     WHERE table_name = 'ETAPA_PRODUCTIVA' AND column_name = 'FECHA_TERMINACION';

    IF v_columna_vieja = 1 AND v_columna_nueva = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE ETAPA_PRODUCTIVA RENAME COLUMN FECHA_POR_CERTIFICAR TO FECHA_TERMINACION';
    END IF;
END;
/
