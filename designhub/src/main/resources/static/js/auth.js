/**
 * DesignHub - Auth Module
 * Quản lý trạng thái đăng nhập, modal Đăng nhập / Đăng ký, hiển thị thông tin người dùng
 */
let currentUser = null;

async function initAuth() {
    if (TokenStorage.isAuthenticated()) {
        try {
            currentUser = await api.auth.getMe();
            TokenStorage.setUser(currentUser);
        } catch (error) {
            console.warn('Không thể lấy thông tin người dùng:', error);
            TokenStorage.clear();
            currentUser = null;
        }
    } else {
        currentUser = null;
    }
    updateAuthUI();
}

function updateAuthUI() {
    const guestElements = document.querySelectorAll('.auth-guest');
    const loggedElements = document.querySelectorAll('.auth-logged');
    const userNameElements = document.querySelectorAll('.user-display-name');
    const userRoleBadge = document.getElementById('userRoleBadge');
    const userAvatarEl = document.getElementById('userAvatarInitial');

    if (currentUser) {
        guestElements.forEach(el => el.classList.add('d-none'));
        loggedElements.forEach(el => el.classList.remove('d-none'));

        userNameElements.forEach(el => {
            el.textContent = currentUser.fullName || currentUser.username || 'Người dùng';
        });

        if (userAvatarEl) {
            const initial = (currentUser.fullName || currentUser.username || 'U').charAt(0).toUpperCase();
            userAvatarEl.textContent = initial;
        }

        if (userRoleBadge) {
            userRoleBadge.textContent = currentUser.role;
            userRoleBadge.className = 'badge';
            if (currentUser.role === 'ADMIN') {
                userRoleBadge.classList.add('bg-danger');
            } else if (currentUser.role === 'DESIGNER') {
                userRoleBadge.classList.add('bg-primary');
            } else {
                userRoleBadge.classList.add('bg-secondary');
            }
        }

        if (window.loadCartBadge) {
            window.loadCartBadge();
        }
        if (window.loadFavorites) {
            window.loadFavorites();
        }
    } else {
        guestElements.forEach(el => el.classList.remove('d-none'));
        loggedElements.forEach(el => el.classList.add('d-none'));
    }

    const adminElements = document.querySelectorAll('.auth-admin');
    if (currentUser && currentUser.role === 'ADMIN') {
        adminElements.forEach(el => el.classList.remove('d-none'));
    } else {
        adminElements.forEach(el => el.classList.add('d-none'));
    }
}

async function handleLogin(e) {
    e.preventDefault();
    const form = e.target;
    const email = form.email.value.trim();
    const password = form.password.value;
    const submitBtn = form.querySelector('button[type="submit"]');
    const alertBox = document.getElementById('loginAlert');

    if (alertBox) alertBox.classList.add('d-none');
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang đăng nhập...';

    try {
        const res = await api.auth.login(email, password);
        TokenStorage.setTokens(res.token, res.refreshToken);
        TokenStorage.setUser({
            id: res.id,
            username: res.username,
            email: res.email,
            fullName: res.fullName,
            phone: res.phone,
            role: res.role
        });
        currentUser = TokenStorage.getUser();

        const modalEl = document.getElementById('loginModal');
        const modal = bootstrap.Modal.getInstance(modalEl);
        if (modal) modal.hide();
        form.reset();

        updateAuthUI();
        if (window.showToast) {
            showToast(`Chào mừng ${currentUser.fullName || currentUser.username} quay trở lại!`, 'success');
        }
        window.dispatchEvent(new CustomEvent('auth:login_success', { detail: currentUser }));
    } catch (err) {
        if (alertBox) {
            alertBox.textContent = err.message || 'Email hoặc mật khẩu không chính xác';
            alertBox.classList.remove('d-none');
        } else if (window.showToast) {
            showToast(err.message || 'Đăng nhập không thành công', 'danger');
        }
    } finally {
        submitBtn.disabled = false;
        submitBtn.innerHTML = 'Đăng nhập';
    }
}

