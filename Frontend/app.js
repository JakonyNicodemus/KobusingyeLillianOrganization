// ============================================
// UGANDA EMPLOYEE MANAGEMENT SYSTEM - app.js
// FIXED - No Duplicates!
// ============================================

const API_URL = 'http://localhost:8080/api';

let employees = [];
let editingId = null;
let currentPage = 1;
const itemsPerPage = 10;

// ============================================
// INITIALIZATION
// ============================================

document.addEventListener('DOMContentLoaded', () => {
    console.log('🚀 App started! API URL:', API_URL);
    loadEmployees();
    
    const form = document.getElementById('employeeForm');
    if (form) {
        form.addEventListener('submit', saveEmployee);
    }
});

// ============================================
// NAVIGATION
// ============================================

function showPage(page) {
    document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
    
    const pageElement = document.getElementById(page + '-page');
    if (pageElement) pageElement.classList.add('active');
    
    document.querySelectorAll('.nav-links a').forEach(l => l.classList.remove('active'));
    const pageMap = { 'dashboard': 0, 'employees': 1, 'payroll': 2, 'nssf': 3, 'ura': 4 };
    const links = document.querySelectorAll('.nav-links a');
    if (pageMap[page] !== undefined && links[pageMap[page]]) {
        links[pageMap[page]].classList.add('active');
    }
    
    switch(page) {
        case 'dashboard': updateDashboard(); break;
        case 'employees': loadEmployees(); break;
        case 'payroll': loadPayroll(); break;
        case 'nssf': loadNSSF(); break;
        case 'ura': loadURA(); break;
    }
}

// ============================================
// LOAD EMPLOYEES - FIXED to remove duplicates
// ============================================

