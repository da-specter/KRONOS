// ✅ Validación visual en tiempo real del login:
// pinta el borde del campo en rojo neón mientras no cumple la regla y en verde cuando cumple.
// Es solo feedback visual: no bloquea el envío (el servidor valida las credenciales reales).
document.addEventListener('DOMContentLoaded', function () {

    // Correo: misma regla del resto de KRONOS → debe tener '@' y un '.co' en el dominio
    const correoValido = function (valor) {
        const v = valor.trim();
        const arroba = v.indexOf('@');
        return /^[^@\s]+@[^@\s]+$/.test(v) && arroba > 0 && v.substring(arroba).includes('.co');
    };

    // Contraseña de login: solo se exige un mínimo razonable (6+ caracteres).
    // La política fuerte (mayúscula/minúscula/números/especial) aplica al CAMBIARLA, no al entrar,
    // porque las cuentas existentes deben poder iniciar sesión con su contraseña actual.
    const contrasenaValida = function (valor) {
        return valor.length >= 6;
    };

    const conectar = function (idCampo, regla) {
        const campo = document.getElementById(idCampo);
        if (!campo) return;
        const contenedor = campo.closest('.input-container');
        if (!contenedor) return;

        campo.addEventListener('input', function () {
            contenedor.classList.remove('input-valido', 'input-invalido');
            if (campo.value === '') return; // vacío: vuelve al estado neutro
            contenedor.classList.add(regla(campo.value) ? 'input-valido' : 'input-invalido');
        });
    };

    conectar('correoElectronico', correoValido);
    conectar('contrasena', contrasenaValida);
});
