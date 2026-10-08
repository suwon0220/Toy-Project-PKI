(function () {
    'use strict';
    var overlay = document.querySelector('[data-certificate-dialog]');
    if (!overlay) return;
    var dialog = overlay.querySelector('[role="dialog"]');
    var close = overlay.querySelector('[data-dialog-close]');
    var result = overlay.querySelector('[data-ocsp-result], [data-revoke-error]');
    (result || close || dialog).focus();
    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape') {
            event.preventDefault();
            close.click();
        }
        if (event.key !== 'Tab') return;
        var items = Array.from(dialog.querySelectorAll('a[href], button:not(:disabled), input:not(:disabled), select, textarea, [tabindex="0"]'))
            .filter(function (item) {
                return item.getClientRects().length > 0;
            });
        var first = items[0];
        var last = items[items.length - 1];
        if (event.shiftKey && (document.activeElement === first || document.activeElement === result)) {
            event.preventDefault();
            last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
            event.preventDefault();
            first.focus();
        }
    });
    var form = overlay.querySelector('[data-ocsp-form]');
    if (form) {
        var button = form.querySelector('button[type="submit"]');
        form.addEventListener('submit', function () {
            button.disabled = true;
            button.textContent = 'OCSP 응답 확인 중…';
            form.setAttribute('aria-busy', 'true');
        });
        window.addEventListener('pageshow', function () {
            button.disabled = false;
            button.textContent = 'OCSP 요청';
            form.removeAttribute('aria-busy');
        });
    }
    var revokeForm = overlay.querySelector('[data-revoke-form]');
    if (revokeForm) {
        var revokeButton = revokeForm.querySelector('button[type="submit"]');
        var confirmation = revokeForm.querySelector('[data-confirm-input]');
        confirmation.addEventListener('input', function () {
            confirmation.value = confirmation.value.toUpperCase();
            revokeButton.disabled = confirmation.value.trim() !== confirmation.getAttribute('data-confirm-value');
        });
        revokeForm.addEventListener('submit', function () {
            revokeButton.disabled = true;
            revokeButton.textContent = '폐기 중…';
            revokeForm.setAttribute('aria-busy', 'true');
        });
        window.addEventListener('pageshow', function () {
            revokeButton.disabled = confirmation.value.trim() !== confirmation.getAttribute('data-confirm-value');
            revokeButton.textContent = '인증서 폐기';
            revokeForm.removeAttribute('aria-busy');
        });
    }
}());
