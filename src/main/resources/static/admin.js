// admin.js - Admin dashboard logic

let pendingPage = 0;
let allPage = 0;
let managerPage = 0;
const pageSize = 5;
let pendingTotalPages = 0;
let allTotalPages = 0;
let managerTotalPages = 0;

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    if (!checkAuth()) return;

    const user = getCurrentUser();
    if (user.role === 'EMPLOYEE') {
        alert('Access denied. This page is for admin/manager only.');
        logout();
        return;
    }

    updateNavbarUser();
    setupAdminEventListeners();

    // Show/hide features based on role
    if (user.role === 'ADMIN') {
        // Show manager approval features for admin only
        document.getElementById('managers-tab-btn').style.display = 'inline-block';
        document.getElementById('pending-managers-card').style.display = 'block';
    } else if (user.role === 'MANAGER') {
        // Hide manager approval features for managers
        document.getElementById('managers-tab-btn').style.display = 'none';
        document.getElementById('pending-managers-card').style.display = 'none';
    }

    // Add delay to ensure session is properly initialized
    setTimeout(() => {
        loadSummary();
        loadPendingLeaves();
        loadAdminHolidays();
        if (user.role === 'ADMIN') {
            loadPendingManagersCount();
        }
    }, 500);
});

/**
 * Setup event listeners
 */
function setupAdminEventListeners() {
    document.getElementById('holiday-form').addEventListener('submit', addHoliday);
    setMinDate('holiday-date');
}

/**
 * Admin tab switching
 */
function showAdminTab(tabName) {
    document.querySelectorAll('.tab-content').forEach(tab => {
        tab.classList.remove('active');
    });
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.remove('active');
    });

    document.getElementById(`${tabName}-tab`).classList.add('active');
    event.target.classList.add('active');

    if (tabName === 'pending') {
        pendingPage = 0;
        loadPendingLeaves();
    } else if (tabName === 'all') {
        allPage = 0;
        loadAllLeaves();
    } else if (tabName === 'managers') {
        managerPage = 0;
        loadManagerApprovals();
    } else if (tabName === 'holidays') {
        loadAdminHolidays();
    }
}

/**
 * Load summary statistics
 */
async function loadSummary() {
    try {
        // Load pending count
        const pendingResult = await apiCall(
            `${API_BASE}/leaves?page=0&size=1&status=PENDING`
        );
        if (pendingResult && pendingResult.success) {
            document.getElementById('pending-count').textContent = pendingResult.data.totalElements;
        }

        // Load approved count
        const approvedResult = await apiCall(
            `${API_BASE}/leaves?page=0&size=1&status=APPROVED`
        );
        if (approvedResult && approvedResult.success) {
            document.getElementById('approved-count').textContent = approvedResult.data.totalElements;
        }

        // Load rejected count
        const rejectedResult = await apiCall(
            `${API_BASE}/leaves?page=0&size=1&status=REJECTED`
        );
        if (rejectedResult && rejectedResult.success) {
            document.getElementById('rejected-count').textContent = rejectedResult.data.totalElements;
        }
    } catch (error) {
        console.error('Error loading summary:', error);
    }
}

/**
 * Load pending managers count (Admin only)
 */
async function loadPendingManagersCount() {
    try {
        const result = await apiCall(
            `${API_BASE}/manager-approvals/pending?page=0&size=1`
        );
        if (result && result.success) {
            document.getElementById('pending-managers-count').textContent = result.data.totalElements;
        }
    } catch (error) {
        console.error('Error loading pending managers count:', error);
    }
}

/**
 * Load manager approvals
 */
async function loadManagerApprovals() {
    const filter = document.getElementById('manager-filter')?.value || 'pending';

    const endpoint = filter === 'pending'
        ? `${API_BASE}/manager-approvals/pending?page=${managerPage}&size=${pageSize}`
        : `${API_BASE}/manager-approvals?page=${managerPage}&size=${pageSize}`;

    try {
        const result = await apiCall(endpoint);

        if (result && result.success) {
            displayManagerApprovals(result.data);
            loadPendingManagersCount(); // Refresh count
        }
    } catch (error) {
        document.getElementById('managers-container').innerHTML =
            '<p class="error">Error loading manager approvals</p>';
    }
}

