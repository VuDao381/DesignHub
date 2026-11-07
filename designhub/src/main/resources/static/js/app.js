/**
 * DesignHub - Main Application Controller
 * Quản lý danh mục, tìm kiếm, hiển thị thiết kế, giỏ hàng, thanh toán và tải file
 */

// Global State
const appState = {
    categories: [],
    designsPage: null,
    currentFilters: {
        keyword: '',
        categoryId: null,
        designerId: null,
        designerName: '',
        minPrice: '',
        maxPrice: '',
        sortBy: 'createdAt_desc',
        page: 0,
        size: 8
    },
    cart: null,
    currentCheckout: null,
    favorites: [],
    favoriteMap: {}
};

function formatVND(amount) {
    if (amount === undefined || amount === null) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

function formatDate(dateStr) {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleDateString('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function showToast(message, type = 'success') {
    const toastContainer = document.getElementById('toastContainer');
    if (!toastContainer) return;

    const toastId = 'toast_' + Date.now();
    const bgClass = type === 'success' ? 'bg-success' :
        type === 'danger' ? 'bg-danger' :
            type === 'warning' ? 'bg-warning text-dark' : 'bg-primary';

    const icon = type === 'success' ? 'bi-check-circle-fill' :
        type === 'danger' ? 'bi-x-circle-fill' :
            type === 'warning' ? 'bi-exclamation-triangle-fill' : 'bi-info-circle-fill';

    const toastHtml = `
        <div id="${toastId}" class="toast align-items-center text-white ${bgClass} border-0 shadow" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body d-flex align-items-center gap-2">
                    <i class="bi ${icon} fs-5"></i>
                    <div>${message}</div>
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        </div>
    `;

    toastContainer.insertAdjacentHTML('beforeend', toastHtml);
    const toastEl = document.getElementById(toastId);
    const bsToast = new bootstrap.Toast(toastEl, { delay: 4000 });
    bsToast.show();
    toastEl.addEventListener('hidden.bs.toast', () => toastEl.remove());
}

window.appState = appState;
window.showToast = showToast;

async function loadCategories() {
    try {
        const res = await api.categories.getAll(); const categories = Array.isArray(res) ? res : (res && Array.isArray(res.content) ? res.content : []);
        appState.categories = categories || [];
        renderCategoryFilters();
        renderCategoryCards();
    } catch (err) {
        console.error('Lỗi khi tải danh mục:', err);
    }
}

function renderCategoryFilters() {
    const container = document.getElementById('categoryPills');
    if (!container) return;

    let html = `
        <button class="cat-pill ${appState.currentFilters.categoryId === null ? 'active' : ''}" data-cat-id="">
            Tất cả
        </button>
    `;

    appState.categories.forEach(cat => {
        const isActive = appState.currentFilters.categoryId === cat.id ? 'active' : '';
        html += `
            <button class="cat-pill ${isActive}" data-cat-id="${cat.id}">
                ${cat.name}
            </button>
        `;
    });

    container.innerHTML = html;

    container.querySelectorAll('.cat-pill').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const catId = e.currentTarget.getAttribute('data-cat-id');
            appState.currentFilters.categoryId = catId ? parseInt(catId) : null;
            appState.currentFilters.page = 0;
            renderCategoryFilters();
            loadDesigns();
        });
    });
}

function renderCategoryCards() {
    const container = document.getElementById('categoryCardsRow');
    if (!container || !appState.categories.length) return;

    const icons = ['bi-brush', 'bi-palette', 'bi-window-fullscreen', 'bi-layers', 'bi-badge-ad', 'bi-gem'];
    const colors = ['purple-icon', 'blue-icon', 'orange-icon', 'green-icon'];

    container.innerHTML = appState.categories.slice(0, 4).map((cat, idx) => `
        <div class="col-6 col-md-3">
            <div class="category-card cursor-pointer" onclick="filterByCategory(${cat.id})">
                <div class="category-icon ${colors[idx % colors.length]}">
                    <i class="bi ${icons[idx % icons.length]}"></i>
                </div>
                <h3>${cat.name}</h3>
                <p>${cat.description || 'Khám phá ngay'}</p>
            </div>
        </div>
    `).join('');
}

window.filterByCategory = (catId) => {
    appState.currentFilters.categoryId = catId;
    appState.currentFilters.page = 0;
    renderCategoryFilters();
    renderActiveFilters();
    loadDesigns();

    const target = document.getElementById('designsSection');
    if (target) {
        target.scrollIntoView({ behavior: 'smooth' });
    }
};

window.filterByDesigner = (designerId, designerName = 'Designer', event = null) => {
    if (event) {
        event.stopPropagation();
        event.preventDefault();
    }
    if (!designerId) return;

    appState.currentFilters.designerId = designerId;
    appState.currentFilters.designerName = designerName;
    appState.currentFilters.page = 0;

    const detailModalEl = document.getElementById('designDetailModal');
    if (detailModalEl) {
        const detailModal = bootstrap.Modal.getInstance(detailModalEl);
        if (detailModal) detailModal.hide();
    }

    renderActiveFilters();
    loadDesigns(0);

    const target = document.getElementById('designsSection');
    if (target) {
        target.scrollIntoView({ behavior: 'smooth' });
    }
};

window.clearDesignerFilter = () => {
    appState.currentFilters.designerId = null;
    appState.currentFilters.designerName = '';
    appState.currentFilters.page = 0;
    renderActiveFilters();
    loadDesigns(0);
};

window.clearPriceFilter = () => {
    appState.currentFilters.minPrice = '';
    appState.currentFilters.maxPrice = '';
    appState.currentFilters.page = 0;
    const minEl = document.getElementById('minPriceInput');
    const maxEl = document.getElementById('maxPriceInput');
    if (minEl) minEl.value = '';
    if (maxEl) maxEl.value = '';
    renderActiveFilters();
    loadDesigns(0);
};

function applyPriceFilter() {
    const minEl = document.getElementById('minPriceInput');
    const maxEl = document.getElementById('maxPriceInput');
    const minVal = minEl && minEl.value !== '' ? Number(minEl.value) : '';
    const maxVal = maxEl && maxEl.value !== '' ? Number(maxEl.value) : '';

    if (minVal !== '' && minVal < 0) {
        showToast('Giá tối thiểu không được nhỏ hơn 0', 'warning');
        return;
    }
    if (maxVal !== '' && maxVal < 0) {
        showToast('Giá tối đa không được nhỏ hơn 0', 'warning');
        return;
    }
    if (minVal !== '' && maxVal !== '' && minVal > maxVal) {
        showToast('Giá tối thiểu không được lớn hơn giá tối đa!', 'warning');
        return;
    }

    appState.currentFilters.minPrice = minVal;
    appState.currentFilters.maxPrice = maxVal;
    appState.currentFilters.page = 0;
    renderActiveFilters();
    loadDesigns(0);
}
window.applyPriceFilter = applyPriceFilter;

function renderActiveFilters() {
    const bar = document.getElementById('activeFiltersBar');
    if (!bar) return;

    const chips = [];
    if (appState.currentFilters.designerId) {
        chips.push(`
            <span class="badge bg-primary-subtle text-primary border border-primary-subtle d-inline-flex align-items-center gap-1 py-2 px-2">
                <i class="bi bi-person-badge"></i> Tác giả: ${appState.currentFilters.designerName || '#' + appState.currentFilters.designerId}
                <button type="button" class="btn-close ms-1" style="font-size: 9px;" onclick="clearDesignerFilter()" title="Bỏ lọc tác giả"></button>
            </span>
        `);
    }

    if (appState.currentFilters.minPrice !== '' || appState.currentFilters.maxPrice !== '') {
        const minText = appState.currentFilters.minPrice !== '' ? formatVND(appState.currentFilters.minPrice) : '0 ₫';
        const maxText = appState.currentFilters.maxPrice !== '' ? formatVND(appState.currentFilters.maxPrice) : 'Trở lên';
        chips.push(`
            <span class="badge bg-success-subtle text-success border border-success-subtle d-inline-flex align-items-center gap-1 py-2 px-2">
                <i class="bi bi-cash-stack"></i> Giá: ${minText} - ${maxText}
                <button type="button" class="btn-close ms-1" style="font-size: 9px;" onclick="clearPriceFilter()" title="Bỏ lọc khoảng giá"></button>
            </span>
        `);
    }

    bar.innerHTML = chips.join('');
}

