-- 🔧 EVALUACION_MOMENTO.OBSERVACION e ID_INSTRUCTOR nacieron NOT NULL (solo el instructor escribía).
-- Ahora el aprendiz también escribe su parte en paralelo y puede crear la fila primero, así que
-- ambas columnas deben admitir NULL hasta que el instructor complete la suya. Hibernate
-- ddl-auto=update NO relaja constraints NOT NULL existentes, así que hace falta este ALTER manual
-- (mismo caso ya documentado para los fix de CHECK constraint de este proyecto).
--
-- ⚠️ Ejecutar UNA VEZ como KRONOS_DEV en cada entorno/PC.
-- Re-ejecutable: si la columna ya es nullable, el ALTER es un no-op (Oracle no falla).

ALTER TABLE EVALUACION_MOMENTO MODIFY (OBSERVACION NULL);
ALTER TABLE EVALUACION_MOMENTO MODIFY (ID_INSTRUCTOR NULL);

COMMIT;
