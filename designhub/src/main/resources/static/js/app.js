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
        sortBy: 'createdAt_desc',
        page: 0,
        size: 8
    },
    cart: null,
    currentCheckout: null
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
    loadDesigns();

    const target = document.getElementById('designsSection');
    if (target) {
        target.scrollIntoView({ behavior: 'smooth' });
    }
};

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
    const designerName = d.designer && d.designer.user ? (d.designer.user.fullName || d.designer.user.username) : 'Designer';
    const initial = designerName.charAt(0).toUpperCase();
    const categoryName = d.category ? d.category.name : 'Thiết kế';

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
                </div>

                <div class="product-info d-flex flex-column flex-grow-1">
                    <span class="product-category">${categoryName}</span>
                    <h3 class="product-title cursor-pointer" onclick="openDesignDetail(${d.id})" title="${d.name}">
                        ${d.name}
                    </h3>

                    <div class="designer mb-2">
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
    appState.currentFilters.sortBy = 'createdAt_desc';
    appState.currentFilters.page = 0;

    const searchInput = document.getElementById('keywordInput');
    if (searchInput) searchInput.value = '';
    const heroSearchInput = document.getElementById('heroSearchInput');
    if (heroSearchInput) heroSearchInput.value = '';

    renderCategoryFilters();
    loadDesigns();
};

