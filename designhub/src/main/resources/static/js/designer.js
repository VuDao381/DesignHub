let currentDesigner = null;
let uploadedImages = [];
let uploadedSourceFiles = [];
let selectedDesignForFiles = null;

const DEFAULT_THUMBNAIL = "data:image/svg+xml;charset=UTF-8,%3Csvg xmlns='http://www.w3.org/2000/svg' width='120' height='120' viewBox='0 0 120 120'%3E%3Crect width='120' height='120' fill='%23eef2ff'/%3E%3Ctext x='50%25' y='50%25' dominant-baseline='middle' text-anchor='middle' fill='%236366f1' font-family='sans-serif' font-size='14' font-weight='bold'%3EDesignHub%3C/text%3E%3C/svg%3E";
const DEFAULT_AVATAR = "data:image/svg+xml;charset=UTF-8,%3Csvg xmlns='http://www.w3.org/2000/svg' width='120' height='120' viewBox='0 0 120 120'%3E%3Ccircle cx='60' cy='60' r='60' fill='%236366f1'/%3E%3Ctext x='50%25' y='52%25' dominant-baseline='middle' text-anchor='middle' fill='%23ffffff' font-family='sans-serif' font-size='44' font-weight='bold'%3ED%3C/text%3E%3C/svg%3E";

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
            await loadDesignerCustomRequests();
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
    if (avatarEl) {
        avatarEl.src = designer.avatar || DEFAULT_AVATAR;
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
                        <img src="${item.imageUrl || DEFAULT_THUMBNAIL}" alt="${escapeHtml(item.designName)}"
                            class="rounded-3 border object-fit-cover" style="width: 50px; height: 50px;"
                            onerror="this.onerror=null; this.src='${DEFAULT_THUMBNAIL}'">
                        <div>
                            <div class="fw-bold text-dark">${escapeHtml(item.designName)}</div>
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
    const editSelect = document.getElementById('editDesignCategorySelect');

    try {
        const res = await api.categories.getAll();
        const categories = Array.isArray(res) ? res : (res && Array.isArray(res.content) ? res.content : []);
        const optionsHtml = categories.map(cat => `<option value="${cat.id}">${escapeHtml(cat.name)}</option>`).join('');
        if (select) {
            select.innerHTML = '<option value="" disabled selected>Chọn danh mục thiết kế...</option>' + optionsHtml;
        }
        if (editSelect) {
            editSelect.innerHTML = optionsHtml;
        }
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
            let primaryImg = DEFAULT_THUMBNAIL;
            try {
                if (design.images && design.images.length > 0) {
                    const primary = design.images.find(img => img.isPrimary) || design.images[0];
                    primaryImg = primary.imageUrl;
                } else {
                    const images = await api.designs.getImages(design.id);
                    if (images && images.length > 0) {
                        const primary = images.find(img => img.isPrimary) || images[0];
                        primaryImg = primary.imageUrl;
                    }
                }
            } catch { }

            let filesCount = 0;
            try {
                if (design.files && design.files.length > 0) {
                    filesCount = design.files.length;
                } else {
                    const files = await api.designs.getFiles(design.id);
                    filesCount = files ? files.length : 0;
                }
            } catch { }

            const statusBadge = getStatusBadge(design.status);
            const isRejected = String(design.status || '').toUpperCase() === 'REJECTED';
            const rejectBox = (isRejected && design.rejectReason) ? `
                <div class="alert alert-danger py-2 px-2 small mb-2 border-danger-subtle">
                    <i class="bi bi-exclamation-octagon-fill me-1"></i><strong>Lý do từ chối:</strong> ${escapeHtml(design.rejectReason)}
                </div>
            ` : '';

            return `
                <div class="col-sm-6 col-lg-4 col-xl-3">
                    <div class="card h-100 border rounded-4 overflow-hidden shadow-sm product-card transition-all">
                        <div class="position-relative overflow-hidden" style="height: 180px; background: #f8f9fa;">
                            <img src="${primaryImg}" class="w-100 h-100 object-fit-cover" alt="${escapeHtml(design.name)}"
                                onerror="this.onerror=null; this.src='${DEFAULT_THUMBNAIL}'">
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
                                ${escapeHtml(design.category?.name || 'Khác')}
                            </span>
                            <h6 class="fw-bold mb-1 text-truncate" title="${escapeHtml(design.name)}">${escapeHtml(design.name)}</h6>
                            <p class="text-primary fw-bold fs-6 mb-2">${formatCurrency(design.price)}</p>
                            ${rejectBox}

                            <div class="mt-auto d-flex flex-wrap gap-2">
                                <button type="button" class="btn btn-outline-secondary btn-sm flex-fill rounded-pill"
                                    onclick="openEditDesignModal(${design.id})" title="Chỉnh sửa thông tin thiết kế">
                                    <i class="bi bi-pencil-square me-1"></i> Sửa
                                </button>
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
                    <img src="${img.fileUrl}" class="w-100 h-100 object-fit-cover" alt="Preview"
                        onerror="this.onerror=null; this.src='${DEFAULT_THUMBNAIL}'">
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
                        <div class="fw-bold text-dark text-truncate" style="max-width: 350px;">${escapeHtml(file.fileName)}</div>
                        <small class="text-muted">${formatFileSize(file.fileSize)} • ${escapeHtml(file.fileType)}</small>
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
                            <div class="fw-bold text-dark">${escapeHtml(file.fileName)}</div>
                            <small class="text-muted">${formatFileSize(file.fileSize)} • ${escapeHtml(file.fileType || 'N/A')}</small>
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

function openEditDesignerProfileModal() {
    if (!currentDesigner) return;

    const alertBox = document.getElementById('editDesignerProfileAlert');
    if (alertBox) alertBox.classList.add('d-none');

    const avatarUrlInput = document.getElementById('editDesignerAvatarUrl');
    const avatarPreview = document.getElementById('editDesignerAvatarPreview');
    const bioInput = document.getElementById('editDesignerBioInput');

    if (avatarUrlInput) avatarUrlInput.value = currentDesigner.avatar || '';
    if (avatarPreview) avatarPreview.src = currentDesigner.avatar || DEFAULT_AVATAR;
    if (bioInput) bioInput.value = currentDesigner.bio || '';

    const modalEl = document.getElementById('editDesignerProfileModal');
    if (modalEl) bootstrap.Modal.getOrCreateInstance(modalEl).show();
}
window.openEditDesignerProfileModal = openEditDesignerProfileModal;

async function handleUpdateDesignerProfile(e) {
    e.preventDefault();
    if (!currentDesigner) return;

    const alertBox = document.getElementById('editDesignerProfileAlert');
    const btn = document.getElementById('btnSaveDesignerProfile');
    const bio = document.getElementById('editDesignerBioInput').value.trim();
    const avatar = document.getElementById('editDesignerAvatarUrl').value.trim();

    if (alertBox) alertBox.classList.add('d-none');
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang lưu...';

    try {
        const user = TokenStorage.getUser();
        const updated = await api.designer.update(currentDesigner.id, {
            bio,
            avatar: avatar || null,
            user: user && user.id ? { id: user.id } : undefined
        });
        currentDesigner = { ...currentDesigner, ...updated };
        renderDesignerHeader(user || {}, currentDesigner);
        showToast('Cập nhật hồ sơ Designer thành công!', 'success');

        const modalEl = document.getElementById('editDesignerProfileModal');
        bootstrap.Modal.getInstance(modalEl)?.hide();
    } catch (err) {
        if (alertBox) {
            alertBox.textContent = err.message || 'Không thể cập nhật hồ sơ Designer';
            alertBox.classList.remove('d-none');
        }
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-check2-circle me-1"></i> Lưu hồ sơ';
    }
}

async function openEditDesignModal(designId) {
    const modalEl = document.getElementById('editDesignModal');
    if (!modalEl) return;

    const alertBox = document.getElementById('editDesignAlert');
    const rejectNotice = document.getElementById('editDesignRejectNotice');
    if (alertBox) alertBox.classList.add('d-none');
    if (rejectNotice) rejectNotice.classList.add('d-none');

    const catSelect = document.getElementById('editDesignCategorySelect');
    if (catSelect && catSelect.options.length === 0) {
        await loadCategories();
    }

    bootstrap.Modal.getOrCreateInstance(modalEl).show();

    try {
        const design = await api.designs.getById(designId);
        document.getElementById('editDesignIdInput').value = design.id;
        document.getElementById('editDesignNameInput').value = design.name || '';
        document.getElementById('editDesignPriceInput').value = design.price || 0;
        document.getElementById('editDesignDescriptionInput').value = design.description || '';
        if (design.category && design.category.id && catSelect) {
            catSelect.value = String(design.category.id);
        }

        if (String(design.status || '').toUpperCase() === 'REJECTED' && design.rejectReason && rejectNotice) {
            rejectNotice.innerHTML = `<i class="bi bi-exclamation-octagon-fill me-1"></i><strong>Lý do bị từ chối trước đó:</strong> ${escapeHtml(design.rejectReason)}`;
            rejectNotice.classList.remove('d-none');
        }

        await refreshEditDesignImages(designId);
    } catch (err) {
        showToast('Không thể tải thông tin thiết kế: ' + err.message, 'danger');
    }
}
window.openEditDesignModal = openEditDesignModal;

async function refreshEditDesignImages(designId) {
    const listEl = document.getElementById('editDesignImagesList');
    if (!listEl) return;

    listEl.innerHTML = '<div class="col-12 text-center py-2 text-muted small"><span class="spinner-border spinner-border-sm me-1"></span>Đang tải ảnh...</div>';

    try {
        const images = await api.designs.getImages(designId);
        if (!images || images.length === 0) {
            listEl.innerHTML = '<div class="col-12 text-muted small py-2 text-center">Chưa có hình ảnh xem trước nào.</div>';
            return;
        }

        listEl.innerHTML = images.map(img => `
            <div class="col-4 col-sm-3">
                <div class="position-relative border rounded-3 overflow-hidden bg-white" style="height: 85px;">
                    <img src="${img.imageUrl}" class="w-100 h-100 object-fit-cover" onerror="this.onerror=null; this.src='${DEFAULT_THUMBNAIL}'">
                    ${img.isPrimary ? '<span class="badge bg-primary position-absolute top-0 start-0 m-1" style="font-size:10px;">Bìa</span>' : ''}
                    <button type="button" class="btn btn-danger btn-sm position-absolute top-0 end-0 m-1 p-0 px-1" onclick="handleDeleteEditDesignImage(${img.id}, ${designId})" title="Xóa ảnh">
                        <i class="bi bi-x"></i>
                    </button>
                </div>
            </div>
        `).join('');
    } catch (err) {
        listEl.innerHTML = `<div class="col-12 text-danger small">${err.message}</div>`;
    }
}

async function handleDeleteEditDesignImage(imageId, designId) {
    if (!confirm('Xóa hình ảnh xem trước này?')) return;
    try {
        await api.designs.deleteImage(imageId);
        showToast('Đã xóa hình ảnh', 'info');
        await refreshEditDesignImages(designId);
        await loadMyDesigns();
    } catch (err) {
        showToast('Không thể xóa ảnh: ' + err.message, 'danger');
    }
}
window.handleDeleteEditDesignImage = handleDeleteEditDesignImage;

async function handleUpdateDesignSubmit(e) {
    e.preventDefault();
    const designId = Number(document.getElementById('editDesignIdInput').value);
    const name = document.getElementById('editDesignNameInput').value.trim();
    const categoryId = Number(document.getElementById('editDesignCategorySelect').value);
    const price = parseFloat(document.getElementById('editDesignPriceInput').value);
    const description = document.getElementById('editDesignDescriptionInput').value.trim();
    const alertBox = document.getElementById('editDesignAlert');
    const btn = document.getElementById('btnSaveEditedDesign');

    if (alertBox) alertBox.classList.add('d-none');
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang lưu...';

    try {
        await api.designs.update(designId, {
            name,
            description,
            price,
            category: { id: categoryId }
        });
        showToast('Cập nhật thiết kế thành công! Tác phẩm đã được gửi duyệt lại.', 'success');
        const modalEl = document.getElementById('editDesignModal');
        bootstrap.Modal.getInstance(modalEl)?.hide();
        await loadMyDesigns();
    } catch (err) {
        if (alertBox) {
            alertBox.textContent = err.message || 'Không thể cập nhật thiết kế';
            alertBox.classList.remove('d-none');
        }
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-check2-circle me-1"></i> Lưu & Gửi duyệt lại';
    }
}

document.addEventListener('DOMContentLoaded', () => {
    initDesignerPortal();

    // Form đăng ký Designer
    const regForm = document.getElementById('registerDesignerForm');
    if (regForm) regForm.addEventListener('submit', handleRegisterDesigner);

    // Chỉnh sửa hồ sơ Designer
    const btnEditProfile = document.getElementById('btnEditDesignerProfile');
    if (btnEditProfile) btnEditProfile.addEventListener('click', openEditDesignerProfileModal);

    const editProfileForm = document.getElementById('editDesignerProfileForm');
    if (editProfileForm) editProfileForm.addEventListener('submit', handleUpdateDesignerProfile);

    const avatarUrlInput = document.getElementById('editDesignerAvatarUrl');
    if (avatarUrlInput) {
        avatarUrlInput.addEventListener('input', () => {
            const preview = document.getElementById('editDesignerAvatarPreview');
            if (preview) preview.src = avatarUrlInput.value.trim() || DEFAULT_AVATAR;
        });
    }

    const avatarFileInput = document.getElementById('editDesignerAvatarFileInput');
    if (avatarFileInput) {
        avatarFileInput.addEventListener('change', async () => {
            if (!avatarFileInput.files || avatarFileInput.files.length === 0) return;
            const file = avatarFileInput.files[0];
            try {
                showToast('Đang tải ảnh đại diện lên...', 'info');
                const res = await api.upload.image(file);
                const urlInput = document.getElementById('editDesignerAvatarUrl');
                const preview = document.getElementById('editDesignerAvatarPreview');
                if (urlInput) urlInput.value = res.fileUrl;
                if (preview) preview.src = res.fileUrl;
                showToast('Đã tải ảnh đại diện thành công!', 'success');
            } catch (err) {
                showToast('Không thể tải ảnh đại diện: ' + err.message, 'danger');
            } finally {
                avatarFileInput.value = '';
            }
        });
    }

    // Form sửa thiết kế
    const editDesignForm = document.getElementById('editDesignForm');
    if (editDesignForm) editDesignForm.addEventListener('submit', handleUpdateDesignSubmit);

    const editAddImgInput = document.getElementById('editDesignAddImageInput');
    if (editAddImgInput) {
        editAddImgInput.addEventListener('change', async () => {
            const designId = Number(document.getElementById('editDesignIdInput').value);
            if (!designId || !editAddImgInput.files || editAddImgInput.files.length === 0) return;
            for (const file of Array.from(editAddImgInput.files)) {
                try {
                    const res = await api.upload.image(file);
                    await api.designs.addImage(designId, res.fileUrl, false);
                    showToast(`Đã thêm ảnh ${file.name}`, 'success');
                } catch (err) {
                    showToast(`Lỗi thêm ảnh ${file.name}: ${err.message}`, 'danger');
                }
            }
            editAddImgInput.value = '';
            await refreshEditDesignImages(designId);
            await loadMyDesigns();
        });
    }

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

// =========================================================================
// YÊU CẦU THIẾT KẾ RIÊNG TỪ KHÁCH HÀNG
// =========================================================================

async function loadDesignerCustomRequests() {
    const container = document.getElementById('designerRequestsContainer');
    const badge = document.getElementById('designerRequestsBadge');
    if (!container) return;

    try {
        const requests = await api.designRequests.getDesignerRequests();
        const list = Array.isArray(requests) ? requests : [];

        const activeCount = list.filter(r => r.status === 'OPEN' || r.status === 'IN_PROGRESS' || r.status === 'REVISION_REQUESTED').length;
        if (badge) {
            badge.textContent = activeCount;
            badge.classList.toggle('d-none', activeCount === 0);
        }

        if (list.length === 0) {
            container.innerHTML = `
                <div class="text-center py-5 bg-light rounded-4 border">
                    <i class="bi bi-briefcase fs-1 text-muted"></i>
                    <h6 class="mt-3 fw-bold">Chưa có yêu cầu thiết kế nào</h6>
                </div>
            `;
            return;
        }

        const totalEscrowHeld = list
            .filter(r => (r.paymentStatus || 'ESCROW_HELD') === 'ESCROW_HELD' && r.status !== 'CANCELLED' && r.status !== 'REFUNDED')
            .reduce((sum, r) => sum + Number(r.budget || 0), 0);
        const totalReleased = list
            .filter(r => r.paymentStatus === 'RELEASED')
            .reduce((sum, r) => sum + Number(r.budget || 0), 0);

        container.innerHTML = `
            <div class="row g-3 mb-4">
                <div class="col-md-6">
                    <div class="p-3 rounded-4 bg-light border d-flex align-items-center justify-content-between">
                        <div class="small fw-bold text-secondary">Ngân sách đang thực hiện</div>
                        <h5 class="fw-bold text-dark mb-0">${formatCurrency(totalEscrowHeld)}</h5>
                    </div>
                </div>
                <div class="col-md-6">
                    <div class="p-3 rounded-4 bg-success-subtle border border-success-subtle d-flex align-items-center justify-content-between">
                        <div class="small fw-bold text-success">Doanh thu đã nhận</div>
                        <h5 class="fw-bold text-success mb-0">${formatCurrency(totalReleased)}</h5>
                    </div>
                </div>
            </div>

            <div class="d-flex flex-column gap-3">
                ${list.map(req => {
                    const st = req.status || 'OPEN';
                    let statusBadge = '<span class="badge bg-info text-dark">Chờ nhận</span>';
                    if (st === 'IN_PROGRESS') statusBadge = '<span class="badge bg-primary">Đang thực hiện</span>';
                    else if (st === 'REVISION_REQUESTED') statusBadge = `<span class="badge bg-danger">Cần chỉnh sửa (#${req.revisionCount || 1})</span>`;
                    else if (st === 'PREVIEW_READY') statusBadge = '<span class="badge bg-warning text-dark">Đã gửi sản phẩm</span>';
                    else if (st === 'APPROVED') statusBadge = '<span class="badge bg-success">Hoàn tất</span>';
                    else if (st === 'REFUNDED') statusBadge = '<span class="badge bg-secondary">Đã hoàn tiền</span>';
                    else if (st === 'CANCELLED') statusBadge = '<span class="badge bg-secondary">Đã hủy</span>';

                    const customerName = req.customer ? (req.customer.fullName || req.customer.username) : 'Khách hàng';
                    const safeTitle = escapeHtml(req.title || 'Yêu cầu thiết kế');
                    const safeNote = escapeHtml(req.designerNote || '');
                    const safeFeedback = escapeHtml(req.customerFeedback || '');
                    const deliveryName = escapeHtml(req.deliveryFileName || '');
                    const deliveryUrl = req.deliveryFileUrl || '';

                    return `
                        <div class="card border rounded-4 shadow-sm p-3 ${st === 'REVISION_REQUESTED' ? 'border-danger border-2' : ''}">
                            <div class="d-flex flex-wrap justify-content-between align-items-center border-bottom pb-2 mb-3 gap-2">
                                <div>
                                    <span class="badge bg-dark me-2">#REQ-${req.id}</span>
                                    <strong class="fs-6 text-dark">${safeTitle}</strong>
                                    ${req.category ? `<span class="badge bg-light text-secondary border ms-2">${escapeHtml(req.category.name)}</span>` : ''}
                                </div>
                                <div class="d-flex flex-wrap gap-1 align-items-center">
                                    ${statusBadge}
                                </div>
                            </div>

                            ${safeFeedback ? `
                                <div class="alert alert-warning border-warning py-2 px-3 mb-3 small">
                                    <strong>Phản hồi từ khách hàng:</strong> ${safeFeedback}
                                </div>
                            ` : ''}

                            <div class="row g-3 align-items-center">
                                <div class="col-md-3">
                                    <div class="small text-muted fw-semibold mb-1">Ảnh tham khảo:</div>
                                    <div class="rounded-3 border overflow-hidden bg-light d-flex align-items-center justify-content-center" style="height: 115px;">
                                        ${req.referenceImageUrl ? `<img src="${req.referenceImageUrl}" class="w-100 h-100 object-fit-cover">` : '<span class="text-muted small">Không có ảnh</span>'}
                                    </div>
                                </div>

                                <div class="col-md-5">
                                    <p class="small text-secondary mb-2">${escapeHtml(req.productDescription)}</p>
                                    <div class="small text-muted">
                                        <div><strong>Khách hàng:</strong> ${escapeHtml(customerName)}</div>
                                        <div><strong>Ngân sách:</strong> <span class="text-success fw-bold">${formatCurrency(req.budget)}</span> • <strong>Hạn:</strong> ${escapeHtml(req.deadline || 'Linh hoạt')}</div>
                                    </div>
                                    ${req.designerNote ? `
                                        <div class="alert alert-info py-2 px-3 mt-2 mb-0 small">
                                            <strong>Ghi chú đã gửi:</strong> ${safeNote}
                                        </div>
                                    ` : ''}
                                </div>

                                <div class="col-md-4 border-start ps-md-3">
                                    ${req.previewImageUrl ? `
                                        <div class="small fw-bold text-success mb-1">Sản phẩm đã gửi:</div>
                                        <div class="rounded border overflow-hidden mb-2" style="height: 80px;">
                                            <img src="${req.previewImageUrl}" class="w-100 h-100 object-fit-cover">
                                        </div>
                                        ${deliveryName ? `
                                            <div class="small text-dark mb-2 text-truncate" title="${deliveryName}">
                                                <i class="bi bi-file-earmark-zip-fill text-primary me-1"></i>${deliveryName}
                                            </div>
                                        ` : ''}
                                    ` : ''}

                                    <div class="d-grid gap-2">
                                        ${st === 'OPEN' ? `
                                            <button type="button" class="btn btn-primary btn-sm fw-bold rounded-pill" onclick="handleAcceptCustomRequest(${req.id})">
                                                <i class="bi bi-check2 me-1"></i> Nhận yêu cầu
                                            </button>
                                        ` : ''}

                                        ${(st === 'OPEN' || st === 'IN_PROGRESS' || st === 'REVISION_REQUESTED' || st === 'PREVIEW_READY') ? `
                                            <button type="button" class="btn btn-success btn-sm fw-bold rounded-pill" onclick="openSubmitPreviewModal(${req.id}, '${safeTitle}', '${req.previewImageUrl || ''}', 'STANDARD_2D', '${deliveryUrl}', '${deliveryName}')">
                                                <i class="bi bi-upload me-1"></i> ${st === 'REVISION_REQUESTED' ? 'Gửi lại bản sửa' : req.previewImageUrl ? 'Cập nhật sản phẩm' : 'Bàn giao sản phẩm'}
                                            </button>
                                        ` : ''}

                                        ${st === 'PREVIEW_READY' ? `
                                            <button type="button" class="btn btn-outline-secondary btn-sm fw-semibold rounded-pill" onclick="handleForceReleaseEscrow(${req.id})">
                                                Yêu cầu giải ngân
                                            </button>
                                        ` : ''}
                                    </div>
                                </div>
                            </div>
                        </div>
                    `;
                }).join('')}
            </div>
        `;
    } catch (err) {
        container.innerHTML = `<div class="alert alert-danger">Không thể tải yêu cầu đặt thiết kế: ${err.message}</div>`;
    }
}
window.loadDesignerCustomRequests = loadDesignerCustomRequests;

async function handleAcceptCustomRequest(reqId) {
    try {
        await api.designRequests.accept(reqId);
        showToast('Đã nhận yêu cầu thiết kế!', 'success');
        await loadDesignerCustomRequests();
    } catch (err) {
        showToast(err.message || 'Không thể nhận yêu cầu', 'danger');
    }
}
window.handleAcceptCustomRequest = handleAcceptCustomRequest;

async function handleForceReleaseEscrow(reqId) {
    const note = prompt('Nhập ghi chú xác nhận hoàn thành yêu cầu để giải ngân:', 'Đã bàn giao đầy đủ sản phẩm theo đúng yêu cầu.');
    if (!note || !note.trim()) return;
    try {
        await api.designRequests.forceRelease(reqId, { note: note.trim() });
        showToast('Đã giải ngân thành công!', 'success');
        await loadDesignerCustomRequests();
    } catch (err) {
        showToast(err.message || 'Không thể giải ngân yêu cầu', 'danger');
    }
}
window.handleForceReleaseEscrow = handleForceReleaseEscrow;

function openSubmitPreviewModal(reqId, title, currentPreviewUrl = '', mockupType = 'STANDARD_2D', currentDeliveryUrl = '', currentDeliveryName = '') {
    document.getElementById('submitPreviewReqId').value = reqId;
    document.getElementById('submitPreviewReqTitle').textContent = `#REQ-${reqId}: ${title}`;
    document.getElementById('submitPreviewImageUrl').value = currentPreviewUrl || '';
    document.getElementById('submitPreviewMockupType').value = 'STANDARD_2D';
    document.getElementById('submitDeliveryFileUrl').value = currentDeliveryUrl || '';
    document.getElementById('submitDeliveryFileName').value = currentDeliveryName || 'San-Pham-Thiet-Ke.zip';
    document.getElementById('submitPreviewNote').value = 'Đã hoàn thiện sản phẩm thiết kế theo yêu cầu của bạn.';

    const fileInput = document.getElementById('submitPreviewImageFile');
    if (fileInput) {
        fileInput.value = '';
        fileInput.onchange = async () => {
            if (!fileInput.files || !fileInput.files[0]) return;
            try {
                showToast('Đang tải ảnh lên...', 'info');
                const res = await api.upload.image(fileInput.files[0]);
                const uploadedUrl = res.fileUrl || res.url || '';
                document.getElementById('submitPreviewImageUrl').value = uploadedUrl;
                if (!document.getElementById('submitDeliveryFileUrl').value) {
                    document.getElementById('submitDeliveryFileUrl').value = uploadedUrl;
                }
                showToast('Tải ảnh thành công!', 'success');
            } catch (err) {
                showToast('Lỗi tải ảnh: ' + err.message, 'danger');
            }
        };
    }

    const deliveryInput = document.getElementById('submitDeliveryFileInput');
    if (deliveryInput) {
        deliveryInput.value = '';
        deliveryInput.onchange = async () => {
            if (!deliveryInput.files || !deliveryInput.files[0]) return;
            const f = deliveryInput.files[0];
            document.getElementById('submitDeliveryFileName').value = f.name;
            try {
                showToast('Đang tải file lên...', 'info');
                let res;
                if (f.type && f.type.startsWith('image/')) {
                    res = await api.upload.image(f);
                } else {
                    res = await api.upload.file(f);
                }
                document.getElementById('submitDeliveryFileUrl').value = res.fileUrl || res.url || '';
                showToast('Đã tải file lên thành công!', 'success');
            } catch (err) {
                showToast('Lỗi tải file: ' + err.message, 'danger');
            }
        };
    }

    const modalEl = document.getElementById('submitRequestPreviewModal');
    if (modalEl) bootstrap.Modal.getOrCreateInstance(modalEl).show();
}
window.openSubmitPreviewModal = openSubmitPreviewModal;

document.addEventListener('DOMContentLoaded', () => {
    const previewForm = document.getElementById('submitRequestPreviewForm');
    if (previewForm) {
        previewForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const reqId = Number(document.getElementById('submitPreviewReqId').value);
            const previewImageUrl = document.getElementById('submitPreviewImageUrl').value.trim();
            const deliveryFileUrl = document.getElementById('submitDeliveryFileUrl').value.trim() || previewImageUrl;
            const deliveryFileName = document.getElementById('submitDeliveryFileName').value.trim() || 'San-Pham-Thiet-Ke.zip';
            const designerNote = document.getElementById('submitPreviewNote').value.trim();
            const btn = document.getElementById('btnConfirmSubmitPreview');

            btn.disabled = true;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang gửi...';

            try {
                await api.designRequests.submitPreview(reqId, {
                    previewImageUrl,
                    deliveryFileUrl,
                    deliveryFileName,
                    mockupType: 'STANDARD_2D',
                    designerNote
                });
                showToast('Đã bàn giao sản phẩm thành công!', 'success');
                bootstrap.Modal.getInstance(document.getElementById('submitRequestPreviewModal'))?.hide();
                await loadDesignerCustomRequests();
            } catch (err) {
                showToast(err.message || 'Không thể gửi sản phẩm', 'danger');
            } finally {
                btn.disabled = false;
                btn.innerHTML = 'Gửi sản phẩm';
            }
        });
    }
});

