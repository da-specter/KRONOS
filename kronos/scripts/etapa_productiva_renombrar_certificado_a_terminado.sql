-- 🎓 Cambio de lógica: la certificación final del aprendiz ya no es un proceso de KRONOS (ocurre
-- en Sofía Plus). El enum Java EstadoEtapa renombró su valor CERTIFICADO a TERMINADO para reflejar
-- solo que, dentro de KRONOS, el ciclo de la Etapa Productiva quedó cerrado. Este script:
--   1) Migra las filas existentes en ESTADO_ETAPA = 'CERTIFICADO' a 'TERMINADO'.
--   2) Recrea el CHECK constraint de ESTADO_ETAPA con 'TERMINADO' en vez de 'CERTIFICADO'.
--
-- ⚠️ Ejecutar UNA VEZ como KRONOS_DEV. Re-ejecutable.

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

UPDATE ETAPA_PRODUCTIVA SET ESTADO_ETAPA = 'TERMINADO' WHERE ESTADO_ETAPA = 'CERTIFICADO';

ALTER TABLE ETAPA_PRODUCTIVA ADD CONSTRAINT CK_ETAPA_ESTADO
    CHECK (ESTADO_ETAPA IN ('EN_PROGRESO','APROBADO','REPROBADO','POR_CERTIFICAR','TERMINADO','EN_SUSPENSION'));

COMMIT;
