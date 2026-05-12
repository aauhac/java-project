(function () {
    const navItems = [
        {key: 'home', label: '메인', href: '/'},
        {key: 'dashboard', label: '대시보드', href: '/dashboard/index.html'},
        {key: 'chart', label: '차트', href: '/chart/index.html'},
        {key: 'evaluation', label: '평가', href: '/evaluation/index.html'},
        {key: 'opportunity', label: '기회/위험', href: '/opportunity/index.html'},
        {key: 'sector', label: '섹터', href: '/sector/index.html'},
        {key: 'api-test', label: 'API 테스트', href: '/api-test.html'}
    ];

    function escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }

    function mountNavigation(activeKey) {
        const host = document.getElementById('app-nav');
        if (!host) {
            return;
        }

        host.className = 'app-nav';
        host.innerHTML = `
            <div class="app-nav__inner">
                <a class="app-brand" href="/"><span>Trade</span>Agent</a>
                <nav class="app-nav__links">
                    ${navItems.map(item => `
                        <a class="app-nav__link ${item.key === activeKey ? 'is-active' : ''}" href="${item.href}">
                            ${item.label}
                        </a>
                    `).join('')}
                </nav>
            </div>
        `;
    }

    function alert(message, tone = 'info') {
        return `<div class="alert alert-${tone}">${escapeHtml(message)}</div>`;
    }

    function empty(message) {
        return `<div class="alert alert-info">${escapeHtml(message)}</div>`;
    }

    function statusPill(status) {
        const resolved = String(status || 'NEUTRAL').toUpperCase();
        const tone = resolved === 'STRONG' ? 'pill-strong' : resolved === 'WEAK' ? 'pill-weak' : 'pill-neutral';
        return `<span class="pill ${tone}">${escapeHtml(resolved)}</span>`;
    }

    window.Layout = {
        mountNavigation,
        escapeHtml,
        alert,
        empty,
        statusPill
    };
})();
