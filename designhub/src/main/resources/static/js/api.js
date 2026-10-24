/**
 * DesignHub - Core API Client
 * Quản lý kết nối REST API, xác thực JWT, tự động làm mới token
 */
const API_BASE_URL = '';

const TokenStorage = {
    ACCESS_TOKEN_KEY: 'dh_access_token',
    REFRESH_TOKEN_KEY: 'dh_refresh_token',
    USER_KEY: 'dh_user_profile',

    getAccessToken() {
        return localStorage.getItem(this.ACCESS_TOKEN_KEY);
    },
    getRefreshToken() {
        return localStorage.getItem(this.REFRESH_TOKEN_KEY);
    },
    getUser() {
        try {
            const raw = localStorage.getItem(this.USER_KEY);
            return raw ? JSON.parse(raw) : null;
        } catch {
            return null;
        }
    },
    setTokens(accessToken, refreshToken) {
        if (accessToken) localStorage.setItem(this.ACCESS_TOKEN_KEY, accessToken);
        if (refreshToken) localStorage.setItem(this.REFRESH_TOKEN_KEY, refreshToken);
    },
    setUser(user) {
        if (user) {
            localStorage.setItem(this.USER_KEY, JSON.stringify(user));
        } else {
            localStorage.removeItem(this.USER_KEY);
        }
    },
    clear() {
        localStorage.removeItem(this.ACCESS_TOKEN_KEY);
        localStorage.removeItem(this.REFRESH_TOKEN_KEY);
        localStorage.removeItem(this.USER_KEY);
    },
    isAuthenticated() {
        return !!this.getAccessToken();
    }
};

let isRefreshing = false;
let refreshSubscribers = [];

function subscribeTokenRefresh(cb) {
    refreshSubscribers.push(cb);
}

function onRefreshed(token) {
    refreshSubscribers.forEach(cb => cb(token));
    refreshSubscribers = [];
}

async function request(endpoint, options = {}) {
    const url = endpoint.startsWith('http') ? endpoint : `${API_BASE_URL}${endpoint}`;
    const headers = options.headers || {};

    if (!(options.body instanceof FormData)) {
        headers['Content-Type'] = headers['Content-Type'] || 'application/json';
    }

    const token = TokenStorage.getAccessToken();
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const config = {
        ...options,
        headers
    };

    try {
        let response = await fetch(url, config);

        if (response.status === 401 && TokenStorage.getRefreshToken() && !endpoint.includes('/api/auth/')) {
            if (!isRefreshing) {
                isRefreshing = true;
                try {
                    const refreshRes = await fetch(`${API_BASE_URL}/api/auth/refresh-token`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ refreshToken: TokenStorage.getRefreshToken() })
                    });

                    if (refreshRes.ok) {
                        const refreshData = await refreshRes.json();
                        const newToken = refreshData.accessToken || refreshData.token; TokenStorage.setTokens(newToken, refreshData.refreshToken || TokenStorage.getRefreshToken());
                        isRefreshing = false;
                        onRefreshed(refreshData.accessToken || refreshData.token);
                    } else {
                        isRefreshing = false;
                        TokenStorage.clear();
                        window.dispatchEvent(new CustomEvent('auth:expired'));
                        throw new Error('Phiên đăng nhập đã hết hạn');
                    }
                } catch (err) {
                    isRefreshing = false;
                    TokenStorage.clear();
                    window.dispatchEvent(new CustomEvent('auth:expired'));
                    throw err;
                }
            }

            return new Promise((resolve) => {
                subscribeTokenRefresh((newToken) => {
                    headers['Authorization'] = `Bearer ${newToken}`;
                    resolve(fetch(url, { ...config, headers }).then(handleResponse));
                });
            });
        }

        return await handleResponse(response);
    } catch (error) {
        console.error(`[API Error] ${endpoint}:`, error);
        throw error;
    }
}

async function handleResponse(response) {
    const contentType = response.headers.get('content-type');
    const isJson = contentType && contentType.includes('application/json');

    if (!response.ok) {
        let errorMessage = `Yêu cầu thất bại (${response.status})`;
        if (isJson) {
            try {
                const errData = await response.json();
                errorMessage = errData.message || errData.error || errData.detail || JSON.stringify(errData);
            } catch { }
        } else {
            const text = await response.text();
            if (text) errorMessage = text;
        }
        const error = new Error(errorMessage);
        error.status = response.status;
        throw error;
    }

    if (response.status === 204) {
        return null;
    }

    return isJson ? await response.json() : await response.text();
}

