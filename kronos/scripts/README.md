# Scripts de base de datos de KRONOS

Esta carpeta reúne todo lo que Hibernate (`spring.jpa.hibernate.ddl-auto=update`) **no** hace
solo: sembrar datos de catálogo y corregir columnas/constraints que Hibernate no toca en tablas
que ya existen. Sigue esta guía para levantar KRONOS en un PC nuevo sin toparte con los ORA-*
que motivaron cada uno de estos scripts.

## Instalación en un PC nuevo (base de datos vacía)

Ejecuta en este orden exacto:

1. **Crear el esquema Oracle.** Conectado como `SYSDBA`, corre
   [`instalacion/00_crear_esquema.sql`](instalacion/00_crear_esquema.sql). Sáltate este paso si
   ya tienes el usuario `KRONOS_DEV` creado (por ejemplo, restauraste un export).
2. **Arrancar la app una vez** (`mvnw.cmd spring-boot:run` desde `kronos/`, o el Run del IDE).
   Con el esquema vacío, Hibernate crea todas las tablas, columnas y `CHECK` constraints
   exactamente como están definidos en las entidades **hoy** — por eso casi ninguno de los
   scripts "históricos" de más abajo hace falta en una instalación nueva: esos scripts existen
   para corregir una tabla que Hibernate ya había creado con una forma antigua, no para crear
   nada desde cero. Detén la app después de que termine de arrancar.
3. **Sembrar catálogos**, conectado como `KRONOS_DEV`, en este orden:
   1. [`instalacion/01_roles_base.sql`](instalacion/01_roles_base.sql)
   2. [`instalacion/02_secciones_formato_base.sql`](instalacion/02_secciones_formato_base.sql)
   3. [`plantilla_formato_catalogo_modalidades.sql`](plantilla_formato_catalogo_modalidades.sql)
      (depende de que el paso 3.2 ya haya corrido)
   4. [`departamento_municipio_datos_reales.sql`](departamento_municipio_datos_reales.sql)
4. Arranca la app de nuevo. Deberías poder crear usuarios de cualquier rol desde
   `/admin/usuarios` y ver las 5 modalidades en `/formatos`.

Todos los scripts de `instalacion/` y los cuatro del paso 3 son **idempotentes** (usan `MERGE` o
`WHERE NOT EXISTS`): si los corres dos veces, o en una base que ya tenía algunos de estos datos,
no duplican filas ni fallan.

## Por qué existen `instalacion/01_roles_base.sql` y `02_secciones_formato_base.sql`

Antes de esta carpeta, los 7 roles y las 4 modalidades de contrato base (Pasantía, Vinculación
Laboral, Monitoría, Proyecto Productivo) se insertaron a mano, una sola vez, directo en la base
de datos de desarrollo — nunca quedaron en un script. Con eso, clonar el repo en un PC nuevo y
seguir solo los scripts existentes dejaba la base de datos sin esas filas: `/admin/usuarios` no
dejaba crear un Aprendiz o un Instructor Técnico, y `/formatos` mostraba vacías todas las
modalidades salvo Contrato de Aprendizaje. Estos dos scripts nuevos cierran ese hueco.

## Scripts históricos — NO ejecutar en una instalación nueva

Los siguientes scripts corrigen tablas que **ya existían** en una versión anterior del esquema
(columnas angostas, `CHECK` desalineados de un enum viejo, nombres de columna que cambiaron,
backfill de filas históricas). En una base de datos vacía esa tabla nunca existió con la forma
vieja, así que estos scripts no aplican; algunos, si igual se ejecutan, **fallan con error**.
Quedan aquí como registro histórico de la evolución del esquema, por si alguna vez hay que
actualizar una base de datos que viene de una versión antigua de KRONOS.