/**
 * Display manager approvals
 */
function displayManagerApprovals(pageData) {
    const container = document.getElementById('managers-container');

    if (!pageData.content || pageData.content.length === 0) {
        container.innerHTML = '<p class="loading">No manager approval requests found</p>';
        return;
    }

    managerTotalPages = pageData.totalPages;
    document.getElementById('managers-page-info').textContent =
        `Page ${managerPage + 1} of ${managerTotalPages}`;

    document.getElementById('managers-prev').disabled = managerPage === 0;
    document.getElementById('managers-next').disabled = managerPage >= managerTotalPages - 1;

    container.innerHTML = pageData.content.map(manager => `
        <div class="leave-item">
            <div class="leave-header">
                <div class="leave-employee">
                    ${manager.employeeName || manager.username}
                    <br><small>${manager.employeeEmail || 'No email'}</small>
                </div>
                ${manager.isApproved
                    ? '<span class="status-badge status-approved">APPROVED</span>'
                    : '<span class="status-badge status-pending">PENDING APPROVAL</span>'}
            </div>

            <div class="leave-info">
                <div class="info-item">
                    <span class="info-label">Username</span>
                    <span class="info-value">${manager.username}</span>
                </div>
                <div class="info-item">
                    <span class="info-label">Department</span>
                    <span class="info-value">${manager.department || 'N/A'}</span>
                </div>
                <div class="info-item">
                    <span class="info-label">Registration Date</span>
                    <span class="info-value">${formatDateTime(manager.createdAt)}</span>
                </div>
                ${manager.approvedAt ? `
                    <div class="info-item">
                        <span class="info-label">Approved On</span>
                        <span class="info-value">${formatDateTime(manager.approvedAt)}</span>
                    </div>
                ` : ''}
                ${manager.approvedBy ? `
                    <div class="info-item">
                        <span class="info-label">Approved By</span>
                        <span class="info-value">${manager.approvedBy}</span>
                    </div>
                ` : ''}
            </div>

            ${!manager.isApproved ? `
                <div class="leave-actions">
                    <button class="btn btn-approve" onclick="approveManager(${manager.id})">
                        ✓ Approve Manager
                    </button>
                </div>
            ` : `
                <div class="leave-actions">
                    <button class="btn btn-reject" onclick="rejectManager(${manager.id})">
                        ✗ Revoke Approval
                    </button>
                </div>
            `}
        </div>
    `).join('');
}

/**
 * Approve manager
 */
async function approveManager(id) {
    if (!confirm('Are you sure you want to approve this manager? They will be able to approve/reject employee leave requests.')) return;

    try {
        const result = await apiCall(`${API_BASE}/manager-approvals/${id}/approve`, 'PUT');

        if (result && result.success) {
            alert(result.message);
            loadManagerApprovals();
            loadPendingManagersCount();
        } else {
            alert(result.message || 'Failed to approve manager');
        }
    } catch (error) {
        alert('Error approving manager');
    }
}

/**
 * Reject/Revoke manager approval
 */
async function rejectManager(id) {
    if (!confirm('Are you sure you want to revoke this manager\'s approval? They will NOT be able to approve/reject employee leave requests.')) return;

    try {
        const result = await apiCall(`${API_BASE}/manager-approvals/${id}/reject`, 'PUT');

        if (result && result.success) {
            alert(result.message);
            loadManagerApprovals();
            loadPendingManagersCount();
        } else {
            alert(result.message || 'Failed to reject manager');
        }
    } catch (error) {
        alert('Error rejecting manager');
    }
}

/**
 * Load pending leaves
 */
async function loadPendingLeaves() {
    const sortBy = document.getElementById('pending-sort')?.value || 'createdAt';

    const url = `${API_BASE}/leaves?page=${pendingPage}&size=${pageSize}&sortBy=${sortBy}&status=PENDING`;

    try {
        const result = await apiCall(url);

        if (result && result.success) {
            displayPendingLeaves(result.data);
            loadSummary(); // Refresh counts
        }
    } catch (error) {
        document.getElementById('pending-leaves-container').innerHTML =
            '<p class="error">Error loading pending leaves</p>';
    }
}

