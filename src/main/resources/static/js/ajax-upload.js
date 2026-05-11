(function () {
    function getCsrf() {
        const t = document.querySelector('meta[name="_csrf"]');
        const h = document.querySelector('meta[name="_csrf_header"]');
        if (!t || !t.content) return null;
        return { header: h && h.content ? h.content : 'X-CSRF-TOKEN', token: t.content };
    }

    async function ensureWork(form, fileInput) {
        const switcher = document.querySelector('[data-file-switcher]');
        let workId = switcher ? switcher.dataset.workId : null;
        if (workId) return workId;
        const autosaveUrl = form ? form.dataset.autosaveUrl : null;
        if (!autosaveUrl) return null;
        const fd = new FormData();
        form.querySelectorAll('[data-autosave-field]').forEach(el => {
            const name = el.name || el.dataset.name;
            if (!name) return;
            fd.append(name, el.value || '');
        });
        const csrf = getCsrf();
        const headers = {};
        if (csrf) headers[csrf.header] = csrf.token;
        const resp = await fetch(autosaveUrl, { method: 'POST', body: fd, headers, credentials: 'same-origin' });
        if (!resp.ok) return null;
        const data = await resp.json().catch(() => ({}));
        return data.workId || null;
    }

    function addTabFor(att, workId) {
        const switcher = document.querySelector('[data-file-switcher]');
        if (!switcher) return;
        switcher.dataset.workId = workId;
        const wrap = document.createElement('span');
        wrap.className = 'row gap-1';
        const a = document.createElement('a');
        a.href = '#';
        a.className = 'btn btn--sm btn--ghost';
        a.dataset.fileTab = att.id;
        a.dataset.fileName = att.filename;
        a.textContent = att.filename;
        a.addEventListener('click', (e) => {
            e.preventDefault();
            const viewer = document.querySelector('.viewer');
            const fn = viewer ? viewer.querySelector('.viewer__filename') : null;
            if (fn) fn.textContent = att.filename;
            const iframe = viewer ? viewer.querySelector('iframe') : null;
            if (iframe) iframe.src = '/files/work/' + att.id + '/preview';
            switcher.querySelectorAll('[data-file-tab]').forEach(x => {
                x.classList.toggle('btn--primary', x === a);
                x.classList.toggle('btn--ghost', x !== a);
            });
        });
        wrap.appendChild(a);
        switcher.appendChild(wrap);
        a.click();
    }

    function setup(input) {
        const form = input.closest('form');
        input.addEventListener('change', async () => {
            if (!input.files || input.files.length === 0) return;
            const csrf = getCsrf();
            const workId = await ensureWork(form, input);
            if (!workId) {
                alert('Не удалось сохранить черновик работы.');
                return;
            }
            for (const file of input.files) {
                const fd = new FormData();
                fd.append('file', file);
                const headers = {};
                if (csrf) headers[csrf.header] = csrf.token;
                try {
                    const resp = await fetch('/student/works/' + workId + '/files', {
                        method: 'POST',
                        body: fd,
                        headers,
                        credentials: 'same-origin'
                    });
                    if (!resp.ok) {
                        alert('Не удалось загрузить файл: ' + file.name);
                        continue;
                    }
                    const data = await resp.json().catch(() => ({}));
                    if (data.attachment) addTabFor(data.attachment, workId);
                } catch (e) {
                    alert('Ошибка загрузки: ' + file.name);
                }
            }
            input.value = '';
        });
    }

    document.addEventListener('DOMContentLoaded', () => {
        document.querySelectorAll('input[data-ajax-upload]').forEach(setup);
    });
})();
