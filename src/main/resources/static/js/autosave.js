(function () {
    function getCsrf() {
        const t = document.querySelector('meta[name="_csrf"]');
        const h = document.querySelector('meta[name="_csrf_header"]');
        if (!t || !t.content) return null;
        return { header: h && h.content ? h.content : 'X-CSRF-TOKEN', token: t.content };
    }

    function debounce(fn, delay) {
        let t;
        return function () {
            clearTimeout(t);
            const args = arguments;
            t = setTimeout(() => fn.apply(this, args), delay);
        };
    }

    function showStatus(form, text) {
        let badge = form.querySelector('.autosave-status');
        if (!badge) {
            badge = document.createElement('span');
            badge.className = 'autosave-status muted small';
            badge.style.marginLeft = '12px';
            const target = form.querySelector('[data-autosave-anchor]') || form;
            target.appendChild(badge);
        }
        badge.textContent = text;
    }

    function serialize(form) {
        const fd = new FormData();
        form.querySelectorAll('[data-autosave-field]').forEach(el => {
            const name = el.name || el.dataset.name;
            if (!name) return;
            fd.append(name, el.value || '');
        });
        return fd;
    }

    function setup(form) {
        const url = form.dataset.autosaveUrl;
        if (!url) return;
        const send = debounce(async () => {
            showStatus(form, 'Сохранение…');
            try {
                const csrf = getCsrf();
                const headers = {};
                if (csrf) headers[csrf.header] = csrf.token;
                const resp = await fetch(url, {
                    method: 'POST',
                    body: serialize(form),
                    headers,
                    credentials: 'same-origin'
                });
                if (!resp.ok) {
                    showStatus(form, 'Ошибка сохранения');
                    return;
                }
                const data = await resp.json().catch(() => ({}));
                if (data.workId && form.dataset.autosaveTargetWorkId !== data.workId) {
                    form.dataset.autosaveTargetWorkId = data.workId;
                    document.querySelectorAll('[data-needs-work-id]').forEach(node => {
                        node.dataset.workId = data.workId;
                    });
                }
                const t = new Date();
                const hh = String(t.getHours()).padStart(2, '0');
                const mm = String(t.getMinutes()).padStart(2, '0');
                const ss = String(t.getSeconds()).padStart(2, '0');
                showStatus(form, 'Сохранено ' + hh + ':' + mm + ':' + ss);
            } catch (e) {
                showStatus(form, 'Нет связи');
            }
        }, 800);

        form.querySelectorAll('[data-autosave-field]').forEach(el => {
            el.addEventListener('input', send);
            el.addEventListener('change', send);
        });
    }

    document.addEventListener('DOMContentLoaded', () => {
        document.querySelectorAll('form[data-autosave-url]').forEach(setup);
    });
})();