/**
 * Display pending leaves
 */
function displayPendingLeaves(pageData) {
    const container = document.getElementById('pending-leaves-container');

    if (!pageData.content || pageData.content.length === 0) {
        container.innerHTML = '<p class="loading">No pending leave requests</p>';
        return;
    }

    pendingTotalPages = pageData.totalPages;
    document.getElementById('pending-page-info').textContent =
        `Page ${pendingPage + 1} of ${pendingTotalPages}`;

    document.getElementById('pending-prev').disabled = pendingPage === 0;
    document.getElementById('pending-next').disabled = pendingPage >= pendingTotalPages - 1;

    container.innerHTML = pageData.content.map(leave => `
        <div class="leave-item">
            <div class="leave-header">
                <div class="leave-employee">
                    ${leave.employeeName}
                    <br><small>${leave.employeeEmail}</small>
                </div>
                ${getStatusBadge(leave.status)}
            </div>

            <div class="leave-info">
                <div class="info-item">
                    <span class="info-label">Period</span>
                    <span class="info-value">${formatDate(leave.startDate)} to ${formatDate(leave.endDate)}</span>
                </div>
                <div class="info-item">
                    <span class="info-label">Working Days</span>
                    <span class="info-value">${leave.workingDays} days</span>
                </div>
                <div class="info-item">
                    <span class="info-label">Duration</span>
                    <span class="info-value">${leave.duration}</span>
                </div>
                ${leave.duration === 'HALF_DAY' ? `
                    <div class="info-item">
                        <span class="info-label">Half Day Type</span>
                        <span class="info-value">${leave.halfDayType}</span>
                    </div>
                ` : ''}
                <div class="info-item">
                    <span class="info-label">Applied On</span>
                    <span class="info-value">${formatDateTime(leave.createdAt)}</span>
                </div>
            </div>

            <div class="info-item">
                <span class="info-label">Reason</span>
                <span class="info-value">${leave.reason}</span>
            </div>

            <div class="leave-actions">
                <button class="btn btn-approve" onclick="approveLeave(${leave.id})">
                    ✓ Approve
                </button>
                <button class="btn btn-reject" onclick="rejectLeave(${leave.id})">
                    ✗ Reject
                </button>
            </div>
        </div>
    `).join('');
}

/**
 * Load all leaves
 */
async function loadAllLeaves() {
    const sortBy = document.getElementById('all-sort')?.value || 'createdAt';
    const status = document.getElementById('all-status-filter')?.value || '';

    let url = `${API_BASE}/leaves?page=${allPage}&size=${pageSize}&sortBy=${sortBy}`;
    if (status) {
        url += `&status=${status}`;
    }

    try {
        const result = await apiCall(url);

        if (result && result.success) {
            displayAllLeaves(result.data);
        }
    } catch (error) {
        document.getElementById('all-leaves-container').innerHTML =
            '<p class="error">Error loading leaves</p>';
    }
}

/**
 * Display all leaves
 */
function displayAllLeaves(pageData) {
    const container = document.getElementById('all-leaves-container');

    if (!pageData.content || pageData.content.length === 0) {
        container.innerHTML = '<p class="loading">No leave requests found</p>';
        return;
    }

    allTotalPages = pageData.totalPages;
    document.getElementById('all-page-info').textContent =
        `Page ${allPage + 1} of ${allTotalPages}`;

    document.getElementById('all-prev').disabled = allPage === 0;
    document.getElementById('all-next').disabled = allPage >= allTotalPages - 1;

    container.innerHTML = pageData.content.map(leave => `
        <div class="leave-item">
            <div class="leave-header">
                <div class="leave-employee">
                    ${leave.employeeName}
                    <br><small>${leave.employeeEmail}</small>
                </div>
                ${getStatusBadge(leave.status)}
            </div>

            <div class="leave-info">
                <div class="info-item">
                    <span class="info-label">Period</span>
                    <span class="info-value">${formatDate(leave.startDate)} to ${formatDate(leave.endDate)}</span>
                </div>
                <div class="info-item">
                    <span class="info-label">Working Days</span>
                    <span class="info-value">${leave.workingDays} days</span>
                </div>
                <div class="info-item">
                    <span class="info-label">Duration</span>
                    <span class="info-value">${leave.duration} ${getDurationBadge(leave.duration, leave.halfDayType)}</span>
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
        </div>
    `).join('');
}

