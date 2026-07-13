-- Carga de datos reales de Departamento y Municipio (Colombia, 32 departamentos), generado
-- a partir de la lista oficial DIVIPOLA. Reemplaza los datos de prueba escritos a mano.
--
-- IMPORTANTE: los departamentos/municipios de prueba actuales ("antioquia" en minúscula,
-- "cabo", "cabo verde", "tr", etc.) NO se tocan en este script porque hay 10 filas de
-- EMPRESA (y 9 de ETAPA_PRODUCTIVA) que los referencian por FK obligatoria -- decisión
-- explícita: el rol REGISTRO las reasignará manualmente a un municipio real desde la UI
-- una vez cargados estos datos. Los DELETE de abajo son un no-op hoy (toda la basura actual
-- está referenciada) pero quedan listos para limpiar solos en cuanto se reasignen esas filas.
--
-- ⚠️ Ejecutar UNA VEZ como KRONOS_DEV en cada entorno/PC.
-- Re-ejecutable: los DELETE solo tocan filas sin EMPRESA asociada, y los INSERT solo agregan
-- departamentos/municipios que todavía no existan (por nombre exacto).

-- 1) Limpieza defensiva: borra municipios de prueba que ya no tengan ninguna empresa asociada
DELETE FROM MUNICIPIO m
WHERE NOT EXISTS (SELECT 1 FROM EMPRESA e WHERE e.ID_MUNICIPIO = m.ID_MUNICIPIO);

-- 2) Borra departamentos de prueba que se hayan quedado sin municipios
DELETE FROM DEPARTAMENTO d
WHERE NOT EXISTS (SELECT 1 FROM MUNICIPIO m WHERE m.ID_DEPARTAMENTO = d.ID_DEPARTAMENTO);

-- 3) Departamentos reales (32), idempotente por nombre exacto
MERGE INTO DEPARTAMENTO dst USING (SELECT 'Amazonas' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'Antioquia' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'Arauca' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'Atlántico' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'Bolívar' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'Boyacá' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'Caldas' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'Caquetá' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'Casanare' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'Cauca' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'Cesar' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'Chocó' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'Córdoba' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'Cundinamarca' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'Guainía' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'Guaviare' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'Huila' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'La Guajira' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'Magdalena' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'Meta' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'Nariño' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'Norte de Santander' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'Putumayo' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'Quindío' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'Risaralda' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'San Andrés y Providencia' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'Santander' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'Sucre' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'Tolima' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'Valle del Cauca' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'Vaupés' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);
MERGE INTO DEPARTAMENTO dst USING (SELECT 'Vichada' AS nombre FROM dual) src ON (dst.NOMBRE_DEPARTAMENTO = src.nombre) WHEN NOT MATCHED THEN INSERT (NOMBRE_DEPARTAMENTO) VALUES (src.nombre);

