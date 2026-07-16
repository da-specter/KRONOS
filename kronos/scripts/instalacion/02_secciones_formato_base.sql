-- 📁 Siembra las 5 secciones de SECCION_FORMATO (modalidades de contrato) que alimentan el
-- <select> de modalidad en /formatos (SeccionFormatoRepository.findByEstadoTrue) y las
-- referencias por nombre de ../plantilla_formato_catalogo_modalidades.sql.
--
-- PASANTIA, VINCULACION LABORAL, MONITORIA y PROYECTO PRODUCTIVO habían quedado sembradas a
-- mano en la base de datos de desarrollo (sin script propio); solo CONTRATO APRENDIZAJE tenía
-- script (../seccion_formato_agregar_contrato_aprendizaje.sql). Sin las 4 primeras,
-- plantilla_formato_catalogo_modalidades.sql resuelve su ID_SECCION_FORMATO como NULL y las
-- plantillas de esas modalidades quedan mal asociadas (mezcladas con las de Etapa Práctica
-- general, que también usan ID_SECCION_FORMATO NULL).
--
-- ⚠️ Ejecutar UNA VEZ como KRONOS_END en cada entorno/PC, ANTES de
-- ../plantilla_formato_catalogo_modalidades.sql. Re-ejecutable: si una sección ya existe, no
-- la duplica.

INSERT INTO SECCION_FORMATO (NOMBRE_SECCION, ESTADO)
SELECT 'PASANTIA', 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM SECCION_FORMATO WHERE UPPER(NOMBRE_SECCION) = 'PASANTIA');

INSERT INTO SECCION_FORMATO (NOMBRE_SECCION, ESTADO)
SELECT 'VINCULACION LABORAL', 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM SECCION_FORMATO WHERE UPPER(NOMBRE_SECCION) = 'VINCULACION LABORAL');

INSERT INTO SECCION_FORMATO (NOMBRE_SECCION, ESTADO)
SELECT 'MONITORIA', 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM SECCION_FORMATO WHERE UPPER(NOMBRE_SECCION) = 'MONITORIA');

INSERT INTO SECCION_FORMATO (NOMBRE_SECCION, ESTADO)
SELECT 'PROYECTO PRODUCTIVO', 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM SECCION_FORMATO WHERE UPPER(NOMBRE_SECCION) = 'PROYECTO PRODUCTIVO');

INSERT INTO SECCION_FORMATO (NOMBRE_SECCION, ESTADO)
SELECT 'CONTRATO APRENDIZAJE', 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM SECCION_FORMATO WHERE UPPER(NOMBRE_SECCION) = 'CONTRATO APRENDIZAJE');

COMMIT;