/**
 * Approve leave
 */
async function approveLeave(id) {
    if (!confirm('Are you sure you want to approve this leave?')) return;

    try {
        const result = await apiCall(`${API_BASE}/leaves/${id}/approve`, 'PUT');

        if (result && result.success) {
            alert(result.message);
            loadPendingLeaves();
            loadAllLeaves();
            loadSummary();
        } else {
            alert(result.message || 'Failed to approve leave');
        }
    } catch (error) {
        alert('Error approving leave');
    }
}

/**
 * Reject leave
 */
async function rejectLeave(id) {
    if (!confirm('Are you sure you want to reject this leave?')) return;

    try {
        const result = await apiCall(`${API_BASE}/leaves/${id}/reject`, 'PUT');

        if (result && result.success) {
            alert(result.message);
            loadPendingLeaves();
            loadAllLeaves();
            loadSummary();
        } else {
            alert(result.message || 'Failed to reject leave');
        }
    } catch (error) {
        alert('Error rejecting leave');
    }
}

/**
 * Add holiday
 */
async function addHoliday(e) {
    e.preventDefault();

    const data = {
        name: document.getElementById('holiday-name').value,
        date: document.getElementById('holiday-date').value,
        year: parseInt(document.getElementById('holiday-year').value)
    };

    try {
        const result = await apiCall(`${API_BASE}/holidays`, 'POST', data);

        if (result && result.success) {
            showMessage('holiday-message', result.message, 'success');
            document.getElementById('holiday-form').reset();
            loadAdminHolidays();
        } else {
            showMessage('holiday-message', result.message || 'Failed to add holiday', 'error');
        }
    } catch (error) {
        showMessage('holiday-message', 'Error adding holiday', 'error');
    }
}

/**
 * Load admin holidays
 */
async function loadAdminHolidays() {
    const year = document.getElementById('admin-holiday-year')?.value || new Date().getFullYear();

    try {
        const result = await apiCall(`${API_BASE}/holidays/year/${year}`);

        if (result && result.success) {
            displayAdminHolidays(result.data);
        }
    } catch (error) {
        console.error('Error loading holidays:', error);
    }
}

/**
 * Display admin holidays
 */
function displayAdminHolidays(holidays) {
    const container = document.getElementById('admin-holidays-container');

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
            <button class="btn btn-delete btn-sm" onclick="deleteHoliday(${holiday.id})">
                Delete
            </button>
        </div>
    `).join('');
}

/**
 * Delete holiday
 */
async function deleteHoliday(id) {
    if (!confirm('Are you sure you want to delete this holiday?')) return;

    try {
        const result = await apiCall(`${API_BASE}/holidays/${id}`, 'DELETE');

        if (result && result.success) {
            alert(result.message);
            loadAdminHolidays();
        } else {
            alert(result.message || 'Failed to delete holiday');
        }
    } catch (error) {
        alert('Error deleting holiday');
    }
}

/**
 * Pagination - Pending
 */
function nextPendingPage() {
    if (pendingPage < pendingTotalPages - 1) {
        pendingPage++;
        loadPendingLeaves();
    }
}

function previousPendingPage() {
    if (pendingPage > 0) {
        pendingPage--;
        loadPendingLeaves();
    }
}

/**
 * Pagination - All
 */
function nextAllPage() {
    if (allPage < allTotalPages - 1) {
        allPage++;
        loadAllLeaves();
    }
}

function previousAllPage() {
    if (allPage > 0) {
        allPage--;
        loadAllLeaves();
    }
}

/**
 * Pagination - Managers
 */
function nextManagerPage() {
    if (managerPage < managerTotalPages - 1) {
        managerPage++;
        loadManagerApprovals();
    }
}

function previousManagerPage() {
    if (managerPage > 0) {
        managerPage--;
        loadManagerApprovals();
    }
}