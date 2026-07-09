-- 🏢 Nuevo rol REGISTRO: siembra la fila en ROL para poder crear/asignar usuarios con este
-- rol desde /admin/usuarios (AdminUsuariosService exige que el rol ya exista en la tabla).
--
-- ⚠️ Ejecutar UNA VEZ como KRONOS_DEV en cada entorno/PC.
-- Re-ejecutable (MERGE): no duplica la fila si ya existe.

MERGE INTO ROL r
USING (SELECT 'REGISTRO' AS nombre_rol FROM dual) src
ON (r.NOMBRE_ROL = src.nombre_rol)
WHEN NOT MATCHED THEN INSERT (NOMBRE_ROL) VALUES (src.nombre_rol);

COMMIT;
