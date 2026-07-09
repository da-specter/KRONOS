-- 🏢 Nuevo rol REGISTRO: corrige el CHECK de la columna ESTADO en SOLICITUD_ETAPA_PRACTICA.
--
-- ⚠️ Ejecutar UNA VEZ como KRONOS_DEV antes de calificar/validar cualquier solicitud con el
-- nuevo flujo Gestor -> Registro: la tabla nació con un CHECK que solo permitía
-- ('PENDIENTE_REVISION','FORMATOS_HABILITADOS','FORMATOS_ENVIADOS','APROBADO_EN_ETAPA','RECHAZADO'),
-- desalineado del enum Java EstadoSolicitud (que ahora incluye EN_VALIDACION_REGISTRO y
-- LISTO_PARA_REGISTRO). ddl-auto=update NO modifica checks existentes, así que sin este script
-- la transición a esos dos estados nuevos falla con ORA-02290.

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
                      'EN_VALIDACION_REGISTRO','LISTO_PARA_REGISTRO','APROBADO_EN_ETAPA','RECHAZADO'));

COMMIT;
