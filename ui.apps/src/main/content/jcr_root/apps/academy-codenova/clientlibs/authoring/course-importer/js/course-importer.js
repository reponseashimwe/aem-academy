(function () {
    'use strict';

    var IMPORT_URL = '/bin/academy/course-import';
    var HISTORY_URL = '/bin/academy/course-import/history';
    var progressTimer = null;
    var toastTimer = null;

    document.getElementById('downloadTemplateBtn').addEventListener('click', function () {
        var rows = [
            'title,startdate,abstract,tags,link,filereference',
            '"Introduction to Python Programming",2026-06-01,"Master Python from fundamentals to advanced concepts",technology,https://www.learnpython.org,/content/dam/academy/courses/course-1.png',
            '"Advanced JavaScript Frameworks",2026-06-15,"Build modern web apps with React Vue and Angular",web;technology,https://developer.mozilla.org/en-US/docs/Web/JavaScript,/content/dam/academy/courses/course-2.png'
        ];
        var blob = new Blob([rows.join('\n') + '\n'], { type: 'text/csv;charset=utf-8;' });
        var url = URL.createObjectURL(blob);
        var a = document.createElement('a');
        a.href = url;
        a.download = 'course-import-template.csv';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    });

    function csrf(then) {
        fetch('/libs/granite/csrf/token.json')
            .then(function (r) { return r.json(); })
            .then(then);
    }

    function showToast(msg) {
        var toast = document.getElementById('importToast');
        document.getElementById('importToastMsg').textContent = msg;
        if (toastTimer) { clearTimeout(toastTimer); toastTimer = null; }
        toast.style.display = 'flex';
        toast.style.animation = 'none';
        void toast.offsetWidth;
        toast.style.animation = '';
        toastTimer = setTimeout(function () { closeToast(); }, 4000);
    }

    function closeToast() {
        document.getElementById('importToast').style.display = 'none';
        if (toastTimer) { clearTimeout(toastTimer); toastTimer = null; }
    }

    function showMsg(type, msg) {
        var el = document.getElementById('mainResult');
        el.style.display = 'block';
        el.style.background = type === 'ok' ? '#e6f4ea' : '#fdecea';
        el.style.border = type === 'ok' ? '1px solid #a8d5b5' : '1px solid #f5c2c2';
        el.style.color = type === 'ok' ? '#1e4d2b' : '#7f1d1d';
        el.textContent = msg;
    }

    function clearMsg() {
        var el = document.getElementById('mainResult');
        el.style.display = 'none';
        el.textContent = '';
    }

    function getFieldEl(scopeEl, fieldName) {
        return scopeEl ? scopeEl.querySelector('[name="' + fieldName + '"]') : null;
    }

    function getPathFieldValue(scopeEl, fieldName) {
        var el = getFieldEl(scopeEl, fieldName);
        return el ? el.value.trim() : '';
    }

    function setFieldValueAll(fieldName, value) {
        var els = document.querySelectorAll('[name="' + fieldName + '"]');
        Array.prototype.forEach.call(els, function (el) {
            el.value = value || '';
        });
    }

    document.getElementById('configStatus').style.display = 'none';

    document.getElementById('importToastClose').addEventListener('click', closeToast);

    function resetProgressUi() {
        document.getElementById('importProgressWrap').style.display = 'none';
        document.getElementById('rowLogWrap').style.display = 'none';
        document.getElementById('importProgressBar').style.width = '0%';
        document.getElementById('importProgressPercent').textContent = '0%';
        document.getElementById('importProgressText').innerHTML = '';
        document.getElementById('rowLogList').innerHTML = '';
    }

    function escapeHtml(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function formatDate(isoStr) {
        if (!isoStr) return '\u2014';
        try {
            var d = new Date(isoStr.replace(' ', 'T'));
            if (isNaN(d.getTime())) return isoStr;
            var months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
            var pad = function (n) { return n < 10 ? '0' + n : '' + n; };
            return months[d.getMonth()] + ' ' + pad(d.getDate()) + ', ' + d.getFullYear() +
                ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds());
        } catch (e) { return isoStr; }
    }

    function statusVariant(status) {
        var normalized = String(status || '').toUpperCase();
        if (normalized === 'FAILED') return 'failed';
        if (normalized === 'SKIPPED') return 'skipped';
        if (normalized === 'UPDATED') return 'updated';
        return 'created';
    }

    function statusLabel(status) {
        var normalized = String(status || '').toUpperCase();
        if (normalized === 'FAILED') return 'Failed';
        if (normalized === 'SKIPPED') return 'Skipped';
        if (normalized === 'UPDATED') return 'Updated';
        return 'Created';
    }

    function triggeredByVariant(triggeredBy) {
        var normalized = String(triggeredBy || '').toLowerCase();
        if (normalized === 'scheduled') return 'updated';
        return 'skipped';
    }

    function triggeredByLabel(triggeredBy) {
        var value = String(triggeredBy || '').trim();
        if (!value) return 'Unknown';
        return value.charAt(0).toUpperCase() + value.slice(1);
    }

    function rowTitle(event) {
        return event.title || event.courseTitle || event.name || '';
    }

    function createRowEventHtml(event) {
        var rowNumber = escapeHtml(event.row || '-');
        var badgeText = escapeHtml(statusLabel(event.status));
        return '<tr is="coral-table-row">' +
            '<td is="coral-table-cell">' + rowNumber + '</td>' +
            '<td is="coral-table-cell"><span class="row-log-title">' + escapeHtml(rowTitle(event)) + '</span></td>' +
            '<td is="coral-table-cell"><span class="stat-badge stat-badge--' + statusVariant(event.status) + '">' + badgeText + '</span></td>' +
            '<td is="coral-table-cell"><span class="row-log-msg">' + escapeHtml(event.message || '\u2014') + '</span></td>' +
            '</tr>';
    }

    function openHistoryRowDialog(run) {
        var dialog = document.getElementById('historyRowDialog');
        var summary = document.getElementById('historyDialogSummary');
        var tbody = document.getElementById('historyDialogBody');
        var rowEvents = Array.isArray(run.rowEvents) ? run.rowEvents : [];

        summary.innerHTML =
            '<span class="history-dialog-meta-item"><strong>File:</strong> ' + escapeHtml(run.filePath || '\u2014') + '</span>' +
            '<span class="history-dialog-meta-item"><strong>Triggered:</strong> ' +
            '<span class="coral-Label coral-Label--' + triggeredByVariant(run.triggeredBy) + '">' +
            escapeHtml(triggeredByLabel(run.triggeredBy)) + '</span></span>' +
            '<span class="history-dialog-meta-item"><strong>Rows:</strong> ' + rowEvents.length + '</span>';
        tbody.innerHTML = rowEvents.map(createRowEventHtml).join('');

        if (typeof dialog.show === 'function') {
            dialog.show();
            return;
        }
        dialog.setAttribute('open', '');
    }

    function renderProgress(progress) {
        var processed = progress.processedRows || 0;
        var total = progress.totalRows || 0;
        var isFinal = progress.status === 'COMPLETED' || progress.status === 'FAILED';
        var rowEvents = Array.isArray(progress.rowEvents) ? progress.rowEvents : [];
        var percent = total > 0 ? Math.min(100, Math.round((processed / total) * 100)) : 0;
        document.getElementById('importProgressWrap').style.display = 'block';
        document.getElementById('rowLogWrap').style.display = isFinal && rowEvents.length ? 'block' : 'none';
        document.getElementById('importProgressBar').style.width = percent + '%';
        document.getElementById('importProgressPercent').textContent = percent + '%';
        document.getElementById('importProgressText').innerHTML =
            '<span>Processed <strong>' + processed + '</strong> / <strong>' + total + '</strong></span>' +
            '<span class="stat-badge stat-badge--created">' + (progress.created || 0) + ' created</span>' +
            '<span class="stat-badge stat-badge--updated">' + (progress.updated || 0) + ' updated</span>' +
            '<span class="stat-badge stat-badge--failed">' + (progress.failed || 0) + ' failed</span>' +
            '<span class="stat-badge stat-badge--skipped">' + (progress.skipped || 0) + ' skipped</span>';

        var rowLogList = document.getElementById('rowLogList');
        rowLogList.innerHTML = rowEvents.map(createRowEventHtml).join('');
        rowLogList.parentElement.scrollTop = rowLogList.parentElement.scrollHeight;
    }

    function stopProgressPolling() {
        if (progressTimer) {
            clearInterval(progressTimer);
            progressTimer = null;
        }
    }

    function pollProgress(jobId) {
        stopProgressPolling();
        progressTimer = setInterval(function () {
            fetch(HISTORY_URL + '?jobId=' + encodeURIComponent(jobId))
                .then(function (r) { return r.json(); })
                .then(function (json) {
                    if (!json || json.status === 'not_found' || json.status === 'error') return;
                    renderProgress(json);
                    if (json.status === 'COMPLETED' || json.status === 'FAILED') {
                        stopProgressPolling();
                        loadHistory();
                    }
                })
                .catch(function () { });
        }, 100);
    }

    function readCommonValues(scopeEl) {
        var fp = getPathFieldValue(scopeEl, 'filePath');
        var tp = getPathFieldValue(scopeEl, 'targetPath');
        var dup = getPathFieldValue(scopeEl, 'duplicateHandling') || 'SKIP';
        return { filePath: fp, targetPath: tp, duplicateHandling: dup };
    }

    document.getElementById('onDemandForm').addEventListener('submit', function (e) { e.preventDefault(); });

    document.getElementById('onDemandBtn').addEventListener('click', function () {
        var btn = this;
        var values = readCommonValues(document.getElementById('onDemandForm'));
        clearMsg();
        resetProgressUi();

        if (!values.filePath) {
            showMsg('error', 'DAM File Path is required.');
            return;
        }

        btn.setAttribute('disabled', '');
        btn.innerHTML = 'Submitting\u2026';
        csrf(function (token) {
            fetch(IMPORT_URL, {
                method: 'POST',
                headers: { 'CSRF-Token': token.token },
                body: new URLSearchParams({
                    filePath: values.filePath,
                    duplicateHandling: values.duplicateHandling,
                    targetPath: values.targetPath
                })
            })
                .then(function (r) { return r.json(); })
                .then(function (json) {
                    if (json.status === 'queued') {
                        showToast('Import job started successfully.');
                        pollProgress(json.jobId);
                    } else {
                        showMsg('error', 'Error: ' + (json.message || JSON.stringify(json)));
                    }
                })
                .catch(function (err) { showMsg('error', 'Request failed: ' + err); })
                .finally(function () {
                    btn.removeAttribute('disabled');
                    btn.innerHTML = '<coral-icon icon="uploadToCloud" size="S"></coral-icon>&nbsp;Start Import';
                });
        });
    });

    function loadHistory() {
        document.getElementById('historyLoading').style.display = 'block';
        document.getElementById('historyEmpty').style.display = 'none';
        document.getElementById('historyTableWrap').style.display = 'none';
        document.getElementById('historyBody').innerHTML = '';
        fetch(HISTORY_URL)
            .then(function (r) { return r.json(); })
            .then(function (runs) {
                document.getElementById('historyLoading').style.display = 'none';
                if (!runs || runs.length === 0) {
                    document.getElementById('historyEmpty').style.display = 'block';
                    return;
                }
                var tbody = document.getElementById('historyBody');
                tbody.innerHTML = '';
                runs.forEach(function (run) {
                    var normalizedStatus = String(run.status || '').toUpperCase();
                    var ok = normalizedStatus === 'OK' || normalizedStatus === 'COMPLETED';
                    var statusLabel = ok ? 'COMPLETED' : (normalizedStatus || 'FAILED');
                    var statusVariantValue = ok ? 'created' : (normalizedStatus === 'RUNNING' || normalizedStatus === 'QUEUED' ? 'updated' : 'failed');
                    var fileName = run.filePath ? run.filePath.split('/').pop() : '\u2014';
                    var rowEvents = Array.isArray(run.rowEvents) ? run.rowEvents : [];
                    var tr = document.createElement('tr');
                    tr.setAttribute('is', 'coral-table-row');
                    var viewCell = rowEvents.length
                        ? '<button type="button" class="history-view-btn">View&nbsp;<coral-icon icon="chevronRight" size="XS"></coral-icon></button>'
                        : '<span style="color:#ccc">\u2014</span>';
                    tr.innerHTML =
                        '<td is="coral-table-cell">' +
                        '<span class="stat-badge stat-badge--' + statusVariantValue + '">' +
                        statusLabel + '</span>' +
                        '</td>' +
                        '<td is="coral-table-cell"><span class="stat-badge stat-badge--' + triggeredByVariant(run.triggeredBy) + '">' +
                        escapeHtml(triggeredByLabel(run.triggeredBy)) + '</span></td>' +
                        '<td is="coral-table-cell" style="white-space:nowrap">' + formatDate(run.createdAt) + '</td>' +
                        '<td is="coral-table-cell" style="white-space:nowrap">' + formatDate(run.scheduledAt) + '</td>' +
                        '<td is="coral-table-cell" style="max-width:160px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="' + (run.filePath || '') + '">' + fileName + '</td>' +
                        '<td is="coral-table-cell" style="text-align:center"><span class="stat-badge stat-badge--created">' + (run.created || 0) + '</span></td>' +
                        '<td is="coral-table-cell" style="text-align:center"><span class="stat-badge stat-badge--updated">' + (run.updated || 0) + '</span></td>' +
                        '<td is="coral-table-cell" style="text-align:center"><span class="stat-badge stat-badge--failed">' + (run.failed || 0) + '</span></td>' +
                        '<td is="coral-table-cell" style="text-align:center"><span class="stat-badge stat-badge--skipped">' + (run.skipped || 0) + '</span></td>' +
                        '<td is="coral-table-cell" style="text-align:center">' + viewCell + '</td>';
                    tbody.appendChild(tr);

                    if (rowEvents.length > 0) {
                        var detailsBtn = tr.querySelector('.history-view-btn');
                        detailsBtn.addEventListener('click', function () {
                            openHistoryRowDialog(run);
                        });
                    }
                });
                document.getElementById('historyTableWrap').style.display = 'block';
            })
            .catch(function () {
                document.getElementById('historyLoading').textContent = 'Could not load run history.';
            });
    }

    var historyTab = document.querySelectorAll('coral-tab')[1];
    if (historyTab) {
        historyTab.addEventListener('click', function () { loadHistory(); });
    }

    loadHistory();
}());
