/*
 * Toy PKI — 테마 편의 기능 (없어도 모든 화면·저장은 동작한다)
 *  1. 상단바 모드 토글: system → light → dark 순환, 쿠키 갱신
 *  2. 설정 화면: 입력 즉시 페이지 전체 미리보기
 *  3. 복사 버튼, 다이얼로그 열기/닫기
 *
 * 쿠키 형식은 ThemeCookie.java 와 같다: {mode}.{palette}.{hue}.{harmony}.{chroma%}
 */
(() => {
  const COOKIE = 'pki_theme';
  const DEFAULT = ['system', 'default', '200', 'triadic', '12'];
  const MODES = ['system', 'light', 'dark'];
  const MODE_LABEL = { system: '시스템', light: '라이트', dark: '다크' };
  const MODE_ICON = { system: '#i-monitor', light: '#i-sun', dark: '#i-moon' };
  const root = document.documentElement;

  const readCookie = () => {
    const raw = document.cookie.split('; ').find(c => c.startsWith(COOKIE + '='));
    const parts = raw ? decodeURIComponent(raw.slice(COOKIE.length + 1)).split('.') : [];
    return parts.length === 5 ? parts : [...DEFAULT];
  };
  const writeCookie = parts => {
    document.cookie = `${COOKIE}=${parts.join('.')}; Path=/; Max-Age=${60 * 60 * 24 * 365}; SameSite=Lax`;
  };

  /* ---------- 1. 모드 토글 ---------- */
  const toggle = document.querySelector('[data-mode-toggle]');
  const renderToggle = mode => {
    if (!toggle) return;
    toggle.querySelector('use').setAttribute('href', MODE_ICON[mode]);
    toggle.title = `화면 모드: ${MODE_LABEL[mode]}`;
    toggle.setAttribute('aria-label', `화면 모드: ${MODE_LABEL[mode]}. 눌러서 변경`);
  };
  toggle?.addEventListener('click', e => {
    e.preventDefault();
    const parts = readCookie();
    const next = MODES[(MODES.indexOf(root.dataset.mode) + 1) % MODES.length];
    parts[0] = next;
    writeCookie(parts);
    root.dataset.mode = next;
    renderToggle(next);
    // 설정 화면이 열려 있으면 라디오도 맞춰 준다
    const radio = document.querySelector(`#appearance-form input[name="mode"][value="${next}"]`);
    if (radio) radio.checked = true;
  });

  /* ---------- 2. 설정 화면 실시간 미리보기 ---------- */
  const form = document.getElementById('appearance-form');
  if (form) {
    const hueOut = document.getElementById('hue-out');
    const chromaOut = document.getElementById('chroma-out');
    const hint = document.getElementById('harmony-hint');
    const customScopes = form.querySelectorAll('[data-custom-scope]');

    const setVars = (el, hue, chroma) => {
      el.style.setProperty('--h1', hue);
      el.style.setProperty('--c', chroma);
    };

    const apply = () => {
      const data = new FormData(form);
      const hue = data.get('hue');
      const chroma = (Number(data.get('chroma')) / 100).toFixed(2);
      const harmonyInput = form.querySelector('input[name="harmony"]:checked');
      const paletteInput = form.querySelector('input[name="palette"]:checked');

      hueOut.textContent = `${hue}°`;
      chromaOut.textContent = chroma;
      hint.textContent = harmonyInput.dataset.description;

      // 사용자 지정 값에 의존하는 스코프(사용자 지정 카드, 패널, 규칙 옵션)
      customScopes.forEach(el => {
        setVars(el, hue, chroma);
        if (!el.classList.contains('harmony-option')) {
          el.dataset.harmony = harmonyInput.value;
          const label = el.querySelector('[data-harmony-label]');
          if (label) label.textContent = harmonyInput.dataset.label;
        }
      });

      // 페이지 전체
      root.dataset.mode = data.get('mode');
      root.dataset.palette = paletteInput.value;
      if (paletteInput.value === 'custom') {
        root.dataset.harmony = harmonyInput.value;
        setVars(root, hue, chroma);
      } else {
        root.dataset.harmony = paletteInput.dataset.harmony;
        setVars(root, paletteInput.dataset.hue, paletteInput.dataset.chroma);
      }
      renderToggle(root.dataset.mode);
    };

    form.addEventListener('input', apply);
    form.addEventListener('change', apply);
  }

  /* ---------- 3. 복사 / 다이얼로그 ---------- */
  document.addEventListener('click', async e => {
    const copyBtn = e.target.closest('[data-copy]');
    if (copyBtn) {
      const target = document.querySelector(copyBtn.dataset.copy);
      try {
        await navigator.clipboard.writeText(target.textContent.trim());
        const prev = copyBtn.title;
        copyBtn.title = '복사됨';
        copyBtn.classList.add('is-copied');
        setTimeout(() => { copyBtn.title = prev; copyBtn.classList.remove('is-copied'); }, 1500);
      } catch { /* 클립보드 권한이 없으면 무시 */ }
    }
    const opener = e.target.closest('[data-open-dialog]');
    if (opener) document.getElementById(opener.dataset.openDialog)?.showModal();
    const closer = e.target.closest('[data-close-dialog]');
    if (closer) closer.closest('dialog')?.close();
  });
})();