async function handleRegister(e) {
    e.preventDefault();
    const form = e.target;
    const username = form.username.value.trim();
    const email = form.email.value.trim();
    const fullName = form.fullName.value.trim();
    const phone = form.phone.value.trim();
    const password = form.password.value;
    const confirmPassword = form.confirmPassword.value;
    const alertBox = document.getElementById('registerAlert');
    const submitBtn = form.querySelector('button[type="submit"]');

    if (alertBox) alertBox.classList.add('d-none');

    if (password !== confirmPassword) {
        if (alertBox) {
            alertBox.textContent = 'Mật khẩu xác nhận không khớp!';
            alertBox.classList.remove('d-none');
        }
        return;
    }

    if (password.length < 8 || password.length > 72) {
        if (alertBox) {
            alertBox.textContent = 'Mật khẩu phải có độ dài từ 8 đến 72 ký tự!';
            alertBox.classList.remove('d-none');
        }
        return;
    }

    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang tạo tài khoản...';

    try {
        const res = await api.auth.register({
            username,
            email,
            fullName,
            phone: phone || null,
            password
        });

        TokenStorage.setTokens(res.token, res.refreshToken);
        TokenStorage.setUser({
            id: res.id,
            username: res.username,
            email: res.email,
            fullName: res.fullName,
            phone: res.phone,
            role: res.role
        });
        currentUser = TokenStorage.getUser();

        const modalEl = document.getElementById('registerModal');
        const modal = bootstrap.Modal.getInstance(modalEl);
        if (modal) modal.hide();
        form.reset();

        updateAuthUI();
        if (window.showToast) {
            showToast('Đăng ký tài khoản thành công! Bạn đã được tự động đăng nhập.', 'success');
        }
        window.dispatchEvent(new CustomEvent('auth:login_success', { detail: currentUser }));
    } catch (err) {
        if (alertBox) {
            alertBox.textContent = err.message || 'Đăng ký không thành công';
            alertBox.classList.remove('d-none');
        } else if (window.showToast) {
            showToast(err.message || 'Đăng ký không thành công', 'danger');
        }
    } finally {
        submitBtn.disabled = false;
        submitBtn.innerHTML = 'Tạo tài khoản';
    }
}

function handleLogout() {
    api.auth.logout();
    currentUser = null;
    updateAuthUI();
    if (window.showToast) {
        showToast('Bạn đã đăng xuất thành công', 'info');
    }
    window.location.reload();
}

window.addEventListener('auth:change', updateAuthUI);
window.addEventListener('auth:expired', () => {
    currentUser = null;
    updateAuthUI();
    if (window.showToast) {
        showToast('Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.', 'warning');
    }
});

