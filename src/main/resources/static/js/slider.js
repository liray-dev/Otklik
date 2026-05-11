(function () {
    document.querySelectorAll('.js-criterion-slider').forEach(slider => {
        const targetId = slider.getAttribute('data-target');
        const target = targetId ? document.getElementById(targetId) : null;
        const update = () => {
            const v = parseFloat(slider.value);
            if (target) target.textContent = Number.isFinite(v) ? v.toFixed(1) : '—';
        };
        slider.addEventListener('input', update);
        update();
    });
})();
