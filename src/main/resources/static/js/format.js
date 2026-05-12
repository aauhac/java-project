(function () {
    const currencyFormatter = new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD',
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });

    const integerFormatter = new Intl.NumberFormat('en-US');

    function toNumber(value, fallback = 0) {
        const numeric = Number(value);
        return Number.isFinite(numeric) ? numeric : fallback;
    }

    function currency(value) {
        return currencyFormatter.format(toNumber(value));
    }

    function number(value, digits = 2) {
        return toNumber(value).toFixed(digits);
    }

    function integer(value) {
        return integerFormatter.format(toNumber(value));
    }

    function percent(value) {
        const numeric = toNumber(value);
        const sign = numeric > 0 ? '+' : '';
        return `${sign}${numeric.toFixed(2)}%`;
    }

    function percentClass(value) {
        const numeric = toNumber(value);
        if (numeric > 0) {
            return 'positive';
        }
        if (numeric < 0) {
            return 'negative';
        }
        return 'neutral';
    }

    function date(value) {
        if (!value) {
            return '-';
        }
        return String(value).slice(0, 10);
    }

    function dateTime(value) {
        if (!value) {
            return '-';
        }
        return String(value).replace('T', ' ').slice(0, 16);
    }

    function score(value) {
        return `${number(value, 2)}점`;
    }

    window.Format = {
        currency,
        number,
        integer,
        percent,
        percentClass,
        date,
        dateTime,
        score
    };
})();
