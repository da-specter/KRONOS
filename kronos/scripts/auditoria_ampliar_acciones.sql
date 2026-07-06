-- 🔍 Módulo de Auditoría del Administrador: amplía la columna AUDITORIA.ACCION
-- para soportar los nuevos tipos de acción del enum AccionAuditoria.
--
-- ⚠️ Ejecutar UNA VEZ como KRONOS_DEV antes de usar los nuevos valores del enum:
--    la columna nació como VARCHAR2(10) y Hibernate le creó un CHECK con solo
--    INSERT/UPDATE/DELETE; ddl-auto=update NO modifica columnas ni checks existentes,
--    así que sin este script los inserts nuevos fallan con ORA-12899 / ORA-02290.

-- 1) Ensanchar la columna (EVALUACION_BITACORA tiene 19 caracteres)
ALTER TABLE AUDITORIA MODIFY (ACCION VARCHAR2(30));

-- 2) Eliminar el CHECK viejo de la columna ACCION (nombre generado por Hibernate/Oracle)
BEGIN
    FOR c IN (SELECT uc.constraint_name
                FROM user_constraints uc
                JOIN user_cons_columns ucc
                  ON ucc.constraint_name = uc.constraint_name
               WHERE uc.table_name = 'AUDITORIA'
                 AND uc.constraint_type = 'C'
                 AND ucc.column_name = 'ACCION'
                 AND uc.search_condition_vc LIKE '%INSERT%') LOOP
        EXECUTE IMMEDIATE 'ALTER TABLE AUDITORIA DROP CONSTRAINT ' || c.constraint_name;
    END LOOP;
END;
/

-- 3) Recrear el CHECK con la lista completa de acciones del enum AccionAuditoria
ALTER TABLE AUDITORIA ADD CONSTRAINT CK_AUDITORIA_ACCION CHECK (
    ACCION IN ('INSERT','UPDATE','DELETE','IMPORTACION','EXPORTACION','ASIGNACION',
               'SUBIDA_BITACORA','EVALUACION_BITACORA','NOVEDAD','ALERTA','RESET_PASSWORD')
);

COMMIT;
