(function () {
    const container = document.getElementById('criteria');
    const addBtn = document.getElementById('add-criterion');
    const totalEl = document.getElementById('weight-total');
    const summaryAnon = document.getElementById('summary-anon');
    if (!container) return;

    function updateTotal() {
        let total = 0;
        container.querySelectorAll('.criterion-weight').forEach(i => {
            const v = parseFloat(i.value.replace(',', '.'));
            if (!Number.isNaN(v)) total += v;
        });
        if (!totalEl) return;
        totalEl.textContent = 'Итого: ' + (Math.round(total * 100) / 100) + '%';
        totalEl.classList.toggle('is-bad', Math.round(total) !== 100);
    }

    function rowTemplate() {
        const row = document.createElement('div');
        row.className = 'criterion-row';
        row.innerHTML = `
            <div class="handle">⋮⋮</div>
            <div class="field">
                <label class="field__label">Название метрики</label>
                <input name="criterionName" class="input" placeholder="Новая метрика" required>
            </div>
            <div class="field">
                <label class="field__label">Шкала</label>
                <input class="input" value="Абсолютная 1-10" readonly>
            </div>
            <div class="field">
                <label class="field__label">Вес, %</label>
                <input name="criterionWeight" class="input criterion-weight" type="number" min="0" max="100" step="1" value="0" required>
            </div>
            <button type="button" class="delete" aria-label="Удалить">🗑</button>
        `;
        return row;
    }

    container.addEventListener('input', e => {
        if (e.target.classList.contains('criterion-weight')) updateTotal();
    });
    container.addEventListener('click', e => {
        if (e.target.classList.contains('delete')) {
            const row = e.target.closest('.criterion-row');
            if (row && container.querySelectorAll('.criterion-row').length > 1) {
                row.remove();
                updateTotal();
            }
        }
    });
    if (addBtn) {
        addBtn.addEventListener('click', () => {
            container.appendChild(rowTemplate());
            updateTotal();
        });
    }

    if (summaryAnon) {
        document.querySelectorAll('input[name="anonymity"]').forEach(r => {
            r.addEventListener('change', () => {
                const labels = {OPEN: 'Open', SINGLE_BLIND: 'Single-Blind', DOUBLE_BLIND: 'Double-Blind'};
                summaryAnon.textContent = labels[r.value] || r.value;
            });
        });
    }

    updateTotal();
})();
