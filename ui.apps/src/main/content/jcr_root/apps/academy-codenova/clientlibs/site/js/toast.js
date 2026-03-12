document.addEventListener('DOMContentLoaded', function () {
    // Find all buttons/links that have a toast message configured
    var toastButtons = document.querySelectorAll('[data-toast-message]');

    if (!toastButtons.length) {
        return;
    }

    toastButtons.forEach(function (button) {
        var msg = button.getAttribute('data-toast-message');

        if (!msg) {
            return;
        }

        button.addEventListener('click', function (event) {
            // If this is a non-navigation button (link is "#"), stop navigation
            if (button.getAttribute('href') === '#' || button.tagName === 'BUTTON') {
                event.preventDefault();
            }

            fetch('/bin/codenova/toast')
                .then(function (response) {
                    if (!response.ok) {
                        throw new Error('Network response was not ok');
                    }
                    return response.text();
                })
                .then(function (serverHtml) {
                    // Use shared container for all toasts (must exist in page markup)
                    var container = document.querySelector('.cn-toast-container');
                    if (!container) {
                        return;
                    }

                    // Insert the servlet HTML and grab the new toast element
                    container.insertAdjacentHTML('beforeend', serverHtml);
                    var toastEl = container.lastElementChild;
                    if (!toastEl || !toastEl.classList.contains('cn-toast-message')) {
                        return;
                    }

                    // Extract time from servlet response
                    var timeSpan = toastEl.querySelector('.cn-toast-time');
                    var timeText = timeSpan ? timeSpan.textContent.trim() : toastEl.textContent.trim();

                    // Rebuild inner content: message + time + dismiss button
                    toastEl.innerHTML = '';

                    var textSpan = document.createElement('span');
                    var baseText = msg || 'Toast';
                    var timeSuffix = timeText ? ' (' + timeText + ')' : '';
                    textSpan.textContent = baseText + timeSuffix;
                    toastEl.appendChild(textSpan);

                    var closeBtn = document.createElement('button');
                    closeBtn.type = 'button';
                    closeBtn.className = 'cn-toast-close btn-close';
                    closeBtn.setAttribute('aria-label', 'Close');
                    closeBtn.addEventListener('click', function () {
                        toastEl.remove();
                    });
                    toastEl.appendChild(closeBtn);
                })
                .catch(function () {
                    var container = document.querySelector('.cn-toast-container');
                    if (!container) {
                        return;
                    }

                    var toastEl = document.createElement('div');
                    toastEl.className = 'cn-toast-message alert alert-primary';
                    toastEl.setAttribute('role', 'status');
                    toastEl.setAttribute('aria-live', 'polite');

                    var textSpan = document.createElement('span');
                    textSpan.textContent = msg || 'Toast';
                    toastEl.appendChild(textSpan);

                    var closeBtn = document.createElement('button');
                    closeBtn.type = 'button';
                    closeBtn.className = 'cn-toast-close btn-close';
                    closeBtn.setAttribute('aria-label', 'Close');
                    closeBtn.addEventListener('click', function () {
                        toastEl.remove();
                    });
                    toastEl.appendChild(closeBtn);

                    container.appendChild(toastEl);
                });
        });
    });
});

