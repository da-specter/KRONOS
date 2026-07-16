-- 📁 Módulo "Formatos": catálogo de plantillas por modalidad de contrato (Pasantía,
-- Vinculación Laboral, Monitoría, Proyecto Productivo) más los formatos generales de
-- Etapa Práctica (Planeación/Seguimiento y Bitácora), apuntando a los archivos que ya
-- viven en uploads/plantillas/ (esos sí viajan con git, esta tabla no).
--
-- ⚠️ Ejecutar UNA VEZ como KRONOS_END en cada entorno/PC: sin este script, /formatos
-- muestra vacías las secciones de Vinculación Laboral, Monitoría y Proyecto Productivo
-- (nunca tuvieron fila), y Pasantía/Etapa Práctica pueden seguir apuntando a archivos
-- viejos que ya no existen en disco.
--
-- Es re-ejecutable (MERGE): identifica cada plantilla por su combinación
-- (sección de contrato + nombre de documento), no por ID_PLANTILLA, para no depender del
-- orden en que cada máquina haya generado sus propios IDs.

-- 1) Etapa Práctica (general, sin sección de contrato)

MERGE INTO PLANTILLA_FORMATO p
USING (
    SELECT NULL AS id_seccion, 'Formato Planeacion Seguimiento EP' AS nombre_documento,
           '/uploads/plantillas/etapa_practica/Formato Planeacion Seguimiento EP.docx' AS ruta,
           'SOLO_APRENDIZ' AS visibilidad
    FROM dual
) src
ON (NVL(p.ID_SECCION_FORMATO, -1) = NVL(src.id_seccion, -1) AND p.NOMBRE_DOCUMENTO = src.nombre_documento)
WHEN MATCHED THEN UPDATE SET p.RUTA_ARCHIVO_PLANTILLA = src.ruta, p.FECHA_SUBIDA = SYSTIMESTAMP, p.ESTADO = 1
WHEN NOT MATCHED THEN INSERT (ID_SECCION_FORMATO, NOMBRE_DOCUMENTO, RUTA_ARCHIVO_PLANTILLA, VISIBILIDAD, FECHA_SUBIDA, ESTADO)
VALUES (src.id_seccion, src.nombre_documento, src.ruta, src.visibilidad, SYSTIMESTAMP, 1);

MERGE INTO PLANTILLA_FORMATO p
USING (
    SELECT NULL AS id_seccion, 'Formato Bitacora' AS nombre_documento,
           '/uploads/plantillas/etapa_practica/Formato Bitacora.xlsx' AS ruta,
           'SOLO_APRENDIZ' AS visibilidad
    FROM dual
) src
ON (NVL(p.ID_SECCION_FORMATO, -1) = NVL(src.id_seccion, -1) AND p.NOMBRE_DOCUMENTO = src.nombre_documento)
WHEN MATCHED THEN UPDATE SET p.RUTA_ARCHIVO_PLANTILLA = src.ruta, p.FECHA_SUBIDA = SYSTIMESTAMP, p.ESTADO = 1
WHEN NOT MATCHED THEN INSERT (ID_SECCION_FORMATO, NOMBRE_DOCUMENTO, RUTA_ARCHIVO_PLANTILLA, VISIBILIDAD, FECHA_SUBIDA, ESTADO)
VALUES (src.id_seccion, src.nombre_documento, src.ruta, src.visibilidad, SYSTIMESTAMP, 1);

-- 2) Pasantía

MERGE INTO PLANTILLA_FORMATO p
USING (
    SELECT (SELECT ID_SECCION_FORMATO FROM SECCION_FORMATO WHERE NOMBRE_SECCION = 'PASANTIA') AS id_seccion,
           'Requisitos vinculo formativo' AS nombre_documento,
           '/uploads/plantillas/pasantia/Requisitos para vinculo formativo.pdf' AS ruta,
           'SOLO_COORDINADOR' AS visibilidad
    FROM dual
) src
ON (NVL(p.ID_SECCION_FORMATO, -1) = NVL(src.id_seccion, -1) AND p.NOMBRE_DOCUMENTO = src.nombre_documento)
WHEN MATCHED THEN UPDATE SET p.RUTA_ARCHIVO_PLANTILLA = src.ruta, p.FECHA_SUBIDA = SYSTIMESTAMP, p.ESTADO = 1
WHEN NOT MATCHED THEN INSERT (ID_SECCION_FORMATO, NOMBRE_DOCUMENTO, RUTA_ARCHIVO_PLANTILLA, VISIBILIDAD, FECHA_SUBIDA, ESTADO)
VALUES (src.id_seccion, src.nombre_documento, src.ruta, src.visibilidad, SYSTIMESTAMP, 1);