async function loadEmployees() {
    try {
        console.log('📡 Fetching employees...');
        const response = await fetch(`${API_URL}/employees`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const rawData = await response.json();
        console.log('📦 Raw data received:', rawData.length);
        
        // Filter out invalid entries
        let validEmployees = rawData.filter(emp => 
            emp && 
            emp.id !== null && 
            emp.id !== undefined &&
            emp.firstName && 
            emp.firstName.trim() !== '' &&
            emp.employeeId && 
            emp.employeeId.trim() !== ''
        );
        
        // Remove duplicates by ID
        const seen = new Set();
        employees = validEmployees.filter(emp => {
            if (seen.has(emp.id)) return false;
            seen.add(emp.id);
            return true;
        });
        
        console.log('✅ Valid unique employees:', employees.length);
        renderEmployees(employees);
        updateDashboard();
        
        const countEl = document.getElementById('employeeCount');
        if (countEl) countEl.textContent = employees.length;
        
    } catch (error) {
        console.error('❌ Error:', error);
        showToast('❌ Cannot connect to backend! Make sure server is running on port 8080', 'error');
        
        const tbody = document.getElementById('employeeTableBody');
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" style="text-align:center; padding:40px;">
                        <i class="fas fa-exclamation-circle" style="font-size:32px; color:#ef4444; display:block; margin-bottom:10px;"></i>
                        <strong style="color:#ef4444;">Cannot connect to backend server!</strong><br>
                        <span style="font-size:14px; color:#64748b;">
                            Make sure the backend is running on <strong>http://localhost:8080</strong><br>
                            Run: <code style="background:#f1f5f9; padding:2px 8px; border-radius:4px;">java com.ems.Main</code> in your terminal
                        </span>
                    </td>
                </tr>
            `;
        }
    }
}

// ============================================
// RENDER EMPLOYEES - FIXED for duplicates
// ============================================

function renderEmployees(data) {
    const tbody = document.getElementById('employeeTableBody');
    if (!tbody) return;
    
    // Filter out invalid entries
    let validEmployees = data.filter(emp => 
        emp && 
        emp.id !== null && 
        emp.id !== undefined &&
        emp.firstName && 
        emp.firstName.trim() !== '' &&
        emp.employeeId && 
        emp.employeeId.trim() !== ''
    );
    
    // Remove duplicates by ID
    const seen = new Set();
    const uniqueEmployees = validEmployees.filter(emp => {
        if (seen.has(emp.id)) return false;
        seen.add(emp.id);
        return true;
    });
    
    if (uniqueEmployees.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" style="text-align:center; padding:40px; color:var(--text-secondary);">
                    <i class="fas fa-users" style="font-size:24px; display:block; margin-bottom:10px; color:#94a3b8;"></i>
                    No employees found. Click "Add Employee" to get started.
                </td>
            </tr>
        `;
        return;
    }
    
    const start = (currentPage - 1) * itemsPerPage;
    const end = start + itemsPerPage;
    const pageData = uniqueEmployees.slice(start, end);
    
    tbody.innerHTML = pageData.map(emp => `
        <tr>
            <td><strong>${emp.employeeId || 'N/A'}</strong></td>
            <td>${emp.firstName || ''} ${emp.lastName || ''}</td>
            <td>${emp.email || ''}</td>
            <td>${emp.department || ''}</td>
            <td>${emp.position || ''}</td>
            <td>
                <span class="status-badge status-${(emp.status || 'ACTIVE').toLowerCase()}">
                    ${emp.status || 'ACTIVE'}
                </span>
            </td>
            <td>
                <button class="btn-edit" onclick="editEmployee(${emp.id})">
                    <i class="fas fa-edit"></i>
                </button>
                <button class="btn-danger" onclick="deleteEmployee(${emp.id})">
                    <i class="fas fa-trash"></i>
                </button>
            </td>
        </tr>
    `).join('');
    
    const recordCount = document.getElementById('recordCount');
    if (recordCount) {
        recordCount.textContent = `Showing ${pageData.length} of ${uniqueEmployees.length} employees`;
    }
}

// ============================================
// SEARCH & FILTER
// ============================================

function searchEmployees() {
    const term = document.getElementById('searchInput')?.value?.toLowerCase() || '';
    const dept = document.getElementById('departmentFilter')?.value || '';
    
    // Get unique employees
    const seen = new Set();
    const validEmployees = employees.filter(emp => {
        if (seen.has(emp.id)) return false;
        seen.add(emp.id);
        return true;
    });
    
    let filtered = validEmployees.filter(emp => {
        const match = (emp.firstName || '').toLowerCase().includes(term) ||
                     (emp.lastName || '').toLowerCase().includes(term) ||
                     (emp.email || '').toLowerCase().includes(term) ||
                     (emp.employeeId || '').toLowerCase().includes(term);
        const deptMatch = dept ? emp.department === dept : true;
        return match && deptMatch;
    });
    
    renderEmployees(filtered);
}

function resetFilters() {
    const searchInput = document.getElementById('searchInput');
    const deptFilter = document.getElementById('departmentFilter');
    if (searchInput) searchInput.value = '';
    if (deptFilter) deptFilter.value = '';
    renderEmployees(employees);
}

// ============================================
// DASHBOARD
// ============================================

function updateDashboard() {
    // Get unique employees
    const seen = new Set();
    const validEmployees = employees.filter(emp => {
        if (seen.has(emp.id)) return false;
        seen.add(emp.id);
        return emp && emp.id !== null && emp.firstName && emp.firstName.trim() !== '';
    });
    
    const total = validEmployees.length;
    const active = validEmployees.filter(e => e.status === 'ACTIVE').length;
    const depts = new Set(validEmployees.map(e => e.department));
    
    const totalEl = document.getElementById('totalEmployees');
    const activeEl = document.getElementById('activeEmployees');
    const deptEl = document.getElementById('totalDepartments');
    const payrollEl = document.getElementById('monthlyPayroll');
    
    if (totalEl) totalEl.textContent = total;
    if (activeEl) activeEl.textContent = active;
    if (deptEl) deptEl.textContent = depts.size;
    
    let totalPayroll = 0;
    validEmployees.forEach(emp => {
        if (emp.salary) totalPayroll += emp.salary;
    });
    if (payrollEl) payrollEl.textContent = `UGX ${formatCurrency(totalPayroll)}`;
}

function formatCurrency(amount) {
    return amount.toLocaleString('en-US', {
        minimumFractionDigits: 0,
        maximumFractionDigits: 0
    });
}

// ============================================
// SAVE EMPLOYEE - FIXED validation
// ============================================

async function saveEmployee(event) {
    event.preventDefault();
    
    // Get form values
    const firstName = document.getElementById('firstName').value.trim();
    const lastName = document.getElementById('lastName').value.trim();
    const email = document.getElementById('email').value.trim();
    const employeeId = document.getElementById('employeeId').value.trim();
    const department = document.getElementById('department').value;
    const position = document.getElementById('position').value.trim();
    const hireDate = document.getElementById('hireDate').value;
    const salary = parseFloat(document.getElementById('salary').value) || 0;
    
    // VALIDATION
    if (!firstName) {
        showToast('❌ First name is required!', 'error');
        return;
    }
    if (!lastName) {
        showToast('❌ Last name is required!', 'error');
        return;
    }
    if (!email) {
        showToast('❌ Email is required!', 'error');
        return;
    }
    if (!employeeId) {
        showToast('❌ Employee ID is required!', 'error');
        return;
    }
    if (!department) {
        showToast('❌ Department is required!', 'error');
        return;
    }
    if (!position) {
        showToast('❌ Position is required!', 'error');
        return;
    }
    if (!hireDate) {
        showToast('❌ Hire date is required!', 'error');
        return;
    }
    if (salary <= 0) {
        showToast('❌ Salary must be greater than 0!', 'error');
        return;
    }
    
    const employee = {
        firstName: firstName,
        lastName: lastName,
        email: email,
        employeeId: employeeId,
        department: department,
        position: position,
        hireDate: hireDate,
        salary: salary,
        phoneNumber: document.getElementById('phoneNumber').value.trim() || '',
        status: document.getElementById('status').value || 'ACTIVE',
        nssfNumber: document.getElementById('nssfNumber').value.trim() || '',
        tinNumber: document.getElementById('tinNumber').value.trim() || '',
        address: document.getElementById('address').value.trim() || ''
    };
    
    const editId = document.getElementById('editId').value;
    
    try {
        let response;
        let url = `${API_URL}/employees`;
        let method = 'POST';
        
        if (editId) {
            url = `${API_URL}/employees/${editId}`;
            method = 'PUT';
        }
        
        response = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(employee)
        });
        
        if (response.ok) {
            const message = editId ? '✅ Employee updated successfully!' : '✅ Employee added successfully!';
            showToast(message, 'success');
            resetForm();
            loadEmployees();
            showPage('employees');
        } else {
            const error = await response.text();
            showToast('❌ Error: ' + error, 'error');
        }
    } catch (error) {
        console.error('Error saving employee:', error);
        showToast('❌ Error saving employee. Make sure the backend is running.', 'error');
    }
}

