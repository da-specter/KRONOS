-- Backfill de DOCUMENTO_SOLICITUD.ESTADO_VALIDACION para solicitudes que ya llegaron a
-- APROBADO_EN_ETAPA antes de que KronosWorkflowService.registroValidarDocumentos empezara
-- a marcar APROBADO cada documento al momento real de la validación de Registro. Sin este
-- backfill, esas solicitudes históricas quedaban con sus documentos en PENDIENTE para
-- siempre, inflando el widget "Documentos Pendientes de Validación" del Gestor de Etapa
-- con expedientes que en realidad ya estaban cerrados. Idempotente: solo toca documentos
-- todavía en PENDIENTE cuya solicitud ya está en APROBADO_EN_ETAPA (única forma de llegar
-- ahí es haber pasado por la validación de Registro).
UPDATE documento_solicitud ds
SET estado_validacion = 'APROBADO'
WHERE ds.estado_validacion = 'PENDIENTE'
AND ds.id_solicitud IN (
    SELECT id_solicitud FROM solicitud_etapa_practica WHERE estado = 'APROBADO_EN_ETAPA'
);

COMMIT;