MERGE INTO PLANTILLA_FORMATO p
USING (
    SELECT (SELECT ID_SECCION_FORMATO FROM SECCION_FORMATO WHERE NOMBRE_SECCION = 'PASANTIA') AS id_seccion,
           'Introduccion Etapa Productiva' AS nombre_documento,
           '/uploads/plantillas/pasantia/introduccion Etapa practica.docx' AS ruta,
           'SOLO_COORDINADOR' AS visibilidad
    FROM dual
) src
ON (NVL(p.ID_SECCION_FORMATO, -1) = NVL(src.id_seccion, -1) AND p.NOMBRE_DOCUMENTO = src.nombre_documento)
WHEN MATCHED THEN UPDATE SET p.RUTA_ARCHIVO_PLANTILLA = src.ruta, p.FECHA_SUBIDA = SYSTIMESTAMP, p.ESTADO = 1
WHEN NOT MATCHED THEN INSERT (ID_SECCION_FORMATO, NOMBRE_DOCUMENTO, RUTA_ARCHIVO_PLANTILLA, VISIBILIDAD, FECHA_SUBIDA, ESTADO)
VALUES (src.id_seccion, src.nombre_documento, src.ruta, src.visibilidad, SYSTIMESTAMP, 1);

-- 3) Vinculación Laboral

MERGE INTO PLANTILLA_FORMATO p
USING (
    SELECT (SELECT ID_SECCION_FORMATO FROM SECCION_FORMATO WHERE NOMBRE_SECCION = 'VINCULACION LABORAL') AS id_seccion,
           'Requisitos vinculo laboral' AS nombre_documento,
           '/uploads/plantillas/vinculo_laboral/Requisitos para vinculo laboral.pdf' AS ruta,
           'SOLO_COORDINADOR' AS visibilidad
    FROM dual
) src
ON (NVL(p.ID_SECCION_FORMATO, -1) = NVL(src.id_seccion, -1) AND p.NOMBRE_DOCUMENTO = src.nombre_documento)
WHEN MATCHED THEN UPDATE SET p.RUTA_ARCHIVO_PLANTILLA = src.ruta, p.FECHA_SUBIDA = SYSTIMESTAMP, p.ESTADO = 1
WHEN NOT MATCHED THEN INSERT (ID_SECCION_FORMATO, NOMBRE_DOCUMENTO, RUTA_ARCHIVO_PLANTILLA, VISIBILIDAD, FECHA_SUBIDA, ESTADO)
VALUES (src.id_seccion, src.nombre_documento, src.ruta, src.visibilidad, SYSTIMESTAMP, 1);

MERGE INTO PLANTILLA_FORMATO p
USING (
    SELECT (SELECT ID_SECCION_FORMATO FROM SECCION_FORMATO WHERE NOMBRE_SECCION = 'VINCULACION LABORAL') AS id_seccion,
           'Introduccion Etapa Productiva' AS nombre_documento,
           '/uploads/plantillas/vinculo_laboral/introduccion Etapa practica.docx' AS ruta,
           'SOLO_COORDINADOR' AS visibilidad
    FROM dual
) src
ON (NVL(p.ID_SECCION_FORMATO, -1) = NVL(src.id_seccion, -1) AND p.NOMBRE_DOCUMENTO = src.nombre_documento)
WHEN MATCHED THEN UPDATE SET p.RUTA_ARCHIVO_PLANTILLA = src.ruta, p.FECHA_SUBIDA = SYSTIMESTAMP, p.ESTADO = 1
WHEN NOT MATCHED THEN INSERT (ID_SECCION_FORMATO, NOMBRE_DOCUMENTO, RUTA_ARCHIVO_PLANTILLA, VISIBILIDAD, FECHA_SUBIDA, ESTADO)
VALUES (src.id_seccion, src.nombre_documento, src.ruta, src.visibilidad, SYSTIMESTAMP, 1);

