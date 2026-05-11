(function () {
    const dropzone = document.getElementById('dropzone');
    const input = document.getElementById('file-input');
    const list = document.getElementById('file-list');
    if (!dropzone || !input) return;

    dropzone.addEventListener('click', () => input.click());
    dropzone.addEventListener('dragover', e => {
        e.preventDefault();
        dropzone.classList.add('is-dragover');
    });
    dropzone.addEventListener('dragleave', () => dropzone.classList.remove('is-dragover'));
    dropzone.addEventListener('drop', e => {
        e.preventDefault();
        dropzone.classList.remove('is-dragover');
        const dt = e.dataTransfer;
        if (dt && dt.files && dt.files.length) {
            input.files = dt.files;
            input.dispatchEvent(new Event('change', {bubbles: true}));
        }
    });
    input.addEventListener('change', () => render(input.files));

    function render(files) {
        if (!list) return;
        list.innerHTML = '';
        Array.from(files).forEach(f => {
            const li = document.createElement('li');
            li.style.cssText = 'display:flex;justify-content:space-between;align-items:center;padding:10px 14px;border:1px solid var(--border);border-radius:10px;background:var(--surface);';
            li.innerHTML = `<span style="font-weight:500;">${escapeHtml(f.name)}</span><span class="muted small">${formatSize(f.size)}</span>`;
            list.appendChild(li);
        });
    }

    function formatSize(b) {
        if (b < 1024) return b + ' B';
        if (b < 1024 * 1024) return (b / 1024).toFixed(1) + ' KB';
        return (b / 1024 / 1024).toFixed(1) + ' MB';
    }

    function escapeHtml(s) {
        return s.replace(/[&<>"']/g, c => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[c]));
    }

    const addLink = document.getElementById('add-link');
    const linkList = document.getElementById('link-list');
    if (addLink && linkList) {
        addLink.addEventListener('click', () => {
            const i = document.createElement('input');
            i.name = 'links';
            i.className = 'input';
            i.placeholder = 'https://...';
            linkList.appendChild(i);
        });
    }
})();
