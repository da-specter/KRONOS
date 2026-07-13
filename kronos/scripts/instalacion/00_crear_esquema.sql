-- 🏗️ Crea el usuario/esquema de Oracle que usa KRONOS en desarrollo (KRONOS_DEV).
-- Ejecutar UNA SOLA VEZ en un PC nuevo, conectado como SYSDBA (no como KRONOS_DEV,
-- todavía no existe) a la PDB que vas a usar:
--
--   sqlplus sys/<clave_sys>@//localhost:1521/XEPDB1 as sysdba
--   @00_crear_esquema.sql
--
-- Si ya tienes el usuario creado (por ejemplo, restauraste un export), sáltate este script.
--
-- Usuario y clave iguales a los que trae por defecto kronos/src/main/resources/application.properties
-- (DB_USERNAME / DB_PASSWORD). Si vas a usar otro usuario/clave, cámbialos aquí Y en las
-- variables de entorno DB_URL / DB_USERNAME / DB_PASSWORD antes de arrancar la app.

CREATE USER KRONOS_DEV IDENTIFIED BY KRONOS2026;

GRANT CONNECT, RESOURCE TO KRONOS_DEV;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW, CREATE PROCEDURE TO KRONOS_DEV;
GRANT UNLIMITED TABLESPACE TO KRONOS_DEV;
