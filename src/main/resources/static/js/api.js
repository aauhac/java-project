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
