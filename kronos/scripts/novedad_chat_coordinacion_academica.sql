-- 💬 Chat informativo GESTOR_ETAPA ↔ COORDINADOR_ACADEMICO: mismo patrón que el chat con
-- REGISTRO (entidad NOVEDAD, tipo nuevo COORD_ACADEMICO en vez de una tabla aparte).
-- ID_ETAPA ya quedó nullable desde el script del chat con Registro, así que aquí solo hace
-- falta ampliar el CHECK de NOVEDAD.TIPO_NOVEDAD con el valor nuevo.
--
-- ⚠️ Ejecutar UNA VEZ como KRONOS_DEV. Re-ejecutable.

BEGIN
    FOR c IN (SELECT uc.constraint_name
                FROM user_constraints uc
                JOIN user_cons_columns ucc
                  ON ucc.constraint_name = uc.constraint_name
               WHERE uc.table_name = 'NOVEDAD'
                 AND uc.constraint_type = 'C'
                 AND ucc.column_name = 'TIPO_NOVEDAD'
                 AND uc.search_condition_vc LIKE '%SUSPENSION%') LOOP
        EXECUTE IMMEDIATE 'ALTER TABLE NOVEDAD DROP CONSTRAINT ' || c.constraint_name;
    END LOOP;
END;
/

ALTER TABLE NOVEDAD ADD CONSTRAINT CK_NOVEDAD_TIPO
    CHECK (TIPO_NOVEDAD IN ('SUSPENSION','REINCORPORACION','APLAZAMIENTO','DESERCION','CAMBIO_ALT',
                            'RECLAMO','OTRO','CAMBIO_EMPRESA','SOLICITUD_ETAPA','INFORMATIVO','COORD_ACADEMICO'));

COMMIT;