async function loadDesigns(page = 0) {
    const grid = document.getElementById('designsGrid');
    if (!grid) return;

    appState.currentFilters.page = page;
    grid.innerHTML = `
        <div class="col-12 text-center py-5">
            <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">Đang tải...</span>
            </div>
            <p class="mt-2 text-muted">Đang tải danh sách thiết kế...</p>
        </div>
    `;

    try {
        const data = await api.designs.getAll(appState.currentFilters);
        appState.designsPage = data;
        const designs = data.content || [];

        if (designs.length === 0) {
            grid.innerHTML = `
                <div class="col-12 text-center py-5">
                    <div class="empty-state">
                        <i class="bi bi-search fs-1 text-muted"></i>
                        <h4 class="mt-3">Không tìm thấy thiết kế phù hợp</h4>
                        <p class="text-muted">Vui lòng thử tìm với từ khóa khác hoặc bỏ chọn bộ lọc danh mục.</p>
                        <button class="btn btn-outline-primary btn-sm mt-2" onclick="resetFilters()">Đặt lại bộ lọc</button>
                    </div>
                </div>
            `;
            renderPagination(0, 0);
            return;
        }

        grid.innerHTML = designs.map(d => renderDesignCard(d)).join('');
        renderPagination(data.number, data.totalPages);
    } catch (err) {
        grid.innerHTML = `
            <div class="col-12 text-center py-5 text-danger">
                <i class="bi bi-exclamation-circle fs-2"></i>
                <p class="mt-2">Lỗi tải dữ liệu: ${err.message}</p>
                <button class="btn btn-sm btn-outline-danger" onclick="loadDesigns()">Thử lại</button>
            </div>
        `;
    }
}

