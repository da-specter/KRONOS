-- 👤 Siembra los 7 roles base de KRONOS en la tabla ROL. Necesario porque AdminUsuariosService
-- exige que el rol ya exista en la tabla antes de poder crear/asignar un usuario con ese rol
-- (ROL no tiene ningún seed automático: a diferencia de ConfiguracionGlobalService, nada la
-- puebla sola al arrancar la app).
--
-- Hasta ahora solo REGISTRO y COORDINADOR_ACADEMICO tenían script propio
-- (../rol_registro_seed.sql, ../rol_coordinador_academico_seed.sql); los otros 5 roles habían
-- quedado sembrados a mano en la base de datos de desarrollo, sin script. Este archivo los cubre
-- todos para que una base de datos nueva no dependa de esos inserts manuales perdidos.
--
-- ⚠️ Ejecutar UNA VEZ como KRONOS_DEV en cada entorno/PC, después de que la app haya creado
-- el esquema (ver README.md). Re-ejecutable (MERGE): no duplica filas ya existentes.

MERGE INTO ROL r USING (SELECT 'APRENDIZ' AS nombre_rol FROM dual) src
ON (r.NOMBRE_ROL = src.nombre_rol)
WHEN NOT MATCHED THEN INSERT (NOMBRE_ROL) VALUES (src.nombre_rol);

MERGE INTO ROL r USING (SELECT 'INSTRUCTOR_SEGUIMIENTO' AS nombre_rol FROM dual) src
ON (r.NOMBRE_ROL = src.nombre_rol)
WHEN NOT MATCHED THEN INSERT (NOMBRE_ROL) VALUES (src.nombre_rol);

MERGE INTO ROL r USING (SELECT 'INSTRUCTOR_TECNICO' AS nombre_rol FROM dual) src
ON (r.NOMBRE_ROL = src.nombre_rol)
WHEN NOT MATCHED THEN INSERT (NOMBRE_ROL) VALUES (src.nombre_rol);

MERGE INTO ROL r USING (SELECT 'GESTOR_ETAPA' AS nombre_rol FROM dual) src
ON (r.NOMBRE_ROL = src.nombre_rol)
WHEN NOT MATCHED THEN INSERT (NOMBRE_ROL) VALUES (src.nombre_rol);

MERGE INTO ROL r USING (SELECT 'ADMINISTRADOR' AS nombre_rol FROM dual) src
ON (r.NOMBRE_ROL = src.nombre_rol)
WHEN NOT MATCHED THEN INSERT (NOMBRE_ROL) VALUES (src.nombre_rol);

MERGE INTO ROL r USING (SELECT 'REGISTRO' AS nombre_rol FROM dual) src
ON (r.NOMBRE_ROL = src.nombre_rol)
WHEN NOT MATCHED THEN INSERT (NOMBRE_ROL) VALUES (src.nombre_rol);

MERGE INTO ROL r USING (SELECT 'COORDINADOR_ACADEMICO' AS nombre_rol FROM dual) src
ON (r.NOMBRE_ROL = src.nombre_rol)
WHEN NOT MATCHED THEN INSERT (NOMBRE_ROL) VALUES (src.nombre_rol);

COMMIT;
