-- 💬 El chat GESTOR_ETAPA ↔ REGISTRO se rehízo reutilizando la entidad NOVEDAD (tipo
-- INFORMATIVO, ver novedad_chat_registro_gestor.sql) en vez de la tabla MENSAJE_REGISTRO que
-- se había creado para un primer diseño (chat aparte, por Etapa Productiva). Esa tabla ya no
-- tiene entidad Java asociada: se elimina para no dejar basura en el esquema.
--
-- ⚠️ Ejecutar UNA VEZ como KRONOS_DEV. Re-ejecutable: no falla si la tabla ya no existe.

BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE MENSAJE_REGISTRO CASCADE CONSTRAINTS PURGE';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN -- ORA-00942: table or view does not exist
            RAISE;
        END IF;
END;
/

COMMIT;
