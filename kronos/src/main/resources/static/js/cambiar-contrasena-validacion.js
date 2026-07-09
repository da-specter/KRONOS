// ✅ Validación visual en tiempo real del cambio de contraseña (mismo mecanismo que login-validacion.js):
// pinta el borde del campo en rojo mientras no cumple la regla y en verde cuando cumple.
// Es solo feedback visual: el servidor vuelve a validar todo al enviar el formulario.
document.addEventListener('DOMContentLoaded', function () {

    // Misma política fuerte que exige el servidor (ValidacionCampos.validarContrasena):
    // al menos una mayúscula, una minúscula, dos dígitos y un carácter especial (! @ # $ %).
    const nuevaContrasenaValida = function (valor) {
        const mayuscula = /[A-Z]/.test(valor);
        const minuscula = /[a-z]/.test(valor);
        const dosNumeros = (valor.match(/\d/g) || []).length >= 2;
        const especial = /[!@#$%]/.test(valor);
        return mayuscula && minuscula && dosNumeros && especial;
    };

    const conectar = function (idCampo, regla) {
        const campo = document.getElementById(idCampo);
        if (!campo) return null;
        const contenedor = campo.closest('.input-container');
        if (!contenedor) return null;

        const evaluar = function () {
            contenedor.classList.remove('input-valido', 'input-invalido');
            if (campo.value === '') return; // vacío: vuelve al estado neutro
            contenedor.classList.add(regla(campo.value) ? 'input-valido' : 'input-invalido');
        };
        campo.addEventListener('input', evaluar);
        return evaluar;
    };

    conectar('nuevaContrasena', nuevaContrasenaValida);

    // La confirmación depende del valor actual de "nueva contraseña", así que también
    // se revalida cuando esta última cambia (por si el usuario la edita después de confirmar).
    const evaluarConfirmacion = conectar('confirmarContrasena', function (valor) {
        const campoNueva = document.getElementById('nuevaContrasena');
        return !!campoNueva && valor === campoNueva.value;
    });
    const campoNueva = document.getElementById('nuevaContrasena');
    if (campoNueva && evaluarConfirmacion) {
        campoNueva.addEventListener('input', evaluarConfirmacion);
    }
});
