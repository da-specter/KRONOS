package com.etapa_productiva.kronos.service;

import com.etapa_productiva.kronos.entity.ConfiguracionGlobal;
import com.etapa_productiva.kronos.repository.ConfiguracionGlobalRepository;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ⚙️ Configuración Global del Administrador: variables del sistema ajustables "en caliente".
 * Los valores se leen de Oracle en cada consulta (sin caché), así que un cambio desde
 * /admin/config aplica de inmediato sin reiniciar la aplicación.
 */
@Service
public class ConfiguracionGlobalService {

    // Claves oficiales usadas por el resto del sistema
    public static final String DIAS_ALERTA_INSTRUCTOR = "DIAS_ALERTA_INSTRUCTOR";
    public static final String DIAS_ALERTA_APRENDIZ = "DIAS_ALERTA_APRENDIZ";
    public static final String DIAS_PRIMERA_VISITA = "DIAS_PRIMERA_VISITA";
    public static final String DIAS_ATRASO_BITACORA = "DIAS_ATRASO_BITACORA";
    public static final String MODO_MANTENIMIENTO = "MODO_MANTENIMIENTO";
    public static final String MENSAJE_MANTENIMIENTO = "MENSAJE_MANTENIMIENTO";

    @Autowired
    private ConfiguracionGlobalRepository configuracionGlobalRepository;

    // 🌱 Siembra las variables por defecto la primera vez que arranca la aplicación,
    // sin pisar los valores que el Administrador ya haya ajustado.
    @PostConstruct
    public void sembrarValoresPorDefecto() {
        try {
            crearSiNoExiste(DIAS_ALERTA_INSTRUCTOR, "3",
                    "Días de anticipación con que se alerta al Instructor de Seguimiento sobre una visita");
            crearSiNoExiste(DIAS_ALERTA_APRENDIZ, "2",
                    "Días de anticipación con que se alerta al Aprendiz sobre una visita");
            crearSiNoExiste(DIAS_PRIMERA_VISITA, "15",
                    "Días desde el inicio de la Etapa Productiva para alertar al Instructor de Seguimiento que agende la primera visita");
            crearSiNoExiste(DIAS_ATRASO_BITACORA, "0",
                    "Días de gracia después de la fecha límite de una bitácora antes de alertar a Instructor y Aprendiz por atraso");
            crearSiNoExiste(MODO_MANTENIMIENTO, "NO",
                    "SI = solo los Administradores pueden iniciar sesión en el portal");
            crearSiNoExiste(MENSAJE_MANTENIMIENTO, "El portal KRONOS está en mantenimiento. Intenta más tarde.",
                    "Mensaje que ven los usuarios cuando el portal está en mantenimiento");
        } catch (Exception e) {
            // Si Oracle aún no está disponible en el arranque, la siembra se reintenta al primer uso
            System.out.println("⚠️ [CONFIG] No se pudieron sembrar los valores por defecto: " + e.getMessage());
        }
    }

    private void crearSiNoExiste(String clave, String valor, String descripcion) {
        if (configuracionGlobalRepository.findByClave(clave).isEmpty()) {
            configuracionGlobalRepository.save(ConfiguracionGlobal.builder()
                    .clave(clave).valor(valor).descripcion(descripcion).build());
        }
    }

    @Transactional(readOnly = true)
    public List<ConfiguracionGlobal> listar() {
        return configuracionGlobalRepository.findAllByOrderByClaveAsc();
    }

    @Transactional(readOnly = true)
    public String getValor(String clave, String porDefecto) {
        return configuracionGlobalRepository.findByClave(clave)
                .map(ConfiguracionGlobal::getValor)
                .orElse(porDefecto);
    }

    @Transactional(readOnly = true)
    public int getEntero(String clave, int porDefecto) {
        try {
            return Integer.parseInt(getValor(clave, String.valueOf(porDefecto)).trim());
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }

    // Interpreta SI/NO (también acepta true/false y 1/0) para los interruptores del sistema
    @Transactional(readOnly = true)
    public boolean getBooleano(String clave, boolean porDefecto) {
        String valor = getValor(clave, porDefecto ? "SI" : "NO").trim();
        return valor.equalsIgnoreCase("SI") || valor.equalsIgnoreCase("TRUE") || valor.equals("1");
    }

    @Transactional
    public ConfiguracionGlobal actualizar(Long idConfiguracion, String valor) {
        ConfiguracionGlobal config = configuracionGlobalRepository.findById(idConfiguracion)
                .orElseThrow(() -> new IllegalArgumentException("La variable de configuración no existe."));
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El valor de la variable no puede quedar vacío.");
        }
        config.setValor(valor.trim());
        return configuracionGlobalRepository.save(config);
    }
}
