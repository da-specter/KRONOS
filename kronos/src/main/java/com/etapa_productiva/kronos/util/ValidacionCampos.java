package com.etapa_productiva.kronos.util;

/**
 * ✅ Reglas de validación de campos compartidas por todos los formularios de KRONOS
 * (registro de usuarios, perfil, restablecer contraseña, registro de etapa productiva).
 * Cada método lanza IllegalArgumentException con un mensaje claro para el usuario;
 * los campos opcionales (null o en blanco) se aceptan sin validar.
 */
public final class ValidacionCampos {

    private ValidacionCampos() {
    }

    /** Correo: debe contener '@' y un '.co' en el dominio (cubre .co, .com, .edu.co, .com.co). */
    public static void validarCorreo(String correo, String etiqueta) {
        if (correo == null || correo.isBlank()) return;
        String valor = correo.trim();
        int arroba = valor.indexOf('@');
        boolean formaBasica = valor.matches("^[^@\\s]+@[^@\\s]+$");
        boolean dominioConCo = arroba > 0 && valor.substring(arroba).contains(".co");
        if (!formaBasica || !dominioConCo) {
            throw new IllegalArgumentException(etiqueta + " debe ser un correo válido con '@' y dominio '.co' (ej: usuario@sena.edu.co).");
        }
    }

    /** Teléfono: solo dígitos (sin letras, espacios ni símbolos). */
    public static void validarTelefono(String telefono, String etiqueta) {
        if (telefono == null || telefono.isBlank()) return;
        if (!telefono.trim().matches("^\\d+$")) {
            throw new IllegalArgumentException(etiqueta + " solo puede contener números.");
        }
    }

    /** NIT de empresa: solo dígitos. */
    public static void validarNit(String nit) {
        if (nit == null || nit.isBlank()) return;
        if (!nit.trim().matches("^\\d+$")) {
            throw new IllegalArgumentException("El NIT de la empresa solo puede contener números.");
        }
    }

    /** Nombre/apellido: solo letras (incluye tildes y ñ) y espacios entre nombres compuestos. */
    public static void validarNombre(String valor, String etiqueta) {
        if (valor == null || valor.isBlank()) return;
        if (!valor.trim().matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ]+( [a-zA-ZáéíóúÁÉÍÓÚñÑüÜ]+)*$")) {
            throw new IllegalArgumentException(etiqueta + " solo puede contener letras.");
        }
    }

    /** Documento de identidad: solo dígitos. */
    public static void validarDocumento(String documento) {
        if (documento == null || documento.isBlank()) return;
        if (!documento.trim().matches("^\\d+$")) {
            throw new IllegalArgumentException("El documento solo puede contener números.");
        }
    }

    /**
     * Contraseña nueva: al menos una mayúscula, una minúscula, dos dígitos
     * y un carácter especial (! @ # $ %).
     */
    public static void validarContrasena(String contrasena) {
        if (contrasena == null || contrasena.isBlank()) return;
        boolean mayuscula = contrasena.matches(".*[A-Z].*");
        boolean minuscula = contrasena.matches(".*[a-z].*");
        boolean dosNumeros = contrasena.replaceAll("\\D", "").length() >= 2;
        boolean especial = contrasena.matches(".*[!@#$%].*");
        if (!mayuscula || !minuscula || !dosNumeros || !especial) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos una mayúscula, una minúscula, dos números y un carácter especial (! @ # $ %).");
        }
    }
}
