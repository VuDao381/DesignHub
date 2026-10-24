let currentDesigner = null;
let uploadedImages = [];
let uploadedSourceFiles = [];
let selectedDesignForFiles = null;

function formatCurrency(amount) {
    if (amount === undefined || amount === null) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

function formatFileSize(bytes) {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function showToast(message, type = 'info') {
    const toastEl = document.getElementById('appToast');
    const toastMessage = document.getElementById('toastMessage');
    if (!toastEl || !toastMessage) return;

    toastEl.className = 'toast align-items-center text-white border-0 rounded-3 shadow';
    if (type === 'success') toastEl.classList.add('bg-success');
    else if (type === 'danger' || type === 'error') toastEl.classList.add('bg-danger');
    else if (type === 'warning') toastEl.classList.add('bg-warning', 'text-dark');
    else toastEl.classList.add('bg-primary');

    toastMessage.querySelector('span').textContent = message;
    const toast = bootstrap.Toast.getOrCreateInstance(toastEl, { delay: 4000 });
    toast.show();
}

window.showToast = showToast;

async function initDesignerPortal() {
    const sectionGuest = document.getElementById('sectionGuest');
    const sectionRegister = document.getElementById('sectionRegisterDesigner');
    const sectionPortal = document.getElementById('sectionPortal');

    // 1. Kiểm tra xác thực
    if (!TokenStorage.isAuthenticated()) {
        sectionGuest.classList.remove('d-none');
        sectionRegister.classList.add('d-none');
        sectionPortal.classList.add('d-none');
        return;
    }

    try {
        // Lấy thông tin user hiện tại
        const user = await api.auth.getMe();
        TokenStorage.setUser(user);

        // 2. Kiểm tra xem user đã có hồ sơ Designer chưa
        try {
            currentDesigner = await api.designer.getMe();
        } catch (err) {
            currentDesigner = null;
        }

        if (!currentDesigner) {
            // User đăng nhập nhưng chưa đăng ký Designer
            sectionGuest.classList.add('d-none');
            sectionRegister.classList.remove('d-none');
            sectionPortal.classList.add('d-none');

            const fullNameInput = document.getElementById('regDesignerFullName');
            if (fullNameInput) {
                fullNameInput.value = user.fullName || user.username || '';
            }
        } else {
            // Đã là Designer
            sectionGuest.classList.add('d-none');
            sectionRegister.classList.add('d-none');
            sectionPortal.classList.remove('d-none');

            renderDesignerHeader(user, currentDesigner);
            await loadDesignerDashboard();
            await loadMyDesigns();
            await loadCategories();
        }
    } catch (error) {
        console.error('Lỗi khởi tạo Designer Portal:', error);
        sectionGuest.classList.remove('d-none');
        sectionRegister.classList.add('d-none');
        sectionPortal.classList.add('d-none');
    }
}

function renderDesignerHeader(user, designer) {
    const nameEl = document.getElementById('designerProfileName');
    const emailEl = document.getElementById('designerProfileEmail');
    const bioEl = document.getElementById('designerProfileBio');
    const avatarEl = document.getElementById('designerProfileAvatar');
    const roleBadge = document.getElementById('designerRoleBadge');

    if (nameEl) nameEl.textContent = user.fullName || user.username || 'Designer';
    if (emailEl) emailEl.textContent = user.email || '';
    if (bioEl) bioEl.textContent = designer.bio || 'Chưa có thông tin giới thiệu.';
    if (avatarEl && designer.avatar) {
        avatarEl.src = designer.avatar;
    }

    if (roleBadge) {
        if (user.role === 'ADMIN') {
            roleBadge.textContent = 'QUẢN TRỊ VIÊN / DESIGNER';
            roleBadge.className = 'badge bg-danger px-3 py-1 rounded-pill';
        } else {
            roleBadge.textContent = 'DESIGNER CHÍNH THỨC';
            roleBadge.className = 'badge bg-primary px-3 py-1 rounded-pill';
        }
    }
}

async function loadDesignerDashboard() {
    try {
        const stats = await api.designer.getDashboard();

        document.getElementById('kpiTotalRevenue').textContent = formatCurrency(stats.totalRevenue);
        document.getElementById('kpiTotalSold').textContent = stats.totalSold || 0;
        document.getElementById('kpiTotalDownloads').textContent = stats.totalDownloads || 0;
        document.getElementById('kpiTotalFavorites').textContent = stats.totalFavorites || 0;

        const tbody = document.getElementById('topSellingTableBody');
        if (!tbody) return;

        if (!stats.topDesigns || stats.topDesigns.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="5" class="text-center py-5 text-muted">
                        <i class="bi bi-inbox fs-2 d-block mb-2 text-secondary"></i>
                        Bạn chưa có đơn hàng nào. Hãy đăng tải thêm thiết kế hấp dẫn để bứt phá doanh thu!
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = stats.topDesigns.map((item, index) => `
            <tr>
                <td class="ps-3 fw-bold text-muted">${index + 1}</td>
                <td>
                    <div class="d-flex align-items-center gap-3">
                        <img src="${item.imageUrl || '/images/card.png'}" alt="${item.designName}"
                            class="rounded-3 border object-fit-cover" style="width: 50px; height: 50px;"
                            onerror="this.src='/images/card.png'">
                        <div>
                            <div class="fw-bold text-dark">${item.designName}</div>
                            <small class="text-muted">Mã thiết kế: #${item.designId}</small>
                        </div>
                    </div>
                </td>
                <td class="text-center fw-semibold text-secondary">${formatCurrency(item.price)}</td>
                <td class="text-center">
                    <span class="badge bg-primary-subtle text-primary px-3 py-2 rounded-pill fw-bold">
                        ${item.totalSold ?? item.salesCount ?? 0} lượt bán
                    </span>
                </td>
                <td class="text-end pe-3 fw-bold text-success fs-6">${formatCurrency(item.totalRevenue)}</td>
            </tr>
        `).join('');
    } catch (error) {
        console.error('Lỗi khi tải số liệu thống kê:', error);
        showToast('Không thể tải số liệu thống kê: ' + error.message, 'danger');
    }
}

async function loadCategories() {
    const select = document.getElementById('designCategorySelect');
    if (!select) return;

    try {
        const categories = await api.categories.getAll();
        select.innerHTML = '<option value="" disabled selected>Chọn danh mục thiết kế...</option>' +
            categories.map(cat => `<option value="${cat.id}">${cat.name}</option>`).join('');
    } catch (err) {
        console.warn('Lỗi tải danh mục:', err);
    }
}

async function loadMyDesigns() {
    const container = document.getElementById('myDesignsContainer');
    if (!container) return;

    const keyword = document.getElementById('myDesignSearchKeyword')?.value || '';

    container.innerHTML = `
        <div class="col-12 text-center py-5">
            <span class="spinner-border text-primary"></span>
            <p class="text-muted mt-2">Đang tải danh sách thiết kế...</p>
        </div>
    `;

    try {
        const res = await api.designs.getMyDesigns({ keyword, size: 50 });
        const designs = res.content || res || [];

        if (designs.length === 0) {
            container.innerHTML = `
                <div class="col-12 text-center py-5">
                    <div class="bg-light p-5 rounded-4 border d-inline-block max-w-500 mx-auto">
                        <i class="bi bi-palette fs-1 text-primary mb-3 d-block"></i>
                        <h5 class="fw-bold mb-2">Chưa có thiết kế nào</h5>
                        <p class="text-muted small mb-4">Bạn chưa đăng tải thiết kế nào hoặc không tìm thấy kết quả phù hợp.</p>
                        <button class="btn btn-primary rounded-pill px-4 fw-bold" onclick="document.getElementById('tab-upload-btn').click()">
                            <i class="bi bi-cloud-arrow-up-fill me-2"></i> Đăng tải thiết kế đầu tiên
                        </button>
                    </div>
                </div>
            `;
            return;
        }

        // Lấy danh sách ảnh & files cho từng design
        const cardsHtml = await Promise.all(designs.map(async (design) => {
            let primaryImg = '/images/card.png';
            try {
                const images = await api.designs.getImages(design.id);
                if (images && images.length > 0) {
                    const primary = images.find(img => img.isPrimary) || images[0];
                    primaryImg = primary.imageUrl;
                }
            } catch { }

            let filesCount = 0;
            try {
                const files = await api.designs.getFiles(design.id);
                filesCount = files ? files.length : 0;
            } catch { }

            const statusBadge = getStatusBadge(design.status);

            return `
                <div class="col-sm-6 col-lg-4 col-xl-3">
                    <div class="card h-100 border rounded-4 overflow-hidden shadow-sm product-card transition-all">
                        <div class="position-relative overflow-hidden" style="height: 180px; background: #f8f9fa;">
                            <img src="${primaryImg}" class="w-100 h-100 object-fit-cover" alt="${design.name}"
                                onerror="this.src='/images/card.png'">
                            <div class="position-absolute top-0 end-0 m-2">
                                ${statusBadge}
                            </div>
                            <div class="position-absolute bottom-0 start-0 m-2">
                                <span class="badge bg-dark bg-opacity-75 text-white rounded-pill px-2 py-1 small">
                                    <i class="bi bi-paperclip"></i> ${filesCount} file gốc
                                </span>
                            </div>
                        </div>
                        <div class="card-body p-3 d-flex flex-column">
                            <span class="badge bg-secondary-subtle text-secondary align-self-start mb-2" style="font-size: 11px;">
                                ${design.category?.name || 'Khác'}
                            </span>
                            <h6 class="fw-bold mb-1 text-truncate" title="${design.name}">${design.name}</h6>
                            <p class="text-primary fw-bold fs-6 mb-3">${formatCurrency(design.price)}</p>

                            <div class="mt-auto d-flex gap-2">
                                <button type="button" class="btn btn-outline-primary btn-sm flex-fill rounded-pill"
                                    onclick="openManageFilesModal(${design.id}, '${escapeHtml(design.name)}')">
                                    <i class="bi bi-folder-fill me-1"></i> File gốc
                                </button>
                                <button type="button" class="btn btn-outline-danger btn-sm rounded-circle p-2"
                                    title="Xoá thiết kế" onclick="handleDeleteDesign(${design.id}, '${escapeHtml(design.name)}')">
                                    <i class="bi bi-trash"></i>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        }));

        container.innerHTML = cardsHtml.join('');
    } catch (error) {
        console.error('Lỗi khi tải danh sách thiết kế:', error);
        container.innerHTML = `
            <div class="col-12 text-center py-4 text-danger">
                Không thể tải danh sách thiết kế: ${error.message}
            </div>
        `;
    }
}

function getStatusBadge(status) {
    const s = (status || 'PENDING').toUpperCase();
    if (s === 'APPROVED') {
        return '<span class="badge bg-success px-2 py-1 shadow-sm"><i class="bi bi-check-circle me-1"></i> Đã duyệt</span>';
    } else if (s === 'REJECTED') {
        return '<span class="badge bg-danger px-2 py-1 shadow-sm"><i class="bi bi-x-circle me-1"></i> Từ chối</span>';
    }
    return '<span class="badge bg-warning text-dark px-2 py-1 shadow-sm"><i class="bi bi-clock-history me-1"></i> Chờ duyệt</span>';
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/['"&<>]/g, (match) => {
        const escape = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#39;'
        };
        return escape[match];
    });
}

async function handleRegisterDesigner(e) {
    e.preventDefault();
    const btn = document.getElementById('btnSubmitRegisterDesigner');
    const alertBox = document.getElementById('registerDesignerAlert');
    const bio = document.getElementById('regDesignerBio').value.trim();
    const avatar = document.getElementById('regDesignerAvatar').value.trim();

    if (alertBox) alertBox.classList.add('d-none');
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang kích hoạt...';

    try {
        await api.designer.register({ bio, avatar: avatar || null });
        showToast('Chúc mừng bạn đã trở thành Designer đối tác của DesignHub!', 'success');
        setTimeout(() => {
            window.location.reload();
        }, 1000);
    } catch (err) {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-check-circle me-2"></i> Kích hoạt Kênh Designer ngay';
        if (alertBox) {
            alertBox.textContent = err.message || 'Đăng ký thất bại. Vui lòng thử lại sau.';
            alertBox.classList.remove('d-none');
        }
    }
}

async function handleImageFiles(files) {
    if (!files || files.length === 0) return;

    for (const file of Array.from(files)) {
        if (!file.type.startsWith('image/')) {
            showToast(`Tệp ${file.name} không phải là hình ảnh hợp lệ!`, 'warning');
            continue;
        }

        try {
            showToast(`Đang tải ảnh ${file.name}...`, 'info');
            const res = await api.upload.image(file);

            const isFirst = uploadedImages.length === 0;
            uploadedImages.push({
                fileUrl: res.fileUrl,
                isPrimary: isFirst,
                originalName: res.originalFileName || file.name
            });

            renderUploadedImages();
            showToast(`Tải ảnh ${file.name} thành công!`, 'success');
        } catch (err) {
            showToast(`Lỗi tải ảnh ${file.name}: ${err.message}`, 'danger');
        }
    }
}

function renderUploadedImages() {
    const container = document.getElementById('uploadedImagesContainer');
    if (!container) return;

    if (uploadedImages.length === 0) {
        container.innerHTML = '';
        return;
    }

    container.innerHTML = uploadedImages.map((img, index) => `
        <div class="col-6 col-sm-4 col-md-3">
            <div class="card h-100 border rounded-3 overflow-hidden position-relative shadow-sm ${img.isPrimary ? 'border-primary border-2' : ''}">
                <div style="height: 120px; background: #f8f9fa;">
                    <img src="${img.fileUrl}" class="w-100 h-100 object-fit-cover" alt="Preview">
                </div>
                <div class="p-2 bg-white d-flex justify-content-between align-items-center">
                    <div class="form-check mb-0">
                        <input class="form-check-input" type="radio" name="primaryImageRadio" id="imgPrimary_${index}"
                            ${img.isPrimary ? 'checked' : ''} onchange="setPrimaryImage(${index})">
                        <label class="form-check-label small" for="imgPrimary_${index}">
                            ${img.isPrimary ? '<strong class="text-primary">Ảnh bìa</strong>' : 'Đặt làm bìa'}
                        </label>
                    </div>
                    <button type="button" class="btn btn-link text-danger p-0" title="Xoá ảnh" onclick="removeUploadedImage(${index})">
                        <i class="bi bi-trash"></i>
                    </button>
                </div>
            </div>
        </div>
    `).join('');
}

function setPrimaryImage(index) {
    uploadedImages.forEach((img, i) => {
        img.isPrimary = (i === index);
    });
    renderUploadedImages();
}

function removeUploadedImage(index) {
    uploadedImages.splice(index, 1);
    if (uploadedImages.length > 0 && !uploadedImages.some(img => img.isPrimary)) {
        uploadedImages[0].isPrimary = true;
    }
    renderUploadedImages();
}

async function handleSourceFile(file) {
    if (!file) return;

    const progressBar = document.getElementById('fileUploadProgressBar');
    if (progressBar) progressBar.classList.remove('d-none');

    try {
        showToast(`Đang tải file gốc ${file.name}...`, 'info');
        const res = await api.upload.file(file);

        uploadedSourceFiles.push({
            fileName: res.originalFileName || file.name,
            fileUrl: res.fileUrl,
            fileSize: res.fileSize || file.size,
            fileType: res.contentType || file.type || 'application/octet-stream'
        });

        renderUploadedSourceFiles();
        showToast(`Đã tải lên tệp gốc: ${file.name}`, 'success');
    } catch (err) {
        showToast(`Lỗi tải tệp gốc: ${err.message}`, 'danger');
    } finally {
        if (progressBar) progressBar.classList.add('d-none');
    }
}

function renderUploadedSourceFiles() {
    const container = document.getElementById('uploadedSourceFilesContainer');
    if (!container) return;

    if (uploadedSourceFiles.length === 0) {
        container.innerHTML = '';
        return;
    }

    container.innerHTML = uploadedSourceFiles.map((file, index) => `
        <div class="card p-3 rounded-3 border bg-white mb-2 shadow-sm">
            <div class="d-flex align-items-center justify-content-between">
                <div class="d-flex align-items-center gap-3">
                    <div class="bg-success-subtle text-success p-2 rounded-3">
                        <i class="bi bi-file-earmark-zip fs-4"></i>
                    </div>
                    <div>
                        <div class="fw-bold text-dark text-truncate" style="max-width: 350px;">${file.fileName}</div>
                        <small class="text-muted">${formatFileSize(file.fileSize)} • ${file.fileType}</small>
                    </div>
                </div>
                <button type="button" class="btn btn-outline-danger btn-sm rounded-circle p-2" title="Xóa file này"
                    onclick="removeUploadedSourceFile(${index})">
                    <i class="bi bi-trash"></i>
                </button>
            </div>
        </div>
    `).join('');
}

function removeUploadedSourceFile(index) {
    uploadedSourceFiles.splice(index, 1);
    renderUploadedSourceFiles();
}

async function handleCreateDesignSubmit(e) {
    e.preventDefault();
    const alertBox = document.getElementById('uploadDesignAlert');
    const submitBtn = document.getElementById('btnSubmitNewDesign');

    if (alertBox) alertBox.classList.add('d-none');

    // Kiểm tra đã upload ảnh chưa
    if (uploadedImages.length === 0) {
        if (alertBox) {
            alertBox.textContent = 'Vui lòng upload ít nhất 1 hình ảnh xem trước (mockup)!';
            alertBox.classList.remove('d-none');
        }
        showToast('Vui lòng upload ít nhất 1 hình ảnh xem trước!', 'warning');
        return;
    }

    // Kiểm tra đã upload file gốc chưa
    if (uploadedSourceFiles.length === 0) {
        if (alertBox) {
            alertBox.textContent = 'Vui lòng upload ít nhất 1 tệp tin thiết kế gốc (ZIP, RAR, PSD, AI...)!';
            alertBox.classList.remove('d-none');
        }
        showToast('Vui lòng upload ít nhất 1 tệp tin thiết kế gốc!', 'warning');
        return;
    }

    const name = document.getElementById('designNameInput').value.trim();
    const categoryId = document.getElementById('designCategorySelect').value;
    const price = parseFloat(document.getElementById('designPriceInput').value);
    const description = document.getElementById('designDescriptionInput').value.trim();

    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang tạo thiết kế & liên kết tệp...';

    try {
        // 1. Tạo bản ghi Design
        const newDesign = await api.designs.create({
            name,
            description,
            price,
            category: { id: parseInt(categoryId) }
        });

        const designId = newDesign.id;

        // 2. Liên kết các hình ảnh
        for (const img of uploadedImages) {
            await api.designs.addImage(designId, img.fileUrl, img.isPrimary);
        }

        // 3. Liên kết các file gốc
        for (const f of uploadedSourceFiles) {
            await api.designs.addFile(designId, f);
        }

        showToast('Thiết kế đã được đăng tải và gửi duyệt thành công!', 'success');

        // Reset form
        document.getElementById('formCreateDesign').reset();
        uploadedImages = [];
        uploadedSourceFiles = [];
        renderUploadedImages();
        renderUploadedSourceFiles();

        // Chuyển sang Tab "Thiết kế của tôi" và reload
        document.getElementById('tab-designs-btn').click();
        await loadMyDesigns();
        await loadDesignerDashboard();
    } catch (err) {
        console.error('Lỗi tạo thiết kế:', err);
        if (alertBox) {
            alertBox.textContent = err.message || 'Lỗi khi lưu thiết kế. Vui lòng thử lại!';
            alertBox.classList.remove('d-none');
        }
        showToast('Lỗi khi lưu thiết kế: ' + err.message, 'danger');
    } finally {
        submitBtn.disabled = false;
        submitBtn.innerHTML = '<i class="bi bi-check-lg me-2"></i> Hoàn tất & Gửi duyệt thiết kế';
    }
}

async function handleDeleteDesign(designId, designName) {
    if (!confirm(`Bạn có chắc chắn muốn xóa thiết kế "${designName}"? Thao tác này không thể hoàn tác!`)) {
        return;
    }

    try {
        await api.designs.delete(designId);
        showToast(`Đã xóa thiết kế "${designName}" thành công!`, 'success');
        await loadMyDesigns();
        await loadDesignerDashboard();
    } catch (err) {
        showToast(`Không thể xóa thiết kế: ${err.message}`, 'danger');
    }
}

async function openManageFilesModal(designId, designName) {
    selectedDesignForFiles = designId;
    const modalEl = document.getElementById('manageFilesModal');
    const titleEl = document.getElementById('manageFilesModalTitle');
    const listEl = document.getElementById('modalFilesList');

    if (titleEl) titleEl.textContent = `Tệp tin gốc: ${designName}`;
    if (listEl) {
        listEl.innerHTML = '<div class="text-center py-3 text-muted"><span class="spinner-border spinner-border-sm me-2"></span> Đang tải file...</div>';
    }

    const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
    modal.show();

    await refreshModalFilesList(designId);
}

async function refreshModalFilesList(designId) {
    const listEl = document.getElementById('modalFilesList');
    if (!listEl) return;

    try {
        const files = await api.designs.getFiles(designId);

        if (!files || files.length === 0) {
            listEl.innerHTML = `
                <div class="text-center py-4 text-muted bg-light rounded-3 border">
                    <i class="bi bi-folder-x fs-2 d-block mb-1 text-secondary"></i>
                    Thiết kế này chưa có tệp tin gốc đính kèm.
                </div>
            `;
            return;
        }

        listEl.innerHTML = files.map(file => `
            <div class="card p-3 rounded-3 border bg-white mb-2 shadow-sm">
                <div class="d-flex align-items-center justify-content-between">
                    <div class="d-flex align-items-center gap-3">
                        <div class="bg-primary-subtle text-primary p-2 rounded-3">
                            <i class="bi bi-file-earmark-code fs-4"></i>
                        </div>
                        <div>
                            <div class="fw-bold text-dark">${file.fileName}</div>
                            <small class="text-muted">${formatFileSize(file.fileSize)} • ${file.fileType || 'N/A'}</small>
                        </div>
                    </div>
                    <div class="d-flex gap-2">
                        <a href="${api.files.getDownloadUrl(file.id)}" class="btn btn-outline-success btn-sm rounded-pill" target="_blank" title="Tải thử">
                            <i class="bi bi-download"></i> Tải thử
                        </a>
                        <button type="button" class="btn btn-outline-danger btn-sm rounded-circle p-2" title="Xóa file"
                            onclick="handleDeleteFileInModal(${file.id})">
                            <i class="bi bi-trash"></i>
                        </button>
                    </div>
                </div>
            </div>
        `).join('');
    } catch (err) {
        listEl.innerHTML = `<div class="text-danger py-3 text-center">Không thể tải danh sách file: ${err.message}</div>`;
    }
}

async function handleDeleteFileInModal(fileId) {
    if (!confirm('Bạn có chắc muốn xóa tệp tin này khỏi thiết kế?')) return;

    try {
        await api.designs.deleteFile(fileId);
        showToast('Đã xóa tệp tin thành công!', 'success');
        if (selectedDesignForFiles) {
            await refreshModalFilesList(selectedDesignForFiles);
            await loadMyDesigns();
        }
    } catch (err) {
        showToast('Không thể xóa tệp: ' + err.message, 'danger');
    }
}

document.addEventListener('DOMContentLoaded', () => {
    initDesignerPortal();

    // Form đăng ký Designer
    const regForm = document.getElementById('registerDesignerForm');
    if (regForm) regForm.addEventListener('submit', handleRegisterDesigner);

    // Form tạo thiết kế mới
    const createForm = document.getElementById('formCreateDesign');
    if (createForm) createForm.addEventListener('submit', handleCreateDesignSubmit);

    // Nút làm mới analytics
    const btnRefresh = document.getElementById('btnRefreshAnalytics');
    if (btnRefresh) {
        btnRefresh.addEventListener('click', async () => {
            btnRefresh.disabled = true;
            await loadDesignerDashboard();
            btnRefresh.disabled = false;
            showToast('Đã cập nhật số liệu mới nhất!', 'info');
        });
    }

    // Nút chuyển tab
    const btnGoToUpload = document.getElementById('btnGoToUploadTab');
    if (btnGoToUpload) {
        btnGoToUpload.addEventListener('click', () => {
            document.getElementById('tab-upload-btn')?.click();
        });
    }

    const btnSwitchToUpload = document.getElementById('btnSwitchToUpload');
    if (btnSwitchToUpload) {
        btnSwitchToUpload.addEventListener('click', () => {
            document.getElementById('tab-upload-btn')?.click();
        });
    }

    // Tìm kiếm thiết kế
    const searchInput = document.getElementById('myDesignSearchKeyword');
    if (searchInput) {
        let debounceTimer = null;
        searchInput.addEventListener('input', () => {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => {
                loadMyDesigns();
            }, 300);
        });
    }

    // Dropzone Mockup Image
    const imgDropzone = document.getElementById('imageDropzone');
    const imgInput = document.getElementById('imageFileInput');
    if (imgDropzone && imgInput) {
        imgDropzone.addEventListener('click', () => imgInput.click());
        imgDropzone.addEventListener('dragover', (e) => {
            e.preventDefault();
            imgDropzone.classList.add('border-primary', 'bg-primary-subtle');
        });
        imgDropzone.addEventListener('dragleave', () => {
            imgDropzone.classList.remove('border-primary', 'bg-primary-subtle');
        });
        imgDropzone.addEventListener('drop', (e) => {
            e.preventDefault();
            imgDropzone.classList.remove('border-primary', 'bg-primary-subtle');
            handleImageFiles(e.dataTransfer.files);
        });
        imgInput.addEventListener('change', () => {
            handleImageFiles(imgInput.files);
            imgInput.value = '';
        });
    }

    // Dropzone Source File
    const fileDropzone = document.getElementById('fileDropzone');
    const fileInput = document.getElementById('designSourceFileInput');
    if (fileDropzone && fileInput) {
        fileDropzone.addEventListener('click', () => fileInput.click());
        fileDropzone.addEventListener('dragover', (e) => {
            e.preventDefault();
            fileDropzone.classList.add('border-success', 'bg-success-subtle');
        });
        fileDropzone.addEventListener('dragleave', () => {
            fileDropzone.classList.remove('border-success', 'bg-success-subtle');
        });
        fileDropzone.addEventListener('drop', (e) => {
            e.preventDefault();
            fileDropzone.classList.remove('border-success', 'bg-success-subtle');
            if (e.dataTransfer.files.length > 0) {
                handleSourceFile(e.dataTransfer.files[0]);
            }
        });
        fileInput.addEventListener('change', () => {
            if (fileInput.files.length > 0) {
                handleSourceFile(fileInput.files[0]);
            }
            fileInput.value = '';
        });
    }

    // Modal add file directly to existing design
    const btnModalUpload = document.getElementById('btnModalUploadFile');
    const modalFileInput = document.getElementById('modalAddFileInput');
    if (btnModalUpload && modalFileInput) {
        btnModalUpload.addEventListener('click', async () => {
            if (!modalFileInput.files || modalFileInput.files.length === 0) {
                showToast('Vui lòng chọn một tệp để tải lên!', 'warning');
                return;
            }
            if (!selectedDesignForFiles) return;

            const file = modalFileInput.files[0];
            btnModalUpload.disabled = true;
            btnModalUpload.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Đang tải...';

            try {
                const res = await api.upload.file(file);
                await api.designs.addFile(selectedDesignForFiles, {
                    fileName: res.originalFileName || file.name,
                    fileUrl: res.fileUrl,
                    fileSize: res.fileSize || file.size,
                    fileType: res.contentType || file.type
                });
                showToast('Đã thêm tệp thiết kế thành công!', 'success');
                modalFileInput.value = '';
                await refreshModalFilesList(selectedDesignForFiles);
                await loadMyDesigns();
            } catch (err) {
                showToast('Lỗi khi thêm tệp: ' + err.message, 'danger');
            } finally {
                btnModalUpload.disabled = false;
                btnModalUpload.innerHTML = '<i class="bi bi-upload me-1"></i> Tải lên';
            }
        });
    }
});

window.addEventListener('auth:login_success', () => {
    initDesignerPortal();
});
