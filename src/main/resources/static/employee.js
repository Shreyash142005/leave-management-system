// employee.js - Employee dashboard logic

let currentPage = 0;
const pageSize = 5;
let totalPages = 0;

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    if (!checkAuth()) return;

    const user = getCurrentUser();
    if (user.role !== 'EMPLOYEE') {
        alert('Access denied. This page is for employees only.');
        logout();
        return;
    }

    updateNavbarUser();
    setupEventListeners();

    // Add small delay to ensure token is properly set
    setTimeout(() => {
        loadLeaveBalance();
        loadMyLeaves();
        loadHolidays();
    }, 100);
});

/**
 * Setup event listeners
 */
function setupEventListeners() {
    document.getElementById('leave-form').addEventListener('submit', applyLeave);
    setMinDate('startDate');
    setMinDate('endDate');
}

/**
 * Tab switching
 */
function showTab(tabName) {
    document.querySelectorAll('.tab-content').forEach(tab => {
        tab.classList.remove('active');
    });
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.remove('active');
    });

    document.getElementById(`${tabName}-tab`).classList.add('active');
    event.target.classList.add('active');

    if (tabName === 'my-leaves') {
        currentPage = 0;
        loadMyLeaves();
    } else if (tabName === 'holidays') {
        loadHolidays();
    }
}

/**
 * Toggle half-day options
 */
function toggleHalfDayOptions() {
    const duration = document.getElementById('duration').value;
    const halfDayGroup = document.getElementById('half-day-type-group');
    const halfDayType = document.getElementById('halfDayType');
    const startDate = document.getElementById('startDate');
    const endDate = document.getElementById('endDate');

    if (duration === 'HALF_DAY') {
        halfDayGroup.style.display = 'block';
        halfDayType.required = true;

        // Set end date same as start date for half-day
        if (startDate.value) {
            endDate.value = startDate.value;
            endDate.disabled = true;
        }
    } else {
        halfDayGroup.style.display = 'none';
        halfDayType.required = false;
        halfDayType.value = '';
        endDate.disabled = false;
    }
}

/**
 * Apply for leave
 */
async function applyLeave(e) {
    e.preventDefault();

    const duration = document.getElementById('duration').value;

    const data = {
        startDate: document.getElementById('startDate').value,
        endDate: document.getElementById('endDate').value,
        reason: document.getElementById('reason').value,
        duration: duration
    };

    if (duration === 'HALF_DAY') {
        data.halfDayType = document.getElementById('halfDayType').value;
    }

    try {
        const result = await apiCall(`${API_BASE}/leaves`, 'POST', data);

        if (result && result.success) {
            showMessage('apply-message', result.message, 'success');
            document.getElementById('leave-form').reset();
            document.getElementById('half-day-type-group').style.display = 'none';
            document.getElementById('endDate').disabled = false;

            // Show auto-approval message if applicable
            if (result.data.status === 'APPROVED') {
                showMessage('apply-message',
                    `${result.message} - Auto-approved! (${result.data.workingDays} working days)`,
                    'success');
            }

            loadLeaveBalance();
            loadMyLeaves();
        } else {
            showMessage('apply-message', result.message || 'Failed to apply leave', 'error');
        }
    } catch (error) {
        showMessage('apply-message', 'Error applying for leave', 'error');
    }
}

/**
 * Load leave balance
 */
async function loadLeaveBalance() {
    const user = getCurrentUser();
    const currentYear = new Date().getFullYear();

    try {
        const result = await apiCall(
            `${API_BASE}/leave-balance/employee/${user.employeeId}?year=${currentYear}`
        );

        if (result && result.success) {
            const balance = result.data;
            document.getElementById('total-entitlement').textContent = balance.totalEntitlement.toFixed(1);
            document.getElementById('used-leaves').textContent = balance.usedLeaves.toFixed(1);
            document.getElementById('remaining-leaves').textContent = balance.remainingLeaves.toFixed(1);
            document.getElementById('carried-forward').textContent = balance.carriedForward.toFixed(1);
        }
    } catch (error) {
        console.error('Error loading balance:', error);
    }
}

/**
 * Load my leaves
 */
async function loadMyLeaves() {
    const user = getCurrentUser();
    const sortBy = document.getElementById('sort-by').value;

    const url = `${API_BASE}/leaves/employee/${user.employeeId}?page=${currentPage}&size=${pageSize}&sortBy=${sortBy}`;

    try {
        const result = await apiCall(url);

        if (result && result.success) {
            displayMyLeaves(result.data);
        }
    } catch (error) {
        document.getElementById('leaves-container').innerHTML =
            '<p class="error">Error loading leaves</p>';
    }
}

