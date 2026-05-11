(function () {
    function update(viewer, fileId, filename, baseFileUrl, previewUrl) {
        const filenameEl = viewer.querySelector('.viewer__filename span') || viewer.querySelector('.viewer__filename');
        if (filenameEl) filenameEl.textContent = filename;
        const iframe = viewer.querySelector('iframe');
        if (iframe) iframe.src = previewUrl;
        viewer.querySelectorAll('[data-viewer-link]').forEach(a => {
            a.href = baseFileUrl + (a.dataset.viewerLink === 'attachment' ? '?disposition=attachment' : '');
        });
    }

    function setActive(container, fileId) {
        container.querySelectorAll('[data-file-tab]').forEach(a => {
            const id = a.dataset.fileTab;
            a.classList.toggle('btn--primary', id === fileId);
            a.classList.toggle('btn--ghost', id !== fileId);
        });
    }

    function setup(container) {
        const viewer = document.querySelector(container.dataset.viewerTarget) || container.parentElement.querySelector('.viewer');
        if (!viewer) return;
        const baseFile = container.dataset.fileBase || '/files/work/';
        const basePreview = container.dataset.previewBase || '/files/work/';
        container.querySelectorAll('[data-file-tab]').forEach(a => {
            a.addEventListener('click', (e) => {
                e.preventDefault();
                const id = a.dataset.fileTab;
                const filename = a.dataset.fileName || a.textContent.trim();
                update(viewer, id, filename, baseFile + id, basePreview + id + '/preview');
                setActive(container, id);
                if (a.href) {
                    try { history.replaceState({}, '', a.href); } catch (_) {}
                }
            });
        });
    }

    document.addEventListener('DOMContentLoaded', () => {
        document.querySelectorAll('[data-file-switcher]').forEach(setup);
    });
})();