// ============================================
// EDIT EMPLOYEE
// ============================================

async function editEmployee(id) {
    try {
        const response = await fetch(`${API_URL}/employees/${id}`);
        if (!response.ok) throw new Error('Employee not found');
        const emp = await response.json();
        
        document.getElementById('editId').value = emp.id;
        document.getElementById('firstName').value = emp.firstName || '';
        document.getElementById('lastName').value = emp.lastName || '';
        document.getElementById('email').value = emp.email || '';
        document.getElementById('employeeId').value = emp.employeeId || '';
        document.getElementById('department').value = emp.department || '';
        document.getElementById('position').value = emp.position || '';
        document.getElementById('hireDate').value = emp.hireDate || '';
        document.getElementById('salary').value = emp.salary || '';
        document.getElementById('phoneNumber').value = emp.phoneNumber || '';
        document.getElementById('status').value = emp.status || 'ACTIVE';
        document.getElementById('nssfNumber').value = emp.nssfNumber || '';
        document.getElementById('tinNumber').value = emp.tinNumber || '';
        document.getElementById('address').value = emp.address || '';
        
        document.getElementById('submitBtn').innerHTML = '<i class="fas fa-save"></i> Update Employee';
        document.getElementById('employeeId').disabled = true;
        
        showPage('add-employee');
    } catch (error) {
        console.error('Error loading employee:', error);
        showToast('❌ Error loading employee', 'error');
    }
}

