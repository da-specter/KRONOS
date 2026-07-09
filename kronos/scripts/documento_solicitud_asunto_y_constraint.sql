-- 📤 Módulo "Formatos": subida múltiple de formatos requisito con asunto obligatorio.
-- La entidad DocumentoSolicitud ahora tiene un campo ASUNTO (NOT NULL) y ya no exige una
-- restricción única (ID_SOLICITUD, ID_PLANTILLA) a nivel de BD: puede haber muchas filas
-- con ID_PLANTILLA nulo para la misma solicitud (formatos requisito libres).
--
-- ⚠️ Ejecutar UNA VEZ como KRONOS_DEV en cada entorno/PC antes de usar la subida múltiple
-- de formatos: ddl-auto=update no agrega columnas nuevas con datos por defecto para filas
-- existentes ni elimina restricciones existentes, así que sin este script fallan con
-- ORA-00904 (columna ASUNTO no existe) u ORA-00001 (restricción única violada).

-- 1) Agregar la columna ASUNTO si todavía no existe (script re-ejecutable)
DECLARE
    v_existe NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_existe FROM user_tab_columns
     WHERE table_name = 'DOCUMENTO_SOLICITUD' AND column_name = 'ASUNTO';
    IF v_existe = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE DOCUMENTO_SOLICITUD ADD (ASUNTO VARCHAR2(200))';
        EXECUTE IMMEDIATE 'ALTER TABLE DOCUMENTO_SOLICITUD MODIFY (ASUNTO NOT NULL)';
    END IF;
END;
/

-- 2) Eliminar la restricción única vieja sobre (ID_SOLICITUD, ID_PLANTILLA), si existe
--    (nombre generado por Hibernate/Oracle, por eso se busca por columnas y no por nombre).
--    Oracle sí compara como duplicadas dos filas con la misma clave compuesta aunque una
--    columna sea NULL, así que con esta restricción activa no se puede subir más de un
--    formato requisito libre por solicitud.
BEGIN
    FOR c IN (SELECT uc.constraint_name
                FROM user_constraints uc
               WHERE uc.table_name = 'DOCUMENTO_SOLICITUD'
                 AND uc.constraint_type = 'U'
                 AND EXISTS (SELECT 1 FROM user_cons_columns ucc
                              WHERE ucc.constraint_name = uc.constraint_name
                                AND ucc.column_name = 'ID_PLANTILLA')
                 AND EXISTS (SELECT 1 FROM user_cons_columns ucc
                              WHERE ucc.constraint_name = uc.constraint_name
                                AND ucc.column_name = 'ID_SOLICITUD')) LOOP
        EXECUTE IMMEDIATE 'ALTER TABLE DOCUMENTO_SOLICITUD DROP CONSTRAINT ' || c.constraint_name;
    END LOOP;
END;
/

COMMIT;
