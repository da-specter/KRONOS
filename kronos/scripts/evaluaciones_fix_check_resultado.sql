-- 📓 Módulo "Evaluación de Formatos" del Instructor Técnico: corrige el CHECK de la
-- columna RESULTADO en EVALUACION_BITACORA y EVALUACION_PLANEACION.
--
-- ⚠️ Ejecutar UNA VEZ como KRONOS_DEV antes de evaluar como REPROBADO o CORREGIR:
-- ambas tablas nacieron con un CHECK que solo permitía ('APROBADO','CON_OBSERVACIONES'),
-- desalineado con el enum Java ResultadoEvaluacion (APROBADO, REPROBADO, CORREGIR).
-- ddl-auto=update NO modifica checks existentes, así que sin este script los inserts
-- con REPROBADO/CORREGIR fallan con ORA-02290.

BEGIN
    FOR c IN (SELECT uc.constraint_name
                FROM user_constraints uc
                JOIN user_cons_columns ucc
                  ON ucc.constraint_name = uc.constraint_name
               WHERE uc.table_name = 'EVALUACION_BITACORA'
                 AND uc.constraint_type = 'C'
                 AND ucc.column_name = 'RESULTADO'
                 AND uc.search_condition_vc LIKE '%APROBADO%') LOOP
        EXECUTE IMMEDIATE 'ALTER TABLE EVALUACION_BITACORA DROP CONSTRAINT ' || c.constraint_name;
    END LOOP;

    FOR c IN (SELECT uc.constraint_name
                FROM user_constraints uc
                JOIN user_cons_columns ucc
                  ON ucc.constraint_name = uc.constraint_name
               WHERE uc.table_name = 'EVALUACION_PLANEACION'
                 AND uc.constraint_type = 'C'
                 AND ucc.column_name = 'RESULTADO'
                 AND uc.search_condition_vc LIKE '%APROBADO%') LOOP
        EXECUTE IMMEDIATE 'ALTER TABLE EVALUACION_PLANEACION DROP CONSTRAINT ' || c.constraint_name;
    END LOOP;
END;
/

ALTER TABLE EVALUACION_BITACORA ADD CONSTRAINT CK_EVAL_BITACORA_RESULTADO
    CHECK (RESULTADO IN ('APROBADO','REPROBADO','CORREGIR'));

ALTER TABLE EVALUACION_PLANEACION ADD CONSTRAINT CK_EVAL_PLANEACION_RESULTADO
    CHECK (RESULTADO IN ('APROBADO','REPROBADO','CORREGIR'));

COMMIT;
