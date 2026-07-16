-- 📎 Evidencia de Sofía Plus: Registro adjunta una imagen al registrar la Etapa Productiva,
-- guardada como Novedad tipo nuevo EVIDENCIA_SOFIA_PLUS dirigida al Gestor de Etapa.
-- Mismo patrón que novedad_chat_coordinacion_academica.sql: solo hace falta ampliar el CHECK
-- de NOVEDAD.TIPO_NOVEDAD con el valor nuevo (una base nueva ya sale con Hibernate alineada).
--
-- ⚠️ Ejecutar UNA VEZ como KRONOS_END. Re-ejecutable.

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
                            'RECLAMO','OTRO','CAMBIO_EMPRESA','SOLICITUD_ETAPA','INFORMATIVO','COORD_ACADEMICO',
                            'EVIDENCIA_SOFIA_PLUS'));

COMMIT;