-- 4) Monitoría (solo tiene la introducción; no hay "Requisitos" propio todavía)

MERGE INTO PLANTILLA_FORMATO p
USING (
    SELECT (SELECT ID_SECCION_FORMATO FROM SECCION_FORMATO WHERE NOMBRE_SECCION = 'MONITORIA') AS id_seccion,
           'Introduccion Etapa Productiva' AS nombre_documento,
           '/uploads/plantillas/monitoria/introduccion Etapa practica.docx' AS ruta,
           'SOLO_COORDINADOR' AS visibilidad
    FROM dual
) src
ON (NVL(p.ID_SECCION_FORMATO, -1) = NVL(src.id_seccion, -1) AND p.NOMBRE_DOCUMENTO = src.nombre_documento)
WHEN MATCHED THEN UPDATE SET p.RUTA_ARCHIVO_PLANTILLA = src.ruta, p.FECHA_SUBIDA = SYSTIMESTAMP, p.ESTADO = 1
WHEN NOT MATCHED THEN INSERT (ID_SECCION_FORMATO, NOMBRE_DOCUMENTO, RUTA_ARCHIVO_PLANTILLA, VISIBILIDAD, FECHA_SUBIDA, ESTADO)
VALUES (src.id_seccion, src.nombre_documento, src.ruta, src.visibilidad, SYSTIMESTAMP, 1);

-- 5) Proyecto Productivo

MERGE INTO PLANTILLA_FORMATO p
USING (
    SELECT (SELECT ID_SECCION_FORMATO FROM SECCION_FORMATO WHERE NOMBRE_SECCION = 'PROYECTO PRODUCTIVO') AS id_seccion,
           'Requisitos proyecto productivo' AS nombre_documento,
           '/uploads/plantillas/proyecto_productivo/Requisitos para proyecto productivo.pdf' AS ruta,
           'SOLO_COORDINADOR' AS visibilidad
    FROM dual
) src
ON (NVL(p.ID_SECCION_FORMATO, -1) = NVL(src.id_seccion, -1) AND p.NOMBRE_DOCUMENTO = src.nombre_documento)
WHEN MATCHED THEN UPDATE SET p.RUTA_ARCHIVO_PLANTILLA = src.ruta, p.FECHA_SUBIDA = SYSTIMESTAMP, p.ESTADO = 1
WHEN NOT MATCHED THEN INSERT (ID_SECCION_FORMATO, NOMBRE_DOCUMENTO, RUTA_ARCHIVO_PLANTILLA, VISIBILIDAD, FECHA_SUBIDA, ESTADO)
VALUES (src.id_seccion, src.nombre_documento, src.ruta, src.visibilidad, SYSTIMESTAMP, 1);

MERGE INTO PLANTILLA_FORMATO p
USING (
    SELECT (SELECT ID_SECCION_FORMATO FROM SECCION_FORMATO WHERE NOMBRE_SECCION = 'PROYECTO PRODUCTIVO') AS id_seccion,
           'Introduccion Etapa Productiva' AS nombre_documento,
           '/uploads/plantillas/proyecto_productivo/introduccion Etapa practica.docx' AS ruta,
           'SOLO_COORDINADOR' AS visibilidad
    FROM dual
) src
ON (NVL(p.ID_SECCION_FORMATO, -1) = NVL(src.id_seccion, -1) AND p.NOMBRE_DOCUMENTO = src.nombre_documento)
WHEN MATCHED THEN UPDATE SET p.RUTA_ARCHIVO_PLANTILLA = src.ruta, p.FECHA_SUBIDA = SYSTIMESTAMP, p.ESTADO = 1
WHEN NOT MATCHED THEN INSERT (ID_SECCION_FORMATO, NOMBRE_DOCUMENTO, RUTA_ARCHIVO_PLANTILLA, VISIBILIDAD, FECHA_SUBIDA, ESTADO)
VALUES (src.id_seccion, src.nombre_documento, src.ruta, src.visibilidad, SYSTIMESTAMP, 1);

COMMIT;
