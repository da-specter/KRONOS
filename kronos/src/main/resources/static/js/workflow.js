// Anima el llenado de la línea de tiempo de la Etapa Productiva (de 0% al progreso real)
document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('.timeline-track-fill').forEach(function (barra) {
        const progreso = barra.getAttribute('data-progreso') || 0;
        requestAnimationFrame(function () {
            requestAnimationFrame(function () {
                barra.style.width = 'calc((100% - 4px) * ' + progreso + ' / 100)';
            });
        });
    });
});

// 🕒 Reloj en vivo del header del index: hora local corriendo (formato 12h) + día actual en español.
// Solo se activa si la página tiene la cápsula del reloj (#relojKronos).
document.addEventListener('DOMContentLoaded', function () {
    const capsula = document.getElementById('relojKronos');
    if (!capsula) return;

    const elHora = document.getElementById('relojHora');
    const elSeg = document.getElementById('relojSeg');
    const elAmPm = document.getElementById('relojAmPm');
    const elFecha = document.getElementById('relojFecha');
    const pad = n => String(n).padStart(2, '0');

    function actualizarReloj() {
        const ahora = new Date();
        const horas24 = ahora.getHours();
        const horas12 = horas24 % 12 || 12;

        elHora.textContent = pad(horas12) + ':' + pad(ahora.getMinutes());
        elSeg.textContent = ':' + pad(ahora.getSeconds());
        elAmPm.textContent = horas24 < 12 ? 'AM' : 'PM';

        // Ej: "lunes, 6 de julio" (con la inicial en mayúscula)
        const fecha = ahora.toLocaleDateString('es-CO', { weekday: 'long', day: 'numeric', month: 'long' });
        elFecha.textContent = fecha.charAt(0).toUpperCase() + fecha.slice(1);
    }

    actualizarReloj();
    setInterval(actualizarReloj, 1000);
});

// Muestra/oculta el desplegable de notificaciones al hacer clic en la campana
function toggleNotificaciones() {
    const panel = document.getElementById('panelNotificaciones');
    if (!panel) return;
    panel.style.display = panel.style.display === 'none' ? 'block' : 'none';
}

document.addEventListener('click', function (evento) {
    const panel = document.getElementById('panelNotificaciones');
    const campana = document.querySelector('.bell-btn-circle');
    if (!panel || panel.style.display === 'none') return;
    if (!panel.contains(evento.target) && !campana.contains(evento.target)) {
        panel.style.display = 'none';
    }
});

// Despliega/colapsa un módulo del sidebar con sub-ítems (Ej: Gestión Etapa)
function toggleMenuGrupo(boton) {
    const grupo = boton.closest('.menu-group');
    if (grupo) grupo.classList.toggle('open');
}

// Muestra/oculta la fila con los aprendices matriculados en una ficha (Gestión de Fichas)
function toggleDetalleFicha(boton, idFila) {
    const fila = document.getElementById(idFila);
    if (!fila) return;
    const visible = fila.style.display !== 'none';
    fila.style.display = visible ? 'none' : 'table-row';
    if (boton) {
        boton.textContent = visible ? 'Ver aprendices' : 'Ocultar aprendices';
    }
}

// Función para procesar los primeros checks del Coordinador
function enviarEvaluacionInicial(idSolicitud) {
    // 1. Capturamos el estado real de los checkboxes del HTML
    const checkFecha = document.getElementById(`checkFecha_${idSolicitud}`).checked;
    const checkCompetencias = document.getElementById(`checkCompetencias_${idSolicitud}`).checked;
    const observacionEl = document.getElementById(`observacion_${idSolicitud}`);
    const observacion = observacionEl ? observacionEl.value.trim() : '';

    // Si se va a rechazar algún check, la novedad es obligatoria para que el aprendiz sepa el motivo
    if ((!checkFecha || !checkCompetencias) && !observacion) {
        alert('Debes escribir una novedad explicando el motivo del rechazo antes de continuar.');
        return;
    }

    // 2. Armamos la URL con las variables que espera nuestro @PutMapping del Controller
    const url = `/api/workflow/coordinador/evaluar-inicial/${idSolicitud}?fechaOk=${checkFecha}&competenciasOk=${checkCompetencias}&observacion=${encodeURIComponent(observacion)}`;

    // 3. Disparamos la petición asíncrona al servidor de Spring Boot
    fetch(url, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => {
        // No parseamos el cuerpo: solo nos interesa si la operación fue exitosa.
        // (La entidad devuelta trae relaciones JPA que Jackson no siempre serializa limpio.)
        if (response.ok) {
            document.getElementById('modalRespuestaEnviada').style.display = 'flex';
            return;
        }
        throw new Error('Hubo un error al procesar los requisitos en el servidor.');
    })
    .catch(error => {
        console.error(error);
        alert(error.message);
    });
}

// Función para que el Gestor de Etapa habilite el panel de plantillas del aprendiz
function habilitarFormatosGestor(idSolicitud) {
    const url = `/api/workflow/gestor/habilitar-formatos/${idSolicitud}`;

    fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => {
        // No parseamos el cuerpo: solo nos interesa si la operación fue exitosa.
        // (La entidad devuelta trae relaciones JPA que Jackson no siempre serializa limpio.)
        if (response.ok) {
            document.getElementById('modalHabilitacionExitosa').style.display = 'flex';
            return;
        }
        throw new Error('Hubo un error al habilitar las plantillas en el servidor.');
    })
    .catch(error => {
        console.error(error);
        alert(error.message);
    });
}