/**
 * Display my leaves
 */
function displayMyLeaves(pageData) {
    const container = document.getElementById('leaves-container');

    if (!pageData.content || pageData.content.length === 0) {
        container.innerHTML = '<p class="loading">No leave requests found</p>';
        return;
    }

    totalPages = pageData.totalPages;
    document.getElementById('page-info').textContent = `Page ${currentPage + 1} of ${totalPages}`;

    document.getElementById('prev-btn').disabled = currentPage === 0;
    document.getElementById('next-btn').disabled = currentPage >= totalPages - 1;

    container.innerHTML = pageData.content.map(leave => `
        <div class="leave-item">
            <div class="leave-header">
                <div>
                    <strong>${formatDate(leave.startDate)} to ${formatDate(leave.endDate)}</strong>
                    ${getDurationBadge(leave.duration, leave.halfDayType)}
                </div>
                ${getStatusBadge(leave.status)}
            </div>

            <div class="leave-info">
                <div class="info-item">
                    <span class="info-label">Working Days</span>
                    <span class="info-value">${leave.workingDays} days</span>
                </div>
                <div class="info-item">
                    <span class="info-label">Applied On</span>
                    <span class="info-value">${formatDateTime(leave.createdAt)}</span>
                </div>
                ${leave.processedAt ? `
                    <div class="info-item">
                        <span class="info-label">Processed On</span>
                        <span class="info-value">${formatDateTime(leave.processedAt)}</span>
                    </div>
                ` : ''}
                ${leave.processedBy ? `
                    <div class="info-item">
                        <span class="info-label">Processed By</span>
                        <span class="info-value">${leave.processedBy}</span>
                    </div>
                ` : ''}
            </div>

            <div class="info-item">
                <span class="info-label">Reason</span>
                <span class="info-value">${leave.reason}</span>
            </div>

            ${leave.canCancel ? `
                <div class="leave-actions">
                    <button class="btn btn-cancel" onclick="cancelLeave(${leave.id})">
                        Cancel Leave
                    </button>
                </div>
            ` : ''}
        </div>
    `).join('');
}

/**
 * Cancel leave
 */
async function cancelLeave(id) {
    if (!confirm('Are you sure you want to cancel this leave?')) return;

    try {
        const result = await apiCall(`${API_BASE}/leaves/${id}/cancel`, 'PUT');

        if (result && result.success) {
            alert(result.message);
            loadMyLeaves();
            loadLeaveBalance();
        } else {
            alert(result.message || 'Failed to cancel leave');
        }
    } catch (error) {
        alert('Error cancelling leave');
    }
}

/**
 * Load holidays
 */
async function loadHolidays() {
    const year = document.getElementById('holiday-year')?.value || new Date().getFullYear();

    try {
        const result = await apiCall(`${API_BASE}/holidays/year/${year}`);

        if (result && result.success) {
            displayHolidays(result.data);
        }
    } catch (error) {
        console.error('Error loading holidays:', error);
    }
}

/**
 * Display holidays
 */
function displayHolidays(holidays) {
    const container = document.getElementById('holidays-container');

    if (!holidays || holidays.length === 0) {
        container.innerHTML = '<p class="loading">No holidays found for this year</p>';
        return;
    }

    container.innerHTML = holidays.map(holiday => `
        <div class="holiday-item">
            <div class="holiday-info">
                <h4>${holiday.name}</h4>
                <p class="holiday-date">${formatDate(holiday.date)}</p>
            </div>
        </div>
    `).join('');
}

/**
 * Process year-end action
 */
async function processYearEnd(action) {
    const currentYear = new Date().getFullYear();
    const user = getCurrentUser();

    const confirmMsg = action === 'CARRY_FORWARD'
        ? 'Carry forward unused leaves to next year (max 12 leaves)?'
        : 'Encash unused leaves (max 10 leaves)?';

    if (!confirm(confirmMsg)) return;

    const data = {
        year: currentYear,
        action: action
    };

    try {
        const result = await apiCall(
            `${API_BASE}/leave-balance/employee/${user.employeeId}/year-end`,
            'POST',
            data
        );

        if (result && result.success) {
            showMessage('year-end-message', result.message, 'success');
            loadLeaveBalance();
        } else {
            showMessage('year-end-message', result.message || 'Failed to process year-end action', 'error');
        }
    } catch (error) {
        showMessage('year-end-message', 'Error processing year-end action', 'error');
    }
}

/**
 * Pagination
 */
function nextPage() {
    if (currentPage < totalPages - 1) {
        currentPage++;
        loadMyLeaves();
    }
}

function previousPage() {
    if (currentPage > 0) {
        currentPage--;
        loadMyLeaves();
    }
}