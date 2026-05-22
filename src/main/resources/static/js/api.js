const API_BASE = '/api';

// Store & retrieve token
const Auth = {
    setSession(data) {
        localStorage.setItem('token', data.token);
        localStorage.setItem('user', JSON.stringify({
            id: data.id, name: data.name,
            email: data.email, role: data.role
        }));
    },
    getToken()  { return localStorage.getItem('token'); },
    getUser()   { return JSON.parse(localStorage.getItem('user') || 'null'); },
    clear()     { localStorage.removeItem('token'); localStorage.removeItem('user'); },
    isLoggedIn(){ return !!this.getToken(); },
};

// Base fetch wrapper
async function apiFetch(path, options = {}) {
    const token = Auth.getToken();
    const res = await fetch(API_BASE + path, {
        headers: {
            'Content-Type': 'application/json',
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
            ...options.headers,
        },
        ...options,
    });
    const data = await res.json().catch(() => ({}));

    // ── Handle force logout or blocked account ──
    if (res.status === 401) {
        const msg = data.message || '';
        if (msg.includes('terminated') || msg.includes('session')) {
            Auth.clear();
            alert('⚠️ ' + msg);
            window.location.href = '/login.html';
            return;
        }
    }

    if (!res.ok) throw new Error(data.message || `Error ${res.status}`);
    return data;
}

// Auth endpoints
const AuthAPI = {
    login:    (body) => apiFetch('/auth/login',    { method: 'POST', body: JSON.stringify(body) }),
    register: (body) => apiFetch('/auth/register', { method: 'POST', body: JSON.stringify(body) }),
};

// Property endpoints (to be expanded)
const PropertyAPI = {
    list:         ()           => apiFetch('/properties'),
    get:          (id)         => apiFetch(`/properties/${id}`),
    create:       (body)       => apiFetch('/properties',     { method: 'POST', body: JSON.stringify(body) }),
    update:       (id, body)   => apiFetch(`/properties/${id}`,{ method: 'PUT',  body: JSON.stringify(body) }),
    delete:       (id)         => apiFetch(`/properties/${id}`,{ method: 'DELETE' }),
    // Units
    getUnits:     (propId)     => apiFetch(`/properties/${propId}/units`),
    createUnit:   (propId, body) => apiFetch(`/properties/${propId}/units`, { method: 'POST', body: JSON.stringify(body) }),
    updateUnit:   (unitId, body) => apiFetch(`/units/${unitId}`, { method: 'PUT',  body: JSON.stringify(body) }),
    deleteUnit:   (unitId)     => apiFetch(`/units/${unitId}`,   { method: 'DELETE' }),
};

// Tenant endpoints
const TenantAPI = {
    list:     ()      => apiFetch('/tenants'),
    active:   ()      => apiFetch('/tenants/active'),
    get:      (id)    => apiFetch(`/tenants/${id}`),
    register: (body)  => apiFetch('/tenants', { method: 'POST', body: JSON.stringify(body) }),
    update:   (id, body) => apiFetch(`/tenants/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    vacate:   (id)    => apiFetch(`/tenants/${id}/vacate`, { method: 'PUT' }),
    stats:    ()      => apiFetch('/tenants/stats'),
    vacantUnits: ()   => apiFetch('/units/vacant'),
    myArrears:    ()   => apiFetch('/tenants/my-arrears'),
    arrears:      (id) => apiFetch(`/tenants/${id}/arrears`),
};

// Payment endpoints
const PaymentAPI = {
    list:     ()     => apiFetch('/payments'),
    initiate: (body) => apiFetch('/payments/mpesa/stk-push',
        { method: 'POST', body: JSON.stringify(body) }),
    status:   (id)   => apiFetch(`/payments/status/${id}`),
    tenant:   (id)   => apiFetch(`/payments/tenant/${id}`),
    stats:    ()     => apiFetch('/payments/stats'),
};

// Tenant self-service
const TenantSelfAPI = {
    profile:   () => apiFetch('/tenants/my-profile'),
    dashboard: () => apiFetch('/tenants/my-dashboard'),
};

// Maintenance
const MaintenanceAPI = {
    list:         ()         => apiFetch('/maintenance'),
    myRequests:   ()         => apiFetch('/maintenance/my-requests'),
    create:       (body)     => apiFetch('/maintenance',
        { method: 'POST', body: JSON.stringify(body) }),
    updateStatus: (id, body) => apiFetch(`/maintenance/${id}/status`,
        { method: 'PUT', body: JSON.stringify(body) }),
    stats:        ()         => apiFetch('/maintenance/stats'),
};
// Report
const ReportAPI = {
    summary:  () => apiFetch('/reports/summary'),
    revenue:  () => apiFetch('/reports/revenue/monthly'),
    occupancy:() => apiFetch('/reports/occupancy'),
    outstanding:()=> apiFetch('/reports/outstanding'),
};
// Caretaker
const CaretakerAPI = {
    list:       ()         => apiFetch('/caretakers'),
    assign:     (cId, pId) => apiFetch(`/caretakers/${cId}/assign/${pId}`,
        { method: 'PUT' }),
    unassign:   (cId)      => apiFetch(`/caretakers/${cId}/unassign`,
        { method: 'PUT' }),
    myProperty: ()         => apiFetch('/caretakers/my-property'),
    myUnits:    ()         => apiFetch('/caretakers/my-units'),
    myTenants:  ()         => apiFetch('/caretakers/my-tenants'),
};
// User Session
const SessionAPI = {
    active:      ()   => apiFetch('/sessions/active'),
    all:         ()   => apiFetch('/sessions'),
    forceLogout: (id) => apiFetch(`/sessions/force-logout/${id}`,
        { method: 'PUT' }),
    block:       (id) => apiFetch(`/sessions/block/${id}`,
        { method: 'PUT' }),
    unblock:     (id) => apiFetch(`/sessions/unblock/${id}`,
        { method: 'PUT' }),
};
// Notifications
const NotificationAPI = {
    list:        ()      => apiFetch('/notifications'),
    unreadCount: ()      => apiFetch('/notifications/unread-count'),
    markRead:    (id)    => apiFetch(`/notifications/${id}/read`,
        { method: 'PUT' }),
    markAllRead: ()      => apiFetch('/notifications/mark-all-read',
        { method: 'PUT' }),
    delete:      (id)    => apiFetch(`/notifications/${id}`,
        { method: 'DELETE' }),
    deleteAll:   ()      => apiFetch('/notifications/delete-all',
        { method: 'DELETE' }),
    sendNotice:  (body)  => apiFetch('/notifications/send-notice',
        { method: 'POST', body: JSON.stringify(body) }),
};
const PaymentSettingsAPI = {
    get:    ()     => apiFetch('/payment-settings'),
    update: (body) => apiFetch('/payment-settings',
        { method: 'PUT', body: JSON.stringify(body) }),
};