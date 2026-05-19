// ── Routing ───────────────────────────────────────────
const currentPage = window.location.pathname;
const isAuthPage  = currentPage.includes('login') || currentPage.includes('register');

if (isAuthPage && Auth.isLoggedIn()) {
    // Verify token is still valid before redirecting
    const user = Auth.getUser();
    if (user && user.role) {
        redirectByRole(user.role);
    } else {
        // Token exists but is invalid — clear and stay on login
        Auth.clear();
    }
}

function redirectByRole(role) {
    const routes = {
        ADMIN:      '/pages/admin/dashboard.html',
        TENANT:     '/pages/tenant/dashboard.html',
        CARETAKER:  '/pages/caretaker/dashboard.html',
    };
    window.location.href = routes[role] || '/login.html';
}

// ── Login form ────────────────────────────────────────
const loginForm = document.getElementById('loginForm');
if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const btn   = document.getElementById('loginBtn');
        const alert = document.getElementById('alertBox');

        btn.classList.add('loading');
        btn.disabled = true;
        alert.classList.remove('show');

        try {
            const data = await AuthAPI.login({
                email:    document.getElementById('email').value.trim(),
                password: document.getElementById('password').value,
            });
            Auth.setSession(data);
            redirectByRole(data.role);
        } catch (err) {
            alert.textContent = err.message;
            alert.className   = 'alert alert-error show';
            btn.classList.remove('loading');
            btn.disabled = false;
        }
    });
}

// ── Register form ─────────────────────────────────────
const registerForm = document.getElementById('registerForm');
if (registerForm) {
    // Role selector
    let selectedRole = 'TENANT';
    document.querySelectorAll('.role-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.role-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            selectedRole = btn.dataset.role;
        });
    });

    // Password toggles
    document.querySelectorAll('.toggle-pass').forEach(btn => {
        btn.addEventListener('click', () => {
            const input = btn.previousElementSibling;
            input.type  = input.type === 'password' ? 'text' : 'password';
            btn.textContent = input.type === 'password' ? '👁' : '🙈';
        });
    });

    registerForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const btn   = document.getElementById('registerBtn');
        const alert = document.getElementById('alertBox');

        btn.classList.add('loading');
        btn.disabled = true;
        alert.classList.remove('show');

        const password = document.getElementById('password').value;
        const confirm  = document.getElementById('confirmPassword').value;

        if (password !== confirm) {
            alert.textContent = 'Passwords do not match';
            alert.className   = 'alert alert-error show';
            btn.classList.remove('loading');
            btn.disabled = false;
            return;
        }

        try {
            await AuthAPI.register({
                name:     document.getElementById('name').value.trim(),
                email:    document.getElementById('email').value.trim(),
                phone:    document.getElementById('phone').value.trim(),
                password,
                role:     selectedRole,
            });
            alert.textContent = '✓ Account created! Redirecting to login...';
            alert.className   = 'alert alert-success show';
            setTimeout(() => window.location.href = '/login.html', 1500);
        } catch (err) {
            alert.textContent = err.message;
            alert.className   = 'alert alert-error show';
            btn.classList.remove('loading');
            btn.disabled = false;
        }
    });
}

// ── Logout ────────────────────────────────────────────
async function logout() {
    try {
        const token = Auth.getToken();
        if (token) {
            await fetch('/api/auth/logout', {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` }
            });
        }
    } catch (err) { console.error(err); }
    Auth.clear();
    window.location.href = '/login.html';
}

// ── Guard: protect dashboard pages ───────────────────
function requireAuth() {
    if (!Auth.isLoggedIn()) {
        if (!window.location.pathname.includes('login')) {
            window.location.href = '/login.html';
        }
        return;
    }

    // Check token hasn't expired by verifying user object exists
    const user = Auth.getUser();
    if (!user || !user.role) {
        Auth.clear();
        window.location.href = '/login.html';
    }
}