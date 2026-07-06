-- 🎓 Módulo de Certificación (Instructor de Seguimiento + Gestor de Etapa): corrige el CHECK
-- de la columna ESTADO_ETAPA en ETAPA_PRODUCTIVA.
--
-- ⚠️ Ejecutar UNA VEZ como KRONOS_DEV antes de que una etapa pase a POR_CERTIFICAR/CERTIFICADO:
-- la tabla nació con un CHECK que solo permitía ('EN_PROGRESO','APROBADO','REPROBADO',
-- 'EN_SUSPENSION'), desalineado del enum Java EstadoEtapa (que ya incluía POR_CERTIFICAR y
-- CERTIFICADO). ddl-auto=update NO modifica checks existentes, así que sin este script la
-- transición automática a POR_CERTIFICAR (y la certificación final del Gestor) fallan con
-- ORA-02290.

BEGIN
    FOR c IN (SELECT uc.constraint_name
                FROM user_constraints uc
                JOIN user_cons_columns ucc
                  ON ucc.constraint_name = uc.constraint_name
               WHERE uc.table_name = 'ETAPA_PRODUCTIVA'
                 AND uc.constraint_type = 'C'
                 AND ucc.column_name = 'ESTADO_ETAPA'
                 AND uc.search_condition_vc LIKE '%EN_PROGRESO%') LOOP
        EXECUTE IMMEDIATE 'ALTER TABLE ETAPA_PRODUCTIVA DROP CONSTRAINT ' || c.constraint_name;
    END LOOP;
END;
/

ALTER TABLE ETAPA_PRODUCTIVA ADD CONSTRAINT CK_ETAPA_ESTADO
    CHECK (ESTADO_ETAPA IN ('EN_PROGRESO','APROBADO','REPROBADO','POR_CERTIFICAR','CERTIFICADO','EN_SUSPENSION'));

COMMIT;
