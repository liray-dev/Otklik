(function () {
    const controls = document.getElementById('voice-controls');
    if (!controls) return;

    const startBtn = document.getElementById('voice-start');
    const stopBtn = document.getElementById('voice-stop');
    const status = document.getElementById('voice-status');
    const preview = document.getElementById('voice-preview');
    const audioEl = document.getElementById('voice-audio');
    const uploadUrl = controls.getAttribute('data-upload');
    const blobInput = document.getElementById('voice-blob');

    let mediaRecorder = null;
    let chunks = [];
    let startedAt = 0;

    function show(el) { if (el) el.classList.remove('hidden'); }
    function hide(el) { if (el) el.classList.add('hidden'); }

    async function start() {
        if (!navigator.mediaDevices) {
            alert('Запись недоступна — браузер не поддерживает MediaRecorder.');
            return;
        }
        try {
            const stream = await navigator.mediaDevices.getUserMedia({audio: true});
            const mime = MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
                ? 'audio/webm;codecs=opus'
                : 'audio/webm';
            mediaRecorder = new MediaRecorder(stream, {mimeType: mime});
            chunks = [];
            mediaRecorder.ondataavailable = e => { if (e.data && e.data.size) chunks.push(e.data); };
            mediaRecorder.onstop = onStop.bind(null, stream);
            mediaRecorder.start();
            startedAt = Date.now();
            hide(startBtn); show(stopBtn); show(status);
        } catch (err) {
            console.error(err);
            alert('Не удалось получить доступ к микрофону.');
        }
    }

    function stop() {
        if (mediaRecorder && mediaRecorder.state === 'recording') mediaRecorder.stop();
    }

    async function onStop(stream) {
        stream.getTracks().forEach(t => t.stop());
        const duration = Date.now() - startedAt;
        const blob = new Blob(chunks, {type: 'audio/webm'});
        const url = URL.createObjectURL(blob);
        if (audioEl) { audioEl.src = url; }
        show(preview); hide(status); show(startBtn); hide(stopBtn);

        if (uploadUrl) {
            const fd = new FormData();
            fd.append('audio', blob, 'voice.webm');
            fd.append('durationMs', String(duration));
            const csrf = readCsrf();
            const headers = {};
            if (csrf) headers['X-XSRF-TOKEN'] = csrf;
            try {
                const res = await fetch(uploadUrl, {method: 'POST', body: fd, headers, credentials: 'same-origin'});
                if (!res.ok) console.error('voice upload failed', res.status);
                else {
                    const json = await res.json();
                    if (json.url && audioEl) audioEl.src = json.url;
                }
            } catch (e) {
                console.error('voice upload error', e);
            }
        } else if (blobInput) {
            const reader = new FileReader();
            reader.onloadend = () => { blobInput.value = String(reader.result || ''); };
            reader.readAsDataURL(blob);
        }
    }

    function readCsrf() {
        const m = document.cookie.match(/(?:^|;)\s*XSRF-TOKEN=([^;]+)/);
        return m ? decodeURIComponent(m[1]) : null;
    }

    if (startBtn) startBtn.addEventListener('click', start);
    if (stopBtn) stopBtn.addEventListener('click', stop);
})();
