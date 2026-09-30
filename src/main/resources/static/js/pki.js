/*
 * PKI Console — templates/pki/** 보조 스크립트.
 * 화면은 서버 렌더링만으로 동작하고, 여기서는 다음만 덧붙인다.
 *   - [data-sort-list]      목록 헤더 클릭 정렬 (서버 정렬 파라미터가 없는 화면)
 *   - [data-filter-input]   목록 검색 · 선택 필터 (화면에 이미 그려진 행만 거른다)
 *   - [data-confirm-input]  확인 문구를 정확히 입력해야 위험 버튼 활성화
 *   - [data-pubkey-*]       키 팝업의 공개키 SHA-256 지문 · .pem 다운로드 (publicKeyPem 이 있을 때)
 *   - [data-repeat]         추가 EKU 입력 행 추가
 */
(function () {
    'use strict';

    // data-k-created-at → dataset.kCreatedAt
    function datasetKey(key) {
        return 'k' + key.charAt(0).toUpperCase() + key.slice(1);
    }

    /* ---------- 목록 정렬 ---------- */
    function initSortList(list) {
        var body = list.querySelector('[data-sort-body]');
        var buttons = Array.prototype.slice.call(list.querySelectorAll('[data-sort-key]'));
        if (!body || buttons.length === 0) {
            return;
        }
        var initial = (list.getAttribute('data-sort-list') || '').split(',');
        var state = {key: initial[0] || buttons[0].getAttribute('data-sort-key'), dir: initial[1] || 'asc'};

        function valueOf(row, button) {
            var raw = row.dataset[datasetKey(button.getAttribute('data-sort-key'))] || '';
            return button.getAttribute('data-sort-type') === 'number' ? Number(raw) || 0 : raw.toLowerCase();
        }

        function apply() {
            var active = buttons.filter(function (b) {
                return b.getAttribute('data-sort-key') === state.key;
            })[0];
            if (!active) {
                return;
            }
            var rows = Array.prototype.slice.call(body.children);
            rows.sort(function (a, b) {
                var x = valueOf(a, active);
                var y = valueOf(b, active);
                var c = x < y ? -1 : (x > y ? 1 : 0);
                return state.dir === 'asc' ? c : -c;
            });
            rows.forEach(function (row) {
                body.appendChild(row);
            });
            buttons.forEach(function (b) {
                var on = b === active;
                var arrow = b.querySelector('.arrow');
                b.classList.toggle('is-on', on);
                b.setAttribute('aria-pressed', on ? 'true' : 'false');
                if (arrow) {
                    arrow.textContent = on ? (state.dir === 'asc' ? '▲' : '▼') : '↕';
                }
            });
        }

        buttons.forEach(function (b) {
            b.addEventListener('click', function () {
                var key = b.getAttribute('data-sort-key');
                state.dir = state.key === key && state.dir === 'asc' ? 'desc' : 'asc';
                state.key = key;
                apply();
            });
        });
        apply();
    }

    /* ---------- 목록 검색 · 필터 ---------- */
    function initFilter(target) {
        var inputs = Array.prototype.slice.call(
            document.querySelectorAll('[data-filter-input="#' + target.id + '"], [data-filter-target="#' + target.id + '"]'));
        if (inputs.length === 0) {
            return;
        }
        var rows = Array.prototype.slice.call(target.querySelectorAll('[data-sort-body] > *'));
        var count = target.querySelector('[data-filter-count]');
        var empty = target.querySelector('[data-filter-empty]');

        function apply() {
            var shown = 0;
            rows.forEach(function (row) {
                var visible = inputs.every(function (input) {
                    var value = input.value.trim().toLowerCase();
                    if (!value) {
                        return true;
                    }
                    var field = input.getAttribute('data-filter-field');
                    if (field) {
                        return (row.dataset[datasetKey(field)] || '').toLowerCase() === value;
                    }
                    return (row.getAttribute('data-search') || '').toLowerCase().indexOf(value) >= 0;
                });
                row.hidden = !visible;
                if (visible) {
                    shown++;
                }
            });
            if (count) {
                count.textContent = shown === rows.length ? rows.length + '개' : shown + ' / ' + rows.length + '개';
            }
            if (empty) {
                empty.hidden = shown > 0;
            }
        }

        inputs.forEach(function (input) {
            input.addEventListener(input.tagName === 'SELECT' ? 'change' : 'input', apply);
        });
        apply();
    }

    /* ---------- 확인 문구 입력 ---------- */
    function initConfirm(input) {
        var form = input.form;
        var expected = input.getAttribute('data-confirm-value');
        var button = form && form.querySelector('[data-confirm-button]');
        if (!button || !expected) {
            return;
        }

        function apply() {
            button.disabled = input.value.trim() !== expected;
        }

        input.addEventListener('input', apply);
        apply();
    }

    /* ---------- 공개키 지문 · 다운로드 ---------- */
    // 서버가 넣어 준 공개키 PEM(publicKeyPem)으로 SHA-256 지문을 계산하고 .pem 파일로 내려준다.
    function pemToBytes(pem) {
        var body = pem.replace(/-----[^-]+-----/g, '').replace(/\s+/g, '');
        var binary = atob(body);
        var bytes = new Uint8Array(binary.length);
        for (var i = 0; i < binary.length; i++) {
            bytes[i] = binary.charCodeAt(i);
        }
        return bytes;
    }

    function initFingerprint(target) {
        var source = document.querySelector('[data-pubkey-pem]');
        if (!source || !window.crypto || !window.crypto.subtle) {
            return;
        }
        var bytes;
        try {
            bytes = pemToBytes(source.value);
        } catch (e) {
            return;
        }
        window.crypto.subtle.digest('SHA-256', bytes).then(function (hash) {
            var hex = Array.prototype.map.call(new Uint8Array(hash), function (b) {
                return ('0' + b.toString(16)).slice(-2).toUpperCase();
            });
            target.textContent = 'SHA-256 ' + hex.join(':');
        });
    }

    function initPemDownload(button) {
        button.addEventListener('click', function () {
            var pem = button.getAttribute('data-pem') || '';
            var url = URL.createObjectURL(new Blob([pem], {type: 'application/x-pem-file'}));
            var link = document.createElement('a');
            link.href = url;
            link.download = button.getAttribute('data-filename') || 'public-key.pem';
            document.body.appendChild(link);
            link.click();
            link.remove();
            URL.revokeObjectURL(url);
        });
    }

    /* ---------- 입력 행 추가 ---------- */
    function initRepeat(container) {
        var add = container.querySelector('[data-repeat-add]');
        if (!add) {
            return;
        }
        // 비어 있는 행은 보내지 않는다 (빈 OID 가 EKU 로 저장되지 않도록). disabled 입력은 전송되지 않는다.
        var form = container.closest('form');
        if (form) {
            form.addEventListener('submit', function () {
                container.querySelectorAll('[data-repeat-row]').forEach(function (row) {
                    var inputs = Array.prototype.slice.call(row.querySelectorAll('input'));
                    var blank = inputs.every(function (input) {
                        return input.value.trim() === '';
                    });
                    inputs.forEach(function (input) {
                        input.disabled = blank;
                    });
                });
            });
        }
        add.hidden = false;
        add.addEventListener('click', function () {
            var rows = container.querySelectorAll('[data-repeat-row]');
            var last = rows[rows.length - 1];
            var index = rows.length;
            var clone = last.cloneNode(true);
            clone.querySelectorAll('input').forEach(function (input) {
                input.value = '';
                input.name = input.name.replace(/\[\d+]/, '[' + index + ']');
                var label = input.getAttribute('aria-label');
                if (label) {
                    input.setAttribute('aria-label', label.replace(/\d+/, String(index + 1)));
                }
            });
            last.after(clone);
            clone.querySelector('input').focus();
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-sort-list]').forEach(initSortList);
        document.querySelectorAll('[data-sort-list][id]').forEach(initFilter);
        document.querySelectorAll('[data-confirm-input]').forEach(initConfirm);
        document.querySelectorAll('[data-repeat]').forEach(initRepeat);
        document.querySelectorAll('[data-pubkey-fingerprint]').forEach(initFingerprint);
        document.querySelectorAll('[data-pubkey-download]').forEach(initPemDownload);
    });
})();
