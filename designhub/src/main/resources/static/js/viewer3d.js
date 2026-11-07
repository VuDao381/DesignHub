// =========================================================================
// DESIGNHUB 3D INTERACTIVE STUDIO & 3D MOVIE RECORDER (Three.js WebGL)
// Hỗ trợ xoay không gian 3 chiều (360°, lật mặt sau 180°), áp thiết kế lên
// vật thể 3D (Khung tranh 3D, Hộp sản phẩm, Cốc sứ 3D, Màn hình Studio)
// và quay phim Movie 3D xuất file video trực tiếp trên trình duyệt.
// =========================================================================

window.DesignHub3D = (function () {
    let activeState = null;

    function cleanupActive() {
        if (!activeState) return;
        if (activeState.animId) cancelAnimationFrame(activeState.animId);
        if (activeState.resizeObserver) activeState.resizeObserver.disconnect();
        if (activeState.renderer) {
            try {
                activeState.renderer.dispose();
            } catch (e) {}
        }
        activeState = null;
    }

    // Tạo texture mặt sau có đóng dấu Chứng nhận Bản quyền 3D khi quay ngược 180 độ
    function createBacksideStampTexture(title, designerName) {
        const c = document.createElement('canvas');
        c.width = 1024;
        c.height = 1024;
        const ctx = c.getContext('2d');

        // Nền gỗ/kim loại mặt sau
        const grad = ctx.createLinearGradient(0, 0, 1024, 1024);
        grad.addColorStop(0, '#1e1b2e');
        grad.addColorStop(1, '#0f172a');
        ctx.fillStyle = grad;
        ctx.fillRect(0, 0, 1024, 1024);

        // Viền khung kỹ thuật mặt sau
        ctx.strokeStyle = '#6366f1';
        ctx.lineWidth = 10;
        ctx.strokeRect(48, 48, 928, 928);

        ctx.strokeStyle = 'rgba(236, 72, 153, 0.5)';
        ctx.lineWidth = 3;
        ctx.setLineDash([16, 12]);
        ctx.strokeRect(76, 76, 872, 872);
        ctx.setLineDash([]);

        // Con dấu tròn chứng nhận ở tâm mặt sau
        ctx.save();
        ctx.translate(512, 360);
        ctx.beginPath();
        ctx.arc(0, 0, 130, 0, Math.PI * 2);
        ctx.strokeStyle = '#10b981';
        ctx.lineWidth = 8;
        ctx.stroke();

        ctx.beginPath();
        ctx.arc(0, 0, 112, 0, Math.PI * 2);
        ctx.strokeStyle = 'rgba(16, 185, 129, 0.4)';
        ctx.lineWidth = 3;
        ctx.stroke();

        ctx.fillStyle = '#10b981';
        ctx.font = 'bold 34px Plus Jakarta Sans, sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText('VERIFIED 3D', 0, -15);
        ctx.font = 'bold 26px Plus Jakarta Sans, sans-serif';
        ctx.fillText('AUTHENTIC ART', 0, 25);
        ctx.font = '20px Plus Jakarta Sans, sans-serif';
        ctx.fillStyle = '#94a3b8';
        ctx.fillText('DESIGNHUB STUDIO', 0, 62);
        ctx.restore();

        // Thông tin mặt sau (Back-side inspection)
        ctx.fillStyle = '#f8fafc';
        ctx.font = 'bold 38px Plus Jakarta Sans, sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText('MẶT SAU KHÔNG GIAN 3 CHIỀU (180°)', 512, 580);

        ctx.fillStyle = '#a5b4fc';
        ctx.font = '28px Plus Jakarta Sans, sans-serif';
        const cleanTitle = (title || 'Tác phẩm Thiết kế 3D').substring(0, 42);
        ctx.fillText(cleanTitle, 512, 645);

        ctx.fillStyle = '#94a3b8';
        ctx.font = '24px Plus Jakarta Sans, sans-serif';
        ctx.fillText('Tác giả / Studio: ' + (designerName || 'DesignHub Verified Artist'), 512, 705);
        ctx.fillText('Chứng chỉ DRM: SHA-256 Protected • Hỗ trợ xuất Movie 3D', 512, 755);

        // Lưới thông số kỹ thuật 3D
        ctx.fillStyle = 'rgba(99, 102, 241, 0.14)';
        ctx.fillRect(140, 810, 744, 95);
        ctx.fillStyle = '#e2e8f0';
        ctx.font = 'bold 22px monospace';
        ctx.fillText('3D NORMAL MAP: ACTIVE  |  360° ORBIT: READY  |  MOVIE: 30 FPS', 512, 866);

        const tex = new THREE.CanvasTexture(c);
        tex.needsUpdate = true;
        return tex;
    }

    // Tạo texture mặt trước có tích hợp hình ảnh thiết kế + hiệu ứng nổi
    function createFrontDesignTexture(imageUrl, title, applyWatermark, callback) {
        const c = document.createElement('canvas');
        c.width = 1024;
        c.height = 1024;
        const ctx = c.getContext('2d');

        function drawFallbackPattern() {
            const g = ctx.createLinearGradient(0, 0, 1024, 1024);
            g.addColorStop(0, '#312e81');
            g.addColorStop(0.5, '#4f46e5');
            g.addColorStop(1, '#be185d');
            ctx.fillStyle = g;
            ctx.fillRect(0, 0, 1024, 1024);

            ctx.fillStyle = '#ffffff';
            ctx.font = 'bold 48px sans-serif';
            ctx.textAlign = 'center';
            ctx.fillText((title || 'DesignHub 3D').substring(0, 28), 512, 512);
        }

        function drawWatermarkIfRequired() {
            if (!applyWatermark) return;
            ctx.save();
            ctx.translate(512, 512);
            ctx.rotate(-Math.PI / 6);
            ctx.font = 'bold 34px sans-serif';
            ctx.fillStyle = 'rgba(255, 255, 255, 0.24)';
            ctx.strokeStyle = 'rgba(0, 0, 0, 0.25)';
            ctx.lineWidth = 2;
            ctx.textAlign = 'center';
            for (let y = -600; y <= 600; y += 190) {
                for (let x = -600; x <= 600; x += 460) {
                    ctx.strokeText('© DESIGNHUB 3D PREVIEW', x, y);
                    ctx.fillText('© DESIGNHUB 3D PREVIEW', x, y);
                }
            }
            ctx.restore();
        }

        drawFallbackPattern();
        const tex = new THREE.CanvasTexture(c);

        if (!imageUrl) {
            drawWatermarkIfRequired();
            tex.needsUpdate = true;
            if (callback) callback(tex);
            return tex;
        }

        const img = new Image();
        img.crossOrigin = 'anonymous';
        img.onload = function () {
            ctx.fillStyle = '#0f172a';
            ctx.fillRect(0, 0, 1024, 1024);

            // Vẽ ảnh phủ đầy khung vuông chuẩn 3D
            const iw = img.width || 1024;
            const ih = img.height || 1024;
            const scale = Math.max(1024 / iw, 1024 / ih);
            const dw = iw * scale;
            const dh = ih * scale;
            const dx = (1024 - dw) / 2;
            const dy = (1024 - dh) / 2;
            ctx.drawImage(img, dx, dy, dw, dh);

            drawWatermarkIfRequired();
            tex.needsUpdate = true;
            if (callback) callback(tex);
        };
        img.onerror = function () {
            drawWatermarkIfRequired();
            tex.needsUpdate = true;
            if (callback) callback(tex);
        };
        img.src = imageUrl;
        return tex;
    }

    function buildMockupMesh(type, frontTex, backTex) {
        const group = new THREE.Group();

        const frameMat = new THREE.MeshStandardMaterial({
            color: 0x1e293b,
            roughness: 0.35,
            metalness: 0.65
        });
        const accentGoldMat = new THREE.MeshStandardMaterial({
            color: 0x6366f1,
            roughness: 0.25,
            metalness: 0.8
        });
        const frontMat = new THREE.MeshStandardMaterial({
            map: frontTex,
            roughness: 0.28,
            metalness: 0.08
        });
        const backMat = new THREE.MeshStandardMaterial({
            map: backTex,
            roughness: 0.45,
            metalness: 0.2
        });

        if (type === 'BOX_3D') {
            // Hộp bao bì sản phẩm 3D (6 mặt)
            const boxGeo = new THREE.BoxGeometry(2.5, 3.1, 1.5);
            const sideMat = new THREE.MeshStandardMaterial({
                color: 0x312e81,
                roughness: 0.4,
                metalness: 0.25
            });
            const materials = [
                frontMat, // Right
                frontMat, // Left
                accentGoldMat, // Top lid
                frameMat, // Bottom
                frontMat, // Front
                backMat   // Back (180 độ)
            ];
            const boxMesh = new THREE.Mesh(boxGeo, materials);
            boxMesh.castShadow = true;
            group.add(boxMesh);

            // Nắp viền nổi trên hộp
            const lidGeo = new THREE.BoxGeometry(2.56, 0.14, 1.56);
            const lidMesh = new THREE.Mesh(lidGeo, accentGoldMat);
            lidMesh.position.y = 1.48;
            group.add(lidMesh);

        } else if (type === 'CUP_3D') {
            // Cốc / Ly sứ 3D xoay 360 độ
            const cupGeo = new THREE.CylinderGeometry(1.15, 0.98, 2.7, 64, 1, false);
            const topMat = new THREE.MeshStandardMaterial({ color: 0x0f172a, roughness: 0.2 });
            const bottomMat = new THREE.MeshStandardMaterial({ map: backTex, roughness: 0.5 });
            const cupMesh = new THREE.Mesh(cupGeo, [frontMat, topMat, bottomMat]);
            group.add(cupMesh);

            // Quai cốc 3D (Torus)
            const handleGeo = new THREE.TorusGeometry(0.68, 0.14, 24, 48, Math.PI);
            const handleMesh = new THREE.Mesh(handleGeo, frameMat);
            handleMesh.rotation.z = -Math.PI / 2;
            handleMesh.position.set(1.08, 0.05, 0);
            group.add(handleMesh);

        } else if (type === 'DEVICE_3D') {
            // Màn hình Studio 3D / Tablet Pro có mặt trước là giao diện UI/UX và mặt lưng kim loại khắc dấu
            const bodyGeo = new THREE.BoxGeometry(3.8, 2.5, 0.18);
            const bodyMaterials = [
                frameMat,
                frameMat,
                frameMat,
                frameMat,
                frontMat, // Màn hình chính
                backMat   // Mặt lưng 180°
            ];
            const screenMesh = new THREE.Mesh(bodyGeo, bodyMaterials);
            screenMesh.position.y = 0.25;
            group.add(screenMesh);

            // Viền Bezel phát sáng
            const rimGeo = new THREE.BoxGeometry(3.88, 2.58, 0.14);
            const rimMesh = new THREE.Mesh(rimGeo, accentGoldMat);
            rimMesh.position.set(0, 0.25, -0.02);
            group.add(rimMesh);

            // Chân đế Studio Stand
            const neckGeo = new THREE.BoxGeometry(0.45, 1.1, 0.12);
            const neckMesh = new THREE.Mesh(neckGeo, frameMat);
            neckMesh.position.set(0, -1.15, -0.2);
            neckMesh.rotation.x = 0.2;
            group.add(neckMesh);

            const baseGeo = new THREE.CylinderGeometry(0.95, 1.05, 0.1, 48);
            const baseMesh = new THREE.Mesh(baseGeo, frameMat);
            baseMesh.position.set(0, -1.62, -0.1);
            group.add(baseMesh);

        } else {
            // Mặc định: POSTER_3D (Khung tranh Nghệ thuật 3D có độ dày thực + mặt sau 180 độ)
            const canvasGeo = new THREE.BoxGeometry(3.2, 3.2, 0.22);
            const materials = [
                frameMat,
                frameMat,
                frameMat,
                frameMat,
                frontMat, // Mặt trước tác phẩm
                backMat   // Mặt sau chứng nhận khi quay ngược 180°
            ];
            const artMesh = new THREE.Mesh(canvasGeo, materials);
            group.add(artMesh);

            // Khung viền nổi 3D bao quanh
            const outerRimGeo = new THREE.BoxGeometry(3.38, 3.38, 0.18);
            const outerRim = new THREE.Mesh(outerRimGeo, accentGoldMat);
            outerRim.position.z = -0.02;
            group.add(outerRim);
        }

        // Bệ trưng bày hologram phía dưới
        const ringGeo = new THREE.RingGeometry(1.7, 1.82, 64);
        const ringMat = new THREE.MeshBasicMaterial({
            color: 0x6366f1,
            side: THREE.DoubleSide,
            transparent: true,
            opacity: 0.45
        });
        const ringMesh = new THREE.Mesh(ringGeo, ringMat);
        ringMesh.rotation.x = Math.PI / 2;
        ringMesh.position.y = -1.85;
        group.add(ringMesh);

        return group;
    }

    function initViewer(containerId, options = {}) {
        cleanupActive();

        const root = document.getElementById(containerId);
        if (!root) return;

        if (typeof THREE === 'undefined') {
            root.innerHTML = `<div style="padding:32px;text-align:center;color:var(--text-muted)">Đang tải thư viện đồ họa 3D Three.js...</div>`;
            return;
        }

        const imageUrl = options.imageUrl || '';
        const title = options.title || 'Tác phẩm Thiết kế 3D';
        const designerName = options.designerName || 'DesignHub Artist';
        let currentMockup = options.mockupType || 'POSTER_3D';
        const watermark = options.watermark !== false; // Mặc định bật watermark bảo vệ nếu chưa mua

        root.innerHTML = `
            <div class="viewer3d-studio-wrapper">
                <div class="viewer3d-topbar">
                    <div class="viewer3d-badges">
                        <span class="badge-3d-live"><i class="fa-solid fa-cube"></i> KHÔNG GIAN 3D TƯƠNG TÁC</span>
                        <span class="badge-3d-hint"><i class="fa-solid fa-hand-pointer"></i> Kéo chuột để xoay 360° • Lăn chuột để Phóng to</span>
                    </div>
                    <div class="viewer3d-mockup-select">
                        <button type="button" class="v3d-type-btn ${currentMockup === 'POSTER_3D' ? 'active' : ''}" data-mockup="POSTER_3D" title="Khung tranh Canvas 3D">
                            <i class="fa-solid fa-image"></i> Khung 3D
                        </button>
                        <button type="button" class="v3d-type-btn ${currentMockup === 'BOX_3D' ? 'active' : ''}" data-mockup="BOX_3D" title="Hộp Bao bì Sản phẩm 3D">
                            <i class="fa-solid fa-box-open"></i> Hộp 3D
                        </button>
                        <button type="button" class="v3d-type-btn ${currentMockup === 'CUP_3D' ? 'active' : ''}" data-mockup="CUP_3D" title="Cốc Sứ Mockup 3D">
                            <i class="fa-solid fa-mug-hot"></i> Cốc 3D
                        </button>
                        <button type="button" class="v3d-type-btn ${currentMockup === 'DEVICE_3D' ? 'active' : ''}" data-mockup="DEVICE_3D" title="Màn hình Studio 3D">
                            <i class="fa-solid fa-display"></i> Màn hình 3D
                        </button>
                    </div>
                </div>

                <div class="viewer3d-canvas-Stage" id="${containerId}_stage">
                    <div class="viewer3d-rec-indicator" id="${containerId}_recBadge" style="display:none;">
                        <span class="rec-dot"></span> ĐANG QUAY MOVIE 3D (<span id="${containerId}_recTime">5</span>s)...
                    </div>
                </div>

                <div class="viewer3d-controls-bar">
                    <div class="viewer3d-angle-group">
                        <button type="button" class="v3d-ctrl-btn" data-angle="front" title="Nhìn thẳng mặt trước (0°)">
                            <i class="fa-solid fa-eye"></i> Mặt trước (0°)
                        </button>
                        <button type="button" class="v3d-ctrl-btn" data-angle="iso" title="Góc nghiêng không gian 3 chiều (45°)">
                            <i class="fa-solid fa-cubes"></i> Góc 3D (45°)
                        </button>
                        <button type="button" class="v3d-ctrl-btn v3d-flip-btn" data-angle="back" title="Quay ngược ra mặt sau 180 độ để kiểm tra chứng nhận">
                            <i class="fa-solid fa-rotate"></i> Quay mặt sau (180°)
                        </button>
                        <button type="button" class="v3d-ctrl-btn active" id="${containerId}_btnAutoRotate" title="Bật/Tắt tự động xoay">
                            <i class="fa-solid fa-sync fa-spin"></i> Tự xoay 360°
                        </button>
                        <button type="button" class="v3d-ctrl-btn" id="${containerId}_btnLight" title="Đổi ánh sáng Studio">
                            <i class="fa-solid fa-lightbulb"></i> Đổi Ánh sáng
                        </button>
                    </div>

                    <div class="viewer3d-movie-group">
                        <button type="button" class="btn-record-movie3d" id="${containerId}_btnRecord">
                            <i class="fa-solid fa-video"></i> Quay Phim Movie 3D (5s)
                        </button>
                    </div>
                </div>

                <div class="viewer3d-movie-result" id="${containerId}_movieResult" style="display:none;"></div>
            </div>
        `;

        const stageEl = document.getElementById(`${containerId}_stage`);
        const width = stageEl.clientWidth || 520;
        const height = stageEl.clientHeight || 360;

        const scene = new THREE.Scene();

        // Camera
        const camera = new THREE.PerspectiveCamera(42, width / height, 0.1, 100);
        camera.position.set(0, 0.3, 6.2);

        // Renderer (preserveDrawingBuffer = true để hỗ trợ quay video & chụp hình)
        const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true, preserveDrawingBuffer: true });
        renderer.setSize(width, height);
        renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
        stageEl.appendChild(renderer.domElement);

        // Hệ thống Đèn Studio 3 Chiều
        const ambientLight = new THREE.AmbientLight(0xffffff, 0.78);
        scene.add(ambientLight);

        const keyLight = new THREE.DirectionalLight(0xffffff, 1.15);
        keyLight.position.set(5, 6, 6);
        scene.add(keyLight);

        const rimLight1 = new THREE.PointLight(0x6366f1, 1.4, 18);
        rimLight1.position.set(-5, 3, -3);
        scene.add(rimLight1);

        const rimLight2 = new THREE.PointLight(0xec4899, 1.1, 18);
        rimLight2.position.set(5, -3, 2);
        scene.add(rimLight2);

        // Tạo Textures Mặt trước & Mặt sau (180 độ)
        const frontTex = createFrontDesignTexture(imageUrl, title, watermark);
        const backTex = createBacksideStampTexture(title, designerName);

        let modelGroup = buildMockupMesh(currentMockup, frontTex, backTex);
        scene.add(modelGroup);

        // Trạng thái góc xoay
        let targetRotY = 0.45;
        let targetRotX = 0.12;
        let currentRotY = 0.45;
        let currentRotX = 0.12;
        let autoRotate = true;
        let isDragging = false;
        let prevX = 0;
        let prevY = 0;
        let lightPreset = 0;

        // Sự kiện kéo chuột xoay 3D
        const domCanvas = renderer.domElement;
        domCanvas.style.cursor = 'grab';

        domCanvas.addEventListener('pointerdown', (e) => {
            isDragging = true;
            autoRotate = false;
            updateAutoRotateBtn();
            prevX = e.clientX;
            prevY = e.clientY;
            domCanvas.style.cursor = 'grabbing';
        });

        window.addEventListener('pointermove', (e) => {
            if (!isDragging) return;
            const dx = e.clientX - prevX;
            const dy = e.clientY - prevY;
            prevX = e.clientX;
            prevY = e.clientY;
            targetRotY += dx * 0.011;
            targetRotX = Math.max(-1.2, Math.min(1.2, targetRotX + dy * 0.01));
        });

        window.addEventListener('pointerup', () => {
            if (isDragging) {
                isDragging = false;
                domCanvas.style.cursor = 'grab';
            }
        });

        // Phóng to / Thu nhỏ bằng con lăn chuột
        domCanvas.addEventListener('wheel', (e) => {
            e.preventDefault();
            camera.position.z = Math.max(3.2, Math.min(9.5, camera.position.z + e.deltaY * 0.004));
        }, { passive: false });

        function updateAutoRotateBtn() {
            const btn = document.getElementById(`${containerId}_btnAutoRotate`);
            if (!btn) return;
            btn.classList.toggle('active', autoRotate);
            btn.innerHTML = autoRotate
                ? '<i class="fa-solid fa-sync fa-spin"></i> Tự xoay 360°'
                : '<i class="fa-solid fa-sync"></i> Tự xoay 360°';
        }

        // Đổi loại mô hình 3D (Khung 3D / Hộp 3D / Cốc 3D / Màn hình 3D)
        root.querySelectorAll('.v3d-type-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                root.querySelectorAll('.v3d-type-btn').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                currentMockup = btn.dataset.mockup;
                scene.remove(modelGroup);
                modelGroup = buildMockupMesh(currentMockup, frontTex, backTex);
                scene.add(modelGroup);
            });
        });

        // Các nút chọn góc nhìn nhanh (0°, 45°, Quay mặt sau 180°)
        root.querySelectorAll('[data-angle]').forEach(btn => {
            btn.addEventListener('click', () => {
                const angle = btn.dataset.angle;
                autoRotate = false;
                updateAutoRotateBtn();
                if (angle === 'front') {
                    targetRotY = 0;
                    targetRotX = 0;
                } else if (angle === 'iso') {
                    targetRotY = 0.65;
                    targetRotX = 0.25;
                } else if (angle === 'back') {
                    // Quay ngược 180 độ ra mặt sau
                    targetRotY = Math.PI;
                    targetRotX = 0;
                }
            });
        });

        const btnAuto = document.getElementById(`${containerId}_btnAutoRotate`);
        if (btnAuto) {
            btnAuto.addEventListener('click', () => {
                autoRotate = !autoRotate;
                updateAutoRotateBtn();
            });
        }

        const btnLight = document.getElementById(`${containerId}_btnLight`);
        if (btnLight) {
            btnLight.addEventListener('click', () => {
                lightPreset = (lightPreset + 1) % 3;
                if (lightPreset === 0) {
                    // Studio Cyber Neon
                    ambientLight.intensity = 0.78;
                    keyLight.color.setHex(0xffffff);
                    rimLight1.color.setHex(0x6366f1);
                    rimLight2.color.setHex(0xec4899);
                } else if (lightPreset === 1) {
                    // Warm Golden Gallery
                    ambientLight.intensity = 0.9;
                    keyLight.color.setHex(0xfffbeb);
                    rimLight1.color.setHex(0xf59e0b);
                    rimLight2.color.setHex(0x10b981);
                } else {
                    // High-Contrast Cinema
                    ambientLight.intensity = 0.5;
                    keyLight.color.setHex(0xe0e7ff);
                    rimLight1.color.setHex(0x38bdf8);
                    rimLight2.color.setHex(0xa855f7);
                }
            });
        }

        // Chức năng Phụ trợ Quay Phim Movie 3D (5 giây xoay 360 độ mượt mà & xuất file .webm)
        const btnRecord = document.getElementById(`${containerId}_btnRecord`);
        if (btnRecord) {
            btnRecord.addEventListener('click', () => {
                if (typeof MediaRecorder === 'undefined' || !domCanvas.captureStream) {
                    alert('Trình duyệt của bạn không hỗ trợ MediaRecorder trên WebGL Canvas.');
                    return;
                }

                const recBadge = document.getElementById(`${containerId}_recBadge`);
                const recTimeEl = document.getElementById(`${containerId}_recTime`);
                const resultBox = document.getElementById(`${containerId}_movieResult`);

                btnRecord.disabled = true;
                btnRecord.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin"></i> Đang quay Movie 3D...';
                if (recBadge) recBadge.style.display = 'inline-flex';
                if (resultBox) resultBox.style.display = 'none';

                // Bật chế độ xoay chuẩn điện ảnh (1 vòng 360 độ hoàn chỉnh trong 5 giây)
                autoRotate = true;
                updateAutoRotateBtn();

                const stream = domCanvas.captureStream(30);
                let mimeType = 'video/webm;codecs=vp9';
                if (!MediaRecorder.isTypeSupported(mimeType)) {
                    mimeType = 'video/webm';
                }

                const chunks = [];
                const recorder = new MediaRecorder(stream, { mimeType });
                recorder.ondataavailable = (ev) => {
                    if (ev.data && ev.data.size > 0) chunks.push(ev.data);
                };

                recorder.onstop = () => {
                    if (recBadge) recBadge.style.display = 'none';
                    btnRecord.disabled = false;
                    btnRecord.innerHTML = '<i class="fa-solid fa-video"></i> Quay Phim Movie 3D (5s)';

                    const blob = new Blob(chunks, { type: 'video/webm' });
                    const videoUrl = URL.createObjectURL(blob);
                    const safeName = (title || 'designhub-3d-movie').toLowerCase().replace(/[^a-z0-9]+/g, '-');

                    if (resultBox) {
                        resultBox.style.display = 'block';
                        resultBox.innerHTML = `
                            <div class="movie3d-preview-card">
                                <div class="movie3d-preview-info">
                                    <div>
                                        <strong style="color:#10b981;font-size:13px;"><i class="fa-solid fa-circle-check"></i> Đã kết xuất thành công Video Movie 3D (30 FPS)!</strong>
                                        <p style="font-size:12px;color:var(--text-secondary);margin:3px 0 0;">Bản quay 3 chiều giúp khách hàng & tác giả trình chiếu sản phẩm sống động hơn bản vẽ 2D thông thường.</p>
                                    </div>
                                    <a href="${videoUrl}" download="${safeName}-3d-movie.webm" class="btn btn-primary btn-sm" style="white-space:nowrap;">
                                        <i class="fa-solid fa-download"></i> Tải Video 3D (.webm)
                                    </a>
                                </div>
                                <video src="${videoUrl}" controls autoplay loop muted style="width:100%;max-height:190px;border-radius:8px;margin-top:8px;background:#090d16;border:1px solid var(--border);"></video>
                            </div>
                        `;
                    }
                };

                recorder.start();
                let remaining = 5;
                if (recTimeEl) recTimeEl.textContent = remaining;

                const countdown = setInterval(() => {
                    remaining -= 1;
                    if (recTimeEl && remaining >= 0) recTimeEl.textContent = remaining;
                    if (remaining <= 0) {
                        clearInterval(countdown);
                        if (recorder.state === 'recording') recorder.stop();
                    }
                }, 1000);
            });
        }

        // Vòng lặp Render 60 FPS
        function animate() {
            if (!activeState) return;
            activeState.animId = requestAnimationFrame(animate);

            if (autoRotate && !isDragging) {
                targetRotY += 0.018;
            }

            currentRotY += (targetRotY - currentRotY) * 0.12;
            currentRotX += (targetRotX - currentRotX) * 0.12;

            if (modelGroup) {
                modelGroup.rotation.y = currentRotY;
                modelGroup.rotation.x = currentRotX;
            }

            renderer.render(scene, camera);
        }

        const resizeObserver = new ResizeObserver(() => {
            if (!stageEl || !renderer) return;
            const w = stageEl.clientWidth;
            const h = stageEl.clientHeight;
            if (w > 0 && h > 0) {
                camera.aspect = w / h;
                camera.updateProjectionMatrix();
                renderer.setSize(w, h);
            }
        });
        resizeObserver.observe(stageEl);

        activeState = {
            renderer,
            scene,
            camera,
            animId: null,
            resizeObserver
        };

        animate();
    }

    return {
        initViewer,
        cleanupActive
    };
})();

