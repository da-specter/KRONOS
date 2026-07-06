function togglePasswordVisibility(inputId, showIconId, hideIconId) {
    const input = document.getElementById(inputId);
    const eyeShow = document.getElementById(showIconId);
    const eyeHide = document.getElementById(hideIconId);
    const esVisible = input.type === 'text';
    input.type = esVisible ? 'password' : 'text';
    eyeShow.style.display = esVisible ? '' : 'none';
    eyeHide.style.display = esVisible ? 'none' : '';
}
