-- 🎓 Certificación automática (3 meses en POR_CERTIFICAR): el nuevo job diario
-- (CertificacionAutomaticaService) necesita FECHA_POR_CERTIFICAR para saber desde cuándo
-- contar el plazo. Las etapas que YA estaban en POR_CERTIFICAR antes de este cambio no
-- tienen ese dato (no se registró cuándo entraron a ese estado), así que se rellena con la
-- fecha actual como punto de partida — el plazo de 3 meses arranca a contar desde hoy para
-- ellas, en vez de perderse o certificarlas de inmediato.
--
-- ⚠️ Ejecutar UNA VEZ como KRONOS_DEV. Re-ejecutable (solo toca filas con el campo en NULL).

UPDATE ETAPA_PRODUCTIVA
SET FECHA_POR_CERTIFICAR = SYSTIMESTAMP
WHERE ESTADO_ETAPA = 'POR_CERTIFICAR'
  AND FECHA_POR_CERTIFICAR IS NULL;

COMMIT;
