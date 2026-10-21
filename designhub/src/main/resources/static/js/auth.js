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
    } else {
        guestElements.forEach(el => el.classList.remove('d-none'));
        loggedElements.forEach(el => el.classList.add('d-none'));
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

    if (password.length < 8 || password.length > 20) {
        if (alertBox) {
            alertBox.textContent = 'Mật khẩu phải có độ dài từ 8 đến 20 ký tự!';
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
});

window.getCurrentUser = () => currentUser;