async function openDesignDetail(id) {
    const modalEl = document.getElementById('designDetailModal');
    const modal = new bootstrap.Modal(modalEl);
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

        const designerName = design.designer && design.designer.user ? (design.designer.user.fullName || design.designer.user.username) : 'Designer';
        const primaryImg = (design.images && design.images.length > 0) ? design.images[0].imageUrl : null;
        const avgRating = reviews.length ? (reviews.reduce((acc, r) => acc + r.rating, 0) / reviews.length).toFixed(1) : '5.0';

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

                    <div class="d-flex align-items-center gap-2 mb-4 p-2 bg-light rounded-3">
                        <div class="designer-avatar">${designerName.charAt(0).toUpperCase()}</div>
                        <div>
                            <div class="fw-semibold">${designerName}</div>
                            <small class="text-muted">Tác giả thiết kế</small>
                        </div>
                    </div>

                    <div class="display-6 fw-bold text-primary mb-4">
                        ${formatVND(design.price)}
                    </div>

                    <p class="text-secondary mb-4" style="white-space: pre-line;">
                        ${design.description || 'Chưa có mô tả chi tiết cho thiết kế này.'}
                    </p>

                    <div class="d-flex gap-2 mb-4">
                        <button class="btn btn-outline-primary btn-lg flex-grow-1" onclick="handleAddToCart(${design.id})">
                            <i class="bi bi-cart-plus me-1"></i> Thêm vào giỏ
                        </button>
                        <button class="btn btn-primary btn-lg flex-grow-1" onclick="handleBuyNow(${design.id})">
                            <i class="bi bi-lightning-charge me-1"></i> Mua ngay
                        </button>
                    </div>

                    <div class="border-top pt-3">
                        <h5 class="fw-bold mb-3">Đánh giá từ khách hàng (${reviews.length})</h5>
                        <div class="reviews-list" style="max-height: 200px; overflow-y: auto;">
                            ${reviews.length ? reviews.map(r => `
                                <div class="border-bottom py-2">
                                    <div class="d-flex justify-content-between">
                                        <strong class="small">${r.user ? (r.user.fullName || r.user.username) : 'Khách hàng'}</strong>
                                        <div class="text-warning small">${'★'.repeat(r.rating)}${'☆'.repeat(5 - r.rating)}</div>
                                    </div>
                                    <p class="small text-muted mb-0">${r.comment || 'Không có bình luận.'}</p>
                                </div>
                            `).join('') : '<p class="text-muted small">Chưa có đánh giá nào cho sản phẩm này.</p>'}
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

async function openPurchasedModal() {
    if (!TokenStorage.isAuthenticated()) {
        showToast('Vui lòng đăng nhập để xem kho thiết kế!', 'warning');
        const loginModal = new bootstrap.Modal(document.getElementById('loginModal'));
        loginModal.show();
        return;
    }

    const modalEl = document.getElementById('purchasedModal');
    const modal = new bootstrap.Modal(modalEl);
    const body = document.getElementById('purchasedModalBody');

    body.innerHTML = `
        <div class="text-center py-5">
            <div class="spinner-border text-primary" role="status"></div>
            <p class="mt-2 text-muted">Đang tải kho thiết kế của bạn...</p>
        </div>
    `;
    modal.show();

    try {
        const orders = await api.orders.getMyOrders();
        const paidOrders = (orders || []).filter(o => String(o.status || '').toUpperCase() === 'PAID');

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

        if (purchasedDesigns.length === 0) {
            body.innerHTML = `
                <div class="text-center py-5">
                    <i class="bi bi-folder2-open fs-1 text-muted"></i>
                    <h5 class="mt-3">Kho thiết kế của bạn đang trống</h5>
                    <p class="text-muted">Bạn chưa mua hoặc chưa hoàn tất thanh toán đơn hàng nào.</p>
                    <button class="btn btn-primary btn-sm mt-2" data-bs-dismiss="modal" onclick="resetFilters()">Khám phá thiết kế ngay</button>
                </div>
            `;
            return;
        }

        body.innerHTML = `
            <div class="row g-3">
                ${purchasedDesigns.map(item => {
            const d = item.design;
            const primaryImg = (d.images && d.images.length > 0) ? d.images[0].imageUrl : null;
            const files = d.files || [];

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
                                    <button class="btn btn-sm btn-outline-warning" onclick="openReviewModal(${d.id}, '${d.name.replace(/'/g, "\\'")}')">
                                        <i class="bi bi-star me-1"></i> Đánh giá
                                    </button>
                                </div>
                            </div>
                        </div>
                    `;
        }).join('')}
            </div>
        `;
    } catch (err) {
        body.innerHTML = `
            <div class="text-center py-5 text-danger">
                <i class="bi bi-exclamation-circle fs-2"></i>
                <p class="mt-2">Lỗi tải kho thiết kế: ${err.message}</p>
            </div>
        `;
    }
}
window.openPurchasedModal = openPurchasedModal;

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

function openReviewModal(designId, designName) {
    const modalEl = document.getElementById('reviewModal');
    const modal = new bootstrap.Modal(modalEl);

    document.getElementById('reviewDesignId').value = designId;
    document.getElementById('reviewDesignTitle').textContent = designName;
    document.getElementById('reviewComment').value = '';
    document.querySelectorAll('input[name="reviewRating"]').forEach(radio => {
        radio.checked = radio.value === '5';
    });

    modal.show();
}
window.openReviewModal = openReviewModal;

async function submitReview(e) {
    e.preventDefault();
    const form = e.target;
    const designId = parseInt(form.designId.value);
    const rating = parseInt(form.querySelector('input[name="reviewRating"]:checked')?.value || '5');
    const comment = form.comment.value.trim();
    const submitBtn = form.querySelector('button[type="submit"]');

    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang gửi...';

    try {
        await api.reviews.create(designId, rating, comment);
        showToast('Cảm ơn bạn đã đánh giá thiết kế!', 'success');
        const modal = bootstrap.Modal.getInstance(document.getElementById('reviewModal'));
        if (modal) modal.hide();
        form.reset();
    } catch (err) {
        showToast(err.message || 'Gửi đánh giá không thành công', 'danger');
    } finally {
        submitBtn.disabled = false;
        submitBtn.innerHTML = 'Gửi đánh giá';
    }
}

document.addEventListener('DOMContentLoaded', () => {
    loadCategories();
    loadDesigns();
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
        openPurchasedModal();
    }));

    const reviewForm = document.getElementById('reviewForm');
    if (reviewForm) {
        reviewForm.addEventListener('submit', submitReview);
    }
});
