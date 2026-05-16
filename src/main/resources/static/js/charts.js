(function () {
    function toChartDate(value) {
        return String(value || '').slice(0, 10);
    }

    function hasLightweightCharts() {
        return typeof window.LightweightCharts !== 'undefined';
    }

    function createTradingViewChart(containerId, options = {}) {
        const host = document.getElementById(containerId);
        if (!host || !hasLightweightCharts()) {
            return {
                setBars() {
                },
                resize() {
                }
            };
        }

        const height = options.height || 420;
        const chart = LightweightCharts.createChart(host, {
            layout: {
                background: {color: '#1f2937'},
                textColor: '#e5e7eb'
            },
            grid: {
                vertLines: {color: 'rgba(203, 213, 225, 0.08)'},
                horzLines: {color: 'rgba(203, 213, 225, 0.08)'}
            },
            rightPriceScale: {
                borderColor: 'rgba(203, 213, 225, 0.2)'
            },
            timeScale: {
                borderColor: 'rgba(203, 213, 225, 0.2)',
                timeVisible: true
            },
            width: host.clientWidth,
            height
        });

        const candleSeries = chart.addCandlestickSeries({
            upColor: '#22c55e',
            downColor: '#ef4444',
            borderVisible: false,
            wickUpColor: '#22c55e',
            wickDownColor: '#ef4444'
        });

        const volumeSeries = chart.addHistogramSeries({
            priceFormat: {type: 'volume'},
            priceScaleId: 'volume',
            color: 'rgba(148, 163, 184, 0.35)'
        });

        chart.priceScale('volume').applyOptions({
            scaleMargins: {
                top: 0.75,
                bottom: 0
            }
        });

        chart.priceScale('right').applyOptions({
            scaleMargins: {
                top: 0.1,
                bottom: 0.3
            }
        });

        return {
            setBars(bars) {
                const safeBars = Array.isArray(bars) ? bars : [];
                const candles = safeBars.map((bar) => ({
                    time: toChartDate(bar.time),
                    open: Number(bar.open),
                    high: Number(bar.high),
                    low: Number(bar.low),
                    close: Number(bar.close)
                }));
                const volumes = safeBars.map((bar) => ({
                    time: toChartDate(bar.time),
                    value: Number(bar.volume || 0),
                    color: Number(bar.close) >= Number(bar.open)
                        ? 'rgba(34, 197, 94, 0.45)'
                        : 'rgba(239, 68, 68, 0.45)'
                }));

                candleSeries.setData(candles);
                volumeSeries.setData(volumes);
                chart.timeScale().fitContent();
            },
            resize() {
                chart.applyOptions({width: host.clientWidth});
            }
        };
    }

    function createDoughnutChart(canvasId) {
        const canvas = document.getElementById(canvasId);
        if (!canvas || typeof window.Chart === 'undefined') {
            return {
                setData() {
                }
            };
        }

        const chart = new Chart(canvas, {
            type: 'doughnut',
            data: {
                labels: [],
                datasets: [{
                    data: [],
                    borderWidth: 1,
                    borderColor: 'rgba(15, 23, 42, 0.9)',
                    backgroundColor: [
                        '#60a5fa',
                        '#22c55e',
                        '#f59e0b',
                        '#a78bfa',
                        '#f43f5e',
                        '#14b8a6',
                        '#f97316'
                    ]
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            color: '#cbd5e1'
                        }
                    }
                }
            }
        });

        return {
            setData(labels, values) {
                chart.data.labels = labels || [];
                chart.data.datasets[0].data = values || [];
                chart.update();
            }
        };
    }

    window.Charts = {
        createTradingViewChart,
        createDoughnutChart
    };
})();
