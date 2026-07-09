-- 📅 TipoVisita se colapsó a un único valor: SEGUIMIENTO (ya no se distingue entre
-- inicial/parcial/final). Dos pasos necesarios:
--   1) Migrar las filas existentes con CONCERTACION o EVALUACION_FINAL a SEGUIMIENTO.
--   2) Recrear el CHECK de VISITA_SEGUIMIENTO.TIPO_VISITA para que solo acepte 'SEGUIMIENTO'.
--
-- ⚠️ Ejecutar UNA VEZ como KRONOS_DEV. Re-ejecutable.

UPDATE VISITA_SEGUIMIENTO SET TIPO_VISITA = 'SEGUIMIENTO' WHERE TIPO_VISITA != 'SEGUIMIENTO';

BEGIN
    FOR c IN (SELECT uc.constraint_name
                FROM user_constraints uc
                JOIN user_cons_columns ucc
                  ON ucc.constraint_name = uc.constraint_name
               WHERE uc.table_name = 'VISITA_SEGUIMIENTO'
                 AND uc.constraint_type = 'C'
                 AND ucc.column_name = 'TIPO_VISITA'
                 AND uc.search_condition_vc LIKE '%CONCERTACION%') LOOP
        EXECUTE IMMEDIATE 'ALTER TABLE VISITA_SEGUIMIENTO DROP CONSTRAINT ' || c.constraint_name;
    END LOOP;
END;
/

ALTER TABLE VISITA_SEGUIMIENTO ADD CONSTRAINT CK_VISITA_TIPO
    CHECK (TIPO_VISITA IN ('SEGUIMIENTO'));

COMMIT;
