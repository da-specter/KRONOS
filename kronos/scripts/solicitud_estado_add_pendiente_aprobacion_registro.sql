-- 🎓 Nuevo estado PENDIENTE_REGISTRO (solicitudes de Contrato de Aprendizaje: van directo a
-- Registro, sin pasar por los checks del Gestor de Etapa). El CHECK de la columna ESTADO en
-- SOLICITUD_ETAPA_PRACTICA se mantiene a mano (ver solicitud_etapa_practica_fix_check_estado.sql)
-- porque ddl-auto=update no lo actualiza solo, así que sin este script la transición a ese
-- estado falla con ORA-02290. La columna es VARCHAR2(25): "PENDIENTE_APROBACION_REGISTRO" (29
-- caracteres) no cabía (ORA-12899), por eso el nombre corto.
--
-- ⚠️ Ejecutar UNA VEZ como KRONOS_DEV en cada entorno/PC.

BEGIN
    FOR c IN (SELECT uc.constraint_name
                FROM user_constraints uc
                JOIN user_cons_columns ucc
                  ON ucc.constraint_name = uc.constraint_name
               WHERE uc.table_name = 'SOLICITUD_ETAPA_PRACTICA'
                 AND uc.constraint_type = 'C'
                 AND ucc.column_name = 'ESTADO'
                 AND uc.search_condition_vc LIKE '%PENDIENTE_REVISION%') LOOP
        EXECUTE IMMEDIATE 'ALTER TABLE SOLICITUD_ETAPA_PRACTICA DROP CONSTRAINT ' || c.constraint_name;
    END LOOP;
END;
/

ALTER TABLE SOLICITUD_ETAPA_PRACTICA ADD CONSTRAINT CK_SOLICITUD_ESTADO
    CHECK (ESTADO IN ('PENDIENTE_REVISION','FORMATOS_HABILITADOS','FORMATOS_ENVIADOS',
                      'EN_VALIDACION_REGISTRO','LISTO_PARA_REGISTRO','APROBADO_EN_ETAPA','RECHAZADO',
                      'PENDIENTE_REGISTRO'));

COMMIT;
