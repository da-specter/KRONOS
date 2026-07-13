package com.etapa_productiva.kronos.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * 📱 Envío de recordatorios por WhatsApp (Meta Cloud API) para las alertas de visitas de
 * seguimiento y bitácoras atrasadas/por vencer. A diferencia del correo, WhatsApp NO permite
 * texto libre en mensajes que el sistema inicia: hay que usar una PLANTILLA previamente creada
 * y aprobada en Meta Business Manager (con sus variables {{1}}, {{2}}... ya definidas).
 *
 * El bean queda inactivo (solo deja constancia en el log) hasta que se configuren
 * whatsapp.api.token y whatsapp.api.phone-number-id — mismo patrón de "apagado por defecto"
 * que EmailService, para no romper el flujo de la aplicación mientras no exista la cuenta real.
 */
@Service
public class WhatsAppService {

    @Value("${whatsapp.api.token:}")
    private String token;

    @Value("${whatsapp.api.phone-number-id:}")
    private String idNumeroTelefono;

    @Value("${whatsapp.api.version:v21.0}")
    private String versionApi;

    // 🇨🇴 Prefijo que se antepone a un teléfono guardado en formato local (10 dígitos, sin
    // indicativo) para armar el formato internacional que exige la API de WhatsApp.
    @Value("${whatsapp.pais.codigo:57}")
    private String codigoPais;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // 📊 Para que el Administrador pueda ver en algún panel futuro si el canal está activo,
    // igual que EmailService.estaHabilitado()
    public boolean estaHabilitado() {
        return token != null && !token.isBlank() && idNumeroTelefono != null && !idNumeroTelefono.isBlank();
    }

    /**
     * Envía una plantilla aprobada por Meta al número indicado.
     *
     * @param telefonoDestino guardado en Usuario.telefono (normalmente 10 dígitos locales,
     *                        sin indicativo de país); se normaliza automáticamente.
     * @param nombrePlantilla nombre EXACTO de la plantilla ya aprobada en Meta Business Manager.
     * @param codigoIdioma    ej. "es" o "es_CO", el mismo con el que se aprobó la plantilla.
     * @param parametros      valores que reemplazan, en orden, las variables {{1}}, {{2}}...
     *                        del cuerpo de la plantilla.
     */
    public boolean enviarPlantillaSiHabilitado(String telefonoDestino, String nombrePlantilla,
                                                String codigoIdioma, List<String> parametros) {
        if (!estaHabilitado()) {
            System.out.println("📱 [WHATSAPP] Envío deshabilitado (sin credenciales de Meta configuradas). "
                    + "Destino: " + telefonoDestino + " | Plantilla: " + nombrePlantilla);
            return false;
        }

        String numeroNormalizado = normalizarNumero(telefonoDestino);
        if (numeroNormalizado == null) {
            return false;
        }

        try {
            String cuerpoJson = construirCuerpoPlantilla(numeroNormalizado, nombrePlantilla, codigoIdioma, parametros);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://graph.facebook.com/" + versionApi + "/" + idNumeroTelefono + "/messages"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpoJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return true;
            }
            System.out.println("⚠️ [WHATSAPP] Meta respondió " + response.statusCode() + " al enviar a "
                    + numeroNormalizado + ": " + response.body());
            return false;
        } catch (Exception e) {
            System.out.println("⚠️ [WHATSAPP] No se pudo enviar el mensaje a " + numeroNormalizado + ": " + e.getMessage());
            return false;
        }
    }

    // Si el número ya viene con indicativo (más de 10 dígitos) lo deja igual; si es un número
    // local de 10 dígitos (formato típico guardado en Usuario.telefono) le antepone el código
    // de país configurado. Sin este ajuste, Meta rechaza el número por no ser E.164 válido.
    private String normalizarNumero(String telefono) {
        if (telefono == null || telefono.isBlank()) {
            return null;
        }
        String soloDigitos = telefono.replaceAll("\\D", "");
        if (soloDigitos.isEmpty()) {
            return null;
        }
        return soloDigitos.length() <= 10 ? codigoPais + soloDigitos : soloDigitos;
    }

    // Arma a mano el JSON que espera la API de plantillas de Meta (sin traer una librería nueva
    // solo para esto): un mensaje de tipo "template" con un único componente de cuerpo, cuyos
    // parámetros de texto reemplazan {{1}}, {{2}}... en el orden en que llegan.
    private String construirCuerpoPlantilla(String numeroDestino, String nombrePlantilla, String codigoIdioma, List<String> parametros) {
        StringBuilder parametrosJson = new StringBuilder();
        for (int i = 0; i < parametros.size(); i++) {
            if (i > 0) parametrosJson.append(',');
            parametrosJson.append("{\"type\":\"text\",\"text\":\"").append(escaparJson(parametros.get(i))).append("\"}");
        }

        return "{"
                + "\"messaging_product\":\"whatsapp\","
                + "\"to\":\"" + escaparJson(numeroDestino) + "\","
                + "\"type\":\"template\","
                + "\"template\":{"
                + "\"name\":\"" + escaparJson(nombrePlantilla) + "\","
                + "\"language\":{\"code\":\"" + escaparJson(codigoIdioma) + "\"},"
                + "\"components\":[{\"type\":\"body\",\"parameters\":[" + parametrosJson + "]}]"
                + "}"
                + "}";
    }

    private String escaparJson(String valor) {
        return valor == null ? "" : valor.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
