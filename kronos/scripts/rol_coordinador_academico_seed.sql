-- 🎓 Nuevo rol COORDINADOR_ACADEMICO (Coordinación Académica): reutiliza la fila que ya existía
-- en ROL desde una migración anterior (no estaba referenciada en el código). Este script es
-- idempotente por si esa fila no existe en algún entorno.
--
-- ⚠️ Ejecutar UNA VEZ como KRONOS_DEV en cada entorno/PC.
-- Re-ejecutable (MERGE): no duplica la fila si ya existe.

MERGE INTO ROL r
USING (SELECT 'COORDINADOR_ACADEMICO' AS nombre_rol FROM dual) src
ON (r.NOMBRE_ROL = src.nombre_rol)
WHEN NOT MATCHED THEN INSERT (NOMBRE_ROL) VALUES (src.nombre_rol);

COMMIT;