function ensureProfileModal() {
    if (document.getElementById('profileModal')) return;

    const modalHtml = `
    <div class="modal fade" id="profileModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <div class="modal-header border-0 pb-0">
                    <div>
                        <h5 class="modal-title fw-bold"><i class="bi bi-person-gear text-primary me-2"></i>Tài khoản cá nhân</h5>
                        <p class="text-muted small mb-0">Cập nhật thông tin tài khoản hoặc đổi mật khẩu bảo mật</p>
                    </div>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body pt-3">
                    <ul class="nav nav-pills nav-fill mb-3 bg-light p-1 rounded-3" id="profileTabs" role="tablist">
                        <li class="nav-item" role="presentation">
                            <button class="nav-link active fw-semibold py-2" id="tab-profile-info-btn" data-bs-toggle="pill" data-bs-target="#tabProfileInfo" type="button" role="tab">
                                <i class="bi bi-person-vcard me-1"></i>Thông tin cá nhân
                            </button>
                        </li>
                        <li class="nav-item" role="presentation">
                            <button class="nav-link fw-semibold py-2" id="tab-profile-pwd-btn" data-bs-toggle="pill" data-bs-target="#tabProfilePassword" type="button" role="tab">
                                <i class="bi bi-shield-lock me-1"></i>Đổi mật khẩu
                            </button>
                        </li>
                    </ul>

                    <div class="tab-content">
                        <!-- Tab 1: Thông tin cá nhân -->
                        <div class="tab-pane fade show active" id="tabProfileInfo" role="tabpanel">
                            <div id="profileInfoAlert" class="alert alert-danger d-none py-2 small"></div>
                            <form id="profileInfoForm">
                                <div class="row g-2 mb-3">
                                    <div class="col-6">
                                        <label class="form-label small fw-semibold text-muted mb-1">Username</label>
                                        <input type="text" class="form-control bg-light" id="profileUsername" readonly disabled>
                                    </div>
                                    <div class="col-6">
                                        <label class="form-label small fw-semibold text-muted mb-1">Vai trò</label>
                                        <input type="text" class="form-control bg-light" id="profileRole" readonly disabled>
                                    </div>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label small fw-semibold text-muted mb-1">Email đăng nhập</label>
                                    <input type="email" class="form-control bg-light" id="profileEmail" readonly disabled>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label small fw-semibold">Họ và tên <span class="text-danger">*</span></label>
                                    <input type="text" class="form-control" id="profileFullName" name="fullName" required maxlength="100" placeholder="Nhập họ và tên của bạn">
                                </div>
                                <div class="mb-4">
                                    <label class="form-label small fw-semibold">Số điện thoại</label>
                                    <input type="text" class="form-control" id="profilePhone" name="phone" pattern="^$|[0-9]{10,11}" placeholder="Nhập số điện thoại (10-11 chữ số)">
                                </div>
                                <button type="submit" class="btn btn-primary w-100 py-2 fw-semibold" id="btnSaveProfileInfo">
                                    <i class="bi bi-check2-circle me-1"></i>Lưu thay đổi
                                </button>
                            </form>
                        </div>

                        <!-- Tab 2: Đổi mật khẩu -->
                        <div class="tab-pane fade" id="tabProfilePassword" role="tabpanel">
                            <div id="profilePwdAlert" class="alert alert-danger d-none py-2 small"></div>
                            <form id="profilePasswordForm">
                                <div class="mb-3">
                                    <label class="form-label small fw-semibold">Mật khẩu hiện tại <span class="text-danger">*</span></label>
                                    <input type="password" class="form-control" id="profileCurrentPassword" required placeholder="Nhập mật khẩu hiện tại">
                                </div>
                                <div class="mb-3">
                                    <label class="form-label small fw-semibold">Mật khẩu mới <span class="text-danger">*</span></label>
                                    <input type="password" class="form-control" id="profileNewPassword" required minlength="8" maxlength="72" placeholder="Tối thiểu 8 ký tự">
                                </div>
                                <div class="mb-4">
                                    <label class="form-label small fw-semibold">Xác nhận mật khẩu mới <span class="text-danger">*</span></label>
                                    <input type="password" class="form-control" id="profileConfirmPassword" required minlength="8" maxlength="72" placeholder="Nhập lại mật khẩu mới">
                                </div>
                                <button type="submit" class="btn btn-primary w-100 py-2 fw-semibold" id="btnSavePassword">
                                    <i class="bi bi-key me-1"></i>Cập nhật mật khẩu
                                </button>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>`;

    document.body.insertAdjacentHTML('beforeend', modalHtml);

    const infoForm = document.getElementById('profileInfoForm');
    if (infoForm) {
        infoForm.addEventListener('submit', handleUpdateProfileInfo);
    }

    const pwdForm = document.getElementById('profilePasswordForm');
    if (pwdForm) {
        pwdForm.addEventListener('submit', handleChangePassword);
    }
}

async function openProfileModal(defaultTab = 'info') {
    if (!TokenStorage.isAuthenticated()) {
        const loginModalEl = document.getElementById('loginModal');
        if (loginModalEl) new bootstrap.Modal(loginModalEl).show();
        return;
    }

    ensureProfileModal();

    const infoAlert = document.getElementById('profileInfoAlert');
    const pwdAlert = document.getElementById('profilePwdAlert');
    if (infoAlert) infoAlert.classList.add('d-none');
    if (pwdAlert) pwdAlert.classList.add('d-none');

    try {
        const profile = await api.users.getMe();
        currentUser = { ...currentUser, ...profile };
        TokenStorage.setUser(currentUser);
        updateAuthUI();
    } catch { }

    if (currentUser) {
        document.getElementById('profileUsername').value = currentUser.username || '';
        document.getElementById('profileRole').value = currentUser.role || 'CUSTOMER';
        document.getElementById('profileEmail').value = currentUser.email || '';
        document.getElementById('profileFullName').value = currentUser.fullName || '';
        document.getElementById('profilePhone').value = currentUser.phone || '';
    }

    const pwdForm = document.getElementById('profilePasswordForm');
    if (pwdForm) pwdForm.reset();

    if (defaultTab === 'password') {
        const pwdTabBtn = document.getElementById('tab-profile-pwd-btn');
        if (pwdTabBtn) new bootstrap.Tab(pwdTabBtn).show();
    } else {
        const infoTabBtn = document.getElementById('tab-profile-info-btn');
        if (infoTabBtn) new bootstrap.Tab(infoTabBtn).show();
    }

    new bootstrap.Modal(document.getElementById('profileModal')).show();
}

