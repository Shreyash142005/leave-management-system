// auth.js - Authentication logic

const API_BASE = 'http://localhost:8080/api';

// Check if already logged in
if (localStorage.getItem('token')) {
    const role = localStorage.getItem('role');
    redirectToDashboard(role);
}

// Login form handler
document.getElementById('login-form').addEventListener('submit', async (e) => {
    e.preventDefault();

    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;

    await login(username, password);
});

/**
 * Login function
 */
async function login(username, password) {
    const loginBtn = document.querySelector('#login-form button[type="submit"]');
    const loginText = document.getElementById('login-text');
    const loginSpinner = document.getElementById('login-spinner');
    const errorEl = document.getElementById('login-error');

    // Show loading state
    loginBtn.disabled = true;
    loginText.style.display = 'none';
    loginSpinner.style.display = 'inline';
    errorEl.style.display = 'none';

    try {
        const response = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password })
        });

        const result = await response.json();

        if (result.success) {
            // Store authentication data
            localStorage.setItem('token', result.data.token);
            localStorage.setItem('username', result.data.username);
            localStorage.setItem('role', result.data.role);

            if (result.data.employeeId) {
                localStorage.setItem('employeeId', result.data.employeeId);
                localStorage.setItem('employeeName', result.data.employeeName);
            }

            // Redirect based on role
            redirectToDashboard(result.data.role);
        } else {
            showError(result.message || 'Login failed');
        }
    } catch (error) {
        console.error('Login error:', error);
        showError('Network error. Please check your connection and try again.');
    } finally {
        // Reset button state
        loginBtn.disabled = false;
        loginText.style.display = 'inline';
        loginSpinner.style.display = 'none';
    }
}

/**
 * Redirect to appropriate dashboard
 */
function redirectToDashboard(role) {
    if (role === 'EMPLOYEE') {
        window.location.href = 'employee-dashboard.html';
    } else if (role === 'ADMIN' || role === 'MANAGER') {
        window.location.href = 'admin-dashboard.html';
    }
}

/**
 * Show error message
 */
function showError(message) {
    const errorEl = document.getElementById('login-error');
    errorEl.textContent = message;
    errorEl.style.display = 'block';
}