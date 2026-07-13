// ✅ Validación en tiempo real del módulo "Mi Perfil" (disponible para todos los roles):
// correo/teléfono con borde rojo/verde, y un checklist en vivo para la contraseña nueva que
// replica la política real del backend (ValidacionCampos.validarContrasena): 1 mayúscula,
// 1 minúscula, 2 números y 1 carácter especial (!@#$%). También avisa si las dos contraseñas
// nuevas no coinciden y si falta la contraseña actual para poder cambiarla.
// Es solo feedback visual: el servidor vuelve a validar todo al guardar.
document.addEventListener('DOMContentLoaded', function () {

    const correoValido = function (valor) {
        const v = valor.trim();
        const arroba = v.indexOf('@');
        return /^[^@\s]+@[^@\s]+$/.test(v) && arroba > 0 && v.substring(arroba).includes('.co');
    };

    const telefonoValido = function (valor) {
        return /^\d+$/.test(valor.trim());
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
        if (campo.value === '') return; // vacío: estado neutro (estos campos no son obligatorios)
        contenedor.classList.add(cumple ? 'input-valido' : 'input-invalido');
    };

    const conectarSimple = function (idCampo, regla) {
        const campo = document.getElementById(idCampo);
        if (!campo) return;
        campo.addEventListener('input', function () {
            pintarContenedor(campo, regla(campo.value));
        });
    };

    conectarSimple('correoElectronico', correoValido);
    conectarSimple('telefono', telefonoValido);

    // 🎯 Marca una regla del checklist: ✓ verde si cumple, ○ gris mientras no
    const marcarRegla = function (idIcono, cumple) {
        const icono = document.getElementById(idIcono);
        if (!icono) return;
        icono.textContent = cumple ? '✓' : '○';
        icono.parentElement.style.color = cumple ? 'var(--verde-sena)' : 'var(--text-muted)';
        icono.style.color = cumple ? 'var(--verde-sena)' : '#94A3B8';
    };

    const campoActual = document.getElementById('contrasenaActual');
    const campoNueva = document.getElementById('contrasenaNueva');
    const campoConfirmar = document.getElementById('contrasenaConfirmar');
    const hintActual = document.getElementById('hintActual');
    const matchTexto = document.getElementById('matchContrasena');

    const actualizarHintActual = function () {
        if (!hintActual || !campoActual) return;
        hintActual.style.display = (campoNueva.value !== '' && campoActual.value === '') ? 'block' : 'none';
    };

    const actualizarConfirmacion = function () {
        if (!campoConfirmar || !matchTexto) return;
        if (campoConfirmar.value === '') {
            campoConfirmar.closest('.input-container').classList.remove('input-valido', 'input-invalido');
            matchTexto.style.display = 'none';
            return;
        }
        const coincide = campoConfirmar.value === campoNueva.value;
        pintarContenedor(campoConfirmar, coincide);
        matchTexto.style.display = 'block';
        matchTexto.textContent = coincide ? '✓ Las contraseñas coinciden' : '✗ Las contraseñas no coinciden';
        matchTexto.style.color = coincide ? 'var(--verde-sena)' : '#DC2626';
    };

    if (campoNueva) {
        campoNueva.addEventListener('input', function () {
            const reglas = reglasContrasena(campoNueva.value);
            marcarRegla('reglaMay', reglas.mayuscula);
            marcarRegla('reglaMin', reglas.minuscula);
            marcarRegla('reglaNum', reglas.dosNumeros);
            marcarRegla('reglaEsp', reglas.especial);

            const todasOk = reglas.mayuscula && reglas.minuscula && reglas.dosNumeros && reglas.especial;
            pintarContenedor(campoNueva, todasOk);

            actualizarHintActual();
            if (campoConfirmar.value !== '') actualizarConfirmacion();
        });
    }

    if (campoActual) {
        campoActual.addEventListener('input', actualizarHintActual);
    }

    if (campoConfirmar) {
        campoConfirmar.addEventListener('input', actualizarConfirmacion);
    }

    // 🛑 Cinturón y tirantes: si intenta guardar con la contraseña nueva incompleta o sin
    // confirmar/coincidir, se detiene el envío en el navegador (el servidor igual la revalida).
    const form = document.getElementById('formPerfil');
    if (form) {
        form.addEventListener('submit', function (evento) {
            if (campoNueva.value === '') return; // no está cambiando la contraseña: nada que validar aquí

            const reglas = reglasContrasena(campoNueva.value);
            const todasOk = reglas.mayuscula && reglas.minuscula && reglas.dosNumeros && reglas.especial;

            if (!todasOk) {
                evento.preventDefault();
                alert('La contraseña nueva no cumple la política: 1 mayúscula, 1 minúscula, 2 números y 1 carácter especial (! @ # $ %).');
                return;
            }
            if (campoActual.value === '') {
                evento.preventDefault();
                alert('Debes ingresar tu contraseña actual para cambiarla.');
                return;
            }
            if (campoConfirmar.value !== campoNueva.value) {
                evento.preventDefault();
                alert('Las contraseñas nuevas no coinciden.');
            }
        });
    }
});
