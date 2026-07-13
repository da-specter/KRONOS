-- 🎓 Habilita "Contrato de Aprendizaje" como modalidad radicable en KRONOS (antes bloqueada
-- con un modal que redirigía a SGVA). Al insertarla en SECCION_FORMATO aparece sola en los
-- <select> de modalidad (findByEstadoTrue), sin tocar esa consulta.
--
-- ⚠️ Ejecutar UNA VEZ como KRONOS_DEV en cada entorno/PC. Re-ejecutable: si ya existe, no duplica.

INSERT INTO SECCION_FORMATO (NOMBRE_SECCION, ESTADO)
SELECT 'CONTRATO APRENDIZAJE', 1 FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM SECCION_FORMATO WHERE UPPER(NOMBRE_SECCION) = 'CONTRATO APRENDIZAJE'
);

COMMIT;
