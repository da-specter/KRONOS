// ✅ Validación visual en tiempo real del formulario "Crear Cuenta":
// - Correo: mismo criterio de todo KRONOS (debe tener '@' y un '.co' en el dominio).
// - Contraseña: misma política fuerte del backend (ValidacionCampos.validarContrasena):
//   1 mayúscula, 1 minúscula, 2 números y 1 carácter especial (!@#$%), con un checklist
//   que se enciende regla por regla mientras el aprendiz/instructor escribe.
// - Confirmar contraseña: avisa en vivo si coincide o no con la contraseña elegida.
// Es solo feedback visual: el servidor vuelve a validar todo al enviar el formulario.
document.addEventListener('DOMContentLoaded', function () {

    const correoValido = function (valor) {
        const v = valor.trim();
        const arroba = v.indexOf('@');
        return /^[^@\s]+@[^@\s]+$/.test(v) && arroba > 0 && v.substring(arroba).includes('.co');
    };

    const reglasContrasena = function (valor) {
        return {
            mayuscula: /[A-Z]/.test(valor),
            minuscula: /[a-z]/.test(valor),
            dosNumeros: (valor.match(/\d/g) || []).length >= 2,
            especial: /[!@#$%]/.test(valor)
        };
    };

    const pintarContenedor = function (campo, cumple) {
        const contenedor = campo.closest('.input-container');
        if (!contenedor) return;
        contenedor.classList.remove('input-valido', 'input-invalido');
        if (campo.value === '') return; // vacío: estado neutro
        contenedor.classList.add(cumple ? 'input-valido' : 'input-invalido');
    };

    // 📧 Correo: borde rojo/verde neón según cumpla el formato
    const conectarCorreo = function (idCampo) {
        const campo = document.getElementById(idCampo);
        if (!campo) return;
        campo.addEventListener('input', function () {
            pintarContenedor(campo, correoValido(campo.value));
        });
    };

    // 🎯 Marca una regla del checklist: ✓ verde si cumple, ○ gris mientras no
    const marcarRegla = function (idIcono, cumple) {
        const icono = document.getElementById(idIcono);
        if (!icono) return;
        icono.textContent = cumple ? '✓' : '○';
        icono.parentElement.style.color = cumple ? 'var(--verde-oscuro)' : 'var(--text-muted)';
        icono.style.color = cumple ? 'var(--verde-oscuro)' : '#94A3B8';
    };

    // 🔐 Contraseña + checklist de reglas + comparación en vivo con "Confirmar contraseña"
    const conectarContrasena = function (idContrasena, idConfirmar, sufijoReglas) {
        const campo = document.getElementById(idContrasena);
        const confirmar = document.getElementById(idConfirmar);
        if (!campo) return;

        const idsReglas = {
            mayuscula: 'reglaMay' + sufijoReglas,
            minuscula: 'reglaMin' + sufijoReglas,
            dosNumeros: 'reglaNum' + sufijoReglas,
            especial: 'reglaEsp' + sufijoReglas
        };

        const actualizarConfirmacion = function () {
            if (!confirmar) return;
            const texto = document.getElementById('match' + sufijoReglas);
            if (confirmar.value === '') {
                pintarContenedor(confirmar, false);
                confirmar.closest('.input-container').classList.remove('input-valido', 'input-invalido');
                if (texto) texto.style.display = 'none';
                return;
            }
            const coincide = confirmar.value === campo.value;
            pintarContenedor(confirmar, coincide);
            if (texto) {
                texto.style.display = 'block';
                texto.textContent = coincide ? '✓ Las contraseñas coinciden' : '✗ Las contraseñas no coinciden';
                texto.style.color = coincide ? 'var(--verde-oscuro)' : '#DC2626';
            }
        };

        campo.addEventListener('input', function () {
            const reglas = reglasContrasena(campo.value);
            marcarRegla(idsReglas.mayuscula, reglas.mayuscula);
            marcarRegla(idsReglas.minuscula, reglas.minuscula);
            marcarRegla(idsReglas.dosNumeros, reglas.dosNumeros);
            marcarRegla(idsReglas.especial, reglas.especial);

            const todasOk = reglas.mayuscula && reglas.minuscula && reglas.dosNumeros && reglas.especial;
            pintarContenedor(campo, todasOk);

            if (confirmar && confirmar.value !== '') actualizarConfirmacion();
        });

        if (confirmar) {
            confirmar.addEventListener('input', actualizarConfirmacion);
        }
    };

    conectarCorreo('correoAprendiz');
    conectarCorreo('correoInstructor');

    conectarContrasena('contrasenaAprendiz', 'confirmarAprendiz', 'Aprendiz');
    conectarContrasena('contrasenaInstructor', 'confirmarInstructor', 'Instructor');
});