-- 4) Municipios reales por departamento, idempotente por (nombre, departamento)
-- Amazonas (11 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'El Encanto' AS nombre FROM dual UNION ALL
  SELECT 'La Chorrera' AS nombre FROM dual UNION ALL
  SELECT 'La Pedrera' AS nombre FROM dual UNION ALL
  SELECT 'La Victoria' AS nombre FROM dual UNION ALL
  SELECT 'Leticia' AS nombre FROM dual UNION ALL
  SELECT 'Mirití-Paraná' AS nombre FROM dual UNION ALL
  SELECT 'Puerto Alegría' AS nombre FROM dual UNION ALL
  SELECT 'Puerto Arica' AS nombre FROM dual UNION ALL
  SELECT 'Puerto Nariño' AS nombre FROM dual UNION ALL
  SELECT 'Puerto Santander' AS nombre FROM dual UNION ALL
  SELECT 'Tarapacá' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'Amazonas'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- Antioquia (123 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Abejorral' AS nombre FROM dual UNION ALL
  SELECT 'Abriaquí' AS nombre FROM dual UNION ALL
  SELECT 'Alejandría' AS nombre FROM dual UNION ALL
  SELECT 'Amagá' AS nombre FROM dual UNION ALL
  SELECT 'Amalfi' AS nombre FROM dual UNION ALL
  SELECT 'Andes' AS nombre FROM dual UNION ALL
  SELECT 'Angelópolis' AS nombre FROM dual UNION ALL
  SELECT 'Angostura' AS nombre FROM dual UNION ALL
  SELECT 'Anorí' AS nombre FROM dual UNION ALL
  SELECT 'Anzá' AS nombre FROM dual UNION ALL
  SELECT 'Apartadó' AS nombre FROM dual UNION ALL
  SELECT 'Arboletes' AS nombre FROM dual UNION ALL
  SELECT 'Argelia' AS nombre FROM dual UNION ALL
  SELECT 'Armenia' AS nombre FROM dual UNION ALL
  SELECT 'Barbosa' AS nombre FROM dual UNION ALL
  SELECT 'Bello' AS nombre FROM dual UNION ALL
  SELECT 'Belmira' AS nombre FROM dual UNION ALL
  SELECT 'Betania' AS nombre FROM dual UNION ALL
  SELECT 'Betulia' AS nombre FROM dual UNION ALL
  SELECT 'Briceño' AS nombre FROM dual UNION ALL
  SELECT 'Buriticá' AS nombre FROM dual UNION ALL
  SELECT 'Cáceres' AS nombre FROM dual UNION ALL
  SELECT 'Caicedo' AS nombre FROM dual UNION ALL
  SELECT 'Caldas' AS nombre FROM dual UNION ALL
  SELECT 'Campamento' AS nombre FROM dual UNION ALL
  SELECT 'Caracolí' AS nombre FROM dual UNION ALL
  SELECT 'Caramanta' AS nombre FROM dual UNION ALL
  SELECT 'Carepa' AS nombre FROM dual UNION ALL
  SELECT 'Carolina del Príncipe' AS nombre FROM dual UNION ALL
  SELECT 'Caucasia' AS nombre FROM dual UNION ALL
  SELECT 'Chigorodó' AS nombre FROM dual UNION ALL
  SELECT 'Cisneros' AS nombre FROM dual UNION ALL
  SELECT 'Ciudad Bolívar' AS nombre FROM dual UNION ALL
  SELECT 'Cocorná' AS nombre FROM dual UNION ALL
  SELECT 'Concepción' AS nombre FROM dual UNION ALL
  SELECT 'Concordia' AS nombre FROM dual UNION ALL
  SELECT 'Copacabana' AS nombre FROM dual UNION ALL
  SELECT 'Dabeiba' AS nombre FROM dual UNION ALL
  SELECT 'Donmatías' AS nombre FROM dual UNION ALL
  SELECT 'Ebéjico' AS nombre FROM dual UNION ALL
  SELECT 'El Bagre' AS nombre FROM dual UNION ALL
  SELECT 'El Carmen de Viboral' AS nombre FROM dual UNION ALL
  SELECT 'El Peñol' AS nombre FROM dual UNION ALL
  SELECT 'El Retiro' AS nombre FROM dual UNION ALL
  SELECT 'El Santuario' AS nombre FROM dual UNION ALL
  SELECT 'Entrerríos' AS nombre FROM dual UNION ALL
  SELECT 'Envigado' AS nombre FROM dual UNION ALL
  SELECT 'Fredonia' AS nombre FROM dual UNION ALL
  SELECT 'Frontino' AS nombre FROM dual UNION ALL
  SELECT 'Giraldo' AS nombre FROM dual UNION ALL
  SELECT 'Girardota' AS nombre FROM dual UNION ALL
  SELECT 'Gómez Plata' AS nombre FROM dual UNION ALL
  SELECT 'Granada' AS nombre FROM dual UNION ALL
  SELECT 'Guadalupe' AS nombre FROM dual UNION ALL
  SELECT 'Guarne' AS nombre FROM dual UNION ALL
  SELECT 'Guatapé' AS nombre FROM dual UNION ALL
  SELECT 'Heliconia' AS nombre FROM dual UNION ALL
  SELECT 'Hispania' AS nombre FROM dual UNION ALL
  SELECT 'Itagüí' AS nombre FROM dual UNION ALL
  SELECT 'Ituango' AS nombre FROM dual UNION ALL
  SELECT 'Jardín' AS nombre FROM dual UNION ALL
  SELECT 'Jericó' AS nombre FROM dual UNION ALL
  SELECT 'La Ceja' AS nombre FROM dual UNION ALL
  SELECT 'La Estrella' AS nombre FROM dual UNION ALL
  SELECT 'La Pintada' AS nombre FROM dual UNION ALL
  SELECT 'La Unión' AS nombre FROM dual UNION ALL
  SELECT 'Liborina' AS nombre FROM dual UNION ALL
  SELECT 'Maceo' AS nombre FROM dual UNION ALL
  SELECT 'Marinilla' AS nombre FROM dual UNION ALL
  SELECT 'Medellín' AS nombre FROM dual UNION ALL
  SELECT 'Montebello' AS nombre FROM dual UNION ALL
  SELECT 'Murindó' AS nombre FROM dual UNION ALL
  SELECT 'Mutatá' AS nombre FROM dual UNION ALL
  SELECT 'Nariño' AS nombre FROM dual UNION ALL
  SELECT 'Nechí' AS nombre FROM dual UNION ALL
  SELECT 'Necoclí' AS nombre FROM dual UNION ALL
  SELECT 'Olaya' AS nombre FROM dual UNION ALL
  SELECT 'Peque' AS nombre FROM dual UNION ALL
  SELECT 'Pueblorrico' AS nombre FROM dual UNION ALL
  SELECT 'Puerto Berrío' AS nombre FROM dual UNION ALL
  SELECT 'Puerto Nare' AS nombre FROM dual UNION ALL
  SELECT 'Puerto Triunfo' AS nombre FROM dual UNION ALL
  SELECT 'Remedios' AS nombre FROM dual UNION ALL
  SELECT 'Rionegro' AS nombre FROM dual UNION ALL
  SELECT 'Sabanalarga' AS nombre FROM dual UNION ALL
  SELECT 'Sabaneta' AS nombre FROM dual UNION ALL
  SELECT 'Salgar' AS nombre FROM dual UNION ALL
  SELECT 'San Andrés de Cuerquia' AS nombre FROM dual UNION ALL
  SELECT 'San Carlos' AS nombre FROM dual UNION ALL
  SELECT 'San Francisco' AS nombre FROM dual UNION ALL
  SELECT 'San Jerónimo' AS nombre FROM dual UNION ALL
  SELECT 'San José de la Montaña' AS nombre FROM dual UNION ALL
  SELECT 'San Juan de Urabá' AS nombre FROM dual UNION ALL
  SELECT 'San Luis' AS nombre FROM dual UNION ALL
  SELECT 'San Pedro de Urabá' AS nombre FROM dual UNION ALL
  SELECT 'San Pedro de los Milagros' AS nombre FROM dual UNION ALL
  SELECT 'San Rafael' AS nombre FROM dual UNION ALL
  SELECT 'San Roque' AS nombre FROM dual UNION ALL
  SELECT 'San Vicente Ferrer' AS nombre FROM dual UNION ALL
  SELECT 'Santa Bárbara' AS nombre FROM dual UNION ALL
  SELECT 'Santa Fe de Antioquia' AS nombre FROM dual UNION ALL
  SELECT 'Santa Rosa de Osos' AS nombre FROM dual UNION ALL
  SELECT 'Santo Domingo' AS nombre FROM dual UNION ALL
  SELECT 'Segovia' AS nombre FROM dual UNION ALL
  SELECT 'Sonsón' AS nombre FROM dual UNION ALL
  SELECT 'Sopetrán' AS nombre FROM dual UNION ALL
  SELECT 'Tarazá' AS nombre FROM dual UNION ALL
  SELECT 'Tarso' AS nombre FROM dual UNION ALL
  SELECT 'Titiribí' AS nombre FROM dual UNION ALL
  SELECT 'Toledo' AS nombre FROM dual UNION ALL
  SELECT 'Turbo' AS nombre FROM dual UNION ALL
  SELECT 'Uramita' AS nombre FROM dual UNION ALL
  SELECT 'Urrao' AS nombre FROM dual UNION ALL
  SELECT 'Valdivia' AS nombre FROM dual UNION ALL
  SELECT 'Valparaíso' AS nombre FROM dual UNION ALL
  SELECT 'Vegachí' AS nombre FROM dual UNION ALL
  SELECT 'Venecia' AS nombre FROM dual UNION ALL
  SELECT 'Vigía del Fuerte' AS nombre FROM dual UNION ALL
  SELECT 'Yalí' AS nombre FROM dual UNION ALL
  SELECT 'Yarumal' AS nombre FROM dual UNION ALL
  SELECT 'Yolombó' AS nombre FROM dual UNION ALL
  SELECT 'Yondó' AS nombre FROM dual UNION ALL
  SELECT 'Zaragoza' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'Antioquia'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- Arauca (7 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Arauca' AS nombre FROM dual UNION ALL
  SELECT 'Arauquita' AS nombre FROM dual UNION ALL
  SELECT 'Cravo Norte' AS nombre FROM dual UNION ALL
  SELECT 'Fortul' AS nombre FROM dual UNION ALL
  SELECT 'Puerto Rondón' AS nombre FROM dual UNION ALL
  SELECT 'Saravena' AS nombre FROM dual UNION ALL
  SELECT 'Tame' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'Arauca'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- Atlántico (23 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Baranoa' AS nombre FROM dual UNION ALL
  SELECT 'Campo de la Cruz' AS nombre FROM dual UNION ALL
  SELECT 'Candelaria' AS nombre FROM dual UNION ALL
  SELECT 'Galapa' AS nombre FROM dual UNION ALL
  SELECT 'Juan de Acosta' AS nombre FROM dual UNION ALL
  SELECT 'Luruaco' AS nombre FROM dual UNION ALL
  SELECT 'Malambo' AS nombre FROM dual UNION ALL
  SELECT 'Manatí' AS nombre FROM dual UNION ALL
  SELECT 'Palmar de Varela' AS nombre FROM dual UNION ALL
  SELECT 'Piojó' AS nombre FROM dual UNION ALL
  SELECT 'Polonuevo' AS nombre FROM dual UNION ALL
  SELECT 'Ponedera' AS nombre FROM dual UNION ALL
  SELECT 'Puerto Colombia' AS nombre FROM dual UNION ALL
  SELECT 'Repelón' AS nombre FROM dual UNION ALL
  SELECT 'Sabanagrande' AS nombre FROM dual UNION ALL
  SELECT 'Sabanalarga' AS nombre FROM dual UNION ALL
  SELECT 'Santa Lucía' AS nombre FROM dual UNION ALL
  SELECT 'Santo Tomás' AS nombre FROM dual UNION ALL
  SELECT 'Soledad' AS nombre FROM dual UNION ALL
  SELECT 'Suán' AS nombre FROM dual UNION ALL
  SELECT 'Tubará' AS nombre FROM dual UNION ALL
  SELECT 'Usiacurí' AS nombre FROM dual UNION ALL
  SELECT 'Barranquilla' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'Atlántico'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- Bolívar (46 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Achí' AS nombre FROM dual UNION ALL
  SELECT 'Altos del Rosario' AS nombre FROM dual UNION ALL
  SELECT 'Arenal' AS nombre FROM dual UNION ALL
  SELECT 'Arjona' AS nombre FROM dual UNION ALL
  SELECT 'Arroyohondo' AS nombre FROM dual UNION ALL
  SELECT 'Barranco de Loba' AS nombre FROM dual UNION ALL
  SELECT 'Calamar' AS nombre FROM dual UNION ALL
  SELECT 'Cantagallo' AS nombre FROM dual UNION ALL
  SELECT 'Cartagena de Indias' AS nombre FROM dual UNION ALL
  SELECT 'Cicuco' AS nombre FROM dual UNION ALL
  SELECT 'Clemencia' AS nombre FROM dual UNION ALL
  SELECT 'Córdoba' AS nombre FROM dual UNION ALL
  SELECT 'El Carmen de Bolívar' AS nombre FROM dual UNION ALL
  SELECT 'El Guamo' AS nombre FROM dual UNION ALL
  SELECT 'El Peñón' AS nombre FROM dual UNION ALL
  SELECT 'Hatillo de Loba' AS nombre FROM dual UNION ALL
  SELECT 'Magangué' AS nombre FROM dual UNION ALL
  SELECT 'Mahates' AS nombre FROM dual UNION ALL
  SELECT 'Margarita' AS nombre FROM dual UNION ALL
  SELECT 'María la Baja' AS nombre FROM dual UNION ALL
  SELECT 'Montecristo' AS nombre FROM dual UNION ALL
  SELECT 'Mompós' AS nombre FROM dual UNION ALL
  SELECT 'Morales' AS nombre FROM dual UNION ALL
  SELECT 'Norosí' AS nombre FROM dual UNION ALL
  SELECT 'Pinillos' AS nombre FROM dual UNION ALL
  SELECT 'Regidor' AS nombre FROM dual UNION ALL
  SELECT 'Río Viejo' AS nombre FROM dual UNION ALL
  SELECT 'San Cristóbal' AS nombre FROM dual UNION ALL
  SELECT 'San Estanislao' AS nombre FROM dual UNION ALL
  SELECT 'San Fernando' AS nombre FROM dual UNION ALL
  SELECT 'San Jacinto' AS nombre FROM dual UNION ALL
  SELECT 'San Jacinto del Cauca' AS nombre FROM dual UNION ALL
  SELECT 'San Juan Nepomuceno' AS nombre FROM dual UNION ALL
  SELECT 'San Martín de Loba' AS nombre FROM dual UNION ALL
  SELECT 'San Pablo' AS nombre FROM dual UNION ALL
  SELECT 'Santa Catalina' AS nombre FROM dual UNION ALL
  SELECT 'Santa Rosa' AS nombre FROM dual UNION ALL
  SELECT 'Santa Rosa del Sur' AS nombre FROM dual UNION ALL
  SELECT 'Simití' AS nombre FROM dual UNION ALL
  SELECT 'Soplaviento' AS nombre FROM dual UNION ALL
  SELECT 'Talaigua Nuevo' AS nombre FROM dual UNION ALL
  SELECT 'Tiquisio' AS nombre FROM dual UNION ALL
  SELECT 'Turbaco' AS nombre FROM dual UNION ALL
  SELECT 'Turbaná' AS nombre FROM dual UNION ALL
  SELECT 'Villanueva' AS nombre FROM dual UNION ALL
  SELECT 'Zambrano' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'Bolívar'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- Boyacá (121 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Almeida' AS nombre FROM dual UNION ALL
  SELECT 'Aquitania' AS nombre FROM dual UNION ALL
  SELECT 'Arcabuco' AS nombre FROM dual UNION ALL
  SELECT 'Belén' AS nombre FROM dual UNION ALL
  SELECT 'Berbeo' AS nombre FROM dual UNION ALL
  SELECT 'Betéitiva' AS nombre FROM dual UNION ALL
  SELECT 'Boavita' AS nombre FROM dual UNION ALL
  SELECT 'Boyacá' AS nombre FROM dual UNION ALL
  SELECT 'Briceño' AS nombre FROM dual UNION ALL
  SELECT 'Buenavista' AS nombre FROM dual UNION ALL
  SELECT 'Busbanzá' AS nombre FROM dual UNION ALL
  SELECT 'Caldas' AS nombre FROM dual UNION ALL
  SELECT 'Campohermoso' AS nombre FROM dual UNION ALL
  SELECT 'Cerinza' AS nombre FROM dual UNION ALL
  SELECT 'Chinavita' AS nombre FROM dual UNION ALL
  SELECT 'Chiquinquirá' AS nombre FROM dual UNION ALL
  SELECT 'Chíquiza' AS nombre FROM dual UNION ALL
  SELECT 'Chiscas' AS nombre FROM dual UNION ALL
  SELECT 'Chita' AS nombre FROM dual UNION ALL
  SELECT 'Chitaraque' AS nombre FROM dual UNION ALL
  SELECT 'Chivatá' AS nombre FROM dual UNION ALL
  SELECT 'Chivor' AS nombre FROM dual UNION ALL
  SELECT 'Ciénega' AS nombre FROM dual UNION ALL
  SELECT 'Cómbita' AS nombre FROM dual UNION ALL
  SELECT 'Coper' AS nombre FROM dual UNION ALL
  SELECT 'Corrales' AS nombre FROM dual UNION ALL
  SELECT 'Covarachía' AS nombre FROM dual UNION ALL
  SELECT 'Cubará' AS nombre FROM dual UNION ALL
  SELECT 'Cucaita' AS nombre FROM dual UNION ALL
  SELECT 'Cuítiva' AS nombre FROM dual UNION ALL
  SELECT 'Duitama' AS nombre FROM dual UNION ALL
  SELECT 'El Cocuy' AS nombre FROM dual UNION ALL
  SELECT 'El Espino' AS nombre FROM dual UNION ALL
  SELECT 'Firavitoba' AS nombre FROM dual UNION ALL
  SELECT 'Floresta' AS nombre FROM dual UNION ALL
  SELECT 'Gachantivá' AS nombre FROM dual UNION ALL
  SELECT 'Gámeza' AS nombre FROM dual UNION ALL
  SELECT 'Garagoa' AS nombre FROM dual UNION ALL
  SELECT 'Guacamayas' AS nombre FROM dual UNION ALL
  SELECT 'Guateque' AS nombre FROM dual UNION ALL
  SELECT 'Guayatá' AS nombre FROM dual UNION ALL
  SELECT 'Güicán' AS nombre FROM dual UNION ALL
  SELECT 'Iza' AS nombre FROM dual UNION ALL
  SELECT 'Jenesano' AS nombre FROM dual UNION ALL
  SELECT 'Jericó' AS nombre FROM dual UNION ALL
  SELECT 'Labranzagrande' AS nombre FROM dual UNION ALL
  SELECT 'La Capilla' AS nombre FROM dual UNION ALL
  SELECT 'La Victoria' AS nombre FROM dual UNION ALL
  SELECT 'Macanal' AS nombre FROM dual UNION ALL
  SELECT 'Maripí' AS nombre FROM dual UNION ALL
  SELECT 'Miraflores' AS nombre FROM dual UNION ALL
  SELECT 'Mongua' AS nombre FROM dual UNION ALL
  SELECT 'Monguí' AS nombre FROM dual UNION ALL
  SELECT 'Moniquirá' AS nombre FROM dual UNION ALL
  SELECT 'Motavita' AS nombre FROM dual UNION ALL
  SELECT 'Muzo' AS nombre FROM dual UNION ALL
  SELECT 'Nobsa' AS nombre FROM dual UNION ALL
  SELECT 'Nuevo Colón' AS nombre FROM dual UNION ALL
  SELECT 'Oicatá' AS nombre FROM dual UNION ALL
  SELECT 'Otanche' AS nombre FROM dual UNION ALL
  SELECT 'Pachavita' AS nombre FROM dual UNION ALL
  SELECT 'Páez' AS nombre FROM dual UNION ALL
  SELECT 'Paipa' AS nombre FROM dual UNION ALL
  SELECT 'Pajarito' AS nombre FROM dual UNION ALL
  SELECT 'Panqueba' AS nombre FROM dual UNION ALL
  SELECT 'Pauna' AS nombre FROM dual UNION ALL
  SELECT 'Paya' AS nombre FROM dual UNION ALL
  SELECT 'Paz de Río' AS nombre FROM dual UNION ALL
  SELECT 'Pesca' AS nombre FROM dual UNION ALL
  SELECT 'Pisba' AS nombre FROM dual UNION ALL
  SELECT 'Puerto Boyacá' AS nombre FROM dual UNION ALL
  SELECT 'Quípama' AS nombre FROM dual UNION ALL
  SELECT 'Ramiriquí' AS nombre FROM dual UNION ALL
  SELECT 'Ráquira' AS nombre FROM dual UNION ALL
  SELECT 'Rondón' AS nombre FROM dual UNION ALL
  SELECT 'Saboyá' AS nombre FROM dual UNION ALL
  SELECT 'Sáchica' AS nombre FROM dual UNION ALL
  SELECT 'Samacá' AS nombre FROM dual UNION ALL
  SELECT 'San Eduardo' AS nombre FROM dual UNION ALL
  SELECT 'San José de Pare' AS nombre FROM dual UNION ALL
  SELECT 'San Luis de Gaceno' AS nombre FROM dual UNION ALL
  SELECT 'San Mateo' AS nombre FROM dual UNION ALL
  SELECT 'San Miguel de Sema' AS nombre FROM dual UNION ALL
  SELECT 'San Pablo de Borbur' AS nombre FROM dual UNION ALL
  SELECT 'Santa María' AS nombre FROM dual UNION ALL
  SELECT 'Santa Rosa de Viterbo' AS nombre FROM dual UNION ALL
  SELECT 'Santa Sofía' AS nombre FROM dual UNION ALL
  SELECT 'Sativanorte' AS nombre FROM dual UNION ALL
  SELECT 'Sativasur' AS nombre FROM dual UNION ALL
  SELECT 'Siachoque' AS nombre FROM dual UNION ALL
  SELECT 'Soatá' AS nombre FROM dual UNION ALL
  SELECT 'Socotá' AS nombre FROM dual UNION ALL
  SELECT 'Socha' AS nombre FROM dual UNION ALL
  SELECT 'Sogamoso' AS nombre FROM dual UNION ALL
  SELECT 'Somondoco' AS nombre FROM dual UNION ALL
  SELECT 'Sora' AS nombre FROM dual UNION ALL
  SELECT 'Sotaquirá' AS nombre FROM dual UNION ALL
  SELECT 'Soracá' AS nombre FROM dual UNION ALL
  SELECT 'Susacón' AS nombre FROM dual UNION ALL
  SELECT 'Sutamarchán' AS nombre FROM dual UNION ALL
  SELECT 'Sutatenza' AS nombre FROM dual UNION ALL
  SELECT 'Tasco' AS nombre FROM dual UNION ALL
  SELECT 'Tenza' AS nombre FROM dual UNION ALL
  SELECT 'Tibaná' AS nombre FROM dual UNION ALL
  SELECT 'Tibasosa' AS nombre FROM dual UNION ALL
  SELECT 'Tinjacá' AS nombre FROM dual UNION ALL
  SELECT 'Tipacoque' AS nombre FROM dual UNION ALL
  SELECT 'Toca' AS nombre FROM dual UNION ALL
  SELECT 'Togüí' AS nombre FROM dual UNION ALL
  SELECT 'Tópaga' AS nombre FROM dual UNION ALL
  SELECT 'Tota' AS nombre FROM dual UNION ALL
  SELECT 'Tunja' AS nombre FROM dual UNION ALL
  SELECT 'Tununguá' AS nombre FROM dual UNION ALL
  SELECT 'Turmequé' AS nombre FROM dual UNION ALL
  SELECT 'Tuta' AS nombre FROM dual UNION ALL
  SELECT 'Tutazá' AS nombre FROM dual UNION ALL
  SELECT 'Úmbita' AS nombre FROM dual UNION ALL
  SELECT 'Ventaquemada' AS nombre FROM dual UNION ALL
  SELECT 'Villa de Leyva' AS nombre FROM dual UNION ALL
  SELECT 'Viracachá' AS nombre FROM dual UNION ALL
  SELECT 'Zetaquira' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'Boyacá'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- Caldas (27 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Aguadas' AS nombre FROM dual UNION ALL
  SELECT 'Anserma' AS nombre FROM dual UNION ALL
  SELECT 'Aranzazu' AS nombre FROM dual UNION ALL
  SELECT 'Belalcázar' AS nombre FROM dual UNION ALL
  SELECT 'Chinchiná' AS nombre FROM dual UNION ALL
  SELECT 'Filadelfia' AS nombre FROM dual UNION ALL
  SELECT 'La Dorada' AS nombre FROM dual UNION ALL
  SELECT 'La Merced' AS nombre FROM dual UNION ALL
  SELECT 'Manizales' AS nombre FROM dual UNION ALL
  SELECT 'Manzanares' AS nombre FROM dual UNION ALL
  SELECT 'Marmato' AS nombre FROM dual UNION ALL
  SELECT 'Marquetalia' AS nombre FROM dual UNION ALL
  SELECT 'Marulanda' AS nombre FROM dual UNION ALL
  SELECT 'Neira' AS nombre FROM dual UNION ALL
  SELECT 'Norcasia' AS nombre FROM dual UNION ALL
  SELECT 'Pácora' AS nombre FROM dual UNION ALL
  SELECT 'Palestina' AS nombre FROM dual UNION ALL
  SELECT 'Pensilvania' AS nombre FROM dual UNION ALL
  SELECT 'Riosucio' AS nombre FROM dual UNION ALL
  SELECT 'Risaralda' AS nombre FROM dual UNION ALL
  SELECT 'Salamina' AS nombre FROM dual UNION ALL
  SELECT 'Samaná' AS nombre FROM dual UNION ALL
  SELECT 'San José' AS nombre FROM dual UNION ALL
  SELECT 'Supía' AS nombre FROM dual UNION ALL
  SELECT 'Victoria' AS nombre FROM dual UNION ALL
  SELECT 'Villamaría' AS nombre FROM dual UNION ALL
  SELECT 'Viterbo' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'Caldas'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- Caquetá (16 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Albania' AS nombre FROM dual UNION ALL
  SELECT 'Belén de los Andaquíes' AS nombre FROM dual UNION ALL
  SELECT 'Cartagena del Chairá' AS nombre FROM dual UNION ALL
  SELECT 'Curillo' AS nombre FROM dual UNION ALL
  SELECT 'El Doncello' AS nombre FROM dual UNION ALL
  SELECT 'El Paujil' AS nombre FROM dual UNION ALL
  SELECT 'Florencia' AS nombre FROM dual UNION ALL
  SELECT 'La Montañita' AS nombre FROM dual UNION ALL
  SELECT 'Milán' AS nombre FROM dual UNION ALL
  SELECT 'Morelia' AS nombre FROM dual UNION ALL
  SELECT 'Puerto Rico' AS nombre FROM dual UNION ALL
  SELECT 'San José del Fragua' AS nombre FROM dual UNION ALL
  SELECT 'San Vicente del Caguán' AS nombre FROM dual UNION ALL
  SELECT 'Solano' AS nombre FROM dual UNION ALL
  SELECT 'Solita' AS nombre FROM dual UNION ALL
  SELECT 'Valparaíso' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'Caquetá'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- Casanare (19 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Aguazul' AS nombre FROM dual UNION ALL
  SELECT 'Chámeza' AS nombre FROM dual UNION ALL
  SELECT 'Hato Corozal' AS nombre FROM dual UNION ALL
  SELECT 'La Salina' AS nombre FROM dual UNION ALL
  SELECT 'Maní' AS nombre FROM dual UNION ALL
  SELECT 'Monterrey' AS nombre FROM dual UNION ALL
  SELECT 'Nunchía' AS nombre FROM dual UNION ALL
  SELECT 'Orocué' AS nombre FROM dual UNION ALL
  SELECT 'Paz de Ariporo' AS nombre FROM dual UNION ALL
  SELECT 'Pore' AS nombre FROM dual UNION ALL
  SELECT 'Recetor' AS nombre FROM dual UNION ALL
  SELECT 'Sabanalarga' AS nombre FROM dual UNION ALL
  SELECT 'Sácama' AS nombre FROM dual UNION ALL
  SELECT 'San Luis de Palenque' AS nombre FROM dual UNION ALL
  SELECT 'Támara' AS nombre FROM dual UNION ALL
  SELECT 'Tauramena' AS nombre FROM dual UNION ALL
  SELECT 'Trinidad' AS nombre FROM dual UNION ALL
  SELECT 'Villanueva' AS nombre FROM dual UNION ALL
  SELECT 'Yopal' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'Casanare'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- Cauca (42 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Almaguer' AS nombre FROM dual UNION ALL
  SELECT 'Argelia' AS nombre FROM dual UNION ALL
  SELECT 'Balboa' AS nombre FROM dual UNION ALL
  SELECT 'Bolívar' AS nombre FROM dual UNION ALL
  SELECT 'Buenos Aires' AS nombre FROM dual UNION ALL
  SELECT 'Cajibío' AS nombre FROM dual UNION ALL
  SELECT 'Caldono' AS nombre FROM dual UNION ALL
  SELECT 'Caloto' AS nombre FROM dual UNION ALL
  SELECT 'Corinto' AS nombre FROM dual UNION ALL
  SELECT 'El Tambo' AS nombre FROM dual UNION ALL
  SELECT 'Florencia' AS nombre FROM dual UNION ALL
  SELECT 'Guachené' AS nombre FROM dual UNION ALL
  SELECT 'Guapí' AS nombre FROM dual UNION ALL
  SELECT 'Inzá' AS nombre FROM dual UNION ALL
  SELECT 'Jambaló' AS nombre FROM dual UNION ALL
  SELECT 'La Sierra' AS nombre FROM dual UNION ALL
  SELECT 'La Vega' AS nombre FROM dual UNION ALL
  SELECT 'López de Micay' AS nombre FROM dual UNION ALL
  SELECT 'Mercaderes' AS nombre FROM dual UNION ALL
  SELECT 'Miranda' AS nombre FROM dual UNION ALL
  SELECT 'Morales' AS nombre FROM dual UNION ALL
  SELECT 'Padilla' AS nombre FROM dual UNION ALL
  SELECT 'Páez' AS nombre FROM dual UNION ALL
  SELECT 'Patía' AS nombre FROM dual UNION ALL
  SELECT 'Piamonte' AS nombre FROM dual UNION ALL
  SELECT 'Piendamó' AS nombre FROM dual UNION ALL
  SELECT 'Popayán' AS nombre FROM dual UNION ALL
  SELECT 'Puerto Tejada' AS nombre FROM dual UNION ALL
  SELECT 'Puracé' AS nombre FROM dual UNION ALL
  SELECT 'Rosas' AS nombre FROM dual UNION ALL
  SELECT 'San Sebastián' AS nombre FROM dual UNION ALL
  SELECT 'Santander de Quilichao' AS nombre FROM dual UNION ALL
  SELECT 'Santa Rosa' AS nombre FROM dual UNION ALL
  SELECT 'Silvia' AS nombre FROM dual UNION ALL
  SELECT 'Sotará' AS nombre FROM dual UNION ALL
  SELECT 'Suárez' AS nombre FROM dual UNION ALL
  SELECT 'Sucre' AS nombre FROM dual UNION ALL
  SELECT 'Timbío' AS nombre FROM dual UNION ALL
  SELECT 'Timbiquí' AS nombre FROM dual UNION ALL
  SELECT 'Toribío' AS nombre FROM dual UNION ALL
  SELECT 'Totoró' AS nombre FROM dual UNION ALL
  SELECT 'Villa Rica' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'Cauca'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- Cesar (25 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Aguachica' AS nombre FROM dual UNION ALL
  SELECT 'Agustín Codazzi' AS nombre FROM dual UNION ALL
  SELECT 'Astrea' AS nombre FROM dual UNION ALL
  SELECT 'Becerril' AS nombre FROM dual UNION ALL
  SELECT 'Bosconia' AS nombre FROM dual UNION ALL
  SELECT 'Chimichagua' AS nombre FROM dual UNION ALL
  SELECT 'Chiriguaná' AS nombre FROM dual UNION ALL
  SELECT 'Curumaní' AS nombre FROM dual UNION ALL
  SELECT 'El Copey' AS nombre FROM dual UNION ALL
  SELECT 'El Paso' AS nombre FROM dual UNION ALL
  SELECT 'Gamarra' AS nombre FROM dual UNION ALL
  SELECT 'González' AS nombre FROM dual UNION ALL
  SELECT 'La Gloria' AS nombre FROM dual UNION ALL
  SELECT 'La Jagua de Ibirico' AS nombre FROM dual UNION ALL
  SELECT 'La Paz' AS nombre FROM dual UNION ALL
  SELECT 'Manaure Balcón del Cesar' AS nombre FROM dual UNION ALL
  SELECT 'Pailitas' AS nombre FROM dual UNION ALL
  SELECT 'Pelaya' AS nombre FROM dual UNION ALL
  SELECT 'Pueblo Bello' AS nombre FROM dual UNION ALL
  SELECT 'Río de Oro' AS nombre FROM dual UNION ALL
  SELECT 'San Alberto' AS nombre FROM dual UNION ALL
  SELECT 'San Diego' AS nombre FROM dual UNION ALL
  SELECT 'San Martín' AS nombre FROM dual UNION ALL
  SELECT 'Tamalameque' AS nombre FROM dual UNION ALL
  SELECT 'Valledupar' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'Cesar'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- Chocó (31 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Acandí' AS nombre FROM dual UNION ALL
  SELECT 'Alto Baudó' AS nombre FROM dual UNION ALL
  SELECT 'Atrato' AS nombre FROM dual UNION ALL
  SELECT 'Bagadó' AS nombre FROM dual UNION ALL
  SELECT 'Bahía Solano' AS nombre FROM dual UNION ALL
  SELECT 'Bajo Baudó' AS nombre FROM dual UNION ALL
  SELECT 'Belén de Bajirá' AS nombre FROM dual UNION ALL
  SELECT 'Bojayá' AS nombre FROM dual UNION ALL
  SELECT 'El Cantón del San Pablo' AS nombre FROM dual UNION ALL
  SELECT 'Carmen del Darién' AS nombre FROM dual UNION ALL
  SELECT 'Cértegui' AS nombre FROM dual UNION ALL
  SELECT 'Condoto' AS nombre FROM dual UNION ALL
  SELECT 'El Carmen de Atrato' AS nombre FROM dual UNION ALL
  SELECT 'El Litoral del San Juan' AS nombre FROM dual UNION ALL
  SELECT 'Istmina' AS nombre FROM dual UNION ALL
  SELECT 'Juradó' AS nombre FROM dual UNION ALL
  SELECT 'Lloró' AS nombre FROM dual UNION ALL
  SELECT 'Medio Atrato' AS nombre FROM dual UNION ALL
  SELECT 'Medio Baudó' AS nombre FROM dual UNION ALL
  SELECT 'Medio San Juan' AS nombre FROM dual UNION ALL
  SELECT 'Nóvita' AS nombre FROM dual UNION ALL
  SELECT 'Nuquí' AS nombre FROM dual UNION ALL
  SELECT 'Quibdó' AS nombre FROM dual UNION ALL
  SELECT 'Río Iró' AS nombre FROM dual UNION ALL
  SELECT 'Río Quito' AS nombre FROM dual UNION ALL
  SELECT 'Riosucio' AS nombre FROM dual UNION ALL
  SELECT 'San José del Palmar' AS nombre FROM dual UNION ALL
  SELECT 'Sipí' AS nombre FROM dual UNION ALL
  SELECT 'Tadó' AS nombre FROM dual UNION ALL
  SELECT 'Unguía' AS nombre FROM dual UNION ALL
  SELECT 'Unión Panamericana' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'Chocó'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- Córdoba (30 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Ayapel' AS nombre FROM dual UNION ALL
  SELECT 'Buenavista' AS nombre FROM dual UNION ALL
  SELECT 'Canalete' AS nombre FROM dual UNION ALL
  SELECT 'Cereté' AS nombre FROM dual UNION ALL
  SELECT 'Chimá' AS nombre FROM dual UNION ALL
  SELECT 'Chinú' AS nombre FROM dual UNION ALL
  SELECT 'Ciénaga de Oro' AS nombre FROM dual UNION ALL
  SELECT 'Cotorra' AS nombre FROM dual UNION ALL
  SELECT 'La Apartada' AS nombre FROM dual UNION ALL
  SELECT 'Lorica' AS nombre FROM dual UNION ALL
  SELECT 'Los Córdobas' AS nombre FROM dual UNION ALL
  SELECT 'Momil' AS nombre FROM dual UNION ALL
  SELECT 'Moñitos' AS nombre FROM dual UNION ALL
  SELECT 'Montelíbano' AS nombre FROM dual UNION ALL
  SELECT 'Montería' AS nombre FROM dual UNION ALL
  SELECT 'Planeta Rica' AS nombre FROM dual UNION ALL
  SELECT 'Pueblo Nuevo' AS nombre FROM dual UNION ALL
  SELECT 'Puerto Escondido' AS nombre FROM dual UNION ALL
  SELECT 'Puerto Libertador' AS nombre FROM dual UNION ALL
  SELECT 'Purísima' AS nombre FROM dual UNION ALL
  SELECT 'Sahagún' AS nombre FROM dual UNION ALL
  SELECT 'San Andrés de Sotavento' AS nombre FROM dual UNION ALL
  SELECT 'San Antero' AS nombre FROM dual UNION ALL
  SELECT 'San Bernardo del Viento' AS nombre FROM dual UNION ALL
  SELECT 'San Carlos' AS nombre FROM dual UNION ALL
  SELECT 'San José de Uré' AS nombre FROM dual UNION ALL
  SELECT 'San Pelayo' AS nombre FROM dual UNION ALL
  SELECT 'Tierralta' AS nombre FROM dual UNION ALL
  SELECT 'Tuchín' AS nombre FROM dual UNION ALL
  SELECT 'Valencia' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'Córdoba'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- Cundinamarca (117 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Agua de Dios' AS nombre FROM dual UNION ALL
  SELECT 'Albán' AS nombre FROM dual UNION ALL
  SELECT 'Anapoima' AS nombre FROM dual UNION ALL
  SELECT 'Anolaima' AS nombre FROM dual UNION ALL
  SELECT 'Apulo' AS nombre FROM dual UNION ALL
  SELECT 'Arbeláez' AS nombre FROM dual UNION ALL
  SELECT 'Beltrán' AS nombre FROM dual UNION ALL
  SELECT 'Bituima' AS nombre FROM dual UNION ALL
  SELECT 'Bogotá' AS nombre FROM dual UNION ALL
  SELECT 'Bojacá' AS nombre FROM dual UNION ALL
  SELECT 'Cabrera' AS nombre FROM dual UNION ALL
  SELECT 'Cachipay' AS nombre FROM dual UNION ALL
  SELECT 'Cajicá' AS nombre FROM dual UNION ALL
  SELECT 'Caparrapí' AS nombre FROM dual UNION ALL
  SELECT 'Cáqueza' AS nombre FROM dual UNION ALL
  SELECT 'Carmen de Carupa' AS nombre FROM dual UNION ALL
  SELECT 'Chaguaní' AS nombre FROM dual UNION ALL
  SELECT 'Chía' AS nombre FROM dual UNION ALL
  SELECT 'Chipaque' AS nombre FROM dual UNION ALL
  SELECT 'Choachí' AS nombre FROM dual UNION ALL
  SELECT 'Chocontá' AS nombre FROM dual UNION ALL
  SELECT 'Cogua' AS nombre FROM dual UNION ALL
  SELECT 'Cota' AS nombre FROM dual UNION ALL
  SELECT 'Cucunubá' AS nombre FROM dual UNION ALL
  SELECT 'El Colegio' AS nombre FROM dual UNION ALL
  SELECT 'El Peñón' AS nombre FROM dual UNION ALL
  SELECT 'El Rosal' AS nombre FROM dual UNION ALL
  SELECT 'Facatativá' AS nombre FROM dual UNION ALL
  SELECT 'Fómeque' AS nombre FROM dual UNION ALL
  SELECT 'Fosca' AS nombre FROM dual UNION ALL
  SELECT 'Funza' AS nombre FROM dual UNION ALL
  SELECT 'Fúquene' AS nombre FROM dual UNION ALL
  SELECT 'Fusagasugá' AS nombre FROM dual UNION ALL
  SELECT 'Gachalá' AS nombre FROM dual UNION ALL
  SELECT 'Gachancipá' AS nombre FROM dual UNION ALL
  SELECT 'Gachetá' AS nombre FROM dual UNION ALL
  SELECT 'Gama' AS nombre FROM dual UNION ALL
  SELECT 'Girardot' AS nombre FROM dual UNION ALL
  SELECT 'Granada' AS nombre FROM dual UNION ALL
  SELECT 'Guachetá' AS nombre FROM dual UNION ALL
  SELECT 'Guaduas' AS nombre FROM dual UNION ALL
  SELECT 'Guasca' AS nombre FROM dual UNION ALL
  SELECT 'Guataquí' AS nombre FROM dual UNION ALL
  SELECT 'Guatavita' AS nombre FROM dual UNION ALL
  SELECT 'Guayabal de Síquima' AS nombre FROM dual UNION ALL
  SELECT 'Guayabetal' AS nombre FROM dual UNION ALL
  SELECT 'Gutiérrez' AS nombre FROM dual UNION ALL
  SELECT 'Jerusalén' AS nombre FROM dual UNION ALL
  SELECT 'Junín' AS nombre FROM dual UNION ALL
  SELECT 'La Calera' AS nombre FROM dual UNION ALL
  SELECT 'La Mesa' AS nombre FROM dual UNION ALL
  SELECT 'La Palma' AS nombre FROM dual UNION ALL
  SELECT 'La Peña' AS nombre FROM dual UNION ALL
  SELECT 'La Vega' AS nombre FROM dual UNION ALL
  SELECT 'Lenguazaque' AS nombre FROM dual UNION ALL
  SELECT 'Machetá' AS nombre FROM dual UNION ALL
  SELECT 'Madrid' AS nombre FROM dual UNION ALL
  SELECT 'Manta' AS nombre FROM dual UNION ALL
  SELECT 'Medina' AS nombre FROM dual UNION ALL
  SELECT 'Mosquera' AS nombre FROM dual UNION ALL
  SELECT 'Nariño' AS nombre FROM dual UNION ALL
  SELECT 'Nemocón' AS nombre FROM dual UNION ALL
  SELECT 'Nilo' AS nombre FROM dual UNION ALL
  SELECT 'Nimaima' AS nombre FROM dual UNION ALL
  SELECT 'Nocaima' AS nombre FROM dual UNION ALL
  SELECT 'Pacho' AS nombre FROM dual UNION ALL
  SELECT 'Paime' AS nombre FROM dual UNION ALL
  SELECT 'Pandi' AS nombre FROM dual UNION ALL
  SELECT 'Paratebueno' AS nombre FROM dual UNION ALL
  SELECT 'Pasca' AS nombre FROM dual UNION ALL
  SELECT 'Puerto Salgar' AS nombre FROM dual UNION ALL
  SELECT 'Pulí' AS nombre FROM dual UNION ALL
  SELECT 'Quebradanegra' AS nombre FROM dual UNION ALL
  SELECT 'Quetame' AS nombre FROM dual UNION ALL
  SELECT 'Quipile' AS nombre FROM dual UNION ALL
  SELECT 'Ricaurte' AS nombre FROM dual UNION ALL
  SELECT 'San Antonio del Tequendama' AS nombre FROM dual UNION ALL
  SELECT 'San Bernardo' AS nombre FROM dual UNION ALL
  SELECT 'San Cayetano' AS nombre FROM dual UNION ALL
  SELECT 'San Francisco' AS nombre FROM dual UNION ALL
  SELECT 'San Juan de Rioseco' AS nombre FROM dual UNION ALL
  SELECT 'Sasaima' AS nombre FROM dual UNION ALL
  SELECT 'Sesquilé' AS nombre FROM dual UNION ALL
  SELECT 'Sibaté' AS nombre FROM dual UNION ALL
  SELECT 'Silvania' AS nombre FROM dual UNION ALL
  SELECT 'Simijaca' AS nombre FROM dual UNION ALL
  SELECT 'Soacha' AS nombre FROM dual UNION ALL
  SELECT 'Sopó' AS nombre FROM dual UNION ALL
  SELECT 'Subachoque' AS nombre FROM dual UNION ALL
  SELECT 'Suesca' AS nombre FROM dual UNION ALL
  SELECT 'Supatá' AS nombre FROM dual UNION ALL
  SELECT 'Susa' AS nombre FROM dual UNION ALL
  SELECT 'Sutatausa' AS nombre FROM dual UNION ALL
  SELECT 'Tabio' AS nombre FROM dual UNION ALL
  SELECT 'Tausa' AS nombre FROM dual UNION ALL
  SELECT 'Tena' AS nombre FROM dual UNION ALL
  SELECT 'Tenjo' AS nombre FROM dual UNION ALL
  SELECT 'Tibacuy' AS nombre FROM dual UNION ALL
  SELECT 'Tibirita' AS nombre FROM dual UNION ALL
  SELECT 'Tocaima' AS nombre FROM dual UNION ALL
  SELECT 'Tocancipá' AS nombre FROM dual UNION ALL
  SELECT 'Topaipí' AS nombre FROM dual UNION ALL
  SELECT 'Ubalá' AS nombre FROM dual UNION ALL
  SELECT 'Ubaque' AS nombre FROM dual UNION ALL
  SELECT 'Ubaté' AS nombre FROM dual UNION ALL
  SELECT 'Une' AS nombre FROM dual UNION ALL
  SELECT 'Útica' AS nombre FROM dual UNION ALL
  SELECT 'Venecia' AS nombre FROM dual UNION ALL
  SELECT 'Vergara' AS nombre FROM dual UNION ALL
  SELECT 'Vianí' AS nombre FROM dual UNION ALL
  SELECT 'Villagómez' AS nombre FROM dual UNION ALL
  SELECT 'Villapinzón' AS nombre FROM dual UNION ALL
  SELECT 'Villeta' AS nombre FROM dual UNION ALL
  SELECT 'Viotá' AS nombre FROM dual UNION ALL
  SELECT 'Yacopí' AS nombre FROM dual UNION ALL
  SELECT 'Zipacón' AS nombre FROM dual UNION ALL
  SELECT 'Zipaquirá' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'Cundinamarca'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- Guainía (8 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Barranco Minas' AS nombre FROM dual UNION ALL
  SELECT 'Cacahual' AS nombre FROM dual UNION ALL
  SELECT 'Inírida' AS nombre FROM dual UNION ALL
  SELECT 'La Guadalupe' AS nombre FROM dual UNION ALL
  SELECT 'Morichal Nuevo' AS nombre FROM dual UNION ALL
  SELECT 'Pana Pana' AS nombre FROM dual UNION ALL
  SELECT 'Puerto Colombia' AS nombre FROM dual UNION ALL
  SELECT 'San Felipe' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'Guainía'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- Guaviare (4 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Calamar' AS nombre FROM dual UNION ALL
  SELECT 'El Retorno' AS nombre FROM dual UNION ALL
  SELECT 'Miraflores' AS nombre FROM dual UNION ALL
  SELECT 'San José del Guaviare' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'Guaviare'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- Huila (37 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Acevedo' AS nombre FROM dual UNION ALL
  SELECT 'Agrado' AS nombre FROM dual UNION ALL
  SELECT 'Aipe' AS nombre FROM dual UNION ALL
  SELECT 'Algeciras' AS nombre FROM dual UNION ALL
  SELECT 'Altamira' AS nombre FROM dual UNION ALL
  SELECT 'Baraya' AS nombre FROM dual UNION ALL
  SELECT 'Campoalegre' AS nombre FROM dual UNION ALL
  SELECT 'Colombia' AS nombre FROM dual UNION ALL
  SELECT 'Elías' AS nombre FROM dual UNION ALL
  SELECT 'Garzón' AS nombre FROM dual UNION ALL
  SELECT 'Gigante' AS nombre FROM dual UNION ALL
  SELECT 'Guadalupe' AS nombre FROM dual UNION ALL
  SELECT 'Hobo' AS nombre FROM dual UNION ALL
  SELECT 'Íquira' AS nombre FROM dual UNION ALL
  SELECT 'Isnos' AS nombre FROM dual UNION ALL
  SELECT 'La Argentina' AS nombre FROM dual UNION ALL
  SELECT 'La Plata' AS nombre FROM dual UNION ALL
  SELECT 'Nátaga' AS nombre FROM dual UNION ALL
  SELECT 'Neiva' AS nombre FROM dual UNION ALL
  SELECT 'Oporapa' AS nombre FROM dual UNION ALL
  SELECT 'Paicol' AS nombre FROM dual UNION ALL
  SELECT 'Palermo' AS nombre FROM dual UNION ALL
  SELECT 'Palestina' AS nombre FROM dual UNION ALL
  SELECT 'Pital' AS nombre FROM dual UNION ALL
  SELECT 'Pitalito' AS nombre FROM dual UNION ALL
  SELECT 'Rivera' AS nombre FROM dual UNION ALL
  SELECT 'Saladoblanco' AS nombre FROM dual UNION ALL
  SELECT 'San Agustín' AS nombre FROM dual UNION ALL
  SELECT 'Santa María' AS nombre FROM dual UNION ALL
  SELECT 'Suaza' AS nombre FROM dual UNION ALL
  SELECT 'Tarqui' AS nombre FROM dual UNION ALL
  SELECT 'Tesalia' AS nombre FROM dual UNION ALL
  SELECT 'Tello' AS nombre FROM dual UNION ALL
  SELECT 'Teruel' AS nombre FROM dual UNION ALL
  SELECT 'Timaná' AS nombre FROM dual UNION ALL
  SELECT 'Villavieja' AS nombre FROM dual UNION ALL
  SELECT 'Yaguará' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'Huila'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- La Guajira (15 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Albania' AS nombre FROM dual UNION ALL
  SELECT 'Barrancas' AS nombre FROM dual UNION ALL
  SELECT 'Dibulla' AS nombre FROM dual UNION ALL
  SELECT 'Distracción' AS nombre FROM dual UNION ALL
  SELECT 'El Molino' AS nombre FROM dual UNION ALL
  SELECT 'Fonseca' AS nombre FROM dual UNION ALL
  SELECT 'Hatonuevo' AS nombre FROM dual UNION ALL
  SELECT 'La Jagua del Pilar' AS nombre FROM dual UNION ALL
  SELECT 'Maicao' AS nombre FROM dual UNION ALL
  SELECT 'Manaure' AS nombre FROM dual UNION ALL
  SELECT 'Riohacha' AS nombre FROM dual UNION ALL
  SELECT 'San Juan del Cesar' AS nombre FROM dual UNION ALL
  SELECT 'Uribia' AS nombre FROM dual UNION ALL
  SELECT 'Urumita' AS nombre FROM dual UNION ALL
  SELECT 'Villanueva' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'La Guajira'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- Magdalena (30 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Algarrobo' AS nombre FROM dual UNION ALL
  SELECT 'Aracataca' AS nombre FROM dual UNION ALL
  SELECT 'Ariguaní' AS nombre FROM dual UNION ALL
  SELECT 'Cerro de San Antonio' AS nombre FROM dual UNION ALL
  SELECT 'Chibolo' AS nombre FROM dual UNION ALL
  SELECT 'Ciénaga' AS nombre FROM dual UNION ALL
  SELECT 'Concordia' AS nombre FROM dual UNION ALL
  SELECT 'El Banco' AS nombre FROM dual UNION ALL
  SELECT 'El Piñón' AS nombre FROM dual UNION ALL
  SELECT 'El Retén' AS nombre FROM dual UNION ALL
  SELECT 'Fundación' AS nombre FROM dual UNION ALL
  SELECT 'Guamal' AS nombre FROM dual UNION ALL
  SELECT 'Nueva Granada' AS nombre FROM dual UNION ALL
  SELECT 'Pedraza' AS nombre FROM dual UNION ALL
  SELECT 'Pijiño del Carmen' AS nombre FROM dual UNION ALL
  SELECT 'Pivijay' AS nombre FROM dual UNION ALL
  SELECT 'Plato' AS nombre FROM dual UNION ALL
  SELECT 'Puebloviejo' AS nombre FROM dual UNION ALL
  SELECT 'Remolino' AS nombre FROM dual UNION ALL
  SELECT 'Sabanas de San Ángel' AS nombre FROM dual UNION ALL
  SELECT 'Salamina' AS nombre FROM dual UNION ALL
  SELECT 'San Sebastián de Buenavista' AS nombre FROM dual UNION ALL
  SELECT 'Santa Ana' AS nombre FROM dual UNION ALL
  SELECT 'Santa Bárbara de Pinto' AS nombre FROM dual UNION ALL
  SELECT 'Santa Marta' AS nombre FROM dual UNION ALL
  SELECT 'San Zenón' AS nombre FROM dual UNION ALL
  SELECT 'Sitionuevo' AS nombre FROM dual UNION ALL
  SELECT 'Tenerife' AS nombre FROM dual UNION ALL
  SELECT 'Zapayán' AS nombre FROM dual UNION ALL
  SELECT 'Zona Bananera' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'Magdalena'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- Meta (29 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Acacías' AS nombre FROM dual UNION ALL
  SELECT 'Barranca de Upía' AS nombre FROM dual UNION ALL
  SELECT 'Cabuyaro' AS nombre FROM dual UNION ALL
  SELECT 'Castilla la Nueva' AS nombre FROM dual UNION ALL
  SELECT 'Cubarral' AS nombre FROM dual UNION ALL
  SELECT 'Cumaral' AS nombre FROM dual UNION ALL
  SELECT 'El Calvario' AS nombre FROM dual UNION ALL
  SELECT 'El Castillo' AS nombre FROM dual UNION ALL
  SELECT 'El Dorado' AS nombre FROM dual UNION ALL
  SELECT 'Fuente de Oro' AS nombre FROM dual UNION ALL
  SELECT 'Granada' AS nombre FROM dual UNION ALL
  SELECT 'Guamal' AS nombre FROM dual UNION ALL
  SELECT 'La Macarena' AS nombre FROM dual UNION ALL
  SELECT 'Lejanías' AS nombre FROM dual UNION ALL
  SELECT 'Mapiripán' AS nombre FROM dual UNION ALL
  SELECT 'Mesetas' AS nombre FROM dual UNION ALL
  SELECT 'Puerto Concordia' AS nombre FROM dual UNION ALL
  SELECT 'Puerto Gaitán' AS nombre FROM dual UNION ALL
  SELECT 'Puerto Lleras' AS nombre FROM dual UNION ALL
  SELECT 'Puerto López' AS nombre FROM dual UNION ALL
  SELECT 'Puerto Rico' AS nombre FROM dual UNION ALL
  SELECT 'Restrepo' AS nombre FROM dual UNION ALL
  SELECT 'San Carlos de Guaroa' AS nombre FROM dual UNION ALL
  SELECT 'San Juan de Arama' AS nombre FROM dual UNION ALL
  SELECT 'San Juanito' AS nombre FROM dual UNION ALL
  SELECT 'San Martín' AS nombre FROM dual UNION ALL
  SELECT 'Uribe' AS nombre FROM dual UNION ALL
  SELECT 'Villavicencio' AS nombre FROM dual UNION ALL
  SELECT 'Vista Hermosa' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'Meta'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- Nariño (63 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Aldana' AS nombre FROM dual UNION ALL
  SELECT 'Ancuyá' AS nombre FROM dual UNION ALL
  SELECT 'Arboleda' AS nombre FROM dual UNION ALL
  SELECT 'Barbacoas' AS nombre FROM dual UNION ALL
  SELECT 'Belén' AS nombre FROM dual UNION ALL
  SELECT 'Buesaco' AS nombre FROM dual UNION ALL
  SELECT 'Chachagüí' AS nombre FROM dual UNION ALL
  SELECT 'Colón' AS nombre FROM dual UNION ALL
  SELECT 'Consacá' AS nombre FROM dual UNION ALL
  SELECT 'Contadero' AS nombre FROM dual UNION ALL
  SELECT 'Córdoba' AS nombre FROM dual UNION ALL
  SELECT 'Cuaspud' AS nombre FROM dual UNION ALL
  SELECT 'Cumbal' AS nombre FROM dual UNION ALL
  SELECT 'Cumbitara' AS nombre FROM dual UNION ALL
  SELECT 'El Charco' AS nombre FROM dual UNION ALL
  SELECT 'El Peñol' AS nombre FROM dual UNION ALL
  SELECT 'El Rosario' AS nombre FROM dual UNION ALL
  SELECT 'El Tablón de Gómez' AS nombre FROM dual UNION ALL
  SELECT 'El Tambo' AS nombre FROM dual UNION ALL
  SELECT 'Francisco Pizarro' AS nombre FROM dual UNION ALL
  SELECT 'Funes' AS nombre FROM dual UNION ALL
  SELECT 'Guachucal' AS nombre FROM dual UNION ALL
  SELECT 'Guaitarilla' AS nombre FROM dual UNION ALL
  SELECT 'Gualmatán' AS nombre FROM dual UNION ALL
  SELECT 'Iles' AS nombre FROM dual UNION ALL
  SELECT 'Imúes' AS nombre FROM dual UNION ALL
  SELECT 'Ipiales' AS nombre FROM dual UNION ALL
  SELECT 'La Cruz' AS nombre FROM dual UNION ALL
  SELECT 'La Florida' AS nombre FROM dual UNION ALL
  SELECT 'La Llanada' AS nombre FROM dual UNION ALL
  SELECT 'La Tola' AS nombre FROM dual UNION ALL
  SELECT 'La Unión' AS nombre FROM dual UNION ALL
  SELECT 'Leiva' AS nombre FROM dual UNION ALL
  SELECT 'Linares' AS nombre FROM dual UNION ALL
  SELECT 'Los Andes' AS nombre FROM dual UNION ALL
  SELECT 'Magüí Payán' AS nombre FROM dual UNION ALL
  SELECT 'Mallama' AS nombre FROM dual UNION ALL
  SELECT 'Mosquera' AS nombre FROM dual UNION ALL
  SELECT 'Nariño' AS nombre FROM dual UNION ALL
  SELECT 'Olaya Herrera' AS nombre FROM dual UNION ALL
  SELECT 'Ospina' AS nombre FROM dual UNION ALL
  SELECT 'Pasto' AS nombre FROM dual UNION ALL
  SELECT 'Policarpa' AS nombre FROM dual UNION ALL
  SELECT 'Potosí' AS nombre FROM dual UNION ALL
  SELECT 'Providencia' AS nombre FROM dual UNION ALL
  SELECT 'Puerres' AS nombre FROM dual UNION ALL
  SELECT 'Pupiales' AS nombre FROM dual UNION ALL
  SELECT 'Ricaurte' AS nombre FROM dual UNION ALL
  SELECT 'Roberto Payán' AS nombre FROM dual UNION ALL
  SELECT 'Samaniego' AS nombre FROM dual UNION ALL
  SELECT 'San Bernardo' AS nombre FROM dual UNION ALL
  SELECT 'Sandoná' AS nombre FROM dual UNION ALL
  SELECT 'San Lorenzo' AS nombre FROM dual UNION ALL
  SELECT 'San Pablo' AS nombre FROM dual UNION ALL
  SELECT 'San Pedro de Cartago' AS nombre FROM dual UNION ALL
  SELECT 'Santa Bárbara' AS nombre FROM dual UNION ALL
  SELECT 'Santacruz' AS nombre FROM dual UNION ALL
  SELECT 'Sapuyes' AS nombre FROM dual UNION ALL
  SELECT 'Taminango' AS nombre FROM dual UNION ALL
  SELECT 'Tangua' AS nombre FROM dual UNION ALL
  SELECT 'Tumaco' AS nombre FROM dual UNION ALL
  SELECT 'Túquerres' AS nombre FROM dual UNION ALL
  SELECT 'Yacuanquer' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'Nariño'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- Norte de Santander (40 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Ábrego' AS nombre FROM dual UNION ALL
  SELECT 'Arboledas' AS nombre FROM dual UNION ALL
  SELECT 'Bochalema' AS nombre FROM dual UNION ALL
  SELECT 'Bucarasica' AS nombre FROM dual UNION ALL
  SELECT 'Cácota' AS nombre FROM dual UNION ALL
  SELECT 'Cachirá' AS nombre FROM dual UNION ALL
  SELECT 'Chinácota' AS nombre FROM dual UNION ALL
  SELECT 'Chitagá' AS nombre FROM dual UNION ALL
  SELECT 'Convención' AS nombre FROM dual UNION ALL
  SELECT 'Cúcuta' AS nombre FROM dual UNION ALL
  SELECT 'Cucutilla' AS nombre FROM dual UNION ALL
  SELECT 'Durania' AS nombre FROM dual UNION ALL
  SELECT 'El Carmen' AS nombre FROM dual UNION ALL
  SELECT 'El Tarra' AS nombre FROM dual UNION ALL
  SELECT 'El Zulia' AS nombre FROM dual UNION ALL
  SELECT 'Gramalote' AS nombre FROM dual UNION ALL
  SELECT 'Hacarí' AS nombre FROM dual UNION ALL
  SELECT 'Herrán' AS nombre FROM dual UNION ALL
  SELECT 'La Esperanza' AS nombre FROM dual UNION ALL
  SELECT 'La Playa de Belén' AS nombre FROM dual UNION ALL
  SELECT 'Labateca' AS nombre FROM dual UNION ALL
  SELECT 'Los Patios' AS nombre FROM dual UNION ALL
  SELECT 'Lourdes' AS nombre FROM dual UNION ALL
  SELECT 'Mutiscua' AS nombre FROM dual UNION ALL
  SELECT 'Ocaña' AS nombre FROM dual UNION ALL
  SELECT 'Pamplona' AS nombre FROM dual UNION ALL
  SELECT 'Pamplonita' AS nombre FROM dual UNION ALL
  SELECT 'Puerto Santander' AS nombre FROM dual UNION ALL
  SELECT 'Ragonvalia' AS nombre FROM dual UNION ALL
  SELECT 'Salazar de Las Palmas' AS nombre FROM dual UNION ALL
  SELECT 'San Calixto' AS nombre FROM dual UNION ALL
  SELECT 'San Cayetano' AS nombre FROM dual UNION ALL
  SELECT 'Santiago' AS nombre FROM dual UNION ALL
  SELECT 'Sardinata' AS nombre FROM dual UNION ALL
  SELECT 'Silos' AS nombre FROM dual UNION ALL
  SELECT 'Teorama' AS nombre FROM dual UNION ALL
  SELECT 'Tibú' AS nombre FROM dual UNION ALL
  SELECT 'Toledo' AS nombre FROM dual UNION ALL
  SELECT 'Villa Caro' AS nombre FROM dual UNION ALL
  SELECT 'Villa del Rosario' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'Norte de Santander'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- Putumayo (13 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Colón' AS nombre FROM dual UNION ALL
  SELECT 'Mocoa' AS nombre FROM dual UNION ALL
  SELECT 'Orito' AS nombre FROM dual UNION ALL
  SELECT 'Puerto Asís' AS nombre FROM dual UNION ALL
  SELECT 'Puerto Caicedo' AS nombre FROM dual UNION ALL
  SELECT 'Puerto Guzmán' AS nombre FROM dual UNION ALL
  SELECT 'Puerto Leguízamo' AS nombre FROM dual UNION ALL
  SELECT 'San Francisco' AS nombre FROM dual UNION ALL
  SELECT 'San Miguel' AS nombre FROM dual UNION ALL
  SELECT 'Santiago' AS nombre FROM dual UNION ALL
  SELECT 'Sibundoy' AS nombre FROM dual UNION ALL
  SELECT 'Valle del Guamuez' AS nombre FROM dual UNION ALL
  SELECT 'Villagarzón' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'Putumayo'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- Quindío (12 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Armenia' AS nombre FROM dual UNION ALL
  SELECT 'Buenavista' AS nombre FROM dual UNION ALL
  SELECT 'Calarcá' AS nombre FROM dual UNION ALL
  SELECT 'Circasia' AS nombre FROM dual UNION ALL
  SELECT 'Córdoba' AS nombre FROM dual UNION ALL
  SELECT 'Filandia' AS nombre FROM dual UNION ALL
  SELECT 'Génova' AS nombre FROM dual UNION ALL
  SELECT 'La Tebaida' AS nombre FROM dual UNION ALL
  SELECT 'Montenegro' AS nombre FROM dual UNION ALL
  SELECT 'Pijao' AS nombre FROM dual UNION ALL
  SELECT 'Quimbaya' AS nombre FROM dual UNION ALL
  SELECT 'Salento' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'Quindío'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- Risaralda (14 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Apía' AS nombre FROM dual UNION ALL
  SELECT 'Balboa' AS nombre FROM dual UNION ALL
  SELECT 'Belén de Umbría' AS nombre FROM dual UNION ALL
  SELECT 'Dosquebradas' AS nombre FROM dual UNION ALL
  SELECT 'Guática' AS nombre FROM dual UNION ALL
  SELECT 'La Celia' AS nombre FROM dual UNION ALL
  SELECT 'La Virginia' AS nombre FROM dual UNION ALL
  SELECT 'Marsella' AS nombre FROM dual UNION ALL
  SELECT 'Mistrató' AS nombre FROM dual UNION ALL
  SELECT 'Pereira' AS nombre FROM dual UNION ALL
  SELECT 'Pueblo Rico' AS nombre FROM dual UNION ALL
  SELECT 'Quinchía' AS nombre FROM dual UNION ALL
  SELECT 'Santa Rosa de Cabal' AS nombre FROM dual UNION ALL
  SELECT 'Santuario' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'Risaralda'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- San Andrés y Providencia (2 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Providencia y Santa Catalina' AS nombre FROM dual UNION ALL
  SELECT 'San Andrés' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'San Andrés y Providencia'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- Santander (87 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Aguada' AS nombre FROM dual UNION ALL
  SELECT 'Albania' AS nombre FROM dual UNION ALL
  SELECT 'Aratoca' AS nombre FROM dual UNION ALL
  SELECT 'Barbosa' AS nombre FROM dual UNION ALL
  SELECT 'Barichara' AS nombre FROM dual UNION ALL
  SELECT 'Barrancabermeja' AS nombre FROM dual UNION ALL
  SELECT 'Betulia' AS nombre FROM dual UNION ALL
  SELECT 'Bolívar' AS nombre FROM dual UNION ALL
  SELECT 'Bucaramanga' AS nombre FROM dual UNION ALL
  SELECT 'Cabrera' AS nombre FROM dual UNION ALL
  SELECT 'California' AS nombre FROM dual UNION ALL
  SELECT 'Capitanejo' AS nombre FROM dual UNION ALL
  SELECT 'Carcasí' AS nombre FROM dual UNION ALL
  SELECT 'Cépita' AS nombre FROM dual UNION ALL
  SELECT 'Cerrito' AS nombre FROM dual UNION ALL
  SELECT 'Charalá' AS nombre FROM dual UNION ALL
  SELECT 'Charta' AS nombre FROM dual UNION ALL
  SELECT 'Chima' AS nombre FROM dual UNION ALL
  SELECT 'Chipatá' AS nombre FROM dual UNION ALL
  SELECT 'Cimitarra' AS nombre FROM dual UNION ALL
  SELECT 'Concepción' AS nombre FROM dual UNION ALL
  SELECT 'Confines' AS nombre FROM dual UNION ALL
  SELECT 'Contratación' AS nombre FROM dual UNION ALL
  SELECT 'Coromoro' AS nombre FROM dual UNION ALL
  SELECT 'Curití' AS nombre FROM dual UNION ALL
  SELECT 'El Carmen de Chucurí' AS nombre FROM dual UNION ALL
  SELECT 'El Guacamayo' AS nombre FROM dual UNION ALL
  SELECT 'El Peñón' AS nombre FROM dual UNION ALL
  SELECT 'El Playón' AS nombre FROM dual UNION ALL
  SELECT 'Encino' AS nombre FROM dual UNION ALL
  SELECT 'Enciso' AS nombre FROM dual UNION ALL
  SELECT 'Florián' AS nombre FROM dual UNION ALL
  SELECT 'Floridablanca' AS nombre FROM dual UNION ALL
  SELECT 'Galán' AS nombre FROM dual UNION ALL
  SELECT 'Gámbita' AS nombre FROM dual UNION ALL
  SELECT 'Girón' AS nombre FROM dual UNION ALL
  SELECT 'Guaca' AS nombre FROM dual UNION ALL
  SELECT 'Guadalupe' AS nombre FROM dual UNION ALL
  SELECT 'Guapotá' AS nombre FROM dual UNION ALL
  SELECT 'Guavatá' AS nombre FROM dual UNION ALL
  SELECT 'Güepsa' AS nombre FROM dual UNION ALL
  SELECT 'Hato' AS nombre FROM dual UNION ALL
  SELECT 'Jesús María' AS nombre FROM dual UNION ALL
  SELECT 'Jordán' AS nombre FROM dual UNION ALL
  SELECT 'La Belleza' AS nombre FROM dual UNION ALL
  SELECT 'La Paz' AS nombre FROM dual UNION ALL
  SELECT 'Landázuri' AS nombre FROM dual UNION ALL
  SELECT 'Lebrija' AS nombre FROM dual UNION ALL
  SELECT 'Los Santos' AS nombre FROM dual UNION ALL
  SELECT 'Macaravita' AS nombre FROM dual UNION ALL
  SELECT 'Málaga' AS nombre FROM dual UNION ALL
  SELECT 'Matanza' AS nombre FROM dual UNION ALL
  SELECT 'Mogotes' AS nombre FROM dual UNION ALL
  SELECT 'Molagavita' AS nombre FROM dual UNION ALL
  SELECT 'Ocamonte' AS nombre FROM dual UNION ALL
  SELECT 'Oiba' AS nombre FROM dual UNION ALL
  SELECT 'Onzaga' AS nombre FROM dual UNION ALL
  SELECT 'Palmar' AS nombre FROM dual UNION ALL
  SELECT 'Palmas del Socorro' AS nombre FROM dual UNION ALL
  SELECT 'Páramo' AS nombre FROM dual UNION ALL
  SELECT 'Piedecuesta' AS nombre FROM dual UNION ALL
  SELECT 'Pinchote' AS nombre FROM dual UNION ALL
  SELECT 'Puente Nacional' AS nombre FROM dual UNION ALL
  SELECT 'Puerto Parra' AS nombre FROM dual UNION ALL
  SELECT 'Puerto Wilches' AS nombre FROM dual UNION ALL
  SELECT 'Rionegro' AS nombre FROM dual UNION ALL
  SELECT 'Sabana de Torres' AS nombre FROM dual UNION ALL
  SELECT 'San Andrés' AS nombre FROM dual UNION ALL
  SELECT 'San Benito' AS nombre FROM dual UNION ALL
  SELECT 'San Gil' AS nombre FROM dual UNION ALL
  SELECT 'San Joaquín' AS nombre FROM dual UNION ALL
  SELECT 'San José de Miranda' AS nombre FROM dual UNION ALL
  SELECT 'San Miguel' AS nombre FROM dual UNION ALL
  SELECT 'San Vicente de Chucurí' AS nombre FROM dual UNION ALL
  SELECT 'Santa Bárbara' AS nombre FROM dual UNION ALL
  SELECT 'Santa Helena del Opón' AS nombre FROM dual UNION ALL
  SELECT 'Simacota' AS nombre FROM dual UNION ALL
  SELECT 'Socorro' AS nombre FROM dual UNION ALL
  SELECT 'Suaita' AS nombre FROM dual UNION ALL
  SELECT 'Sucre' AS nombre FROM dual UNION ALL
  SELECT 'Suratá' AS nombre FROM dual UNION ALL
  SELECT 'Tona' AS nombre FROM dual UNION ALL
  SELECT 'Valle de San José' AS nombre FROM dual UNION ALL
  SELECT 'Vélez' AS nombre FROM dual UNION ALL
  SELECT 'Vetas' AS nombre FROM dual UNION ALL
  SELECT 'Villanueva' AS nombre FROM dual UNION ALL
  SELECT 'Zapatoca' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'Santander'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- Sucre (26 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Buenavista' AS nombre FROM dual UNION ALL
  SELECT 'Caimito' AS nombre FROM dual UNION ALL
  SELECT 'Chalán' AS nombre FROM dual UNION ALL
  SELECT 'Colosó' AS nombre FROM dual UNION ALL
  SELECT 'Corozal' AS nombre FROM dual UNION ALL
  SELECT 'Coveñas' AS nombre FROM dual UNION ALL
  SELECT 'El Roble' AS nombre FROM dual UNION ALL
  SELECT 'Galeras' AS nombre FROM dual UNION ALL
  SELECT 'Guaranda' AS nombre FROM dual UNION ALL
  SELECT 'La Unión' AS nombre FROM dual UNION ALL
  SELECT 'Los Palmitos' AS nombre FROM dual UNION ALL
  SELECT 'Majagual' AS nombre FROM dual UNION ALL
  SELECT 'Morroa' AS nombre FROM dual UNION ALL
  SELECT 'Ovejas' AS nombre FROM dual UNION ALL
  SELECT 'Palmito' AS nombre FROM dual UNION ALL
  SELECT 'Sampués' AS nombre FROM dual UNION ALL
  SELECT 'San Benito Abad' AS nombre FROM dual UNION ALL
  SELECT 'San Juan de Betulia' AS nombre FROM dual UNION ALL
  SELECT 'San Marcos' AS nombre FROM dual UNION ALL
  SELECT 'San Onofre' AS nombre FROM dual UNION ALL
  SELECT 'San Pedro' AS nombre FROM dual UNION ALL
  SELECT 'Sincé' AS nombre FROM dual UNION ALL
  SELECT 'Sincelejo' AS nombre FROM dual UNION ALL
  SELECT 'Sucre' AS nombre FROM dual UNION ALL
  SELECT 'Tolú' AS nombre FROM dual UNION ALL
  SELECT 'Toluviejo' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'Sucre'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- Tolima (47 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Alpujarra' AS nombre FROM dual UNION ALL
  SELECT 'Alvarado' AS nombre FROM dual UNION ALL
  SELECT 'Ambalema' AS nombre FROM dual UNION ALL
  SELECT 'Anzoátegui' AS nombre FROM dual UNION ALL
  SELECT 'Armero' AS nombre FROM dual UNION ALL
  SELECT 'Ataco' AS nombre FROM dual UNION ALL
  SELECT 'Cajamarca' AS nombre FROM dual UNION ALL
  SELECT 'Carmen de Apicalá' AS nombre FROM dual UNION ALL
  SELECT 'Casabianca' AS nombre FROM dual UNION ALL
  SELECT 'Chaparral' AS nombre FROM dual UNION ALL
  SELECT 'Coello' AS nombre FROM dual UNION ALL
  SELECT 'Coyaima' AS nombre FROM dual UNION ALL
  SELECT 'Cunday' AS nombre FROM dual UNION ALL
  SELECT 'Dolores' AS nombre FROM dual UNION ALL
  SELECT 'Espinal' AS nombre FROM dual UNION ALL
  SELECT 'Falán' AS nombre FROM dual UNION ALL
  SELECT 'Flandes' AS nombre FROM dual UNION ALL
  SELECT 'Fresno' AS nombre FROM dual UNION ALL
  SELECT 'Guamo' AS nombre FROM dual UNION ALL
  SELECT 'Herveo' AS nombre FROM dual UNION ALL
  SELECT 'Honda' AS nombre FROM dual UNION ALL
  SELECT 'Ibagué' AS nombre FROM dual UNION ALL
  SELECT 'Icononzo' AS nombre FROM dual UNION ALL
  SELECT 'Lérida' AS nombre FROM dual UNION ALL
  SELECT 'Líbano' AS nombre FROM dual UNION ALL
  SELECT 'Mariquita' AS nombre FROM dual UNION ALL
  SELECT 'Melgar' AS nombre FROM dual UNION ALL
  SELECT 'Murillo' AS nombre FROM dual UNION ALL
  SELECT 'Natagaima' AS nombre FROM dual UNION ALL
  SELECT 'Ortega' AS nombre FROM dual UNION ALL
  SELECT 'Palocabildo' AS nombre FROM dual UNION ALL
  SELECT 'Piedras' AS nombre FROM dual UNION ALL
  SELECT 'Planadas' AS nombre FROM dual UNION ALL
  SELECT 'Prado' AS nombre FROM dual UNION ALL
  SELECT 'Purificación' AS nombre FROM dual UNION ALL
  SELECT 'Rioblanco' AS nombre FROM dual UNION ALL
  SELECT 'Roncesvalles' AS nombre FROM dual UNION ALL
  SELECT 'Rovira' AS nombre FROM dual UNION ALL
  SELECT 'Saldaña' AS nombre FROM dual UNION ALL
  SELECT 'San Antonio' AS nombre FROM dual UNION ALL
  SELECT 'San Luis' AS nombre FROM dual UNION ALL
  SELECT 'Santa Isabel' AS nombre FROM dual UNION ALL
  SELECT 'Suárez' AS nombre FROM dual UNION ALL
  SELECT 'Valle de San Juan' AS nombre FROM dual UNION ALL
  SELECT 'Venadillo' AS nombre FROM dual UNION ALL
  SELECT 'Villahermosa' AS nombre FROM dual UNION ALL
  SELECT 'Villarrica' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'Tolima'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- Valle del Cauca (41 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Alcalá' AS nombre FROM dual UNION ALL
  SELECT 'Andalucía' AS nombre FROM dual UNION ALL
  SELECT 'Ansermanuevo' AS nombre FROM dual UNION ALL
  SELECT 'Argonautas' AS nombre FROM dual UNION ALL
  SELECT 'Buenaventura' AS nombre FROM dual UNION ALL
  SELECT 'Buga' AS nombre FROM dual UNION ALL
  SELECT 'Bugalagrande' AS nombre FROM dual UNION ALL
  SELECT 'Caicedonia' AS nombre FROM dual UNION ALL
  SELECT 'Cali' AS nombre FROM dual UNION ALL
  SELECT 'Calima' AS nombre FROM dual UNION ALL
  SELECT 'Candelaria' AS nombre FROM dual UNION ALL
  SELECT 'Cartago' AS nombre FROM dual UNION ALL
  SELECT 'Dagua' AS nombre FROM dual UNION ALL
  SELECT 'El Águila' AS nombre FROM dual UNION ALL
  SELECT 'El Cairo' AS nombre FROM dual UNION ALL
  SELECT 'El Cerrito' AS nombre FROM dual UNION ALL
  SELECT 'El Dovio' AS nombre FROM dual UNION ALL
  SELECT 'Florida' AS nombre FROM dual UNION ALL
  SELECT 'Ginebra' AS nombre FROM dual UNION ALL
  SELECT 'Guacarí' AS nombre FROM dual UNION ALL
  SELECT 'Jamundí' AS nombre FROM dual UNION ALL
  SELECT 'La Cumbre' AS nombre FROM dual UNION ALL
  SELECT 'La Unión' AS nombre FROM dual UNION ALL
  SELECT 'La Victoria' AS nombre FROM dual UNION ALL
  SELECT 'Obando' AS nombre FROM dual UNION ALL
  SELECT 'Palmira' AS nombre FROM dual UNION ALL
  SELECT 'Pradera' AS nombre FROM dual UNION ALL
  SELECT 'Restrepo' AS nombre FROM dual UNION ALL
  SELECT 'Riofrío' AS nombre FROM dual UNION ALL
  SELECT 'Roldanillo' AS nombre FROM dual UNION ALL
  SELECT 'San Pedro' AS nombre FROM dual UNION ALL
  SELECT 'Sevilla' AS nombre FROM dual UNION ALL
  SELECT 'Toro' AS nombre FROM dual UNION ALL
  SELECT 'Trujillo' AS nombre FROM dual UNION ALL
  SELECT 'Tuluá' AS nombre FROM dual UNION ALL
  SELECT 'Ulloa' AS nombre FROM dual UNION ALL
  SELECT 'Versalles' AS nombre FROM dual UNION ALL
  SELECT 'Vijes' AS nombre FROM dual UNION ALL
  SELECT 'Yotoco' AS nombre FROM dual UNION ALL
  SELECT 'Yumbo' AS nombre FROM dual UNION ALL
  SELECT 'Zarzal' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'Valle del Cauca'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- Vaupés (6 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Carurú' AS nombre FROM dual UNION ALL
  SELECT 'Mitú' AS nombre FROM dual UNION ALL
  SELECT 'PacoA' AS nombre FROM dual UNION ALL
  SELECT 'Papunaua' AS nombre FROM dual UNION ALL
  SELECT 'Taraira' AS nombre FROM dual UNION ALL
  SELECT 'Yavaraté' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'Vaupés'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

-- Vichada (4 municipios)
INSERT INTO MUNICIPIO (NOMBRE_MUNICIPIO, ID_DEPARTAMENTO)
SELECT v.nombre, dep.ID_DEPARTAMENTO
FROM (
  SELECT 'Cumaribo' AS nombre FROM dual UNION ALL
  SELECT 'La Primavera' AS nombre FROM dual UNION ALL
  SELECT 'Puerto Carreño' AS nombre FROM dual UNION ALL
  SELECT 'Santa Rosalía' AS nombre FROM dual
) v
JOIN DEPARTAMENTO dep ON dep.NOMBRE_DEPARTAMENTO = 'Vichada'
WHERE NOT EXISTS (
  SELECT 1 FROM MUNICIPIO m2 WHERE m2.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO AND m2.NOMBRE_MUNICIPIO = v.nombre
);

COMMIT;