function renderDesignCard(d) {
    const primaryImg = (d.images && d.images.length > 0) ? d.images[0].imageUrl : null;
    const designerId = d.designer ? d.designer.id : null;
    const designerName = d.designer && d.designer.user ? (d.designer.user.fullName || d.designer.user.username) : 'Designer';
    const safeDesignerName = designerName.replace(/'/g, "\\'");
    const initial = designerName.charAt(0).toUpperCase();
    const categoryName = d.category ? d.category.name : 'Thiết kế';
    const isFav = !!appState.favoriteMap[d.id];

    const bgGradients = [
        'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
        'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
        'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)'
    ];
    const cardBg = bgGradients[d.id % bgGradients.length];

    const imageHtml = primaryImg ? `
        <img src="${primaryImg}" class="card-img-top design-img" alt="${d.name}" onerror="this.onerror=null; this.parentElement.style.background='${cardBg}'; this.remove();">
    ` : `
        <div class="design-placeholder d-flex align-items-center justify-content-center" style="background: ${cardBg}; min-height: 200px;">
            <div class="text-center text-white p-3">
                <i class="bi bi-palette fs-1 mb-2"></i>
                <div class="fw-bold">${d.name}</div>
            </div>
        </div>
    `;

    return `
        <div class="col-sm-6 col-lg-3">
            <div class="product-card h-100 d-flex flex-column">
                <div class="product-image-container position-relative cursor-pointer" onclick="openDesignDetail(${d.id})">
                    ${imageHtml}
                    <span class="product-tag">${categoryName}</span>
                    <button type="button" class="favorite-btn ${isFav ? 'active' : ''}" data-design-id="${d.id}" onclick="toggleFavorite(event, ${d.id})" title="${isFav ? 'Bỏ yêu thích' : 'Yêu thích thiết kế này'}">
                        <i class="bi ${isFav ? 'bi-heart-fill' : 'bi-heart'}"></i>
                    </button>
                </div>

                <div class="product-info d-flex flex-column flex-grow-1">
                    <span class="product-category">${categoryName}</span>
                    <h3 class="product-title cursor-pointer" onclick="openDesignDetail(${d.id})" title="${d.name}">
                        ${d.name}
                    </h3>

                    <div class="designer mb-2 ${designerId ? 'cursor-pointer designer-filter-link' : ''}" ${designerId ? `onclick="filterByDesigner(${designerId}, '${safeDesignerName}', event)" title="Xem tất cả thiết kế của ${designerName}"` : ''}>
                        <div class="designer-avatar">${initial}</div>
                        <span>${designerName}</span>
                    </div>

                    <div class="product-bottom mt-auto d-flex align-items-center justify-content-between pt-2 border-top">
                        <strong class="product-price">${formatVND(d.price)}</strong>
                        <div class="btn-group">
                            <button class="btn btn-sm btn-outline-primary" onclick="openDesignDetail(${d.id})" title="Xem chi tiết">
                                <i class="bi bi-eye"></i>
                            </button>
                            <button class="btn btn-sm btn-primary" onclick="handleAddToCart(${d.id})" title="Thêm vào giỏ hàng">
                                <i class="bi bi-cart-plus"></i>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `;
}

function renderPagination(currentPage, totalPages) {
    const container = document.getElementById('designsPagination');
    if (!container) return;

    if (totalPages <= 1) {
        container.innerHTML = '';
        return;
    }

    let html = `
        <nav aria-label="Page navigation">
            <ul class="pagination justify-content-center">
                <li class="page-item ${currentPage === 0 ? 'disabled' : ''}">
                    <button class="page-link" onclick="loadDesigns(${currentPage - 1})">
                        <i class="bi bi-chevron-left"></i>
                    </button>
                </li>
    `;

    for (let i = 0; i < totalPages; i++) {
        html += `
            <li class="page-item ${i === currentPage ? 'active' : ''}">
                <button class="page-link" onclick="loadDesigns(${i})">${i + 1}</button>
            </li>
        `;
    }

    html += `
                <li class="page-item ${currentPage >= totalPages - 1 ? 'disabled' : ''}">
                    <button class="page-link" onclick="loadDesigns(${currentPage + 1})">
                        <i class="bi bi-chevron-right"></i>
                    </button>
                </li>
            </ul>
        </nav>
    `;

    container.innerHTML = html;
}

window.loadDesigns = loadDesigns;
window.resetFilters = () => {
    appState.currentFilters.keyword = '';
    appState.currentFilters.categoryId = null;
    appState.currentFilters.designerId = null;
    appState.currentFilters.designerName = '';
    appState.currentFilters.minPrice = '';
    appState.currentFilters.maxPrice = '';
    appState.currentFilters.sortBy = 'createdAt_desc';
    appState.currentFilters.page = 0;

    const searchInput = document.getElementById('keywordInput');
    if (searchInput) searchInput.value = '';
    const heroSearchInput = document.getElementById('heroSearchInput');
    if (heroSearchInput) heroSearchInput.value = '';
    const minPriceInput = document.getElementById('minPriceInput');
    if (minPriceInput) minPriceInput.value = '';
    const maxPriceInput = document.getElementById('maxPriceInput');
    if (maxPriceInput) maxPriceInput.value = '';
    const sortSelect = document.getElementById('sortSelect');
    if (sortSelect) sortSelect.value = 'createdAt_desc';

    renderCategoryFilters();
    renderActiveFilters();
    loadDesigns();
};

async function loadFavorites() {
    const badge = document.getElementById('favoritesBadge');
    if (!TokenStorage.isAuthenticated()) {
        appState.favorites = [];
        appState.favoriteMap = {};
        if (badge) {
            badge.textContent = '0';
            badge.classList.add('d-none');
        }
        updateFavoriteIconsOnDOM();
        return;
    }

    try {
        const list = await api.favorites.getMyFavorites();
        appState.favorites = Array.isArray(list) ? list : [];
        const map = {};
        appState.favorites.forEach(fav => {
            if (fav.design && fav.design.id) {
                map[fav.design.id] = fav.id;
            }
        });
        appState.favoriteMap = map;

        if (badge) {
            const count = appState.favorites.length;
            badge.textContent = count;
            if (count > 0) {
                badge.classList.remove('d-none');
            } else {
                badge.classList.add('d-none');
            }
        }
        updateFavoriteIconsOnDOM();
    } catch {
        if (badge) badge.classList.add('d-none');
    }
}
window.loadFavorites = loadFavorites;

function updateFavoriteIconsOnDOM() {
    document.querySelectorAll('.favorite-btn[data-design-id]').forEach(btn => {
        const designId = Number(btn.getAttribute('data-design-id'));
        const isFav = !!appState.favoriteMap[designId];
        btn.classList.toggle('active', isFav);
        btn.title = isFav ? 'Bỏ yêu thích' : 'Yêu thích thiết kế này';
        const icon = btn.querySelector('i');
        if (icon) {
            icon.className = `bi ${isFav ? 'bi-heart-fill' : 'bi-heart'}`;
        }
    });
}

async function toggleFavorite(event, designId) {
    if (event) {
        event.stopPropagation();
        event.preventDefault();
    }

    if (!TokenStorage.isAuthenticated()) {
        showToast('Vui lòng đăng nhập để lưu thiết kế yêu thích!', 'warning');
        const loginModalEl = document.getElementById('loginModal');
        if (loginModalEl) new bootstrap.Modal(loginModalEl).show();
        return;
    }

    const existingFavId = appState.favoriteMap[designId];
    try {
        if (existingFavId) {
            await api.favorites.remove(existingFavId);
            showToast('Đã bỏ khỏi danh sách yêu thích', 'info');
        } else {
            await api.favorites.add(designId);
            showToast('Đã thêm vào danh sách yêu thích ♥', 'success');
        }
        await loadFavorites();

        const favModalEl = document.getElementById('favoritesModal');
        if (favModalEl && favModalEl.classList.contains('show')) {
            renderFavoritesModalContent();
        }
    } catch (err) {
        showToast(err.message || 'Không thể cập nhật yêu thích', 'danger');
    }
}
window.toggleFavorite = toggleFavorite;

async function openFavoritesModal() {
    if (!TokenStorage.isAuthenticated()) {
        showToast('Vui lòng đăng nhập để xem thiết kế yêu thích!', 'warning');
        const loginModalEl = document.getElementById('loginModal');
        if (loginModalEl) new bootstrap.Modal(loginModalEl).show();
        return;
    }

    const modalEl = document.getElementById('favoritesModal');
    if (!modalEl) return;
    let modal = bootstrap.Modal.getInstance(modalEl);
    if (!modal) modal = new bootstrap.Modal(modalEl);
    modal.show();

    const body = document.getElementById('favoritesModalBody');
    body.innerHTML = `
        <div class="text-center py-5">
            <div class="spinner-border text-danger" role="status"></div>
            <p class="mt-2 text-muted">Đang tải danh sách yêu thích...</p>
        </div>
    `;

    await loadFavorites();
    renderFavoritesModalContent();
}
window.openFavoritesModal = openFavoritesModal;

function renderFavoritesModalContent() {
    const body = document.getElementById('favoritesModalBody');
    if (!body) return;

    if (!appState.favorites || appState.favorites.length === 0) {
        body.innerHTML = `
            <div class="text-center py-5">
                <i class="bi bi-heart fs-1 text-muted"></i>
                <h5 class="mt-3">Danh sách yêu thích đang trống</h5>
                <p class="text-muted small">Hãy bấm vào biểu tượng trái tim ♥ trên các thiết kế bạn yêu thích để lưu lại nhé!</p>
            </div>
        `;
        return;
    }

    body.innerHTML = `
        <div class="row g-3">
            ${appState.favorites.map(fav => {
                const d = fav.design;
                if (!d) return '';
                const primaryImg = (d.images && d.images.length > 0) ? d.images[0].imageUrl : null;
                const designerName = d.designer && d.designer.user ? (d.designer.user.fullName || d.designer.user.username) : 'Designer';
                return `
                    <div class="col-md-6">
                        <div class="card h-100 border shadow-sm p-3">
                            <div class="d-flex gap-3 align-items-center">
                                <div class="rounded border cursor-pointer" style="width: 76px; height: 76px; background: #f8f9fa; display:flex; align-items:center; justify-content:center; overflow:hidden; flex-shrink:0;" onclick="bootstrap.Modal.getInstance(document.getElementById('favoritesModal'))?.hide(); openDesignDetail(${d.id})">
                                    ${primaryImg ? `<img src="${primaryImg}" style="width:100%; height:100%; object-fit:cover;">` : '<i class="bi bi-palette fs-3 text-muted"></i>'}
                                </div>
                                <div class="flex-grow-1 overflow-hidden">
                                    <h6 class="fw-bold mb-1 text-truncate cursor-pointer" title="${d.name}" onclick="bootstrap.Modal.getInstance(document.getElementById('favoritesModal'))?.hide(); openDesignDetail(${d.id})">${d.name}</h6>
                                    <div class="small text-muted mb-1"><i class="bi bi-person me-1"></i>${designerName}</div>
                                    <div class="fw-bold text-primary">${formatVND(d.price)}</div>
                                </div>
                            </div>
                            <div class="border-top mt-3 pt-2 d-flex gap-2">
                                <button class="btn btn-sm btn-primary flex-grow-1" onclick="handleAddToCart(${d.id})">
                                    <i class="bi bi-cart-plus me-1"></i> Thêm vào giỏ
                                </button>
                                <button class="btn btn-sm btn-outline-secondary" onclick="bootstrap.Modal.getInstance(document.getElementById('favoritesModal'))?.hide(); openDesignDetail(${d.id})">
                                    <i class="bi bi-eye"></i> Xem
                                </button>
                                <button class="btn btn-sm btn-outline-danger" onclick="toggleFavorite(event, ${d.id})" title="Bỏ yêu thích">
                                    <i class="bi bi-trash3"></i>
                                </button>
                            </div>
                        </div>
                    </div>
                `;
            }).join('')}
        </div>
    `;
}

async function openDesignDetail(id) {
    const modalEl = document.getElementById('designDetailModal');
    let modal = bootstrap.Modal.getInstance(modalEl);
    if (!modal) modal = new bootstrap.Modal(modalEl);
    const content = document.getElementById('designDetailContent');

    content.innerHTML = `
        <div class="text-center py-5">
            <div class="spinner-border text-primary" role="status"></div>
            <p class="mt-2 text-muted">Đang tải thông tin thiết kế...</p>
        </div>
    `;
    modal.show();

    try {
        const [design, reviews] = await Promise.all([
            api.designs.getById(id),
            api.reviews.getByDesign(id).catch(() => [])
        ]);

        const designerId = design.designer ? design.designer.id : null;
        const designerName = design.designer && design.designer.user ? (design.designer.user.fullName || design.designer.user.username) : 'Designer';
        const safeDesignerName = designerName.replace(/'/g, "\\'");
        const safeDesignName = (design.name || '').replace(/'/g, "\\'");
        const primaryImg = (design.images && design.images.length > 0) ? design.images[0].imageUrl : null;
        const avgRating = reviews.length ? (reviews.reduce((acc, r) => acc + r.rating, 0) / reviews.length).toFixed(1) : '5.0';
        const isFav = !!appState.favoriteMap[design.id];
        const me = window.getCurrentUser ? window.getCurrentUser() : null;

        content.innerHTML = `
            <div class="row g-4">
                <div class="col-lg-6">
                    <div class="detail-gallery">
                        ${primaryImg ? `
                            <img src="${primaryImg}" class="img-fluid rounded-3 border shadow-sm main-detail-image" alt="${design.name}">
                        ` : `
                            <div class="p-5 text-center text-white rounded-3 shadow-sm" style="background: linear-gradient(135deg, #7357ff 0%, #4facfe 100%); min-height: 280px; display:flex; flex-direction:column; justify-content:center; align-items:center;">
                                <i class="bi bi-file-earmark-image fs-1 mb-2"></i>
                                <h4>${design.name}</h4>
                            </div>
                        `}

                        ${design.images && design.images.length > 1 ? `
                            <div class="d-flex gap-2 mt-3 overflow-auto">
                                ${design.images.map(img => `
                                    <img src="${img.imageUrl}" class="rounded border cursor-pointer thumbnail-preview" style="width: 70px; height: 70px; object-fit: cover;" onclick="document.querySelector('.main-detail-image').src='${img.imageUrl}'">
                                `).join('')}
                            </div>
                        ` : ''}
                    </div>
                </div>

                <div class="col-lg-6">
                    <div class="d-flex align-items-center gap-2 mb-2">
                        <span class="badge bg-primary-subtle text-primary border">${design.category ? design.category.name : 'Chung'}</span>
                        <span class="text-warning"><i class="bi bi-star-fill"></i> ${avgRating} (${reviews.length} đánh giá)</span>
                    </div>

                    <h2 class="h3 fw-bold mb-3">${design.name}</h2>

                    <div class="d-flex align-items-center justify-content-between gap-2 mb-4 p-2 px-3 bg-light rounded-3 ${designerId ? 'cursor-pointer border' : ''}" ${designerId ? `onclick="filterByDesigner(${designerId}, '${safeDesignerName}', event)" title="Lọc xem các thiết kế của ${designerName}"` : ''}>
                        <div class="d-flex align-items-center gap-2">
                            <div class="designer-avatar">${designerName.charAt(0).toUpperCase()}</div>
                            <div>
                                <div class="fw-semibold">${designerName}</div>
                                <small class="text-muted">Tác giả thiết kế</small>
                            </div>
                        </div>
                        ${designerId ? `<span class="small text-primary fw-semibold">Xem các tác phẩm <i class="bi bi-arrow-right"></i></span>` : ''}
                    </div>

                    <div class="display-6 fw-bold text-primary mb-4">
                        ${formatVND(design.price)}
                    </div>

                    <p class="text-secondary mb-4" style="white-space: pre-line;">
                        ${design.description || 'Chưa có mô tả chi tiết cho thiết kế này.'}
                    </p>

                    <div class="d-flex gap-2 mb-4">
                        <button type="button" class="btn ${isFav ? 'btn-danger' : 'btn-outline-danger'} btn-lg px-3 favorite-btn-detail" data-design-id="${design.id}" onclick="toggleFavorite(event, ${design.id}); openDesignDetail(${design.id});" title="${isFav ? 'Bỏ yêu thích' : 'Yêu thích'}">
                            <i class="bi ${isFav ? 'bi-heart-fill' : 'bi-heart'}"></i>
                        </button>
                        <button class="btn btn-outline-primary btn-lg flex-grow-1" onclick="handleAddToCart(${design.id})">
                            <i class="bi bi-cart-plus me-1"></i> Thêm vào giỏ
                        </button>
                        <button class="btn btn-primary btn-lg flex-grow-1" onclick="handleBuyNow(${design.id})">
                            <i class="bi bi-lightning-charge me-1"></i> Mua ngay
                        </button>
                    </div>

                    <div class="border-top pt-3">
                        <h5 class="fw-bold mb-3">Đánh giá từ khách hàng (${reviews.length})</h5>
                        <div class="reviews-list" style="max-height: 220px; overflow-y: auto;">
                            ${reviews.length ? reviews.map(r => {
                                const isOwner = me && r.user && r.user.id === me.id;
                                const isAdmin = me && me.role === 'ADMIN';
                                const safeComment = (r.comment || '').replace(/\\/g, '\\\\').replace(/'/g, "\\'").replace(/\n/g, '\\n');
                                return `
                                <div class="border-bottom py-2">
                                    <div class="d-flex justify-content-between align-items-center">
                                        <div>
                                            <strong class="small">${r.user ? (r.user.fullName || r.user.username) : 'Khách hàng'}</strong>
                                            ${isOwner ? '<span class="badge bg-info-subtle text-info border ms-1" style="font-size:10px;">Bạn</span>' : ''}
                                        </div>
                                        <div class="d-flex align-items-center gap-2">
                                            <span class="text-warning small">${'★'.repeat(r.rating)}${'☆'.repeat(5 - r.rating)}</span>
                                            ${isOwner ? `
                                                <button type="button" class="btn btn-link btn-sm p-0 text-primary text-decoration-none" onclick="openReviewModal(${design.id}, '${safeDesignName}', { id: ${r.id}, rating: ${r.rating}, comment: '${safeComment}' })" title="Sửa đánh giá">
                                                    <i class="bi bi-pencil-square"></i> Sửa
                                                </button>
                                            ` : ''}
                                            ${(isOwner || isAdmin) ? `
                                                <button type="button" class="btn btn-link btn-sm p-0 text-danger text-decoration-none" onclick="handleDeleteReview(${r.id}, ${design.id})" title="Xóa đánh giá">
                                                    <i class="bi bi-trash"></i> Xóa
                                                </button>
                                            ` : ''}
                                        </div>
                                    </div>
                                    <p class="small text-muted mb-0 mt-1">${r.comment || 'Không có bình luận.'}</p>
                                </div>
                            `;
                            }).join('') : '<p class="text-muted small">Chưa có đánh giá nào cho sản phẩm này.</p>'}
                        </div>
                    </div>
                </div>
            </div>
        `;
    } catch (err) {
        content.innerHTML = `
            <div class="text-center py-5 text-danger">
                <i class="bi bi-exclamation-circle fs-1"></i>
                <h5 class="mt-2">Không thể tải thông tin thiết kế</h5>
                <p class="text-muted">${err.message}</p>
            </div>
        `;
    }
}
window.openDesignDetail = openDesignDetail;

async function loadCartBadge() {
    const badge = document.getElementById('cartBadge');
    if (!badge) return;

    if (!TokenStorage.isAuthenticated()) {
        badge.textContent = '0';
        badge.classList.add('d-none');
        return;
    }

    try {
        const cart = await api.cart.getMyCart();
        appState.cart = cart;
        const totalItems = cart.items ? cart.items.length : 0;
        badge.textContent = totalItems;
        if (totalItems > 0) {
            badge.classList.remove('d-none');
        } else {
            badge.classList.add('d-none');
        }
    } catch {
        badge.classList.add('d-none');
    }
}
window.loadCartBadge = loadCartBadge;

async function handleAddToCart(designId) {
    if (!TokenStorage.isAuthenticated()) {
        showToast('Vui lòng đăng nhập để thêm vào giỏ hàng!', 'warning');
        const loginModal = new bootstrap.Modal(document.getElementById('loginModal'));
        loginModal.show();
        return;
    }

    try {
        const cart = await api.cart.getMyCart();
        await api.cart.addItem(cart.id, designId, 1);
        await loadCartBadge();
        showToast('Đã thêm thiết kế vào giỏ hàng!', 'success');
    } catch (err) {
        showToast(err.message || 'Không thể thêm vào giỏ hàng', 'danger');
    }
}
window.handleAddToCart = handleAddToCart;

async function handleBuyNow(designId) {
    await handleAddToCart(designId);
    const detailModalEl = document.getElementById('designDetailModal');
    const modalInstance = bootstrap.Modal.getInstance(detailModalEl);
    if (modalInstance) modalInstance.hide();
    openCartDrawer();
}
window.handleBuyNow = handleBuyNow;

async function openCartDrawer() {
    if (!TokenStorage.isAuthenticated()) {
        showToast('Vui lòng đăng nhập để quản lý giỏ hàng!', 'warning');
        const loginModal = new bootstrap.Modal(document.getElementById('loginModal'));
        loginModal.show();
        return;
    }

    const offcanvasEl = document.getElementById('cartOffcanvas');
    const offcanvas = new bootstrap.Offcanvas(offcanvasEl);
    const body = document.getElementById('cartDrawerBody');
    const totalEl = document.getElementById('cartTotalAmount');
    const checkoutBtn = document.getElementById('cartCheckoutBtn');

    body.innerHTML = `
        <div class="text-center py-5">
            <div class="spinner-border text-primary" role="status"></div>
            <p class="mt-2 text-muted">Đang tải giỏ hàng...</p>
        </div>
    `;
    offcanvas.show();

    try {
        const cart = await api.cart.getMyCart();
        appState.cart = cart;
        const items = cart.items || [];

        if (items.length === 0) {
            body.innerHTML = `
                <div class="text-center py-5">
                    <i class="bi bi-cart-x fs-1 text-muted"></i>
                    <h5 class="mt-3">Giỏ hàng của bạn đang trống</h5>
                    <p class="text-muted small">Hãy khám phá các thiết kế chất lượng và thêm vào giỏ nhé!</p>
                </div>
            `;
            totalEl.textContent = '0 ₫';
            checkoutBtn.disabled = true;
            return;
        }

        let total = 0;
        body.innerHTML = items.map(item => {
            const d = item.design;
            const price = d ? d.price : 0;
            total += (price * item.quantity);
            const primaryImg = (d && d.images && d.images.length > 0) ? d.images[0].imageUrl : null;

            return `
                <div class="cart-item-card d-flex align-items-center gap-3 p-3 border-bottom">
                    <div class="cart-item-thumb rounded border" style="width: 64px; height: 64px; background: #f0f0f0; display:flex; align-items:center; justify-content:center; overflow:hidden;">
                        ${primaryImg ? `<img src="${primaryImg}" style="width: 100%; height: 100%; object-fit: cover;">` : '<i class="bi bi-image text-muted fs-4"></i>'}
                    </div>
                    <div class="flex-grow-1">
                        <div class="fw-semibold text-truncate" style="max-width: 200px;" title="${d ? d.name : ''}">
                            ${d ? d.name : 'Thiết kế'}
                        </div>
                        <div class="text-primary fw-bold small">${formatVND(price)}</div>
                    </div>
                    <button class="btn btn-sm text-danger hover-bg-light" onclick="handleRemoveCartItem(${item.id})" title="Xóa khỏi giỏ">
                        <i class="bi bi-trash3"></i>
                    </button>
                </div>
            `;
        }).join('');

        totalEl.textContent = formatVND(total);
        checkoutBtn.disabled = false;
        loadCartBadge();
    } catch (err) {
        body.innerHTML = `
            <div class="text-center py-5 text-danger">
                <i class="bi bi-exclamation-circle fs-2"></i>
                <p class="mt-2">Không thể tải giỏ hàng: ${err.message}</p>
            </div>
        `;
    }
}
window.openCartDrawer = openCartDrawer;

async function handleRemoveCartItem(itemId) {
    try {
        await api.cart.removeItem(itemId);
        showToast('Đã xóa sản phẩm khỏi giỏ hàng', 'info');
        openCartDrawer();
    } catch (err) {
        showToast(err.message || 'Không thể xóa sản phẩm', 'danger');
    }
}
window.handleRemoveCartItem = handleRemoveCartItem;

async function startCheckout() {
    const offcanvasEl = document.getElementById('cartOffcanvas');
    const offcanvas = bootstrap.Offcanvas.getInstance(offcanvasEl);
    if (offcanvas) offcanvas.hide();

    const checkoutModalEl = document.getElementById('checkoutModal');
    const checkoutModal = new bootstrap.Modal(checkoutModalEl);
    const content = document.getElementById('checkoutModalContent');

    content.innerHTML = `
        <div class="text-center py-5">
            <div class="spinner-border text-primary" role="status"></div>
            <p class="mt-2 text-muted">Đang khởi tạo đơn hàng và thanh toán...</p>
        </div>
    `;
    checkoutModal.show();

    try {
        // Gọi checkout tạo Order
        const order = await api.orders.checkout();

        // Tạo Payment
        const payment = await api.payments.create(order.id, order.totalAmount, 'VNPAY_SANDBOX');
        appState.currentCheckout = { order, payment };

        content.innerHTML = `
            <div class="checkout-step p-2">
                <div class="alert alert-info border-0 d-flex align-items-center gap-2 mb-3">
                    <i class="bi bi-shield-check fs-4 text-primary"></i>
                    <div>
                        <strong>Mô phỏng cổng thanh toán trực tuyến</strong>
                        <div class="small">Đơn hàng #${order.id} đã được tạo thành công.</div>
                    </div>
                </div>

                <div class="card bg-light border-0 p-3 mb-4">
                    <div class="d-flex justify-content-between mb-2">
                        <span class="text-muted">Mã đơn hàng:</span>
                        <strong class="font-monospace">#DH-${order.id}</strong>
                    </div>
                    <div class="d-flex justify-content-between mb-2">
                        <span class="text-muted">Tổng số sản phẩm:</span>
                        <span>${order.orderDetails ? order.orderDetails.length : 1} sản phẩm</span>
                    </div>
                    <div class="d-flex justify-content-between border-top pt-2">
                        <span class="fw-bold">Tổng thanh toán:</span>
                        <span class="fw-bold fs-5 text-primary">${formatVND(order.totalAmount)}</span>
                    </div>
                </div>

                <h6 class="fw-bold mb-2">Chọn phương thức thanh toán mô phỏng:</h6>
                <div class="payment-methods mb-4">
                    <div class="form-check p-3 border rounded-3 mb-2 d-flex align-items-center gap-2 cursor-pointer bg-white">
                        <input class="form-check-input" type="radio" name="paymentMethod" id="vnpayRadio" checked>
                        <label class="form-check-label flex-grow-1 cursor-pointer" for="vnpayRadio">
                            <i class="bi bi-qr-code text-primary me-2"></i>
                            <strong>VNPay Sandbox</strong>
                            <small class="text-muted d-block">Quét mã QR hoặc thanh toán thẻ nội địa (Test môi trường)</small>
                        </label>
                    </div>

                    <div class="form-check p-3 border rounded-3 mb-2 d-flex align-items-center gap-2 cursor-pointer bg-white">
                        <input class="form-check-input" type="radio" name="paymentMethod" id="momoRadio">
                        <label class="form-check-label flex-grow-1 cursor-pointer" for="momoRadio">
                            <i class="bi bi-wallet2 text-danger me-2"></i>
                            <strong>Ví MoMo / QR Code</strong>
                            <small class="text-muted d-block">Mô phỏng ví điện tử MoMo</small>
                        </label>
                    </div>
                </div>

                <div class="d-grid gap-2">
                    <button class="btn btn-success btn-lg" id="btnConfirmPayment" onclick="handleConfirmPayment(${payment.id})">
                        <i class="bi bi-check2-circle me-1"></i> Xác nhận thanh toán thành công
                    </button>
                    <button class="btn btn-outline-secondary" data-bs-dismiss="modal">
                        Hủy bỏ
                    </button>
                </div>
            </div>
        `;
    } catch (err) {
        content.innerHTML = `
            <div class="text-center py-4 text-danger">
                <i class="bi bi-exclamation-triangle fs-1"></i>
                <h5 class="mt-2">Không thể tạo đơn hàng</h5>
                <p class="text-muted">${err.message}</p>
                <button class="btn btn-secondary mt-3" data-bs-dismiss="modal">Đóng</button>
            </div>
        `;
    }
}
window.startCheckout = startCheckout;

async function handleConfirmPayment(paymentId) {
    const btn = document.getElementById('btnConfirmPayment');
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang xử lý thanh toán...';
    }

    try {
        await api.payments.confirm(paymentId, `TXN_SIM_${Date.now()}`);

        const checkoutModalEl = document.getElementById('checkoutModal');
        const checkoutModal = bootstrap.Modal.getInstance(checkoutModalEl);
        if (checkoutModal) checkoutModal.hide();

        await loadCartBadge();

        showToast('Thanh toán thành công! Đơn hàng đã được cấp quyền tải file.', 'success');
        openPurchasedModal();
    } catch (err) {
        showToast(err.message || 'Xác nhận thanh toán thất bại', 'danger');
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = '<i class="bi bi-check2-circle me-1"></i> Thử lại xác nhận thanh toán';
        }
    }
}
window.handleConfirmPayment = handleConfirmPayment;

async function openPurchasedModal(defaultTab = 'purchased') {
    if (!TokenStorage.isAuthenticated()) {
        showToast('Vui lòng đăng nhập để xem kho thiết kế!', 'warning');
        const loginModal = new bootstrap.Modal(document.getElementById('loginModal'));
        loginModal.show();
        return;
    }

    const modalEl = document.getElementById('purchasedModal');
    let modal = bootstrap.Modal.getInstance(modalEl);
    if (!modal) modal = new bootstrap.Modal(modalEl);
    const body = document.getElementById('purchasedModalBody');

    body.innerHTML = `
        <div class="text-center py-5">
            <div class="spinner-border text-primary" role="status"></div>
            <p class="mt-2 text-muted">Đang tải dữ liệu của bạn...</p>
        </div>
    `;
    modal.show();

    try {
        const [orders, downloadHistory, myReviews] = await Promise.all([
            api.orders.getMyOrders().catch(() => []),
            api.downloadHistory.getMyHistory().catch(() => []),
            api.reviews.getMyReviews().catch(() => [])
        ]);

        const reviewByDesignId = {};
        (myReviews || []).forEach(r => {
            if (r.design && r.design.id) {
                reviewByDesignId[r.design.id] = r;
            }
        });

        const sortedOrders = [...(orders || [])].sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0));
        const paidOrders = sortedOrders.filter(o => String(o.status || '').toUpperCase() === 'PAID');
        const pendingCount = sortedOrders.filter(o => String(o.status || '').toUpperCase() === 'PENDING').length;

        const purchasedDesigns = [];
        const seenDesignIds = new Set();

        paidOrders.forEach(order => {
            if (order.orderDetails) {
                order.orderDetails.forEach(detail => {
                    if (detail.design && !seenDesignIds.has(detail.design.id)) {
                        seenDesignIds.add(detail.design.id);
                        purchasedDesigns.push({
                            design: detail.design,
                            orderId: order.id,
                            purchasedAt: order.createdAt
                        });
                    }
                });
            }
        });

        const sortedDownloads = [...(downloadHistory || [])].sort((a, b) => new Date(b.downloadedAt || 0) - new Date(a.downloadedAt || 0));

        const purchasedHtml = purchasedDesigns.length === 0 ? `
            <div class="text-center py-5">
                <i class="bi bi-folder2-open fs-1 text-muted"></i>
                <h5 class="mt-3">Kho thiết kế của bạn đang trống</h5>
                <p class="text-muted">Bạn chưa mua hoặc chưa hoàn tất thanh toán đơn hàng nào.</p>
                <button class="btn btn-primary btn-sm mt-2" data-bs-dismiss="modal" onclick="resetFilters()">Khám phá thiết kế ngay</button>
            </div>
        ` : `
            <div class="row g-3">
                ${purchasedDesigns.map(item => {
                    const d = item.design;
                    const primaryImg = (d.images && d.images.length > 0) ? d.images[0].imageUrl : null;
                    const files = d.files || [];
                    const myRev = reviewByDesignId[d.id];
                    const safeName = (d.name || '').replace(/'/g, "\\'");
                    const safeComment = myRev ? (myRev.comment || '').replace(/\\/g, '\\\\').replace(/'/g, "\\'").replace(/\n/g, '\\n') : '';

                    return `
                        <div class="col-md-6">
                            <div class="card h-100 border shadow-sm p-3">
                                <div class="d-flex gap-3">
                                    <div class="rounded border" style="width: 80px; height: 80px; background: #f8f9fa; display:flex; align-items:center; justify-content:center; overflow:hidden; flex-shrink: 0;">
                                        ${primaryImg ? `<img src="${primaryImg}" style="width: 100%; height: 100%; object-fit: cover;">` : '<i class="bi bi-file-earmark-code fs-2 text-muted"></i>'}
                                    </div>
                                    <div class="flex-grow-1 overflow-hidden">
                                        <h6 class="fw-bold mb-1 text-truncate" title="${d.name}">${d.name}</h6>
                                        <div class="small text-muted mb-2">Đã mua ngày: ${formatDate(item.purchasedAt)}</div>
                                        <div class="badge bg-success-subtle text-success border mb-2">
                                            <i class="bi bi-check2-circle"></i> Đã cấp bản quyền
                                        </div>
                                    </div>
                                </div>

                                <div class="border-top mt-3 pt-3 d-flex flex-wrap gap-2">
                                    ${files.length > 0 ? files.map(f => `
                                        <a href="${api.files.getDownloadUrl(f.id)}" class="btn btn-sm btn-success flex-grow-1" download target="_blank">
                                            <i class="bi bi-cloud-arrow-down-fill me-1"></i> Tải file (${f.fileName || 'Tệp gốc'})
                                        </a>
                                    `).join('') : `
                                        <a href="/api/design-files/design/${d.id}" class="btn btn-sm btn-outline-primary flex-grow-1" onclick="handleDownloadDirect(event, ${d.id})">
                                            <i class="bi bi-cloud-arrow-down me-1"></i> Tải bản thiết kế
                                        </a>
                                    `}
                                    ${myRev ? `
                                        <button class="btn btn-sm btn-warning text-dark" onclick="openReviewModal(${d.id}, '${safeName}', { id: ${myRev.id}, rating: ${myRev.rating}, comment: '${safeComment}' })">
                                            <i class="bi bi-pencil-square me-1"></i> Sửa đánh giá (${myRev.rating}★)
                                        </button>
                                    ` : `
                                        <button class="btn btn-sm btn-outline-warning" onclick="openReviewModal(${d.id}, '${safeName}')">
                                            <i class="bi bi-star me-1"></i> Đánh giá
                                        </button>
                                    `}
                                </div>
                            </div>
                        </div>
                    `;
                }).join('')}
            </div>
        `;

        const ordersHtml = sortedOrders.length === 0 ? `
            <div class="text-center py-5">
                <i class="bi bi-receipt fs-1 text-muted"></i>
                <h5 class="mt-3">Chưa có đơn hàng nào</h5>
                <p class="text-muted small">Lịch sử các đơn hàng bạn đặt sẽ được hiển thị tại đây.</p>
            </div>
        ` : `
            <div class="d-flex flex-column gap-3">
                ${sortedOrders.map(o => {
                    const st = String(o.status || '').toUpperCase();
                    const statusBadge = st === 'PAID'
                        ? '<span class="badge bg-success-subtle text-success border"><i class="bi bi-check-circle me-1"></i>Đã thanh toán</span>'
                        : st === 'PENDING'
                            ? '<span class="badge bg-warning-subtle text-warning-emphasis border"><i class="bi bi-hourglass-split me-1"></i>Chờ thanh toán</span>'
                            : '<span class="badge bg-secondary-subtle text-secondary border"><i class="bi bi-x-circle me-1"></i>Đã hủy</span>';
                    const details = o.orderDetails || [];

                    return `
                        <div class="card border shadow-sm p-3">
                            <div class="d-flex flex-wrap justify-content-between align-items-center border-bottom pb-2 mb-2 gap-2">
                                <div>
                                    <strong class="font-monospace text-primary">#DH-${o.id}</strong>
                                    <span class="text-muted small ms-2"><i class="bi bi-clock me-1"></i>${formatDate(o.createdAt)}</span>
                                </div>
                                <div>${statusBadge}</div>
                            </div>
                            <div class="mb-2">
                                ${details.map(dt => `
                                    <div class="d-flex justify-content-between align-items-center small py-1">
                                        <span class="text-truncate" style="max-width: 70%;"><i class="bi bi-palette2 text-muted me-1"></i>${dt.design ? dt.design.name : 'Thiết kế'}</span>
                                        <span class="fw-semibold">${formatVND(dt.price)}</span>
                                    </div>
                                `).join('')}
                            </div>
                            <div class="d-flex flex-wrap justify-content-between align-items-center border-top pt-2 mt-1 gap-2">
                                <div>
                                    <span class="text-muted small">Tổng cộng:</span>
                                    <strong class="text-primary fs-6 ms-1">${formatVND(o.totalAmount)}</strong>
                                </div>
                                ${st === 'PENDING' ? `
                                    <div class="d-flex gap-2">
                                        <button type="button" class="btn btn-sm btn-outline-danger" onclick="handleCancelOrder(${o.id})">
                                            <i class="bi bi-x-circle me-1"></i>Hủy đơn
                                        </button>
                                        <button type="button" class="btn btn-sm btn-primary" onclick="handlePayPendingOrder(${o.id})">
                                            <i class="bi bi-credit-card me-1"></i>Thanh toán ngay
                                        </button>
                                    </div>
                                ` : ''}
                            </div>
                        </div>
                    `;
                }).join('')}
            </div>
        `;

        const downloadsHtml = sortedDownloads.length === 0 ? `
            <div class="text-center py-5">
                <i class="bi bi-cloud-slash fs-1 text-muted"></i>
                <h5 class="mt-3">Chưa có lịch sử tải xuống</h5>
                <p class="text-muted small">Các lượt tải file thiết kế của bạn sẽ được ghi nhận tại đây.</p>
            </div>
        ` : `
            <div class="list-group">
                ${sortedDownloads.map(item => {
                    const d = item.design;
                    const primaryImg = (d && d.images && d.images.length > 0) ? d.images[0].imageUrl : null;
                    return `
                        <div class="list-group-item d-flex align-items-center justify-content-between gap-3 py-3">
                            <div class="d-flex align-items-center gap-3 overflow-hidden">
                                <div class="rounded border" style="width: 52px; height: 52px; background: #f8f9fa; display:flex; align-items:center; justify-content:center; overflow:hidden; flex-shrink: 0;">
                                    ${primaryImg ? `<img src="${primaryImg}" style="width: 100%; height: 100%; object-fit: cover;">` : '<i class="bi bi-file-earmark-arrow-down text-primary fs-4"></i>'}
                                </div>
                                <div class="overflow-hidden">
                                    <h6 class="fw-bold mb-1 text-truncate">${d ? d.name : 'Thiết kế'}</h6>
                                    <div class="small text-muted"><i class="bi bi-clock-history me-1"></i>Tải xuống lúc: ${formatDate(item.downloadedAt)}</div>
                                </div>
                            </div>
                            ${d ? `
                                <button class="btn btn-sm btn-outline-success flex-shrink-0" onclick="handleDownloadDirect(event, ${d.id})">
                                    <i class="bi bi-cloud-arrow-down me-1"></i>Tải lại
                                </button>
                            ` : ''}
                        </div>
                    `;
                }).join('')}
            </div>
        `;

        body.innerHTML = `
            <ul class="nav nav-pills nav-fill mb-4 bg-light p-1 rounded-3" role="tablist">
                <li class="nav-item" role="presentation">
                    <button class="nav-link ${defaultTab === 'purchased' ? 'active' : ''} fw-semibold py-2" data-bs-toggle="pill" data-bs-target="#tabPurchasedPane" type="button" role="tab">
                        <i class="bi bi-cloud-arrow-down me-1"></i>Kho đã mua (${purchasedDesigns.length})
                    </button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link ${defaultTab === 'orders' ? 'active' : ''} fw-semibold py-2" data-bs-toggle="pill" data-bs-target="#tabOrdersPane" type="button" role="tab">
                        <i class="bi bi-receipt me-1"></i>Đơn hàng (${sortedOrders.length})
                        ${pendingCount > 0 ? `<span class="badge bg-warning text-dark ms-1">${pendingCount} chờ</span>` : ''}
                    </button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link ${defaultTab === 'downloads' ? 'active' : ''} fw-semibold py-2" data-bs-toggle="pill" data-bs-target="#tabDownloadsPane" type="button" role="tab">
                        <i class="bi bi-clock-history me-1"></i>Lịch sử tải (${sortedDownloads.length})
                    </button>
                </li>
            </ul>

            <div class="tab-content">
                <div class="tab-pane fade ${defaultTab === 'purchased' ? 'show active' : ''}" id="tabPurchasedPane" role="tabpanel">
                    ${purchasedHtml}
                </div>
                <div class="tab-pane fade ${defaultTab === 'orders' ? 'show active' : ''}" id="tabOrdersPane" role="tabpanel">
                    ${ordersHtml}
                </div>
                <div class="tab-pane fade ${defaultTab === 'downloads' ? 'show active' : ''}" id="tabDownloadsPane" role="tabpanel">
                    ${downloadsHtml}
                </div>
            </div>
        `;
    } catch (err) {
        body.innerHTML = `
            <div class="text-center py-5 text-danger">
                <i class="bi bi-exclamation-circle fs-2"></i>
                <p class="mt-2">Lỗi tải dữ liệu: ${err.message}</p>
            </div>
        `;
    }
}
window.openPurchasedModal = openPurchasedModal;

