-- 💬 Chat informativo GESTOR_ETAPA ↔ REGISTRO: se implementó reutilizando la entidad NOVEDAD
-- (tipo INFORMATIVO) en lugar de una tabla nueva. Dos ajustes de esquema necesarios:
--   1) NOVEDAD.ID_ETAPA pasa a ser opcional: los mensajes informativos no están atados a
--      ninguna Etapa Productiva puntual.
--   2) El CHECK de NOVEDAD.TIPO_NOVEDAD debe incluir el valor nuevo 'INFORMATIVO'.
--
-- ⚠️ Ejecutar UNA VEZ como KRONOS_DEV. Re-ejecutable.

-- 1) ID_ETAPA nullable
DECLARE
    v_nullable user_tab_columns.nullable%TYPE;
BEGIN
    SELECT nullable INTO v_nullable
    FROM user_tab_columns
    WHERE table_name = 'NOVEDAD' AND column_name = 'ID_ETAPA';

    IF v_nullable = 'N' THEN
        EXECUTE IMMEDIATE 'ALTER TABLE NOVEDAD MODIFY (ID_ETAPA NULL)';
    END IF;
END;
/

-- 2) CHECK de TIPO_NOVEDAD actualizado
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
                            'RECLAMO','OTRO','CAMBIO_EMPRESA','SOLICITUD_ETAPA','INFORMATIVO'));

COMMIT;
