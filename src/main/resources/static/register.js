// register.js - Registration logic

const API_BASE = '/api';

// Check if already logged in
if (localStorage.getItem('token')) {
    const role = localStorage.getItem('role');
    redirectToDashboard(role);
}

// Registration form handler
document.getElementById('register-form').addEventListener('submit', async (e) => {
    e.preventDefault();

    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    const name = document.getElementById('name').value;
    const email = document.getElementById('email').value;
    const department = document.getElementById('department').value;
    const role = document.getElementById('role').value;

    // Validate password match
    if (password !== confirmPassword) {
        showError('Passwords do not match');
        return;
    }

    await register({
        username,
        password,
        name,
        email,
        department,
        role
    });
});

/**
 * Register function
 */
async function register(data) {
    const registerBtn = document.querySelector('#register-form button[type="submit"]');
    const registerText = document.getElementById('register-text');
    const registerSpinner = document.getElementById('register-spinner');
    const errorEl = document.getElementById('register-error');
    const successEl = document.getElementById('register-success');

    // Show loading state
    registerBtn.disabled = true;
    registerText.style.display = 'none';
    registerSpinner.style.display = 'inline';
    errorEl.style.display = 'none';
    successEl.style.display = 'none';

    try {
        const response = await fetch(`${API_BASE}/auth/register`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });

        const result = await response.json();

        if (result.success) {
            showSuccess('Account created successfully! Redirecting to login...');
            document.getElementById('register-form').reset();

            // Redirect to login after 2 seconds
            setTimeout(() => {
                window.location.href = 'login.html';
            }, 2000);
        } else {
            showError(result.message || 'Registration failed');
        }
    } catch (error) {
        console.error('Registration error:', error);
        showError('Network error. Please check your connection and try again.');
    } finally {
        // Reset button state
        registerBtn.disabled = false;
        registerText.style.display = 'inline';
        registerSpinner.style.display = 'none';
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
    const errorEl = document.getElementById('register-error');
    const successEl = document.getElementById('register-success');

    errorEl.textContent = message;
    errorEl.style.display = 'block';
    successEl.style.display = 'none';
}

/**
 * Show success message
 */
function showSuccess(message) {
    const errorEl = document.getElementById('register-error');
    const successEl = document.getElementById('register-success');

    successEl.textContent = message;
    successEl.style.display = 'block';
    errorEl.style.display = 'none';

}
