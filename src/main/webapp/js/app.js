/**
 * ContractIQ Main Application Logic
 * Pair programmed for high responsiveness, glassmorphism UX, and real backend connectivity.
 */

document.addEventListener('DOMContentLoaded', () => {
  // Navigation State
  let currentView = 'dashboard';
  let contractsData = [];
  let obligationsData = [];

  // DOM Elements
  const navItems = document.querySelectorAll('.nav-item');
  const views = document.querySelectorAll('.view-container');
  const pageTitleElem = document.getElementById('current-page-title');
  const healthBadge = document.getElementById('system-health-badge');
  const themeToggleBtn = document.getElementById('theme-toggle-btn');
  const uploadModal = document.getElementById('upload-modal');
  const openUploadModalBtns = document.querySelectorAll('.open-upload-modal-btn');
  const closeUploadModalBtn = document.getElementById('close-upload-modal-btn');
  const fileDropzone = document.getElementById('file-dropzone');
  const fileInput = document.getElementById('contract-file-input');
  const uploadProgress = document.getElementById('upload-progress');
  const progressStepText = document.getElementById('progress-step-text');
  const contractDetailModal = document.getElementById('contract-detail-modal');
  const closeContractModalBtn = document.getElementById('close-contract-modal-btn');
  const editObligationModal = document.getElementById('edit-obligation-modal');
  const closeEditModalBtn = document.getElementById('close-edit-modal-btn');

  // Initialize
  initTheme();
  checkSystemHealth();
  handleRouting();

  // Navigation Click Handlers
  navItems.forEach(item => {
    item.addEventListener('click', (e) => {
      e.preventDefault();
      const targetView = item.getAttribute('data-view');
      navigateTo(targetView);
    });
  });

  window.addEventListener('hashchange', handleRouting);

  function handleRouting() {
    const hash = window.location.hash.replace('#', '') || 'dashboard';
    navigateTo(hash);
  }

  function navigateTo(viewName) {
    if (!document.getElementById(`view-${viewName}`)) {
      viewName = 'dashboard';
    }
    currentView = viewName;
    window.location.hash = viewName;

    navItems.forEach(item => {
      if (item.getAttribute('data-view') === viewName) {
        item.classList.add('active');
      } else {
        item.classList.remove('active');
      }
    });

    views.forEach(v => {
      if (v.id === `view-${viewName}`) {
        v.style.display = 'block';
      } else {
        v.style.display = 'none';
      }
    });

    // Update Header Title
    const titleMap = {
      dashboard: 'Executive Dashboard',
      contracts: 'Contracts Repository',
      obligations: 'Obligation Human Review & Management',
      renewals: 'Renewal & Expiration Tracker',
      activity: 'Audit & Activity Log',
      settings: 'System Diagnostics & Configuration'
    };
    pageTitleElem.textContent = titleMap[viewName] || 'ContractIQ';

    // Load View Data
    if (viewName === 'dashboard') loadDashboard();
    else if (viewName === 'contracts') loadContracts();
    else if (viewName === 'obligations') loadObligations();
    else if (viewName === 'renewals') loadRenewals();
    else if (viewName === 'activity') loadActivity();
    else if (viewName === 'settings') loadSettings();
  }

  // Toast Notification System
  function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    const icon = type === 'success' ? 'check-circle' : type === 'error' ? 'alert-triangle' : 'info';
    toast.innerHTML = `<i data-lucide="${icon}"></i> <span>${escapeHtml(message)}</span>`;
    container.appendChild(toast);
    lucide.createIcons();

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateX(100%)';
      toast.style.transition = 'all 0.3s ease';
      setTimeout(() => toast.remove(), 300);
    }, 4500);
  }

  // Theme Management
  function initTheme() {
    const savedTheme = localStorage.getItem('contractiq_theme') || 'dark';
    document.documentElement.setAttribute('data-theme', savedTheme);
    updateThemeIcon(savedTheme);

    if (themeToggleBtn) {
      themeToggleBtn.addEventListener('click', () => {
        const current = document.documentElement.getAttribute('data-theme') || 'dark';
        const next = current === 'dark' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-theme', next);
        localStorage.setItem('contractiq_theme', next);
        updateThemeIcon(next);
        showToast(`Switched to ${next} theme`, 'info');
      });
    }
  }

  function updateThemeIcon(theme) {
    if (!themeToggleBtn) return;
    themeToggleBtn.innerHTML = theme === 'dark' ? '<i data-lucide="sun"></i>' : '<i data-lucide="moon"></i>';
    lucide.createIcons();
  }

  // System Health
  async function checkSystemHealth() {
    try {
      const health = await API.getHealth();
      if (healthBadge) {
        const isOffline = health.components.llmEngine.includes('OFFLINE');
        healthBadge.className = `status-badge ${isOffline ? 'offline-mode' : ''}`;
        healthBadge.innerHTML = `
          <span class="status-indicator"></span>
          <span>${isOffline ? 'Heuristic Mode' : health.components.llmEngine}</span>
        `;
      }
    } catch (e) {
      if (healthBadge) {
        healthBadge.className = 'status-badge offline-mode';
        healthBadge.innerHTML = `<span class="status-indicator" style="background:#f43f5e"></span> Backend Error`;
      }
    }
  }

  // 1. DASHBOARD CONTROLLER
  async function loadDashboard() {
    try {
      const stats = await API.getDashboardStats();

      // Animate Stats
      animateCounter('stat-total-contracts', stats.totalContracts);
      animateCounter('stat-pending-reviews', stats.pendingReviewContracts);
      animateCounter('stat-approved-obligations', stats.approvedObligations);
      animateCounter('stat-upcoming-renewals', stats.upcomingRenewalsCount);

      // Render Charts
      AppCharts.renderCategoryChart(
        document.getElementById('chart-categories'),
        stats.obligationsByCategory
      );
      AppCharts.renderContractTypeChart(
        document.getElementById('chart-contract-types'),
        stats.contractsByType
      );

      // Render Upcoming Renewals Timeline
      const renewalsList = document.getElementById('dashboard-renewals-list');
      if (renewalsList) {
        if (!stats.upcomingRenewals || stats.upcomingRenewals.length === 0) {
          renewalsList.innerHTML = `<div class="empty-state p-4 text-center text-muted">No contracts currently expiring soon.</div>`;
        } else {
          renewalsList.innerHTML = stats.upcomingRenewals.map(c => {
            const daysLeft = calculateDaysRemaining(c.expirationDate);
            const urgencyClass = daysLeft <= 30 ? 'badge-rejected' : daysLeft <= 60 ? 'badge-pending' : 'badge-approved';
            return `
              <div class="renewal-item d-flex align-items-center justify-content-between p-3 mb-2 rounded" style="background:rgba(255,255,255,0.02); border:1px solid var(--glass-border);">
                <div>
                  <div class="fw-bold">${escapeHtml(c.contractTitle)}</div>
                  <small class="text-muted">${escapeHtml(c.partySecond || 'Counterparty')} &bull; ${escapeHtml(c.contractType)}</small>
                </div>
                <div class="text-end">
                  <span class="badge ${urgencyClass}">${daysLeft} Days Left</span>
                  <div class="small text-muted mt-1">Exp: ${c.expirationDate || 'N/A'}</div>
                </div>
              </div>
            `;
          }).join('');
        }
      }

      // Render Recent Activity Feed
      const activityFeed = document.getElementById('dashboard-activity-feed');
      if (activityFeed) {
        if (!stats.recentActivity || stats.recentActivity.length === 0) {
          activityFeed.innerHTML = `<div class="empty-state p-4 text-center text-muted">No activity records logged yet.</div>`;
        } else {
          activityFeed.innerHTML = stats.recentActivity.slice(0, 5).map(a => `
            <div class="activity-item d-flex gap-3 align-items-start p-2 mb-2">
              <div class="activity-badge p-2 rounded-circle" style="background:rgba(56,189,248,0.1); color:var(--accent-cyan);">
                <i data-lucide="activity" style="width:16px;height:16px;"></i>
              </div>
              <div class="flex-1">
                <div class="small fw-semibold">${escapeHtml(a.description)}</div>
                <small class="text-muted">${formatTimestamp(a.createdAt)} &bull; ${escapeHtml(a.actor)}</small>
              </div>
            </div>
          `).join('');
          lucide.createIcons();
        }
      }

    } catch (err) {
      showToast('Failed to load dashboard data: ' + err.message, 'error');
    }
  }

  // 2. CONTRACTS CONTROLLER
  async function loadContracts() {
    const tableBody = document.getElementById('contracts-table-body');
    const filterStatus = document.getElementById('contracts-status-filter')?.value || '';
    const search = document.getElementById('contracts-search-input')?.value || '';

    if (!tableBody) return;
    tableBody.innerHTML = `<tr><td colspan="7" class="text-center p-4"><i data-lucide="loader" class="spin"></i> Loading contracts...</td></tr>`;
    lucide.createIcons();

    try {
      contractsData = await API.getContracts(filterStatus, search);

      if (contractsData.length === 0) {
        tableBody.innerHTML = `
          <tr>
            <td colspan="7" class="text-center p-5 text-muted">
              <i data-lucide="file-x" style="width:40px;height:40px;margin-bottom:8px;opacity:0.5;"></i>
              <div>No contracts match your criteria. Upload a contract to get started.</div>
            </td>
          </tr>
        `;
        lucide.createIcons();
        return;
      }

      tableBody.innerHTML = contractsData.map(c => {
        const reviewBadgeClass = getReviewStatusBadgeClass(c.reviewStatus);
        const confidencePct = Math.round((c.confidenceScore || 0) * 100);
        return `
          <tr>
            <td>
              <div class="fw-bold">${escapeHtml(c.contractTitle)}</div>
              <small class="text-muted">${escapeHtml(c.fileName)}</small>
            </td>
            <td>
              <div>${escapeHtml(c.partyFirst || 'N/A')}</div>
              <small class="text-muted">vs ${escapeHtml(c.partySecond || 'N/A')}</small>
            </td>
            <td><span class="badge badge-category">${escapeHtml(c.contractType || 'Agreement')}</span></td>
            <td>${c.expirationDate || '<span class="text-muted">Unspecified</span>'}</td>
            <td><span class="badge ${reviewBadgeClass}">${formatStatus(c.reviewStatus)}</span></td>
            <td><span class="badge badge-confidence">${confidencePct}%</span></td>
            <td>
              <div class="d-flex gap-2">
                <button class="btn btn-sm btn-secondary view-contract-btn" data-id="${c.id}" title="View Details">
                  <i data-lucide="eye"></i>
                </button>
                <button class="btn btn-sm btn-primary review-contract-btn" data-id="${c.id}" title="Review Obligations">
                  <i data-lucide="check-square"></i> Review
                </button>
                <button class="btn btn-sm btn-danger delete-contract-btn" data-id="${c.id}" title="Delete Contract">
                  <i data-lucide="trash-2"></i>
                </button>
              </div>
            </td>
          </tr>
        `;
      }).join('');
      lucide.createIcons();

      // Bind Action Buttons
      document.querySelectorAll('.view-contract-btn').forEach(b => {
        b.addEventListener('click', () => openContractDetailModal(b.getAttribute('data-id')));
      });
      document.querySelectorAll('.review-contract-btn').forEach(b => {
        b.addEventListener('click', () => {
          const cId = b.getAttribute('data-id');
          navigateTo('obligations');
          // Filter to this contract in obligations
          setTimeout(() => filterObligationsByContract(cId), 200);
        });
      });
      document.querySelectorAll('.delete-contract-btn').forEach(b => {
        b.addEventListener('click', () => confirmDeleteContract(b.getAttribute('data-id')));
      });

    } catch (err) {
      showToast('Failed to load contracts: ' + err.message, 'error');
    }
  }

  // Filter & Search Event Listeners
  document.getElementById('contracts-status-filter')?.addEventListener('change', loadContracts);
  document.getElementById('contracts-search-input')?.addEventListener('input', debounce(loadContracts, 300));

  // 3. OBLIGATIONS REVIEW CONTROLLER (CORE VERTICAL SLICE)
  async function loadObligations(contractFilterId = null) {
    const container = document.getElementById('obligations-list-container');
    const filterStatus = document.getElementById('obligations-status-filter')?.value || '';
    const filterCategory = document.getElementById('obligations-category-filter')?.value || '';
    const search = document.getElementById('obligations-search-input')?.value || '';

    if (!container) return;
    container.innerHTML = `<div class="p-5 text-center text-muted"><i data-lucide="loader" class="spin"></i> Loading obligations...</div>`;
    lucide.createIcons();

    try {
      if (contractFilterId) {
        obligationsData = await API.getContractObligations(contractFilterId);
      } else {
        obligationsData = await API.getObligations(filterStatus, filterCategory, search);
      }

      if (obligationsData.length === 0) {
        container.innerHTML = `
          <div class="glass-panel text-center p-5 text-muted">
            <i data-lucide="shield-check" style="width:48px;height:48px;margin-bottom:12px;opacity:0.4;"></i>
            <h4>No Obligations Found</h4>
            <p>All extracted obligations for this filter have been reviewed or none exist.</p>
          </div>
        `;
        lucide.createIcons();
        return;
      }

      container.innerHTML = obligationsData.map(o => {
        const isApproved = o.approvalStatus === 'APPROVED';
        const isRejected = o.approvalStatus === 'REJECTED';
        const isPending = o.approvalStatus === 'PENDING';
        const statusClass = isApproved ? 'approved' : isRejected ? 'rejected' : 'pending';
        const badgeClass = isApproved ? 'badge-approved' : isRejected ? 'badge-rejected' : 'badge-pending';
        const confidencePct = Math.round((o.confidence || 0.85) * 100);

        return `
          <div class="review-card ${statusClass}" id="obligation-card-${o.id}">
            <div class="d-flex justify-content-between align-items-start mb-2">
              <div>
                <h4 class="mb-1">${escapeHtml(o.obligationTitle)}</h4>
                <div class="d-flex gap-2 align-items-center flex-wrap">
                  <span class="badge ${badgeClass}">${o.approvalStatus}</span>
                  <span class="badge badge-category">${escapeHtml(o.category)}</span>
                  <span class="badge badge-confidence">${confidencePct}% AI Confidence</span>
                  <small class="text-muted"><i data-lucide="file-text" style="width:14px;height:14px;"></i> Page ${o.pageNumber || 1}</small>
                </div>
              </div>
              <div class="d-flex gap-2">
                <button class="btn btn-sm btn-secondary edit-obligation-btn" data-id="${o.id}">
                  <i data-lucide="edit-3"></i> Edit
                </button>
                ${isPending ? `
                  <button class="btn btn-sm btn-success approve-obligation-btn" data-id="${o.id}">
                    <i data-lucide="check"></i> Approve
                  </button>
                ` : ''}
                ${isPending ? `
                  <button class="btn btn-sm btn-danger reject-obligation-btn" data-id="${o.id}">
                    <i data-lucide="x"></i> Reject
                  </button>
                ` : ''}
              </div>
            </div>

            <p class="mb-2" style="color:var(--text-primary); font-size:0.95rem;">
              ${escapeHtml(o.description)}
            </p>

            <div class="d-flex gap-4 text-muted small mb-2 flex-wrap">
              <div><strong>Responsible:</strong> ${escapeHtml(o.responsibleParty || 'Unspecified')}</div>
              <div><strong>Deadline:</strong> ${o.deadlineDate || 'None / Continuous'}</div>
              <div><strong>Frequency:</strong> ${escapeHtml(o.frequency || 'ONE_TIME')}</div>
            </div>

            ${o.sourceExcerpt ? `
              <div class="review-source-quote">
                <strong>Source Contract Excerpt:</strong> "${escapeHtml(o.sourceExcerpt)}"
              </div>
            ` : ''}

            ${o.approvedBy ? `
              <div class="small mt-2 pt-2 border-top text-muted" style="border-color:var(--glass-border)!important;">
                <strong>Decision Record:</strong> ${o.approvalStatus} by ${escapeHtml(o.approvedBy)} 
                ${o.reviewedAt ? `on ${formatTimestamp(o.reviewedAt)}` : ''} 
                ${o.reviewComment ? `— <em>"${escapeHtml(o.reviewComment)}"</em>` : ''}
              </div>
            ` : ''}
          </div>
        `;
      }).join('');
      lucide.createIcons();

      // Bind Decision Buttons
      document.querySelectorAll('.approve-obligation-btn').forEach(btn => {
        btn.addEventListener('click', () => handleApproveObligation(btn.getAttribute('data-id')));
      });
      document.querySelectorAll('.reject-obligation-btn').forEach(btn => {
        btn.addEventListener('click', () => handleRejectObligation(btn.getAttribute('data-id')));
      });
      document.querySelectorAll('.edit-obligation-btn').forEach(btn => {
        btn.addEventListener('click', () => openEditObligationModal(btn.getAttribute('data-id')));
      });

    } catch (err) {
      showToast('Failed to load obligations: ' + err.message, 'error');
    }
  }

  // Filter bindings for obligations
  document.getElementById('obligations-status-filter')?.addEventListener('change', () => loadObligations());
  document.getElementById('obligations-category-filter')?.addEventListener('change', () => loadObligations());
  document.getElementById('obligations-search-input')?.addEventListener('input', debounce(() => loadObligations(), 300));

  function filterObligationsByContract(contractId) {
    loadObligations(contractId);
    showToast(`Showing obligations for Contract #${contractId}`, 'info');
  }

  // Handle Approve Decision
  async function handleApproveObligation(id) {
    const reviewerName = window.prompt('Enter your reviewer name for the audit record:');
    if (!reviewerName || !reviewerName.trim()) return;
    try {
      const updated = await API.approveObligation(id, reviewerName.trim(), 'Verified and confirmed compliant.');
      showToast(`Obligation #${id} APPROVED successfully.`, 'success');
      loadObligations();
      checkSystemHealth();
    } catch (err) {
      showToast('Error approving obligation: ' + err.message, 'error');
    }
  }

  // Handle Reject Decision
  async function handleRejectObligation(id) {
    const reviewerName = window.prompt('Enter your reviewer name for the audit record:');
    if (!reviewerName || !reviewerName.trim()) return;
    try {
      const updated = await API.rejectObligation(id, reviewerName.trim(), 'Marked as rejected / non-applicable.');
      showToast(`Obligation #${id} marked as REJECTED.`, 'info');
      loadObligations();
      checkSystemHealth();
    } catch (err) {
      showToast('Error rejecting obligation: ' + err.message, 'error');
    }
  }

  // Open Edit Obligation Modal
  function openEditObligationModal(id) {
    const ob = obligationsData.find(o => o.id == id);
    if (!ob || !editObligationModal) return;

    document.getElementById('edit-ob-id').value = ob.id;
    document.getElementById('edit-ob-title').value = ob.obligationTitle || '';
    document.getElementById('edit-ob-description').value = ob.description || '';
    document.getElementById('edit-ob-party').value = ob.responsibleParty || '';
    document.getElementById('edit-ob-deadline').value = ob.deadlineDate || '';
    document.getElementById('edit-ob-frequency').value = ob.frequency || 'ONE_TIME';
    document.getElementById('edit-ob-category').value = ob.category || 'OTHER';
    document.getElementById('edit-ob-comment').value = ob.reviewComment || '';

    editObligationModal.classList.add('active');
  }

  if (closeEditModalBtn) {
    closeEditModalBtn.addEventListener('click', () => editObligationModal.classList.remove('active'));
  }

  document.getElementById('save-obligation-btn')?.addEventListener('click', async () => {
    const id = document.getElementById('edit-ob-id').value;
    const updateData = {
      obligationTitle: document.getElementById('edit-ob-title').value,
      description: document.getElementById('edit-ob-description').value,
      responsibleParty: document.getElementById('edit-ob-party').value,
      deadlineDate: document.getElementById('edit-ob-deadline').value || null,
      frequency: document.getElementById('edit-ob-frequency').value,
      category: document.getElementById('edit-ob-category').value,
      reviewComment: document.getElementById('edit-ob-comment').value
    };

    try {
      await API.updateObligation(id, updateData);
      showToast('Obligation updated successfully.', 'success');
      editObligationModal.classList.remove('active');
      loadObligations();
    } catch (err) {
      showToast('Failed to update obligation: ' + err.message, 'error');
    }
  });

  // 4. RENEWALS TRACKER
  async function loadRenewals() {
    const container = document.getElementById('renewals-cards-container');
    if (!container) return;
    container.innerHTML = `<div class="p-4 text-center text-muted"><i data-lucide="loader" class="spin"></i> Loading renewal milestones...</div>`;
    lucide.createIcons();

    try {
      const renewals = await API.getRenewals(50);
      if (renewals.length === 0) {
        container.innerHTML = `
          <div class="glass-panel text-center p-5 text-muted">
            <i data-lucide="calendar" style="width:48px;height:48px;margin-bottom:12px;opacity:0.4;"></i>
            <h4>No Upcoming Renewals</h4>
            <p>All active contracts have expired or have no specified expiration dates.</p>
          </div>
        `;
        lucide.createIcons();
        return;
      }

      container.innerHTML = renewals.map(c => {
        const daysLeft = calculateDaysRemaining(c.expirationDate);
        const noticeDaysLeft = c.renewalNoticeDeadline ? calculateDaysRemaining(c.renewalNoticeDeadline) : null;
        const urgencyBorder = daysLeft <= 30 ? 'border-danger' : daysLeft <= 60 ? 'border-warning' : 'border-success';

        return `
          <div class="glass-panel mb-3" style="border-left: 5px solid ${daysLeft <= 30 ? 'var(--accent-rose)' : daysLeft <= 60 ? 'var(--accent-amber)' : 'var(--accent-emerald)'}">
            <div class="d-flex justify-content-between align-items-start mb-2">
              <div>
                <h4 class="mb-1">${escapeHtml(c.contractTitle)}</h4>
                <div class="small text-muted">${escapeHtml(c.partyFirst)} &bull; ${escapeHtml(c.partySecond)}</div>
              </div>
              <span class="badge ${daysLeft <= 30 ? 'badge-rejected' : 'badge-pending'}">${daysLeft} Days to Expiry</span>
            </div>

            <div class="row g-3 my-2 small">
              <div class="col-md-3"><strong>Expiration Date:</strong> ${c.expirationDate}</div>
              <div class="col-md-3"><strong>Auto-Renew:</strong> ${c.autoRenewal ? '<span class="text-success fw-bold">YES</span>' : 'NO'}</div>
              <div class="col-md-3"><strong>Notice Period:</strong> ${c.renewalNoticePeriodDays ? c.renewalNoticePeriodDays + ' Days' : 'Unspecified'}</div>
              <div class="col-md-3"><strong>Notice Deadline:</strong> ${c.renewalNoticeDeadline || 'N/A'}</div>
            </div>

            ${c.renewalTerms ? `
              <div class="small text-muted p-2 rounded mt-2" style="background:rgba(255,255,255,0.02); border:1px solid var(--glass-border);">
                <strong>Renewal Clause:</strong> ${escapeHtml(c.renewalTerms)}
              </div>
            ` : ''}
          </div>
        `;
      }).join('');
      lucide.createIcons();

    } catch (err) {
      showToast('Failed to load renewals: ' + err.message, 'error');
    }
  }

  // 5. ACTIVITY LOG CONTROLLER
  async function loadActivity() {
    const tableBody = document.getElementById('activity-table-body');
    if (!tableBody) return;
    tableBody.innerHTML = `<tr><td colspan="5" class="text-center p-4"><i data-lucide="loader" class="spin"></i> Loading audit history...</td></tr>`;
    lucide.createIcons();

    try {
      const logs = await API.getActivity(50);
      if (logs.length === 0) {
        tableBody.innerHTML = `<tr><td colspan="5" class="text-center p-4 text-muted">No audit activity logged.</td></tr>`;
        return;
      }

      tableBody.innerHTML = logs.map(l => `
        <tr>
          <td>${formatTimestamp(l.createdAt)}</td>
          <td><span class="badge badge-category">${escapeHtml(l.actionType)}</span></td>
          <td>${escapeHtml(l.description)}</td>
          <td>${escapeHtml(l.actor || 'System')}</td>
          <td class="text-muted small">${escapeHtml(l.ipAddress || '-')}</td>
        </tr>
      `).join('');
    } catch (err) {
      showToast('Failed to load activity log: ' + err.message, 'error');
    }
  }

  // 6. SETTINGS & DIAGNOSTICS
  async function loadSettings() {
    try {
      const health = await API.getHealth();
      document.getElementById('diag-app-version').textContent = health.version;
      document.getElementById('diag-java-version').textContent = health.javaVersion;
      document.getElementById('diag-db-status').textContent = health.components.database;
      document.getElementById('diag-llm-engine').textContent = health.components.llmEngine;
      document.getElementById('diag-storage-path').textContent = 'Server-managed';
      document.getElementById('diag-server-time').textContent = health.timestamp;
    } catch (err) {
      showToast('Diagnostics check failed: ' + err.message, 'error');
    }
  }

  // 7. UPLOAD AND EXTRACTION WORKFLOW
  openUploadModalBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      resetUploadModal();
      uploadModal.classList.add('active');
    });
  });

  if (closeUploadModalBtn) {
    closeUploadModalBtn.addEventListener('click', () => {
      uploadModal.classList.remove('active');
    });
  }

  if (fileDropzone && fileInput) {
    fileDropzone.addEventListener('click', () => fileInput.click());

    fileDropzone.addEventListener('dragover', (e) => {
      e.preventDefault();
      fileDropzone.classList.add('dragover');
    });

    fileDropzone.addEventListener('dragleave', () => {
      fileDropzone.classList.remove('dragover');
    });

    fileDropzone.addEventListener('drop', (e) => {
      e.preventDefault();
      fileDropzone.classList.remove('dragover');
      if (e.dataTransfer.files.length > 0) {
        handleFileUpload(e.dataTransfer.files[0]);
      }
    });

    fileInput.addEventListener('change', () => {
      if (fileInput.files.length > 0) {
        handleFileUpload(fileInput.files[0]);
      }
    });
  }

  function resetUploadModal() {
    if (uploadProgress) uploadProgress.style.display = 'none';
    if (fileDropzone) fileDropzone.style.display = 'block';
    if (fileInput) fileInput.value = '';
  }

  async function handleFileUpload(file) {
    if (!file.name.toLowerCase().endsWith('.pdf')) {
      showToast('Invalid file format. Please select a valid PDF contract.', 'error');
      return;
    }

    if (file.size > 15 * 1024 * 1024) {
      showToast('File size exceeds the 15MB limit.', 'error');
      return;
    }

    fileDropzone.style.display = 'none';
    uploadProgress.style.display = 'block';

    const steps = [
      'Uploading PDF document to server...',
      'Validating header checksum & magic bytes...',
      'Extracting full contract text via Apache PDFBox...',
      'AI analyzing contract & extracting obligations...',
      'Storing pending suggestions in MySQL database...'
    ];

    let stepIdx = 0;
    const interval = setInterval(() => {
      if (stepIdx < steps.length) {
        progressStepText.textContent = steps[stepIdx];
        stepIdx++;
      }
    }, 1200);

    try {
      const savedContract = await API.uploadContract(file);
      clearInterval(interval);
      progressStepText.textContent = 'Contract processing completed successfully!';
      showToast(`Contract "${file.name}" ingested. Reviewing suggestions...`, 'success');

      setTimeout(() => {
        uploadModal.classList.remove('active');
        navigateTo('obligations');
        setTimeout(() => filterObligationsByContract(savedContract.id), 250);
      }, 800);

    } catch (err) {
      clearInterval(interval);
      uploadProgress.style.display = 'none';
      fileDropzone.style.display = 'block';
      showToast('Upload or extraction failed: ' + err.message, 'error');
    }
  }

  // Contract Detail Modal
  async function openContractDetailModal(id) {
    if (!contractDetailModal) return;
    try {
      const c = await API.getContract(id);
      document.getElementById('modal-contract-title').textContent = c.contractTitle;
      document.getElementById('modal-contract-parties').textContent = `${c.partyFirst || 'N/A'} and ${c.partySecond || 'N/A'}`;
      document.getElementById('modal-contract-type').textContent = c.contractType;
      document.getElementById('modal-contract-effective').textContent = c.effectiveDate || 'Unspecified';
      document.getElementById('modal-contract-expiration').textContent = c.expirationDate || 'Unspecified';
      document.getElementById('modal-contract-renewal').textContent = c.autoRenewal ? `Yes (${c.renewalNoticePeriodDays || 0} days notice)` : 'No';
      document.getElementById('modal-contract-payment').textContent = c.paymentTerms || 'Standard Terms';
      document.getElementById('modal-contract-penalties').textContent = c.penaltiesClause || 'None specified';
      document.getElementById('modal-contract-termination').textContent = c.terminationConditions || 'Standard termination clauses';
      document.getElementById('modal-contract-excerpt').textContent = c.rawTextExcerpt || 'No raw text excerpt available.';

      contractDetailModal.classList.add('active');
    } catch (err) {
      showToast('Error opening contract details: ' + err.message, 'error');
    }
  }

  if (closeContractModalBtn) {
    closeContractModalBtn.addEventListener('click', () => contractDetailModal.classList.remove('active'));
  }

  async function confirmDeleteContract(id) {
    if (confirm(`Are you sure you want to delete Contract #${id}? This will remove all associated obligations.`)) {
      try {
        await API.deleteContract(id);
        showToast(`Contract #${id} deleted.`, 'info');
        loadContracts();
      } catch (err) {
        showToast('Error deleting contract: ' + err.message, 'error');
      }
    }
  }

  // Helper Utilities
  function animateCounter(elemId, target) {
    const el = document.getElementById(elemId);
    if (!el) return;
    const start = 0;
    const duration = 600;
    const startTime = performance.now();

    function update(currentTime) {
      const elapsed = currentTime - startTime;
      const progress = Math.min(elapsed / duration, 1);
      const val = Math.floor(progress * target);
      el.textContent = val.toLocaleString();
      if (progress < 1) {
        requestAnimationFrame(update);
      } else {
        el.textContent = target.toLocaleString();
      }
    }
    requestAnimationFrame(update);
  }

  function calculateDaysRemaining(dateStr) {
    if (!dateStr) return 0;
    const target = new Date(dateStr);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const diff = target.getTime() - today.getTime();
    return Math.ceil(diff / (1000 * 3600 * 24));
  }

  function getReviewStatusBadgeClass(status) {
    switch (status) {
      case 'APPROVED': return 'badge-approved';
      case 'REJECTED': return 'badge-rejected';
      case 'PARTIALLY_APPROVED': return 'badge-category';
      default: return 'badge-pending';
    }
  }

  function formatStatus(s) {
    if (!s) return 'Needs Review';
    return s.replace(/_/g, ' ');
  }

  function formatTimestamp(ts) {
    if (!ts) return '';
    try {
      const d = new Date(ts);
      return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch (e) {
      return ts;
    }
  }

  function escapeHtml(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  function debounce(func, wait) {
    let timeout;
    return function (...args) {
      clearTimeout(timeout);
      timeout = setTimeout(() => func.apply(this, args), wait);
    };
  }
});