const api = {
    auth: {
        login: (email, password) => request('/api/auth/login', {
            method: 'POST',
            body: JSON.stringify({ email, password })
        }),
        register: (data) => request('/api/auth/register', {
            method: 'POST',
            body: JSON.stringify(data)
        }),
        logout: () => {
            const rf = TokenStorage.getRefreshToken();
            if (rf) {
                request('/api/auth/logout', {
                    method: 'POST',
                    body: JSON.stringify({ refreshToken: rf })
                }).catch(() => { });
            }
            TokenStorage.clear();
            window.dispatchEvent(new CustomEvent('auth:change'));
        },
        getMe: () => request('/api/users/me')
    },

    categories: {
        getAll: () => request('/api/categories')
    },

    designs: {
        getAll: (params = {}) => {
            const query = new URLSearchParams();
            if (params.keyword) query.append('keyword', params.keyword);
            if (params.categoryId) query.append('categoryId', params.categoryId);
            if (params.designerId) query.append('designerId', params.designerId);
            if (params.minPrice) query.append('minPrice', params.minPrice);
            if (params.maxPrice) query.append('maxPrice', params.maxPrice);
            if (params.sortBy) query.append('sortBy', params.sortBy);
            if (params.page !== undefined) query.append('page', params.page);
            if (params.size !== undefined) query.append('size', params.size);

            const queryString = query.toString();
            return request(`/api/designs${queryString ? '?' + queryString : ''}`);
        },
        getById: (id) => request(`/api/designs/${id}`),
        create: (data) => request('/api/designs', {
            method: 'POST',
            body: JSON.stringify(data)
        }),
        update: (id, data) => request(`/api/designs/${id}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        }),
        delete: (id) => request(`/api/designs/${id}`, {
            method: 'DELETE'
        }),
        getMyDesigns: (params = {}) => {
            const query = new URLSearchParams();
            if (params.keyword) query.append('keyword', params.keyword);
            if (params.categoryId) query.append('categoryId', params.categoryId);
            if (params.sortBy) query.append('sortBy', params.sortBy);
            if (params.page !== undefined) query.append('page', params.page);
            if (params.size !== undefined) query.append('size', params.size);
            const queryString = query.toString();
            return request(`/api/designs/my${queryString ? '?' + queryString : ''}`);
        },
        getImages: (designId) => request(`/api/design-images/design/${designId}`),
        addImage: (designId, imageUrl, isPrimary = false) => request('/api/design-images', {
            method: 'POST',
            body: JSON.stringify({
                design: { id: designId },
                imageUrl,
                isPrimary
            })
        }),
        deleteImage: (imageId) => request(`/api/design-images/${imageId}`, {
            method: 'DELETE'
        }),
        getFiles: (designId) => request(`/api/design-files/design/${designId}`),
        addFile: (designId, fileInfo) => request('/api/design-files', {
            method: 'POST',
            body: JSON.stringify({
                design: { id: designId },
                fileName: fileInfo.fileName,
                fileUrl: fileInfo.fileUrl,
                fileSize: fileInfo.fileSize,
                fileType: fileInfo.fileType
            })
        }),
        deleteFile: (fileId) => request(`/api/design-files/${fileId}`, {
            method: 'DELETE'
        })
    },

    designer: {
        getMe: () => request('/api/designers/me'),
        register: (data) => request('/api/designers/register', {
            method: 'POST',
            body: JSON.stringify(data || {})
        }),
        update: (id, data) => request(`/api/designers/${id}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        }),
        getDashboard: () => request('/api/analytics/designer')
    },

    upload: {
        image: async (file) => {
            const formData = new FormData();
            formData.append('file', file);
            return request('/api/upload/image', {
                method: 'POST',
                body: formData
            });
        },
        file: async (file) => {
            const formData = new FormData();
            formData.append('file', file);
            return request('/api/upload/file', {
                method: 'POST',
                body: formData
            });
        }
    },

    cart: {
        getMyCart: () => request('/api/carts/me'),
        addItem: (cartId, designId, quantity = 1) => request('/api/cart-items', {
            method: 'POST',
            body: JSON.stringify({
                cart: { id: cartId },
                design: { id: designId },
                quantity
            })
        }),
        removeItem: (cartItemId) => request(`/api/cart-items/${cartItemId}`, {
            method: 'DELETE'
        })
    },

    orders: {
        checkout: () => request('/api/orders/checkout', {
            method: 'POST'
        }),
        getMyOrders: () => request('/api/orders/me'),
        getById: (id) => request(`/api/orders/${id}`)
    },

    payments: {
        create: (orderId, amount, paymentMethod = 'VNPAY') => request('/api/payments', {
            method: 'POST',
            body: JSON.stringify({
                order: { id: orderId },
                amount,
                paymentMethod,
                transactionId: `TXN_${Date.now()}`
            })
        }),
        confirm: (paymentId, transactionId) => {
            const query = transactionId ? `?transactionId=${encodeURIComponent(transactionId)}` : '';
            return request(`/api/payments/${paymentId}/confirm${query}`, {
                method: 'POST'
            });
        }
    },

    files: {
        getDownloadUrl: (fileId) => {
            const token = TokenStorage.getAccessToken();
            return `/api/design-files/${fileId}/download${token ? '?token=' + encodeURIComponent(token) : ''}`;
        }
    },

    reviews: {
        getByDesign: (designId) => request(`/api/reviews/design/${designId}`),
        create: (designId, rating, comment) => request('/api/reviews', {
            method: 'POST',
            body: JSON.stringify({
                design: { id: designId },
                rating,
                comment
            })
        })
    }
};

window.TokenStorage = TokenStorage;
window.api = api;