async function handleCancelOrder(orderId) {
    if (!confirm(`Bạn có chắc chắn muốn hủy đơn hàng #DH-${orderId} không?`)) return;

    try {
        await api.orders.cancel(orderId);
        showToast(`Đã hủy đơn hàng #DH-${orderId}`, 'info');
        openPurchasedModal('orders');
    } catch (err) {
        showToast(err.message || 'Không thể hủy đơn hàng', 'danger');
    }
}
window.handleCancelOrder = handleCancelOrder;

async function handlePayPendingOrder(orderId) {
    const purchasedModalEl = document.getElementById('purchasedModal');
    const purchasedModal = bootstrap.Modal.getInstance(purchasedModalEl);
    if (purchasedModal) purchasedModal.hide();

    const checkoutModalEl = document.getElementById('checkoutModal');
    let checkoutModal = bootstrap.Modal.getInstance(checkoutModalEl);
    if (!checkoutModal) checkoutModal = new bootstrap.Modal(checkoutModalEl);
    const content = document.getElementById('checkoutModalContent');

    content.innerHTML = `
        <div class="text-center py-5">
            <div class="spinner-border text-primary" role="status"></div>
            <p class="mt-2 text-muted">Đang tải thông tin thanh toán đơn hàng #DH-${orderId}...</p>
        </div>
    `;
    checkoutModal.show();

    try {
        const [order, payments] = await Promise.all([
            api.orders.getById(orderId),
            api.payments.getByOrder(orderId).catch(() => [])
        ]);

        let payment = (payments || []).find(p => String(p.status || '').toUpperCase() === 'PENDING');
        if (!payment) {
            payment = await api.payments.create(order.id, order.totalAmount, 'VNPAY_SANDBOX');
        }
        appState.currentCheckout = { order, payment };

        content.innerHTML = `
            <div class="checkout-step p-2">
                <div class="alert alert-info border-0 d-flex align-items-center gap-2 mb-3">
                    <i class="bi bi-shield-check fs-4 text-primary"></i>
                    <div>
                        <strong>Tiếp tục thanh toán đơn hàng</strong>
                        <div class="small">Đơn hàng #DH-${order.id} đang chờ xác nhận thanh toán.</div>
                    </div>
                </div>

                <div class="card bg-light border-0 p-3 mb-4">
                    <div class="d-flex justify-content-between mb-2">
                        <span class="text-muted">Mã đơn hàng:</span>
                        <strong class="font-monospace">#DH-${order.id}</strong>
                    </div>
                    <div class="d-flex justify-content-between mb-2">
                        <span class="text-muted">Tổng số sản phẩm:</span>
                        <span>${order.orderDetails ? order.orderDetails.length : 1} sản phẩm</span>
                    </div>
                    <div class="d-flex justify-content-between border-top pt-2">
                        <span class="fw-bold">Tổng thanh toán:</span>
                        <span class="fw-bold fs-5 text-primary">${formatVND(order.totalAmount)}</span>
                    </div>
                </div>

                <div class="d-grid gap-2">
                    <button class="btn btn-success btn-lg" id="btnConfirmPayment" onclick="handleConfirmPayment(${payment.id})">
                        <i class="bi bi-check2-circle me-1"></i> Xác nhận thanh toán thành công
                    </button>
                    <button class="btn btn-outline-secondary" data-bs-dismiss="modal">
                        Đóng
                    </button>
                </div>
            </div>
        `;
    } catch (err) {
        content.innerHTML = `
            <div class="text-center py-4 text-danger">
                <i class="bi bi-exclamation-triangle fs-1"></i>
                <h5 class="mt-2">Không thể khởi tạo thanh toán</h5>
                <p class="text-muted">${err.message}</p>
                <button class="btn btn-secondary mt-3" data-bs-dismiss="modal">Đóng</button>
            </div>
        `;
    }
}
window.handlePayPendingOrder = handlePayPendingOrder;

