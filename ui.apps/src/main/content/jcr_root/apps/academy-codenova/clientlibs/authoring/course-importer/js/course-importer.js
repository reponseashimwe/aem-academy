(function () {
    'use strict';

    var IMPORT_URL = '/bin/academy/course-import';
    var HISTORY_URL = '/bin/academy/course-import/history';

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

    function showMsg(type, msg) {
        var el = document.getElementById('mainResult');
        el.style.display = 'block';
        el.style.background = type === 'ok' ? '#e6f4ea' : '#fdecea';
        el.style.border = type === 'ok' ? '1px solid #a8d5b5' : '1px solid #f5c2c2';
        el.style.color = type === 'ok' ? '#1e4d2b' : '#7f1d1d';
        el.textContent = msg;
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

    function refreshBadge(cfg) {
        var badge = document.getElementById('configStatus');
        var enabled = cfg.enabled === true || cfg.enabled === 'true';

        if (!enabled || !cfg.scheduledDate) {
            badge.style.background = '#f4f4f4';
            badge.style.color = '#666';
            badge.textContent = '\u25CB No schedule set.';
            document.getElementById('cancelScheduleBtn').style.display = 'none';
            return;
        }
        badge.style.background = '#e6f4ea';
        badge.style.color = '#1e4d2b';
        badge.textContent = '\u25CF Scheduled for ' + cfg.scheduledDate +
            '\nFile: ' + (cfg.filePath || '\u2014') +
            '  \u2192  ' + (cfg.targetPath || '\u2014');
        document.getElementById('cancelScheduleBtn').style.display = '';
    }

    refreshBadge({ enabled: false });

    function readCommonValues(scopeEl) {
        var fp = getPathFieldValue(scopeEl, 'filePath');
        var tp = getPathFieldValue(scopeEl, 'targetPath');
        var dup = getPathFieldValue(scopeEl, 'duplicateHandling') || 'SKIP';
        return { filePath: fp, targetPath: tp, duplicateHandling: dup };
    }

    document.getElementById('onDemandForm').addEventListener('submit', function (e) { e.preventDefault(); });
    document.getElementById('scheduledForm').addEventListener('submit', function (e) { e.preventDefault(); });

    document.getElementById('onDemandBtn').addEventListener('click', function () {
        var btn = this;
        var values = readCommonValues(document.getElementById('onDemandForm'));
        document.getElementById('mainResult').style.display = 'none';

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
                        showMsg('ok',
                            'Job queued!\nJob ID: ' + json.jobId + '\n\n' +
                            'Monitor: crx-quickstart/logs/project-academy-reponse.log');
                        setTimeout(loadHistory, 3000);
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

    document.getElementById('saveScheduleBtn').addEventListener('click', function () {
        showMsg('error', 'Scheduling is temporarily disabled. Use OSGi task configuration for now.');
    });

    document.getElementById('cancelScheduleBtn').addEventListener('click', function () {
        showMsg('error', 'Scheduling is temporarily disabled. Use OSGi task configuration for now.');
    });

    function loadHistory() {
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
                    var ok = run.status === 'OK';
                    var fileName = run.filePath ? run.filePath.split('/').pop() : '\u2014';
                    var tr = document.createElement('tr');
                    tr.setAttribute('is', 'coral-table-row');
                    tr.innerHTML =
                        '<td is="coral-table-cell">' +
                        '<span class="coral-Label coral-Label--' + (ok ? 'success' : 'error') + '">' +
                        (ok ? 'OK' : 'FAILED') + '</span>' +
                        '</td>' +
                        '<td is="coral-table-cell">' + (run.triggeredBy || '\u2014') + '</td>' +
                        '<td is="coral-table-cell" style="white-space:nowrap">' + (run.createdAt || '\u2014') + '</td>' +
                        '<td is="coral-table-cell" style="white-space:nowrap">' + (run.scheduledAt || '\u2014') + '</td>' +
                        '<td is="coral-table-cell" style="max-width:160px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="' + (run.filePath || '') + '">' + fileName + '</td>' +
                        '<td is="coral-table-cell" style="text-align:right">' + (run.created || 0) + '</td>' +
                        '<td is="coral-table-cell" style="text-align:right">' + (run.updated || 0) + '</td>' +
                        '<td is="coral-table-cell" style="text-align:right">' + (run.failed || 0) + '</td>' +
                        '<td is="coral-table-cell" style="text-align:right">' + (run.skipped || 0) + '</td>';
                    tbody.appendChild(tr);
                });
                document.getElementById('historyTableWrap').style.display = 'block';
            })
            .catch(function () {
                document.getElementById('historyLoading').textContent = 'Could not load run history.';
            });
    }

    loadHistory();
}());