// ============================================
// DELETE EMPLOYEE
// ============================================

async function deleteEmployee(id) {
    if (!confirm('Are you sure you want to delete this employee?')) return;
    
    try {
        const response = await fetch(`${API_URL}/employees/${id}`, { 
            method: 'DELETE' 
        });
        
        if (response.ok) {
            showToast('✅ Employee deleted successfully', 'success');
            loadEmployees();
        } else {
            showToast('❌ Error deleting employee', 'error');
        }
    } catch (error) {
        console.error('Error deleting employee:', error);
        showToast('❌ Error deleting employee', 'error');
    }
}

// ============================================
// RESET FORM
// ============================================

function resetForm() {
    document.getElementById('employeeForm').reset();
    document.getElementById('editId').value = '';
    document.getElementById('submitBtn').innerHTML = '<i class="fas fa-save"></i> Add Employee';
    document.getElementById('employeeId').disabled = false;
    document.getElementById('status').value = 'ACTIVE';
}

// ============================================
// PAYROLL
// ============================================

async function loadPayroll() {
    try {
        const response = await fetch(`${API_URL}/payroll`);
        if (!response.ok) throw new Error('Failed to load payroll');
        const data = await response.json();
        
        const totalPayroll = document.getElementById('totalPayroll');
        const totalNSSF = document.getElementById('totalNSSF');
        const totalPAYE = document.getElementById('totalPAYE');
        const tbody = document.getElementById('payrollTableBody');
        
        if (totalPayroll) totalPayroll.textContent = `UGX ${formatCurrency(data.summary?.totalPayroll || 0)}`;
        if (totalNSSF) totalNSSF.textContent = `UGX ${formatCurrency(data.summary?.totalNSSF || 0)}`;
        if (totalPAYE) totalPAYE.textContent = `UGX ${formatCurrency(data.summary?.totalPAYE || 0)}`;
        
        if (tbody) {
            if (!data.details || data.details.length === 0) {
                tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding:30px; color:var(--text-secondary);">No payroll data available.</td></tr>`;
            } else {
                tbody.innerHTML = data.details.map(d => `
                    <tr>
                        <td>${d.employee || 'N/A'}</td>
                        <td>UGX ${formatCurrency(d.grossSalary || 0)}</td>
                        <td>UGX ${formatCurrency(d.nssfEmployee || 0)}</td>
                        <td>UGX ${formatCurrency(d.paye || 0)}</td>
                        <td><strong>UGX ${formatCurrency(d.netPay || 0)}</strong></td>
                    </tr>
                `).join('');
            }
        }
    } catch (error) {
        console.error('Error loading payroll:', error);
        showToast('❌ Error loading payroll', 'error');
    }
}

// ============================================
// NSSF
// ============================================

