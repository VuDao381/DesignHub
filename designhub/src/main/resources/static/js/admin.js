let currentAdminUser = null;
let currentModerationStatus = 'PENDING';
let allLoadedUsers = [];
let allLoadedCategories = [];

// Format Currency VND
function formatCurrency(amount) {
    if (amount === undefined || amount === null) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

// Format File Size
function formatFileSize(bytes) {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

// Toast helper
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

/**
 * Khởi tạo Admin Console
 */
async function initAdminPortal() {
    const sectionUnauthorized = document.getElementById('sectionUnauthorized');
    const sectionAdmin = document.getElementById('sectionAdminPortal');

    if (!TokenStorage.isAuthenticated()) {
        sectionUnauthorized.classList.remove('d-none');
        sectionAdmin.classList.add('d-none');
        return;
    }

    try {
        currentAdminUser = await api.auth.getMe();
        TokenStorage.setUser(currentAdminUser);

        if (currentAdminUser.role !== 'ADMIN') {
            sectionUnauthorized.classList.remove('d-none');
            sectionAdmin.classList.add('d-none');
            return;
        }

        // Quyền hợp lệ: Hiển thị Admin Console
        sectionUnauthorized.classList.add('d-none');
        sectionAdmin.classList.remove('d-none');

        const avatarInitial = document.getElementById('adminAvatarInitial');
        if (avatarInitial) {
            avatarInitial.textContent = (currentAdminUser.fullName || currentAdminUser.username || 'A').charAt(0).toUpperCase();
        }

        // Tải dữ liệu toàn bộ các tab
        await Promise.all([
            loadAdminDashboard(),
            loadModerationDesigns(),
            loadCategoriesTable(),
            loadUsersTable()
        ]);
    } catch (err) {
        console.error('Lỗi khởi tạo Admin Console:', err);
        sectionUnauthorized.classList.remove('d-none');
        sectionAdmin.classList.add('d-none');
    }
}

/**
 * 1. Tải số liệu Dashboard & Analytics toàn sàn
 */
async function loadAdminDashboard() {
    try {
        const stats = await api.analytics.getAdminDashboard();

        // Cập nhật 5 KPI Cards
        document.getElementById('kpiAdminTotalRevenue').textContent = formatCurrency(stats.totalRevenue);
        document.getElementById('kpiAdminTotalOrders').textContent = (stats.totalOrders || 0) + ' đơn';
        document.getElementById('kpiAdminTotalUsers').textContent = (stats.totalUsers || 0) + ' người';
        document.getElementById('kpiAdminTotalDesigners').textContent = (stats.totalDesigners || 0) + ' tác giả';
        document.getElementById('kpiAdminTotalDesigns').textContent = (stats.totalDesigns || 0) + ' mẫu';

        // Render Top 10 Bán Chạy
        const topSellingTbody = document.getElementById('adminTopSellingTableBody');
        if (topSellingTbody) {
            if (!stats.topSellingDesigns || stats.topSellingDesigns.length === 0) {
                topSellingTbody.innerHTML = `
                    <tr><td colspan="5" class="text-center py-4 text-muted">Chưa có giao dịch mua thiết kế nào trên sàn.</td></tr>
                `;
            } else {
                topSellingTbody.innerHTML = stats.topSellingDesigns.map((item, idx) => `
                    <tr>
                        <td class="ps-3 fw-bold text-muted">${idx + 1}</td>
                        <td>
                            <div class="d-flex align-items-center gap-2">
                                <img src="${item.imageUrl || '/images/card.png'}" alt="${item.designName}"
                                    class="rounded border object-fit-cover" style="width: 44px; height: 44px;"
                                    onerror="this.src='/images/card.png'">
                                <div>
                                    <div class="fw-bold text-dark text-truncate" style="max-width: 220px;" title="${item.designName}">
                                        ${item.designName}
                                    </div>
                                    <small class="text-muted">Mã: #${item.designId}</small>
                                </div>
                            </div>
                        </td>
                        <td class="text-center fw-medium">${formatCurrency(item.price)}</td>
                        <td class="text-center">
                            <span class="badge bg-primary-subtle text-primary rounded-pill px-2 py-1">
                                ${item.salesCount} lượt
                            </span>
                        </td>
                        <td class="text-end pe-3 fw-bold text-success">${formatCurrency(item.totalRevenue)}</td>
                    </tr>
                `).join('');
            }
        }

        // Render Tăng Trưởng Người Dùng Hàng Tháng
        const growthContainer = document.getElementById('userGrowthContainer');
        if (growthContainer) {
            if (!stats.userGrowth || stats.userGrowth.length === 0) {
                growthContainer.innerHTML = '<div class="text-center py-4 text-muted">Chưa có dữ liệu tăng trưởng.</div>';
            } else {
                const maxCount = Math.max(...stats.userGrowth.map(g => g.userCount || 1), 1);
                growthContainer.innerHTML = stats.userGrowth.map(item => {
                    const percent = Math.min(Math.round((item.userCount / maxCount) * 100), 100);
                    return `
                        <div class="mb-3">
                            <div class="d-flex justify-content-between align-items-center mb-1">
                                <span class="fw-semibold small text-dark"><i class="bi bi-calendar3 me-1 text-muted"></i> Tháng ${item.month}</span>
                                <span class="badge bg-info-subtle text-info fw-bold">${item.userCount} thành viên</span>
                            </div>
                            <div class="progress" style="height: 8px;">
                                <div class="progress-bar bg-info progress-bar-striped" style="width: ${percent}%;"></div>
                            </div>
                        </div>
                    `;
                }).join('');
            }
        }
    } catch (err) {
        console.error('Lỗi khi tải số liệu Admin Analytics:', err);
    }
}

/**
 * 2. Tải danh sách Kiểm duyệt Thiết kế (Moderation)
 */
async function loadModerationDesigns() {
    const tbody = document.getElementById('moderationTableBody');
    if (!tbody) return;

    const keyword = document.getElementById('moderationSearchInput')?.value || '';

    tbody.innerHTML = `
        <tr>
            <td colspan="8" class="text-center py-5 text-muted">
                <span class="spinner-border spinner-border-sm me-2"></span> Đang tải danh sách thiết kế...
            </td>
        </tr>
    `;

    try {
        const params = {
            keyword,
            status: currentModerationStatus || undefined,
            size: 50,
            sortBy: 'createdAt_desc'
        };

        const res = await api.designs.getAll(params);
        const designs = res.content || res || [];

        // Cập nhật badge số lượng pending nếu đang xem pending
        if (currentModerationStatus === 'PENDING') {
            const badgePending = document.getElementById('badgePendingCount');
            if (badgePending) badgePending.textContent = designs.length;
        }

        if (designs.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" class="text-center py-5 text-muted">
                        <i class="bi bi-check2-all fs-2 d-block mb-2 text-success"></i>
                        Không có thiết kế nào ở trạng thái này hoặc không tìm thấy kết quả phù hợp.
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = designs.map(design => {
            const primaryImg = (design.images && design.images.length > 0)
                ? (design.images.find(i => i.isPrimary) || design.images[0]).imageUrl
                : '/images/card.png';

            const statusBadge = getStatusBadge(design.status);
            const authorName = design.designer?.user?.fullName || design.designer?.user?.username || 'N/A';
            const authorEmail = design.designer?.user?.email || '';
            const filesCount = design.files ? design.files.length : 0;
            const categoryName = design.category?.name || 'Khác';

            return `
                <tr>
                    <td class="ps-3">
                        <img src="${primaryImg}" class="rounded-3 border object-fit-cover cursor-pointer"
                            style="width: 60px; height: 60px;" alt="${design.name}"
                            onclick="openPreviewModal(${design.id})" title="Bấm để phóng to xem chi tiết"
                            onerror="this.src='/images/card.png'">
                    </td>
                    <td>
                        <div class="fw-bold text-dark">${escapeHtml(design.name)}</div>
                        <small class="text-muted">ID: #${design.id} • ${new Date(design.createdAt || Date.now()).toLocaleDateString('vi-VN')}</small>
                        ${design.rejectReason ? `<div class="mt-1 small text-danger"><i class="bi bi-exclamation-circle me-1"></i>Lý do từ chối: <em>${escapeHtml(design.rejectReason)}</em></div>` : ''}
                    </td>
                    <td><span class="badge bg-light text-dark border">${escapeHtml(categoryName)}</span></td>
                    <td>
                        <div class="fw-semibold text-dark">${escapeHtml(authorName)}</div>
                        <small class="text-muted">${escapeHtml(authorEmail)}</small>
                    </td>
                    <td class="text-center fw-bold text-primary">${formatCurrency(design.price)}</td>
                    <td class="text-center">${statusBadge}</td>
                    <td class="text-center">
                        <button type="button" class="btn btn-outline-secondary btn-sm rounded-pill px-2 py-1"
                            onclick="openPreviewModal(${design.id})">
                            <i class="bi bi-paperclip"></i> ${filesCount} file
                        </button>
                    </td>
                    <td class="text-end pe-3">
                        <div class="btn-group btn-group-sm">
                            ${design.status !== 'APPROVED' ? `
                                <button type="button" class="btn btn-success" title="Phê duyệt xuất bản"
                                    onclick="handleApproveDesign(${design.id}, '${escapeHtml(design.name)}')">
                                    <i class="bi bi-check-lg"></i> Duyệt
                                </button>
                            ` : ''}
                            ${design.status !== 'REJECTED' ? `
                                <button type="button" class="btn btn-warning text-dark" title="Từ chối duyệt"
                                    onclick="openRejectModal(${design.id})">
                                    <i class="bi bi-x-circle"></i> Từ chối
                                </button>
                            ` : ''}
                            <button type="button" class="btn btn-outline-danger" title="Xóa vĩnh viễn"
                                onclick="handleAdminDeleteDesign(${design.id}, '${escapeHtml(design.name)}')">
                                <i class="bi bi-trash"></i>
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        console.error('Lỗi khi tải danh sách kiểm duyệt:', err);
        tbody.innerHTML = `<tr><td colspan="8" class="text-danger text-center py-4">Lỗi: ${err.message}</td></tr>`;
    }
}

function getStatusBadge(status) {
    const s = (status || 'PENDING').toUpperCase();
    if (s === 'APPROVED') {
        return '<span class="badge bg-success px-2 py-1 shadow-sm"><i class="bi bi-check-circle me-1"></i> ĐÃ DUYỆT</span>';
    } else if (s === 'REJECTED') {
        return '<span class="badge bg-danger px-2 py-1 shadow-sm"><i class="bi bi-x-circle me-1"></i> TỪ CHỐI</span>';
    } else if (s === 'ARCHIVED') {
        return '<span class="badge bg-secondary px-2 py-1"><i class="bi bi-archive me-1"></i> LƯU TRỮ</span>';
    }
    return '<span class="badge bg-warning text-dark px-2 py-1 shadow-sm"><i class="bi bi-clock-history me-1"></i> CHỜ DUYỆT</span>';
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

/**
 * Duyệt thiết kế (APPROVE)
 */
async function handleApproveDesign(designId, name) {
    if (!confirm(`Bạn có chắc chắn muốn PHÊ DUYỆT thiết kế "${name}"? Tác phẩm sẽ xuất hiện công khai trên trang chủ ngay lập tức.`)) {
        return;
    }

    try {
        await api.designs.updateStatus(designId, 'APPROVED');
        showToast(`Đã duyệt thiết kế "${name}" thành công!`, 'success');
        await loadModerationDesigns();
        await loadAdminDashboard();
    } catch (err) {
        showToast('Lỗi phê duyệt: ' + err.message, 'danger');
    }
}

function openRejectModal(designId) {
    document.getElementById('rejectDesignId').value = designId;
    document.getElementById('rejectReasonInput').value = '';
    const modal = bootstrap.Modal.getOrCreateInstance(document.getElementById('rejectDesignModal'));
    modal.show();
}

async function handleConfirmRejectSubmit(e) {
    e.preventDefault();
    const designId = document.getElementById('rejectDesignId').value;
    const reason = document.getElementById('rejectReasonInput').value.trim();
    const btn = document.getElementById('btnConfirmReject');

    if (!reason) {
        showToast('Vui lòng nhập lý do từ chối!', 'warning');
        return;
    }

    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Đang xử lý...';

    try {
        await api.designs.updateStatus(designId, 'REJECTED', reason);
        showToast('Đã từ chối thiết kế và ghi nhận lý do thành công!', 'warning');
        const modal = bootstrap.Modal.getInstance(document.getElementById('rejectDesignModal'));
        if (modal) modal.hide();
        await loadModerationDesigns();
        await loadAdminDashboard();
    } catch (err) {
        showToast('Lỗi khi từ chối thiết kế: ' + err.message, 'danger');
    } finally {
        btn.disabled = false;
        btn.innerHTML = 'Xác nhận từ chối';
    }
}

async function handleAdminDeleteDesign(designId, name) {
    if (!confirm(`Bạn có chắc muốn XÓA vĩnh viễn thiết kế "${name}"? Thao tác này sẽ xóa toàn bộ hình ảnh và tệp gốc liên quan!`)) {
        return;
    }

    try {
        await api.designs.delete(designId);
        showToast(`Đã xóa thiết kế "${name}" thành công!`, 'success');
        await loadModerationDesigns();
        await loadAdminDashboard();
    } catch (err) {
        showToast('Không thể xóa thiết kế: ' + err.message, 'danger');
    }
}

async function openPreviewModal(designId) {
    const modalEl = document.getElementById('previewDesignModal');
    const titleEl = document.getElementById('previewDesignModalTitle');
    const contentEl = document.getElementById('previewDesignContent');

    contentEl.innerHTML = '<div class="text-center py-4"><span class="spinner-border spinner-border-sm me-2"></span> Đang tải chi tiết thiết kế...</div>';
    const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
    modal.show();

    try {
        const [design, images, files] = await Promise.all([
            api.designs.getById(designId),
            api.designs.getImages(designId).catch(() => []),
            api.designs.getFiles(designId).catch(() => [])
        ]);

        titleEl.textContent = `Xem trước: ${design.name}`;

        let imagesHtml = '';
        if (images && images.length > 0) {
            imagesHtml = `
                <h6 class="fw-bold mb-2 text-dark"><i class="bi bi-images me-1 text-primary"></i> Hình ảnh mockup (${images.length} ảnh):</h6>
                <div class="row g-2 mb-4">
                    ${images.map(img => `
                        <div class="col-4 col-md-3">
                            <div class="border rounded-3 overflow-hidden position-relative" style="height: 120px;">
                                <img src="${img.imageUrl}" class="w-100 h-100 object-fit-cover" alt="Mockup">
                                ${img.isPrimary ? '<span class="position-absolute top-0 start-0 badge bg-primary m-1">Ảnh bìa</span>' : ''}
                            </div>
                        </div>
                    `).join('')}
                </div>
            `;
        }

        let filesHtml = '';
        if (files && files.length > 0) {
            filesHtml = `
                <h6 class="fw-bold mb-2 text-dark"><i class="bi bi-file-earmark-zip me-1 text-success"></i> Tệp thiết kế gốc (${files.length} tệp):</h6>
                <div class="list-group mb-4">
                    ${files.map(f => `
                        <div class="list-group-item d-flex justify-content-between align-items-center">
                            <div>
                                <div class="fw-bold text-dark">${escapeHtml(f.fileName)}</div>
                                <small class="text-muted">${formatFileSize(f.fileSize)} • ${f.fileType || 'N/A'}</small>
                            </div>
                            <a href="${api.files.getDownloadUrl(f.id)}" target="_blank" class="btn btn-outline-success btn-sm rounded-pill">
                                <i class="bi bi-download me-1"></i> Tải thử nghiệm
                            </a>
                        </div>
                    `).join('')}
                </div>
            `;
        } else {
            filesHtml = '<div class="alert alert-warning small mb-3"><i class="bi bi-exclamation-triangle me-1"></i> Thiết kế này chưa có tệp tin gốc đính kèm!</div>';
        }

        contentEl.innerHTML = `
            <div class="mb-3">
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <span class="badge bg-secondary">${design.category?.name || 'Khác'}</span>
                    <span class="fs-5 fw-bold text-primary">${formatCurrency(design.price)}</span>
                </div>
                <h5 class="fw-bold mb-1">${escapeHtml(design.name)}</h5>
                <p class="text-muted small">${escapeHtml(design.description || 'Không có mô tả')}</p>
            </div>
            ${imagesHtml}
            ${filesHtml}
            <div class="d-flex justify-content-end gap-2 pt-2 border-top">
                ${design.status !== 'APPROVED' ? `
                    <button type="button" class="btn btn-success rounded-pill px-4"
                        onclick="handleApproveDesign(${design.id}, '${escapeHtml(design.name)}'); bootstrap.Modal.getInstance(document.getElementById('previewDesignModal')).hide();">
                        <i class="bi bi-check-lg me-1"></i> Phê duyệt thiết kế
                    </button>
                ` : ''}
                ${design.status !== 'REJECTED' ? `
                    <button type="button" class="btn btn-warning rounded-pill px-4 text-dark"
                        onclick="bootstrap.Modal.getInstance(document.getElementById('previewDesignModal')).hide(); openRejectModal(${design.id});">
                        <i class="bi bi-x-circle me-1"></i> Từ chối duyệt
                    </button>
                ` : ''}
            </div>
        `;
    } catch (err) {
        contentEl.innerHTML = `<div class="text-danger py-4 text-center">Không thể tải thông tin: ${err.message}</div>`;
    }
}

async function loadCategoriesTable() {
    const tbody = document.getElementById('categoriesTableBody');
    if (!tbody) return;

    try {
        const res = await api.categories.getAll({ size: 100 });
        allLoadedCategories = res.content || res || [];

        if (allLoadedCategories.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center py-4 text-muted">Chưa có danh mục nào. Hãy bấm "Thêm danh mục mới"!</td></tr>';
            return;
        }

        tbody.innerHTML = allLoadedCategories.map(cat => `
            <tr>
                <td class="ps-3 fw-bold text-muted">#${cat.id}</td>
                <td class="fw-bold text-dark">${escapeHtml(cat.name)}</td>
                <td class="text-muted small">${escapeHtml(cat.description || 'Không có mô tả')}</td>
                <td class="text-end pe-3">
                    <button type="button" class="btn btn-outline-primary btn-sm rounded-circle p-2 me-1" title="Sửa danh mục"
                        onclick="openEditCategoryModal(${cat.id})">
                        <i class="bi bi-pencil"></i>
                    </button>
                    <button type="button" class="btn btn-outline-danger btn-sm rounded-circle p-2" title="Xóa danh mục"
                        onclick="handleDeleteCategory(${cat.id}, '${escapeHtml(cat.name)}')">
                        <i class="bi bi-trash"></i>
                    </button>
                </td>
            </tr>
        `).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="4" class="text-danger text-center py-4">Lỗi: ${err.message}</td></tr>`;
    }
}

function openAddCategoryModal() {
    document.getElementById('categoryModalTitle').textContent = 'Thêm danh mục mới';
    document.getElementById('categoryEditId').value = '';
    document.getElementById('categoryNameInput').value = '';
    document.getElementById('categoryDescInput').value = '';
    const modal = bootstrap.Modal.getOrCreateInstance(document.getElementById('categoryModal'));
    modal.show();
}

function openEditCategoryModal(catId) {
    const cat = allLoadedCategories.find(c => c.id === catId);
    if (!cat) return;

    document.getElementById('categoryModalTitle').textContent = 'Chỉnh sửa danh mục';
    document.getElementById('categoryEditId').value = cat.id;
    document.getElementById('categoryNameInput').value = cat.name;
    document.getElementById('categoryDescInput').value = cat.description || '';
    const modal = bootstrap.Modal.getOrCreateInstance(document.getElementById('categoryModal'));
    modal.show();
}

async function handleCategoryFormSubmit(e) {
    e.preventDefault();
    const id = document.getElementById('categoryEditId').value;
    const name = document.getElementById('categoryNameInput').value.trim();
    const description = document.getElementById('categoryDescInput').value.trim();
    const btn = document.getElementById('btnSubmitCategory');

    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Đang lưu...';

    try {
        if (id) {
            await api.categories.update(id, { name, description });
            showToast(`Đã cập nhật danh mục "${name}"!`, 'success');
        } else {
            await api.categories.create({ name, description });
            showToast(`Đã thêm danh mục mới "${name}"!`, 'success');
        }
        const modal = bootstrap.Modal.getInstance(document.getElementById('categoryModal'));
        if (modal) modal.hide();
        await loadCategoriesTable();
    } catch (err) {
        showToast('Lỗi lưu danh mục: ' + err.message, 'danger');
    } finally {
        btn.disabled = false;
        btn.innerHTML = 'Lưu danh mục';
    }
}

async function handleDeleteCategory(id, name) {
    if (!confirm(`Bạn có chắc muốn xóa danh mục "${name}"?`)) return;

    try {
        await api.categories.delete(id);
        showToast(`Đã xóa danh mục "${name}"!`, 'success');
        await loadCategoriesTable();
    } catch (err) {
        showToast('Không thể xóa danh mục: ' + err.message, 'danger');
    }
}

async function loadUsersTable() {
    const tbody = document.getElementById('usersTableBody');
    if (!tbody) return;

    const keyword = (document.getElementById('userSearchInput')?.value || '').toLowerCase().trim();

    try {
        allLoadedUsers = await api.users.getAll();

        let filtered = allLoadedUsers;
        if (keyword) {
            filtered = allLoadedUsers.filter(u =>
                (u.username && u.username.toLowerCase().includes(keyword)) ||
                (u.fullName && u.fullName.toLowerCase().includes(keyword)) ||
                (u.email && u.email.toLowerCase().includes(keyword))
            );
        }

        if (filtered.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-muted">Không tìm thấy người dùng phù hợp.</td></tr>';
            return;
        }

        tbody.innerHTML = filtered.map(u => {
            let roleBadgeClass = 'bg-secondary';
            if (u.role === 'ADMIN') roleBadgeClass = 'bg-danger';
            else if (u.role === 'DESIGNER') roleBadgeClass = 'bg-primary';

            return `
                <tr>
                    <td class="ps-3 fw-bold text-muted">#${u.id}</td>
                    <td class="fw-semibold text-dark">${escapeHtml(u.fullName || 'Chưa đặt tên')}</td>
                    <td><code>${escapeHtml(u.username)}</code></td>
                    <td>${escapeHtml(u.email)}</td>
                    <td>${escapeHtml(u.phone || '—')}</td>
                    <td class="text-center">
                        <span class="badge ${roleBadgeClass} px-2 py-1">${u.role}</span>
                    </td>
                    <td class="text-end pe-3">
                        <button type="button" class="btn btn-outline-primary btn-sm rounded-circle p-2 me-1" title="Phân quyền / Sửa thông tin"
                            onclick="openEditUserModal(${u.id})">
                            <i class="bi bi-pencil"></i>
                        </button>
                        ${u.id !== currentAdminUser?.id ? `
                            <button type="button" class="btn btn-outline-danger btn-sm rounded-circle p-2" title="Xóa tài khoản"
                                onclick="handleDeleteUser(${u.id}, '${escapeHtml(u.username)}')">
                                <i class="bi bi-trash"></i>
                            </button>
                        ` : ''}
                    </td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-danger text-center py-4">Lỗi tải người dùng: ${err.message}</td></tr>`;
    }
}

function openEditUserModal(userId) {
    const user = allLoadedUsers.find(u => u.id === userId);
    if (!user) return;

    document.getElementById('userEditId').value = user.id;
    document.getElementById('userEditUsername').value = user.username;
    document.getElementById('userEditFullName').value = user.fullName || '';
    document.getElementById('userEditEmail').value = user.email || '';
    document.getElementById('userEditPhone').value = user.phone || '';
    document.getElementById('userEditRole').value = user.role || 'CUSTOMER';

    const modal = bootstrap.Modal.getOrCreateInstance(document.getElementById('userModal'));
    modal.show();
}

async function handleUserEditSubmit(e) {
    e.preventDefault();
    const id = document.getElementById('userEditId').value;
    const fullName = document.getElementById('userEditFullName').value.trim();
    const email = document.getElementById('userEditEmail').value.trim();
    const phone = document.getElementById('userEditPhone').value.trim();
    const role = document.getElementById('userEditRole').value;
    const btn = document.getElementById('btnSubmitUserEdit');

    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Đang cập nhật...';

    try {
        const user = allLoadedUsers.find(u => u.id == id);
        await api.users.update(id, {
            username: user.username,
            fullName,
            email,
            phone,
            role
        });
        showToast('Đã cập nhật thông tin thành viên thành công!', 'success');
        const modal = bootstrap.Modal.getInstance(document.getElementById('userModal'));
        if (modal) modal.hide();
        await loadUsersTable();
        await loadAdminDashboard();
    } catch (err) {
        showToast('Lỗi cập nhật người dùng: ' + err.message, 'danger');
    } finally {
        btn.disabled = false;
        btn.innerHTML = 'Cập nhật tài khoản';
    }
}

async function handleDeleteUser(id, username) {
    if (!confirm(`Bạn có chắc chắn muốn XÓA tài khoản "${username}"?`)) return;

    try {
        await api.users.delete(id);
        showToast(`Đã xóa tài khoản "${username}" thành công!`, 'success');
        await loadUsersTable();
        await loadAdminDashboard();
    } catch (err) {
        showToast('Không thể xóa tài khoản: ' + err.message, 'danger');
    }
}

document.addEventListener('DOMContentLoaded', () => {
    initAdminPortal();

    const btnRefresh = document.getElementById('btnAdminRefreshAll');
    if (btnRefresh) {
        btnRefresh.addEventListener('click', async () => {
            btnRefresh.disabled = true;
            await Promise.all([
                loadAdminDashboard(),
                loadModerationDesigns(),
                loadCategoriesTable(),
                loadUsersTable()
            ]);
            btnRefresh.disabled = false;
            showToast('Đã làm mới toàn bộ số liệu!', 'info');
        });
    }

    const statusBtns = document.querySelectorAll('.filter-status-btn');
    statusBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            statusBtns.forEach(b => {
                b.classList.remove('active', 'btn-warning', 'btn-success', 'btn-danger', 'btn-secondary');
                if (b.dataset.status === 'PENDING') b.classList.add('btn-outline-warning');
                else if (b.dataset.status === 'APPROVED') b.classList.add('btn-outline-success');
                else if (b.dataset.status === 'REJECTED') b.classList.add('btn-outline-danger');
                else b.classList.add('btn-outline-secondary');
            });

            btn.classList.add('active');
            if (btn.dataset.status === 'PENDING') {
                btn.classList.remove('btn-outline-warning');
                btn.classList.add('btn-warning');
            } else if (btn.dataset.status === 'APPROVED') {
                btn.classList.remove('btn-outline-success');
                btn.classList.add('btn-success');
            } else if (btn.dataset.status === 'REJECTED') {
                btn.classList.remove('btn-outline-danger');
                btn.classList.add('btn-danger');
            } else {
                btn.classList.remove('btn-outline-secondary');
                btn.classList.add('btn-secondary');
            }

            currentModerationStatus = btn.dataset.status;
            loadModerationDesigns();
        });
    });

    // Tìm kiếm kiểm duyệt
    const modSearch = document.getElementById('moderationSearchInput');
    if (modSearch) {
        let timer = null;
        modSearch.addEventListener('input', () => {
            clearTimeout(timer);
            timer = setTimeout(loadModerationDesigns, 300);
        });
    }

    // Tìm kiếm người dùng
    const userSearch = document.getElementById('userSearchInput');
    if (userSearch) {
        let timer = null;
        userSearch.addEventListener('input', () => {
            clearTimeout(timer);
            timer = setTimeout(loadUsersTable, 300);
        });
    }

    // Modal form submit handlers
    const rejectForm = document.getElementById('rejectDesignForm');
    if (rejectForm) rejectForm.addEventListener('submit', handleConfirmRejectSubmit);

    const btnOpenAddCat = document.getElementById('btnOpenAddCategoryModal');
    if (btnOpenAddCat) btnOpenAddCat.addEventListener('click', openAddCategoryModal);

    const catForm = document.getElementById('categoryForm');
    if (catForm) catForm.addEventListener('submit', handleCategoryFormSubmit);

    const userEditForm = document.getElementById('userEditForm');
    if (userEditForm) userEditForm.addEventListener('submit', handleUserEditSubmit);
});

window.addEventListener('auth:login_success', () => {
    initAdminPortal();
});
