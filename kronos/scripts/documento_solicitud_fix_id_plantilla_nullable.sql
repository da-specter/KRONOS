-- La entidad DocumentoSolicitud permite ID_PLANTILLA nulo para los "formatos requisito
-- libres" (subidas sin plantilla puntual, identificadas por ASUNTO), pero la columna en
-- Oracle quedó con NOT NULL de una versión anterior del esquema. Hibernate con
-- ddl-auto=update nunca quita restricciones existentes, así que hay que alinearla a mano.
-- Sin este fix, el aprendiz recibe ORA-01400 al subir un formato sin plantilla asociada.
ALTER TABLE DOCUMENTO_SOLICITUD MODIFY (ID_PLANTILLA NULL);