| Script | Por qué no hace falta en una base nueva |
|---|---|
| `auditoria_ampliar_acciones.sql` | La entidad ya define `AUDITORIA.ACCION` como `VARCHAR2(30)` con el `CHECK` completo. |
| `documento_solicitud_asunto_y_constraint.sql` | La entidad ya define `ASUNTO` y ya no tiene la restricción única vieja. |
| `documento_solicitud_backfill_estado_validacion.sql` | Backfill de filas históricas; no hay filas que corregir. |
| `documento_solicitud_fix_id_plantilla_nullable.sql` | La entidad ya define `ID_PLANTILLA` nullable. |
| `etapa_productiva_backfill_fecha_creacion.sql` | Backfill de filas históricas; no hay filas que corregir. |
| `etapa_productiva_backfill_fecha_por_certificar.sql` | Backfill de un estado (`POR_CERTIFICAR`) que ya no existe en el enum. |
| `etapa_productiva_fix_check_estado.sql` | El `CHECK` que genera Hibernate ya sale alineado con el enum `EstadoEtapa` actual. |
| `etapa_productiva_renombrar_certificado_a_terminado.sql` | Migra datos de un valor de enum (`CERTIFICADO`) que ya no existe. |
| `etapa_productiva_renombrar_fecha_por_certificar.sql` | ⚠️ Hace `RENAME COLUMN` de `FECHA_POR_CERTIFICAR`, columna que en una base nueva nunca existe (la entidad ya se llama `FECHA_TERMINACION`) — **falla con ORA-00904 si se ejecuta**. |
| `evaluaciones_fix_check_resultado.sql` | El `CHECK` ya sale alineado con `ResultadoEvaluacion` actual. |
| `evaluacion_momento_columnas_nullable.sql` | La entidad ya define esas columnas nullable. |
| `mensaje_registro_eliminar_definitivo.sql` | Borra una tabla vieja sin entidad Java; una base nueva nunca la crea. |
| `novedad_chat_coordinacion_academica.sql` | El `CHECK`/columna de `NOVEDAD` ya salen alineados. |
| `novedad_chat_registro_gestor.sql` | Igual que el anterior. |
| `solicitud_estado_add_pendiente_aprobacion_registro.sql` | El `CHECK` de `SOLICITUD_ETAPA_PRACTICA.ESTADO` ya incluye `PENDIENTE_REGISTRO`. |
| `solicitud_etapa_practica_fix_check_estado.sql` | Igual, ya incluye todos los estados actuales del enum. |
| `visita_seguimiento_tipo_unico.sql` | Migra datos de valores de enum (`CONCERTACION`, `EVALUACION_FINAL`) que ya no existen. |

`rol_registro_seed.sql`, `rol_coordinador_academico_seed.sql` y
`seccion_formato_agregar_contrato_aprendizaje.sql` sí son seguros de ejecutar en una base nueva
(son idempotentes), pero ya quedan cubiertos por `instalacion/01_roles_base.sql` y
`instalacion/02_secciones_formato_base.sql` — no hace falta correrlos aparte.

## Para el próximo cambio de esquema

Cuando un cambio en una entidad no lo cubra `ddl-auto=update` (columna angosta, `CHECK`
desalineado, `NOT NULL`/nombre que hay que migrar a mano), agrega un script nuevo aquí con el
mismo patrón que los de arriba: comentario explicando el motivo y el error que evita, e
idempotente (`MERGE`, `WHERE NOT EXISTS`, o buscar el constraint por condición antes de
recrearlo). Así el próximo PC nuevo no vuelve a depender de un `ALTER`/`INSERT` que alguien
corrió a mano y no quedó documentado.

## Nota de seguridad

`kronos/src/main/resources/application.properties` trae la contraseña real de `KRONOS_DEV` y la
contraseña de aplicación de Gmail como valor por defecto, y ese archivo viaja en el repositorio.
Antes de compartir este repositorio más allá de tu propio equipo, conviene rotar esas
credenciales y moverlas a variables de entorno (`DB_PASSWORD`, `MAIL_PASSWORD`, ...) en vez de
dejarlas como default en el código.