async function handleDownloadDirect(e, designId) {
    e.preventDefault();
    try {
        const fullDesign = await api.designs.getById(designId);
        if (fullDesign.files && fullDesign.files.length > 0) {
            const fileId = fullDesign.files[0].id;
            window.location.href = api.files.getDownloadUrl(fileId);
        } else {
            showToast('Thiết kế này chưa có file đính kèm trên hệ thống', 'warning');
        }
    } catch (err) {
        showToast(err.message || 'Không thể tải file', 'danger');
    }
}
window.handleDownloadDirect = handleDownloadDirect;

async function openReviewModal(designId, designName, existingReview = null) {
    const modalEl = document.getElementById('reviewModal');
    let modal = bootstrap.Modal.getInstance(modalEl);
    if (!modal) modal = new bootstrap.Modal(modalEl);

    let reviewToEdit = existingReview;
    if (!reviewToEdit && TokenStorage.isAuthenticated()) {
        try {
            const myReviews = await api.reviews.getMyReviews();
            reviewToEdit = (myReviews || []).find(r => r.design && r.design.id === Number(designId)) || null;
        } catch { }
    }

    document.getElementById('reviewDesignId').value = designId;
    document.getElementById('reviewIdInput').value = reviewToEdit && reviewToEdit.id ? reviewToEdit.id : '';
    document.getElementById('reviewDesignTitle').textContent = designName;
    document.getElementById('reviewComment').value = reviewToEdit ? (reviewToEdit.comment || '') : '';

    const targetRating = String(reviewToEdit ? reviewToEdit.rating : 5);
    document.querySelectorAll('input[name="reviewRating"]').forEach(radio => {
        radio.checked = radio.value === targetRating;
    });

    const titleEl = document.getElementById('reviewModalLabel');
    const submitBtn = document.querySelector('#reviewForm button[type="submit"]');
    if (reviewToEdit && reviewToEdit.id) {
        if (titleEl) titleEl.textContent = 'Chỉnh sửa đánh giá của bạn';
        if (submitBtn) submitBtn.textContent = 'Cập nhật đánh giá';
    } else {
        if (titleEl) titleEl.textContent = 'Đánh giá sản phẩm';
        if (submitBtn) submitBtn.textContent = 'Gửi đánh giá';
    }

    modal.show();
}
window.openReviewModal = openReviewModal;

