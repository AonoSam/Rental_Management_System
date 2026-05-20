/**
 * notifications.js
 * Adds a notification bell to the topbar and manages the notification panel.
 * Include after api.js and auth.js on every authenticated page.
 */

(function () {

    // ── Inject styles ───────────────────────────────────────
    const style = document.createElement('style');
    style.textContent = `
    /* Bell button */
    .notif-bell {
      position: relative;
      background: none;
      border: 1.5px solid var(--border);
      border-radius: 10px;
      width: 38px;
      height: 38px;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      font-size: 18px;
      flex-shrink: 0;
      transition: border-color .15s;
    }
    .notif-bell:hover { border-color: var(--primary); }

    .notif-badge {
      position: absolute;
      top: -6px;
      right: -6px;
      background: var(--error);
      color: #fff;
      font-size: 10px;
      font-weight: 700;
      border-radius: 20px;
      min-width: 18px;
      height: 18px;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 0 4px;
      border: 2px solid var(--surface);
    }

    /* Panel overlay */
    .notif-overlay {
      position: fixed;
      inset: 0;
      z-index: 490;
      display: none;
    }
    .notif-overlay.open { display: block; }

    /* Panel */
    .notif-panel {
      position: fixed;
      top: 60px;
      right: 16px;
      width: 360px;
      max-width: calc(100vw - 32px);
      max-height: 520px;
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 16px;
      box-shadow: 0 8px 32px rgba(0,0,0,.14);
      z-index: 491;
      display: none;
      flex-direction: column;
      overflow: hidden;
    }
    .notif-panel.open { display: flex; }

    /* Panel header */
    .notif-panel-header {
      padding: 16px 18px;
      border-bottom: 1px solid var(--border);
      display: flex;
      align-items: center;
      justify-content: space-between;
      flex-shrink: 0;
    }
    .notif-panel-header h3 {
      font-size: 15px;
      font-weight: 600;
      margin: 0;
    }
    .notif-mark-all {
      background: none;
      border: none;
      color: var(--primary);
      font-size: 12px;
      font-weight: 500;
      cursor: pointer;
      padding: 0;
    }
    .notif-mark-all:hover { text-decoration: underline; }

    /* Notification list */
    .notif-list {
      overflow-y: auto;
      flex: 1;
    }

    .notif-item {
      display: flex;
      gap: 12px;
      padding: 14px 18px;
      border-bottom: 1px solid var(--border);
      cursor: pointer;
      transition: background .1s;
      align-items: flex-start;
    }
    .notif-item:last-child { border-bottom: none; }
    .notif-item:hover { background: var(--bg); }
    .notif-item.unread { background: #F0F7FF; }
    .notif-item.unread:hover { background: #E0EFFF; }

    .notif-icon {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 16px;
      flex-shrink: 0;
    }
    .notif-icon-payment   { background: #F0FDF4; }
    .notif-icon-failed    { background: #FEF2F2; }
    .notif-icon-reminder  { background: #FFFBEB; }
    .notif-icon-maintenance { background: #EFF6FF; }
    .notif-icon-welcome   { background: var(--primary-light); }
    .notif-icon-general   { background: var(--bg); }

    .notif-content { flex: 1; min-width: 0; }
    .notif-title {
      font-size: 13px;
      font-weight: 600;
      margin-bottom: 3px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .notif-msg {
      font-size: 12px;
      color: var(--text-2);
      line-height: 1.4;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
    .notif-time {
      font-size: 11px;
      color: var(--text-3);
      margin-top: 4px;
    }

    .notif-actions {
      display: flex;
      flex-direction: column;
      gap: 4px;
      flex-shrink: 0;
    }
    .notif-btn-read {
      background: none;
      border: none;
      font-size: 12px;
      color: var(--primary);
      cursor: pointer;
      padding: 2px 0;
      text-align: right;
    }
    .notif-btn-read:hover { text-decoration: underline; }
    .notif-btn-del {
      background: none;
      border: none;
      font-size: 14px;
      color: var(--text-3);
      cursor: pointer;
      padding: 2px 0;
    }
    .notif-btn-del:hover { color: var(--error); }

    /* Empty state */
    .notif-empty {
      text-align: center;
      padding: 40px 20px;
      color: var(--text-2);
    }
    .notif-empty .icon { font-size: 36px; margin-bottom: 8px; }
    .notif-empty p { font-size: 13px; }

    /* Panel footer */
    .notif-panel-footer {
      padding: 12px 18px;
      border-top: 1px solid var(--border);
      text-align: center;
      flex-shrink: 0;
    }
    .notif-panel-footer span {
      font-size: 12px;
      color: var(--text-2);
    }

    /* Unread dot */
    .unread-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: var(--primary);
      flex-shrink: 0;
      margin-top: 4px;
    }

    /* Toast popup */
    .notif-toast {
      position: fixed;
      bottom: 24px;
      right: 24px;
      background: var(--surface);
      border: 1px solid var(--border);
      border-left: 4px solid var(--primary);
      border-radius: 12px;
      padding: 14px 18px;
      max-width: 320px;
      box-shadow: 0 4px 20px rgba(0,0,0,.12);
      z-index: 600;
      transform: translateY(80px);
      opacity: 0;
      transition: all .3s ease;
      display: flex;
      gap: 12px;
      align-items: flex-start;
    }
    .notif-toast.show {
      transform: translateY(0);
      opacity: 1;
    }
    .notif-toast-icon { font-size: 20px; flex-shrink: 0; }
    .notif-toast-body { flex: 1; }
    .notif-toast-title { font-size: 13px; font-weight: 600; margin-bottom: 3px; }
    .notif-toast-msg   { font-size: 12px; color: var(--text-2); line-height: 1.4; }
    .notif-toast-close {
      background: none; border: none; cursor: pointer;
      color: var(--text-3); font-size: 16px; flex-shrink: 0;
    }
  `;
    document.head.appendChild(style);

    // ── Type config ─────────────────────────────────────────
    const typeConfig = {
        PAYMENT_RECEIVED: { icon: '✅', cls: 'notif-icon-payment' },
        PAYMENT_FAILED:   { icon: '❌', cls: 'notif-icon-failed' },
        RENT_REMINDER:    { icon: '⚠️', cls: 'notif-icon-reminder' },
        MAINTENANCE_UPDATE:{ icon:'🔧', cls: 'notif-icon-maintenance' },
        ACCOUNT_CREATED:  { icon: '🎉', cls: 'notif-icon-welcome' },
        GENERAL_NOTICE:   { icon: '📢', cls: 'notif-icon-general' },
    };

    // ── State ───────────────────────────────────────────────
    let notifications = [];
    let panelOpen     = false;
    let pollTimer     = null;
    let lastCount     = 0;

    // ── Init ────────────────────────────────────────────────
    function init() {
        if (!Auth.isLoggedIn()) return;

        // Build bell button
        const bell = document.createElement('button');
        bell.className  = 'notif-bell';
        bell.innerHTML  = '🔔<span class="notif-badge" id="notifBadge" style="display:none">0</span>';
        bell.title      = 'Notifications';
        bell.onclick    = togglePanel;

        // Insert bell into topbar before user-pill
        const topbar = document.querySelector('.topbar');
        if (topbar) {
            const pill = topbar.querySelector('.user-pill');
            if (pill) topbar.insertBefore(bell, pill);
            else topbar.appendChild(bell);
        }

        // Build overlay
        const overlay = document.createElement('div');
        overlay.className = 'notif-overlay';
        overlay.onclick   = closePanel;
        document.body.appendChild(overlay);

        // Build panel
        const panel = document.createElement('div');
        panel.className = 'notif-panel';
        panel.id        = 'notifPanel';
        panel.innerHTML = `
      <div class="notif-panel-header">
        <h3>🔔 Notifications</h3>
        <button class="notif-mark-all" onclick="window._notifMarkAll()">
          Mark all as read
        </button>
      </div>
      <div class="notif-list" id="notifList">
        <div class="notif-empty">
          <div class="icon">🔔</div>
          <p>Loading...</p>
        </div>
      </div>
      <div class="notif-panel-footer">
        <span id="notifFooterText">—</span>
      </div>
    `;
        document.body.appendChild(panel);

        // Build toast container
        const toast = document.createElement('div');
        toast.className = 'notif-toast';
        toast.id        = 'notifToast';
        toast.innerHTML = `
      <div class="notif-toast-icon" id="toastIcon">🔔</div>
      <div class="notif-toast-body">
        <div class="notif-toast-title" id="toastTitle"></div>
        <div class="notif-toast-msg"   id="toastMsg"></div>
      </div>
      <button class="notif-toast-close" onclick="window._notifHideToast()">×</button>
    `;
        document.body.appendChild(toast);

        // Expose globals
        window._notifMarkAll   = markAll;
        window._notifHideToast = hideToast;

        // Load and start polling
        loadNotifications();
        pollTimer = setInterval(checkNew, 15000); // check every 15s
    }

    // ── Load notifications ───────────────────────────────────
    async function loadNotifications() {
        try {
            notifications = await NotificationAPI.list();
            renderPanel();
            updateBadge();
        } catch (err) { /* silent */ }
    }

    // ── Check for new (polling) ──────────────────────────────
    async function checkNew() {
        try {
            const data  = await NotificationAPI.unreadCount();
            const count = data.count || 0;

            if (count > lastCount && lastCount >= 0) {
                // New notification arrived — reload and show toast
                await loadNotifications();
                const newest = notifications.find(n => !n.read);
                if (newest) showToast(newest);
            } else {
                updateBadge();
            }
            lastCount = count;
        } catch (err) { /* silent */ }
    }

    // ── Render panel list ────────────────────────────────────
    function renderPanel() {
        const list = document.getElementById('notifList');
        if (!list) return;

        const unreadCount = notifications.filter(n => !n.read).length;
        const footer      = document.getElementById('notifFooterText');
        if (footer) footer.textContent =
            `${notifications.length} total · ${unreadCount} unread`;

        if (notifications.length === 0) {
            list.innerHTML = `
        <div class="notif-empty">
          <div class="icon">🔔</div>
          <p>No notifications yet</p>
        </div>`;
            return;
        }

        list.innerHTML = notifications.map(n => {
            const cfg  = typeConfig[n.type] || typeConfig.GENERAL_NOTICE;
            const time = formatTime(n.createdAt);
            return `
        <div class="notif-item ${n.read ? '' : 'unread'}" data-id="${n.id}">
          <div class="notif-icon ${cfg.cls}">${cfg.icon}</div>
          <div class="notif-content">
            <div class="notif-title">${n.title}</div>
            <div class="notif-msg">${n.message}</div>
            <div class="notif-time">${time}</div>
          </div>
          <div class="notif-actions">
            ${!n.read
                ? `<button class="notif-btn-read"
                   onclick="event.stopPropagation();window._notifMarkOne(${n.id})">
                   ✓ Read
                 </button>`
                : ''}
            <button class="notif-btn-del"
              onclick="event.stopPropagation();window._notifDelete(${n.id})">
              🗑
            </button>
          </div>
          ${!n.read ? '<div class="unread-dot"></div>' : ''}
        </div>`;
        }).join('');

        // Expose per-item actions
        window._notifMarkOne = markOne;
        window._notifDelete  = deleteOne;
    }

    // ── Update badge ─────────────────────────────────────────
    function updateBadge() {
        const badge = document.getElementById('notifBadge');
        if (!badge) return;
        const count = notifications.filter(n => !n.read).length;
        if (count > 0) {
            badge.textContent     = count > 99 ? '99+' : count;
            badge.style.display   = 'flex';
        } else {
            badge.style.display   = 'none';
        }
    }

    // ── Toggle panel ─────────────────────────────────────────
    function togglePanel() {
        panelOpen ? closePanel() : openPanel();
    }

    function openPanel() {
        document.getElementById('notifPanel')?.classList.add('open');
        document.querySelector('.notif-overlay')?.classList.add('open');
        panelOpen = true;
        loadNotifications(); // refresh when opened
    }

    function closePanel() {
        document.getElementById('notifPanel')?.classList.remove('open');
        document.querySelector('.notif-overlay')?.classList.remove('open');
        panelOpen = false;
    }

    // ── Mark one as read ─────────────────────────────────────
    async function markOne(id) {
        try {
            await NotificationAPI.markRead(id);
            const n = notifications.find(x => x.id === id);
            if (n) n.read = true;
            renderPanel();
            updateBadge();
        } catch (err) { /* silent */ }
    }

    // ── Mark all as read ─────────────────────────────────────
    async function markAll() {
        try {
            await NotificationAPI.markAllRead();
            notifications.forEach(n => n.read = true);
            renderPanel();
            updateBadge();
        } catch (err) { /* silent */ }
    }

    // ── Delete one ───────────────────────────────────────────
    async function deleteOne(id) {
        try {
            await NotificationAPI.delete(id);
            notifications = notifications.filter(n => n.id !== id);
            renderPanel();
            updateBadge();
        } catch (err) { /* silent */ }
    }

    // ── Toast popup ──────────────────────────────────────────
    function showToast(notification) {
        const toast = document.getElementById('notifToast');
        if (!toast) return;

        const cfg = typeConfig[notification.type] || typeConfig.GENERAL_NOTICE;
        document.getElementById('toastIcon').textContent  = cfg.icon;
        document.getElementById('toastTitle').textContent = notification.title;
        document.getElementById('toastMsg').textContent   = notification.message;

        toast.classList.add('show');

        // Auto-hide after 5 seconds
        setTimeout(hideToast, 5000);
    }

    function hideToast() {
        document.getElementById('notifToast')?.classList.remove('show');
    }

    // ── Format time ──────────────────────────────────────────
    function formatTime(dt) {
        if (!dt) return '';
        const now  = new Date();
        const date = new Date(dt);
        const diff = Math.floor((now - date) / 1000);

        if (diff < 60)   return 'just now';
        if (diff < 3600) return Math.floor(diff / 60) + 'm ago';
        if (diff < 86400)return Math.floor(diff / 3600) + 'h ago';
        return date.toLocaleDateString('en-KE', {
            day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit'
        });
    }

    // ── Start when DOM ready ─────────────────────────────────
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();