async function handleUpdateProfileInfo(e) {
    e.preventDefault();
    const alertBox = document.getElementById('profileInfoAlert');
    const btn = document.getElementById('btnSaveProfileInfo');
    const fullName = document.getElementById('profileFullName').value.trim();
    const phone = document.getElementById('profilePhone').value.trim();

    if (alertBox) alertBox.classList.add('d-none');
    if (!fullName) {
        if (alertBox) {
            alertBox.textContent = 'Họ và tên không được để trống';
            alertBox.classList.remove('d-none');
        }
        return;
    }

    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang lưu...';

    try {
        const updated = await api.users.updateMe({ fullName, phone: phone || '' });
        currentUser = { ...currentUser, ...updated };
        TokenStorage.setUser(currentUser);
        updateAuthUI();

        if (window.showToast) {
            showToast('Cập nhật thông tin tài khoản thành công!', 'success');
        }
        const modal = bootstrap.Modal.getInstance(document.getElementById('profileModal'));
        if (modal) modal.hide();
    } catch (err) {
        if (alertBox) {
            alertBox.textContent = err.message || 'Không thể cập nhật thông tin';
            alertBox.classList.remove('d-none');
        }
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-check2-circle me-1"></i>Lưu thay đổi';
    }
}

async function handleChangePassword(e) {
    e.preventDefault();
    const alertBox = document.getElementById('profilePwdAlert');
    const btn = document.getElementById('btnSavePassword');
    const currentPassword = document.getElementById('profileCurrentPassword').value;
    const newPassword = document.getElementById('profileNewPassword').value;
    const confirmPassword = document.getElementById('profileConfirmPassword').value;

    if (alertBox) alertBox.classList.add('d-none');

    if (newPassword !== confirmPassword) {
        if (alertBox) {
            alertBox.textContent = 'Mật khẩu mới và xác nhận mật khẩu không khớp!';
            alertBox.classList.remove('d-none');
        }
        return;
    }

    if (newPassword.length < 8 || newPassword.length > 72) {
        if (alertBox) {
            alertBox.textContent = 'Mật khẩu mới phải từ 8 đến 72 ký tự!';
            alertBox.classList.remove('d-none');
        }
        return;
    }

    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang cập nhật...';

    try {
        await api.users.changePassword(currentPassword, newPassword);
        e.target.reset();
        if (window.showToast) {
            showToast('Đổi mật khẩu thành công!', 'success');
        }
        const modal = bootstrap.Modal.getInstance(document.getElementById('profileModal'));
        if (modal) modal.hide();
    } catch (err) {
        if (alertBox) {
            alertBox.textContent = err.message || 'Đổi mật khẩu thất bại';
            alertBox.classList.remove('d-none');
        }
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-key me-1"></i>Cập nhật mật khẩu';
    }
}

document.addEventListener('DOMContentLoaded', () => {
    initAuth();

    const loginForm = document.getElementById('loginForm');
    if (loginForm) loginForm.addEventListener('submit', handleLogin);

    const registerForm = document.getElementById('registerForm');
    if (registerForm) registerForm.addEventListener('submit', handleRegister);

    const logoutButtons = document.querySelectorAll('.action-logout');
    logoutButtons.forEach(btn => btn.addEventListener('click', (e) => {
        e.preventDefault();
        handleLogout();
    }));

    document.querySelectorAll('.action-open-profile').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault();
            openProfileModal('info');
        });
    });

    document.querySelectorAll('.action-open-password').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault();
            openProfileModal('password');
        });
    });
});

window.getCurrentUser = () => currentUser;
window.openProfileModal = openProfileModal;
