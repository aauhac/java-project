(function () {
    async function request(url, options = {}) {
        const response = await fetch(url, {
            headers: {
                Accept: 'application/json',
                ...(options.headers || {})
            },
            ...options
        });

        const payload = await response.json().catch(() => null);
        if (!response.ok) {
            throw new Error(payload?.error?.message || `Request failed with status ${response.status}.`);
        }
        if (payload && payload.success === false) {
            throw new Error(payload.error?.message || 'API response indicated failure.');
        }
        return payload;
    }

    function buildQuery(params) {
        const search = new URLSearchParams();
        Object.entries(params || {}).forEach(([key, value]) => {
            if (value !== undefined && value !== null && value !== '') {
                search.set(key, value);
            }
        });
        const query = search.toString();
        return query ? `?${query}` : '';
    }

    window.API = {
        request,
        getPortfolioSummary(userId = 1) {
            return request(`/api/portfolio/${userId}/summary`);
        },
        getPortfolioPositions(userId = 1) {
            return request(`/api/portfolio/${userId}/positions`);
        },
        getSectorAllocation(userId = 1) {
            return request(`/api/portfolio/${userId}/sector-allocation`);
        },
        getTradeHistory(userId = 1) {
            return request(`/api/portfolio/${userId}/trades`);
        },
        buyStock(payload) {
            return request('/api/portfolio/buy', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(payload)
            });
        },
        sellStock(payload) {
            return request('/api/portfolio/sell', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(payload)
            });
        },
        getTradeEvaluations(userId = 1) {
            return request(`/api/evaluations/user/${userId}`);
        },
        getEvaluationSummary(userId = 1) {
            return request(`/api/evaluations/user/${userId}/summary`);
        },
        getTradeFeedback(userId = 1) {
            return request(`/api/feedback/${userId}/trade`);
        },
        getMissedOpportunities(userId = 1) {
            return request(`/api/opportunities/${userId}/missed`);
        },
        getAvoidedLosses(userId = 1) {
            return request(`/api/opportunities/${userId}/avoided`);
        },
        getOpportunityPatterns(userId = 1) {
            return request(`/api/opportunities/${userId}/patterns`);
        },
        getOpportunitySummary(userId = 1) {
            return request(`/api/opportunities/${userId}/summary`);
        },
        getOpportunityFeedback(userId = 1) {
            return request(`/api/feedback/${userId}/opportunity`);
        },
        getSectorScores() {
            return request('/api/sectors/scores');
        },
        getSectorOptions() {
            return request('/api/sectors/masters');
        },
        analyzeSectorTrend(date) {
            return request(`/api/sectors/analyze-trend${buildQuery({date})}`, {
                method: 'POST'
            });
        },
        getSectorTrends(date) {
            return request(`/api/sectors/trends${buildQuery({date})}`);
        },
        getSectorTrendHistory(sectorCode, from, to) {
            return request(`/api/sectors/${encodeURIComponent(sectorCode)}/trends${buildQuery({from, to})}`);
        },
        getPortfolioTrendMatch(userId = 1, date) {
            return request(`/api/sectors/user/${userId}/trend-match${buildQuery({date})}`);
        },
        getSectorDiagnostic(userId = 1) {
            return request(`/api/sectors/user/${userId}/diagnostic`);
        },
        getSectorFeedback(userId = 1) {
            return request(`/api/feedback/${userId}/sector`);
        },
        getOverallFeedback(userId = 1) {
            return request(`/api/feedback/${userId}/overall`);
        },
        getChartBars(params = {}) {
            return request(`/chart/api/bars${buildQuery(params)}`);
        }
    };
})();
