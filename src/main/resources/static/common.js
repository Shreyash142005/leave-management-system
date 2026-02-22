// common.js - Shared utilities and functions

const API_BASE = 'http://localhost:8080/api';
const SESSION_TIMEOUT = 30 * 60 * 1000; // 30 minutes in milliseconds

/**
 * Check session validity
 */
function isSessionValid() {
    const loginTime = localStorage.getItem('loginTime');
    if (!loginTime) return false;

    const currentTime = new Date().getTime();
    const timeDiff = currentTime - parseInt(loginTime);

    return timeDiff < SESSION_TIMEOUT;
}

/**
 * Update session activity
 */
function updateSessionActivity() {
    if (localStorage.getItem('token')) {
        localStorage.setItem('loginTime', new Date().getTime().toString());
    }
}

/**
 * Initialize session (call this after login)
 */
function initializeSession() {
    localStorage.setItem('loginTime', new Date().getTime().toString());
}

/**
 * Make authenticated API call
 */
async function apiCall(url, method = 'GET', body = null) {
    const token = localStorage.getItem('token');

    if (!token) {
        console.error('No token found, redirecting to login');
        window.location.href = 'index.html';
        return null;
    }

    // Check session validity before making API call (skip for first 10 seconds after login)
    const loginTime = localStorage.getItem('loginTime');
    if (loginTime) {
        const timeSinceLogin = new Date().getTime() - parseInt(loginTime);

        // Only check session if more than 10 seconds have passed since login
        if (timeSinceLogin > 10000 && !isSessionValid()) {
            console.error('Session expired');
            alert('Your session has expired. Please login again.');
            logout();
            return null;
        }
    }

    // Update session activity on each API call
    updateSessionActivity();

    const options = {
        method,
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        }
    };

    if (body) {
        options.body = JSON.stringify(body);
    }

    try {
        const response = await fetch(url, options);

        // Handle unauthorized - but not during first 10 seconds
        if (response.status === 401 || response.status === 403) {
            const timeSinceLogin = loginTime ? new Date().getTime() - parseInt(loginTime) : 99999;

            if (timeSinceLogin > 10000) {
                console.error('Unauthorized response, clearing session');
                alert('Session expired or unauthorized. Please login again.');
                logout();
                return null;
            } else {
                console.warn('Got 401/403 but within grace period, continuing...');
            }
        }

        const result = await response.json();
        return result;
    } catch (error) {
        console.error('API call error:', error);
        return null;
    }
}

/**
 * Check authentication and session
 */
function checkAuth() {
    const token = localStorage.getItem('token');
    if (!token) {
        window.location.href = 'index.html';
        return false;
    }

    const loginTime = localStorage.getItem('loginTime');
    if (!loginTime) {
        // Initialize login time if not set (for backward compatibility)
        localStorage.setItem('loginTime', new Date().getTime().toString());
        return true;
    }

    const timeSinceLogin = new Date().getTime() - parseInt(loginTime);

    // Don't check session validity for first 10 seconds after login
    if (timeSinceLogin <= 10000) {
        return true;
    }

    // Only check session if more than 10 seconds have passed since login
    if (!isSessionValid()) {
        alert('Your session has expired. Please login again.');
        logout();
        return false;
    }

    return true;
}

/**
 * Logout user
 */
function logout() {
    localStorage.clear();
    window.location.href = 'index.html';
}

/**
 * Get current user info
 */
function getCurrentUser() {
    return {
        username: localStorage.getItem('username'),
        role: localStorage.getItem('role'),
        employeeId: localStorage.getItem('employeeId'),
        employeeName: localStorage.getItem('employeeName')
    };
}

/**
 * Show message
 */
function showMessage(elementId, message, type) {
    const el = document.getElementById(elementId);
    if (!el) return;

    el.textContent = message;
    el.className = `message ${type}`;
    el.style.display = 'block';

    setTimeout(() => {
        el.style.display = 'none';
    }, 5000);
}

/**
 * Format date
 */
function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
    });
}

/**
 * Format datetime
 */
function formatDateTime(dateTimeString) {
    if (!dateTimeString) return 'N/A';
    const date = new Date(dateTimeString);
    return date.toLocaleString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

/**
 * Get status badge HTML
 */
function getStatusBadge(status) {
    const statusClass = `status-${status.toLowerCase()}`;
    return `<span class="status-badge ${statusClass}">${status}</span>`;
}

/**
 * Get duration badge HTML
 */
function getDurationBadge(duration, halfDayType) {
    if (duration === 'HALF_DAY') {
        const type = halfDayType === 'FIRST_HALF' ? '🌅 First Half' : '🌆 Second Half';
        return `<span class="duration-badge">${type}</span>`;
    }
    return '';
}

/**
 * Set minimum date to today
 */
function setMinDate(elementId) {
    const today = new Date().toISOString().split('T')[0];
    const element = document.getElementById(elementId);
    if (element) {
        element.min = today;
    }
}

/**
 * Update user name in navbar
 */
function updateNavbarUser() {
    const user = getCurrentUser();
    const userNameEl = document.getElementById('user-name');
    if (userNameEl) {
        const displayName = user.employeeName || user.username;
        userNameEl.textContent = `Welcome, ${displayName}`;
    }
}

// Update session activity on user interactions
document.addEventListener('click', updateSessionActivity);
document.addEventListener('keypress', updateSessionActivity);

// Check session periodically (every 2 minutes)
setInterval(() => {
    const token = localStorage.getItem('token');
    const loginTime = localStorage.getItem('loginTime');

    if (token && loginTime) {
        const timeSinceLogin = new Date().getTime() - parseInt(loginTime);

        // Only check if more than 5 seconds since login
        if (timeSinceLogin > 5000 && !isSessionValid()) {
            alert('Your session has expired. Please login again.');
            logout();
        }
    }
}, 120000); // Check every 2 minutes