async function submitReview(e) {
    e.preventDefault();
    const form = e.target;
    const designId = parseInt(form.designId.value);
    const reviewId = form.reviewId && form.reviewId.value ? parseInt(form.reviewId.value) : null;
    const rating = parseInt(form.querySelector('input[name="reviewRating"]:checked')?.value || '5');
    const comment = form.comment.value.trim();
    const submitBtn = form.querySelector('button[type="submit"]');

    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang lưu...';

    try {
        if (reviewId) {
            await api.reviews.update(reviewId, designId, rating, comment);
            showToast('Đã cập nhật đánh giá của bạn!', 'success');
        } else {
            await api.reviews.create(designId, rating, comment);
            showToast('Cảm ơn bạn đã đánh giá thiết kế!', 'success');
        }
        const modal = bootstrap.Modal.getInstance(document.getElementById('reviewModal'));
        if (modal) modal.hide();
        form.reset();

        const detailModalEl = document.getElementById('designDetailModal');
        if (detailModalEl && detailModalEl.classList.contains('show')) {
            openDesignDetail(designId);
        }
        const purchasedModalEl = document.getElementById('purchasedModal');
        if (purchasedModalEl && purchasedModalEl.classList.contains('show')) {
            openPurchasedModal('purchased');
        }
    } catch (err) {
        showToast(err.message || 'Lưu đánh giá không thành công', 'danger');
    } finally {
        submitBtn.disabled = false;
        submitBtn.innerHTML = reviewId ? 'Cập nhật đánh giá' : 'Gửi đánh giá';
    }
}