async function loadNSSF() {
    try {
        const response = await fetch(`${API_URL}/nssf`);
        if (!response.ok) throw new Error('Failed to load NSSF data');
        const data = await response.json();
        
        const activeEl = document.getElementById('nssfActive');
        const employeeEl = document.getElementById('nssfEmployee');
        const employerEl = document.getElementById('nssfEmployer');
        const tbody = document.getElementById('nssfTableBody');
        
        if (activeEl) activeEl.textContent = data.summary?.activeContributors || 0;
        if (employeeEl) employeeEl.textContent = `UGX ${formatCurrency(data.summary?.totalEmployeeContrib || 0)}`;
        if (employerEl) employerEl.textContent = `UGX ${formatCurrency(data.summary?.totalEmployerContrib || 0)}`;
        
        if (tbody) {
            if (!data.details || data.details.length === 0) {
                tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding:30px; color:var(--text-secondary);">No NSSF data available.</td></tr>`;
            } else {
                tbody.innerHTML = data.details.map(d => `
                    <tr>
                        <td>${d.employee || 'N/A'}</td>
                        <td>${d.nssfNumber || 'N/A'}</td>
                        <td>UGX ${formatCurrency(d.employeeContrib || 0)}</td>
                        <td>UGX ${formatCurrency(d.employerContrib || 0)}</td>
                        <td><strong>UGX ${formatCurrency(d.totalContrib || 0)}</strong></td>
                    </tr>
                `).join('');
            }
        }
    } catch (error) {
        console.error('Error loading NSSF:', error);
        showToast('❌ Error loading NSSF data', 'error');
    }
}

// ============================================
// URA
// ============================================

async function loadURA() {
    try {
        const response = await fetch(`${API_URL}/ura`);
        if (!response.ok) throw new Error('Failed to load URA data');
        const data = await response.json();
        
        const totalEl = document.getElementById('uraTotal');
        const payeEl = document.getElementById('uraPAYE');
        const tbody = document.getElementById('uraTableBody');
        
        if (totalEl) totalEl.textContent = (data.details || []).length;
        if (payeEl) payeEl.textContent = `UGX ${formatCurrency(data.totalPAYE || 0)}`;
        
        if (tbody) {
            if (!data.details || data.details.length === 0) {
                tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding:30px; color:var(--text-secondary);">No URA data available.</td></tr>`;
            } else {
                tbody.innerHTML = data.details.map(d => `
                    <tr>
                        <td>${d.employee || 'N/A'}</td>
                        <td>${d.tinNumber || 'N/A'}</td>
                        <td>UGX ${formatCurrency(d.grossSalary || 0)}</td>
                        <td>UGX ${formatCurrency(d.taxableIncome || 0)}</td>
                        <td><strong>UGX ${formatCurrency(d.paye || 0)}</strong></td>
                    </tr>
                `).join('');
            }
        }
    } catch (error) {
        console.error('Error loading URA:', error);
        showToast('❌ Error loading URA data', 'error');
    }
}

// ============================================
// UTILITY FUNCTIONS
// ============================================

function generatePayroll() {
    showToast('⏳ Generating payroll...', 'info');
    loadPayroll();
}

function toggleTheme() {
    const html = document.documentElement;
    const currentTheme = html.getAttribute('data-theme');
    const newTheme = currentTheme === 'light' ? 'dark' : 'light';
    html.setAttribute('data-theme', newTheme);
    
    const icon = document.getElementById('themeIcon');
    if (icon) {
        icon.className = newTheme === 'dark' ? 'fas fa-sun' : 'fas fa-moon';
    }
}

function showToast(message, type = 'info') {
    const existing = document.querySelector('.toast');
    if (existing) existing.remove();
    
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;
    document.body.appendChild(toast);
    
    setTimeout(() => toast.remove(), 5000);
}

function globalSearch(value) {
    if (value.length > 2) {
        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.value = value;
            searchEmployees();
        }
    }
}

function toggleSidebar() {
    const sidebar = document.getElementById('sidebar');
    if (sidebar) {
        sidebar.classList.toggle('active');
    }
}

// Keyboard shortcut for search
document.addEventListener('keydown', function(e) {
    if (e.ctrlKey && e.key === 'k') {
        e.preventDefault();
        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.focus();
        }
    }
});