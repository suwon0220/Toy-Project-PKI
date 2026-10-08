(function () {
    'use strict';
    var overlay = document.querySelector('[data-certificate-dialog]');
    if (!overlay) return;
    var dialog = overlay.querySelector('[role="dialog"]');
    var close = overlay.querySelector('[data-dialog-close]');
    var result = overlay.querySelector('[data-validation-result], [data-ocsp-result], [data-revoke-error]');
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
    function bindRequestForm(selector, pendingText) {
        var form = overlay.querySelector(selector);
        if (!form) return;
        var button = form.querySelector('button[type="submit"]');
        var label = button.textContent;
        form.addEventListener('submit', function () {
            button.disabled = true;
            button.textContent = pendingText;
            form.setAttribute('aria-busy', 'true');
        });
        window.addEventListener('pageshow', function () {
            button.disabled = false;
            button.textContent = label;
            form.removeAttribute('aria-busy');
        });
    }
    bindRequestForm('[data-ocsp-form]', 'OCSP 응답 확인 중…');
    bindRequestForm('[data-validation-form]', '체인 검증 중…');
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