async function handleDeleteReview(reviewId, designId) {
    if (!confirm('Bạn có chắc chắn muốn xóa đánh giá này không?')) return;

    try {
        await api.reviews.delete(reviewId);
        showToast('Đã xóa đánh giá', 'info');
        const detailModalEl = document.getElementById('designDetailModal');
        if (detailModalEl && detailModalEl.classList.contains('show')) {
            openDesignDetail(designId);
        }
    } catch (err) {
        showToast(err.message || 'Không thể xóa đánh giá', 'danger');
    }
}
window.handleDeleteReview = handleDeleteReview;

document.addEventListener('DOMContentLoaded', () => {
    loadCategories();
    loadDesigns();
    loadFavorites();
    const heroSearchForm = document.getElementById('heroSearchForm');
    if (heroSearchForm) {
        heroSearchForm.addEventListener('submit', (e) => {
            e.preventDefault();
            const input = document.getElementById('heroSearchInput');
            appState.currentFilters.keyword = input ? input.value.trim() : '';
            appState.currentFilters.page = 0;
            loadDesigns();
            document.getElementById('designsSection')?.scrollIntoView({ behavior: 'smooth' });
        });
    }

    const designsSearchForm = document.getElementById('designsSearchForm');
    if (designsSearchForm) {
        designsSearchForm.addEventListener('submit', (e) => {
            e.preventDefault();
            const input = document.getElementById('keywordInput');
            appState.currentFilters.keyword = input ? input.value.trim() : '';
            appState.currentFilters.page = 0;
            loadDesigns();
        });
    }

    const sortSelect = document.getElementById('sortSelect');
    if (sortSelect) {
        sortSelect.addEventListener('change', (e) => {
            appState.currentFilters.sortBy = e.target.value;
            appState.currentFilters.page = 0;
            loadDesigns();
        });
    }

    const btnApplyPriceFilter = document.getElementById('btnApplyPriceFilter');
    if (btnApplyPriceFilter) {
        btnApplyPriceFilter.addEventListener('click', applyPriceFilter);
    }

    const btnOpenCart = document.getElementById('btnOpenCart');
    if (btnOpenCart) {
        btnOpenCart.addEventListener('click', openCartDrawer);
    }

    const btnCartCheckout = document.getElementById('cartCheckoutBtn');
    if (btnCartCheckout) {
        btnCartCheckout.addEventListener('click', startCheckout);
    }

    const btnOpenPurchased = document.querySelectorAll('.action-open-purchased');
    btnOpenPurchased.forEach(btn => btn.addEventListener('click', (e) => {
        e.preventDefault();
        openPurchasedModal('purchased');
    }));

    const btnOpenFavorites = document.querySelectorAll('.action-open-favorites');
    btnOpenFavorites.forEach(btn => btn.addEventListener('click', (e) => {
        e.preventDefault();
        openFavoritesModal();
    }));

    const reviewForm = document.getElementById('reviewForm');
    if (reviewForm) {
        reviewForm.addEventListener('submit', submitReview);
    }

    window.addEventListener('auth:login_success', () => {
        loadFavorites();
        loadCartBadge();
    });
});
