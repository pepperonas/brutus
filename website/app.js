// Brutus — product page (generated from templates/apps/product-page).
// English is in the markup; i18n.js carries DE/ES/IT/FR, picked from the browser.
// Download targets and their labels come from latest.json, written by the server timer.
(function () {
  'use strict';

  var I18N = window.SITE_I18N || {};
  var LANGS = ["en", "de", "es", "it", "fr"];
  var LANG_KEY = 'brutus-lang';
  // The only host download links may point at — the timer writes GitHub release URLs.
  var ASSET_URL = /^https:\/\/github\.com\/pepperonas\/brutus\/releases\/download\//;

  var EN = {}; // captured from the markup on first switch, so English lives in one place only
  var nodes = document.querySelectorAll('[data-i18n]');
  var altNodes = document.querySelectorAll('[data-i18n-alt]');
  var ariaNodes = document.querySelectorAll('[data-i18n-aria]');
  var phNodes = document.querySelectorAll('[data-i18n-placeholder]');
  var langBtn = document.getElementById('lang-btn');
  var langMenu = document.getElementById('lang-menu');
  var langItems = Array.prototype.slice.call(langMenu.querySelectorAll('[data-lang]'));
  var LANG_NAMES = { en: 'English', de: 'Deutsch', es: 'Español', it: 'Italiano', fr: 'Français' };
  var release = null;

  function store(k, v) { try { localStorage.setItem(k, v); } catch (e) { /* private mode */ } }
  function load(k) { try { return localStorage.getItem(k); } catch (e) { return null; } }

  function apply(lang) {
    var dict = I18N[lang] || {};
    nodes.forEach(function (n) {
      var k = n.getAttribute('data-i18n');
      if (!(k in EN)) EN[k] = n.innerHTML;
      n.innerHTML = dict[k] || EN[k];
    });
    altNodes.forEach(function (n) {
      var k = n.getAttribute('data-i18n-alt');
      if (!(k in EN)) EN[k] = n.getAttribute('alt');
      n.setAttribute('alt', dict[k] || EN[k]);
    });
    ariaNodes.forEach(function (n) {
      var k = n.getAttribute('data-i18n-aria');
      if (!(k in EN)) EN[k] = n.getAttribute('aria-label');
      n.setAttribute('aria-label', dict[k] || EN[k]);
    });
    phNodes.forEach(function (n) {
      var k = n.getAttribute('data-i18n-placeholder');
      if (!(k + '#ph' in EN)) EN[k + '#ph'] = n.getAttribute('placeholder');
      n.setAttribute('placeholder', dict[k] || EN[k + '#ph']);
    });
    document.documentElement.lang = lang;
    showLang(lang);
    renderMeta();
  }

  function fromBrowser() {
    var list = navigator.languages && navigator.languages.length ? navigator.languages : [navigator.language || 'en'];
    for (var i = 0; i < list.length; i++) {
      var code = String(list[i]).toLowerCase().slice(0, 2);
      if (LANGS.indexOf(code) >= 0) return code;
    }
    return 'en';
  }

  // ---- language menu: a button and a listbox (flags + native names), keyboard like a select
  function showLang(lang) {
    document.getElementById('lang-flag').className = 'flag flag-' + lang;
    document.getElementById('lang-code').textContent = lang.toUpperCase();
    langBtn.setAttribute('aria-label', 'Language: ' + LANG_NAMES[lang]);
    langItems.forEach(function (li) { li.setAttribute('aria-selected', String(li.getAttribute('data-lang') === lang)); });
  }
  var activeIndex = 0;
  function setActive(i) {
    activeIndex = (i + langItems.length) % langItems.length;
    langItems.forEach(function (li, n) { li.classList.toggle('active', n === activeIndex); });
    langMenu.setAttribute('aria-activedescendant', langItems[activeIndex].id);
  }
  function openMenu() {
    langMenu.hidden = false;
    langBtn.setAttribute('aria-expanded', 'true');
    setActive(LANGS.indexOf(current));
    langMenu.focus();
  }
  function closeMenu(refocus) {
    if (langMenu.hidden) return;
    langMenu.hidden = true;
    langBtn.setAttribute('aria-expanded', 'false');
    if (refocus) langBtn.focus();
  }
  function choose(lang) {
    closeMenu(true);
    if (lang === current) return;
    current = lang;
    store(LANG_KEY, current);
    apply(current);
  }
  // For the WebMCP tool set_page_language (webmcp.js).
  window.SITE_setLanguage = function (lang) {
    if (LANGS.indexOf(lang) < 0 || lang === current) return;
    current = lang;
    store(LANG_KEY, current);
    apply(current);
  };
  langBtn.addEventListener('click', function () { if (langMenu.hidden) openMenu(); else closeMenu(true); });
  langBtn.addEventListener('keydown', function (e) {
    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') { e.preventDefault(); openMenu(); }
  });
  langMenu.addEventListener('keydown', function (e) {
    if (e.key === 'ArrowDown') { e.preventDefault(); setActive(activeIndex + 1); }
    else if (e.key === 'ArrowUp') { e.preventDefault(); setActive(activeIndex - 1); }
    else if (e.key === 'Home') { e.preventDefault(); setActive(0); }
    else if (e.key === 'End') { e.preventDefault(); setActive(langItems.length - 1); }
    else if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); choose(langItems[activeIndex].getAttribute('data-lang')); }
    else if (e.key === 'Escape' || e.key === 'Tab') { closeMenu(e.key === 'Escape'); }
  });
  langItems.forEach(function (li, n) {
    li.addEventListener('click', function () { choose(li.getAttribute('data-lang')); });
    li.addEventListener('mousemove', function () { if (n !== activeIndex) setActive(n); });
  });
  document.addEventListener('click', function (e) {
    if (!langMenu.hidden && !document.getElementById('lang').contains(e.target)) closeMenu(false);
  });

  var saved = load(LANG_KEY);
  var current = LANGS.indexOf(saved) >= 0 ? saved : fromBrowser();
  if (current !== 'en') apply(current);
  else showLang('en');

  // ---- latest release (written next to this page by a server-side timer; no third-party call)
  // latest.json: { version, published, notes, assets: [{ target, label, requirement, name, url, size, sha256 }] }
  function mb(bytes) {
    try { return (bytes / 1048576).toLocaleString(current, { minimumFractionDigits: 1, maximumFractionDigits: 1 }) + ' MB'; }
    catch (e) { return (bytes / 1048576).toFixed(1) + ' MB'; }
  }
  function date(iso) {
    try { return new Date(iso).toLocaleDateString(current === 'en' ? 'en-GB' : current, { day: 'numeric', month: 'short', year: 'numeric' }); }
    catch (e) { return ''; }
  }
  // Which target fits this visitor: the same order the server's /download uses.
  function detectTarget(assets) {
    var ids = assets.map(function (a) { return a.target; });
    var ua = (navigator.userAgent || '').toLowerCase();
    var plat = ((navigator.userAgentData && navigator.userAgentData.platform) || navigator.platform || '').toLowerCase();
    var guess =
      /android/.test(ua) ? 'android' :
      /iphone|ipad|ipod/.test(ua) ? 'ios' :
      /mac/.test(plat) || /macintosh|mac os x/.test(ua) ? 'macos' :
      /win/.test(plat) || /windows/.test(ua) ? 'windows' :
      /linux/.test(plat) || /linux/.test(ua) ? 'linux' : '';
    if (guess && ids.indexOf(guess) >= 0) return guess;
    // A family match (linux → linux-appimage) before giving up.
    for (var i = 0; i < ids.length; i++) if (guess && ids[i].indexOf(guess) === 0) return ids[i];
    return ids[0];
  }
  function t(key, fallback) { var d = I18N[current] || {}; return d[key] || fallback; }
  var chosen = null;
  function renderMeta() {
    if (!release || !chosen) return;
    document.getElementById('dl-meta').textContent =
      [release.version, mb(chosen.size), date(release.published), chosen.requirement].filter(Boolean).join(' · ');
    var label = document.getElementById('dl-label');
    label.textContent = t('dl.' + chosen.target, chosen.label);
    var others = document.getElementById('dl-others');
    others.innerHTML = '';
    release.assets.forEach(function (a) {
      if (a === chosen) return;
      var li = document.createElement('li');
      var link = document.createElement('a');
      link.href = a.url;
      link.textContent = t('dl.short.' + a.target, a.short || a.target) + ' ';
      var small = document.createElement('small');
      small.textContent = mb(a.size);
      link.appendChild(small);
      li.appendChild(link);
      others.appendChild(li);
    });
    var sums = document.getElementById('checksums');
    if (sums) {
      sums.innerHTML = '';
      release.assets.forEach(function (a) {
        if (!a.sha256) return;
        var dt = document.createElement('dt'); dt.textContent = a.name;
        var dd = document.createElement('dd'); var code = document.createElement('code'); code.textContent = a.sha256;
        dd.appendChild(code); sums.appendChild(dt); sums.appendChild(dd);
      });
    }
  }

  fetch('latest.json', { cache: 'no-cache' })
    .then(function (r) { return r.ok ? r.json() : null; })
    .then(function (d) {
      if (!d || !Array.isArray(d.assets) || !d.assets.length) return;
      d.assets = d.assets.filter(function (a) { return ASSET_URL.test(a.url || ''); });
      if (!d.assets.length) return;
      release = d;
      var id = detectTarget(d.assets);
      chosen = d.assets.filter(function (a) { return a.target === id; })[0];
      document.getElementById('dl').href = chosen.url;
      renderMeta();
    })
    .catch(function () { /* the button keeps pointing at /download, which the server resolves */ });

  // ---- licence: the full MIT text in a dialog instead of a trip to GitHub
  var licence = document.getElementById('license');
  document.getElementById('license-open').addEventListener('click', function () {
    if (typeof licence.showModal === 'function') licence.showModal();
    else window.open('https://github.com/pepperonas/brutus/blob/main/LICENSE', '_blank', 'noopener');
  });
  document.getElementById('license-close').addEventListener('click', function () { licence.close(); });
  // A click on the backdrop lands on the dialog element itself, outside its content box.
  licence.addEventListener('click', function (e) {
    if (e.target !== licence) return;
    var r = licence.getBoundingClientRect();
    if (e.clientX < r.left || e.clientX > r.right || e.clientY < r.top || e.clientY > r.bottom) licence.close();
  });

  // ---- chrome
  var bar = document.querySelector('.bar');
  function onScroll() { bar.classList.toggle('solid', window.scrollY > 24); }
  window.addEventListener('scroll', onScroll, { passive: true });
  onScroll();

  document.getElementById('year').textContent = new Date().getFullYear();

  if ('IntersectionObserver' in window && !matchMedia('(prefers-reduced-motion: reduce)').matches) {
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (e) {
        if (!e.isIntersecting) return;
        var el = e.target;
        el.classList.add('in');
        io.unobserve(el);
        // Hand the element back to its own transitions (card hover) once it has arrived.
        setTimeout(function () { el.classList.remove('reveal', 'in'); el.style.transitionDelay = ''; }, 1000);
      });
    }, { rootMargin: '0px 0px -8% 0px' });
    document.querySelectorAll('.section .card, .section h2, .shots picture, .platforms li').forEach(function (el, i) {
      el.classList.add('reveal');
      el.style.transitionDelay = (i % 3) * 60 + 'ms';
      io.observe(el);
    });
  }

  // Dialogs close on a click outside their box (the backdrop is the dialog element itself).
  function closeOnBackdrop(d) {
    d.addEventListener('click', function (e) {
      if (e.target !== d) return;
      var r = d.getBoundingClientRect();
      if (e.clientX < r.left || e.clientX > r.right || e.clientY < r.top || e.clientY > r.bottom) d.close();
    });
  }

  // Gallery: a click on a mockup opens it large, with the (already translated) caption.
  (function () {
    var view = document.getElementById('shot-view');
    if (!view || typeof view.showModal !== 'function') return;
    var img = document.getElementById('shot-view-img');
    var webp = document.getElementById('shot-view-webp');
    var title = document.getElementById('shot-view-title');
    var text = document.getElementById('shot-view-text');
    document.querySelectorAll('.shot-open').forEach(function (b) {
      b.addEventListener('click', function () {
        var fig = b.closest('.shot');
        var small = b.querySelector('img');
        var source = b.querySelector('source');
        webp.srcset = source ? source.srcset : '';
        img.src = small.currentSrc || small.src;
        img.alt = small.alt;
        title.innerHTML = fig.querySelector('figcaption h3').innerHTML;
        text.innerHTML = fig.querySelector('figcaption p').innerHTML;
        view.showModal();
      });
    });
    document.getElementById('shot-view-close').addEventListener('click', function () { view.close(); });
    closeOnBackdrop(view);
  })();

  // Easter eggs: a LONG press on the footer's copyright opens them. A short click does nothing, so
  // the text behaves like text; only the cursor hints that it is more.
  (function () {
    var trigger = document.getElementById('egg-trigger');
    var eggs = document.getElementById('eggs');
    if (!trigger || !eggs || typeof eggs.showModal !== 'function') return;
    var HOLD_MS = 700;
    var timer = null;
    function cancel() { if (timer) { clearTimeout(timer); timer = null; } }
    trigger.addEventListener('pointerdown', function (e) {
      if (e.button !== 0) return;
      cancel();
      timer = setTimeout(function () { timer = null; eggs.showModal(); }, HOLD_MS);
    });
    ['pointerup', 'pointerleave', 'pointercancel'].forEach(function (t) { trigger.addEventListener(t, cancel); });
    // A long touch would otherwise open the system's context menu over the dialog.
    trigger.addEventListener('contextmenu', function (e) { e.preventDefault(); });
    document.getElementById('eggs-close').addEventListener('click', function () { eggs.close(); });
    closeOnBackdrop(eggs);
  })();

  // Feature catalogue (optional section, filled by the release timer via SSI): filter + expand all.
  (function () {
    var list = document.getElementById('fc-list');
    if (!list) return;
    var areas = Array.prototype.slice.call(list.querySelectorAll('details.fc-area'));
    var tools = document.querySelector('.fc-tools');
    if (!areas.length) { if (tools) tools.hidden = true; return; }
    var input = document.getElementById('fc-search');
    var toggle = document.getElementById('fc-toggle');
    var empty = document.getElementById('fc-empty');
    var saved = null; // open state before the first filter, restored when the filter is cleared

    function setAll(open) { areas.forEach(function (d) { d.open = open; }); }
    function syncToggle() {
      var allOpen = areas.every(function (d) { return d.hidden || d.open; });
      toggle.querySelector('[data-i18n="fc.expand"]').hidden = allOpen;
      toggle.querySelector('[data-i18n="fc.collapse"]').hidden = !allOpen;
    }
    toggle.addEventListener('click', function () {
      setAll(!areas.every(function (d) { return d.hidden || d.open; }));
      syncToggle();
    });
    areas.forEach(function (d) { d.addEventListener('toggle', syncToggle); });

    input.addEventListener('input', function () {
      var terms = input.value.toLowerCase().split(/\s+/).filter(Boolean);
      if (terms.length && !saved) saved = areas.map(function (d) { return d.open; });
      var any = false;
      areas.forEach(function (d, i) {
        var hits = 0;
        d.querySelectorAll('li').forEach(function (li) {
          var text = li.textContent.toLowerCase();
          var ok = terms.every(function (t) { return text.indexOf(t) >= 0; });
          li.hidden = !ok;
          if (ok) hits++;
        });
        d.hidden = hits === 0;
        if (terms.length) d.open = hits > 0;
        else if (saved) d.open = saved[i];
        any = any || hits > 0;
      });
      if (!terms.length) saved = null;
      empty.hidden = any;
      syncToggle();
    });
    syncToggle();
  })();
})();
