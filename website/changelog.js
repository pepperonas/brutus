// Changelog dialog. The server copies CHANGELOG.md from GitHub next to this page (the release timer,
// every 15 minutes), so it is always current without visitors calling GitHub themselves.
// A deliberately small Markdown renderer for what changelogs actually use: headings, nested bullet
// and numbered lists, paragraphs, fenced code, pipe tables, **bold**, *emphasis*, `code`,
// [links](https://…), <https://…>. Every piece of text is escaped first; only http(s) links become
// anchors.
//
// Rendering is paged by version: the newest BATCH_FIRST sections render when the dialog opens, the
// rest on demand. A long-lived project's changelog runs to hundreds of releases and more than half a
// megabyte — rendering all of it at once built ~11 000 DOM nodes and stalled older machines.
(function () {
  'use strict';

  var BATCH_FIRST = 10;
  var BATCH_MORE = 25;

  function esc(s) {
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }

  function inline(raw) {
    var codes = [];
    // Code spans first, so nothing inside them is treated as formatting.
    var s = raw.replace(/`([^`]+)`/g, function (_, c) { codes.push(c); return '\u0000' + (codes.length - 1) + '\u0000'; });
    s = esc(s);
    s = s.replace(/\[([^\]]+)\]\((https?:\/\/[^)\s]+)\)/g, function (_, t, u) {
      return '<a href="' + u + '" target="_blank" rel="noopener noreferrer">' + t + '</a>';
    });
    s = s.replace(/&lt;(https?:\/\/[^\s&]+)&gt;/g, function (_, u) {
      return '<a href="' + u + '" target="_blank" rel="noopener noreferrer">' + u + '</a>';
    });
    s = s.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    s = s.replace(/(^|[^*\w])\*([^*\s][^*]*?)\*(?!\w)/g, '$1<em>$2</em>');
    s = s.replace(/\u0000(\d+)\u0000/g, function (_, i) { return '<code>' + esc(codes[+i]) + '</code>'; });
    return s;
  }

  // "[1.11.0] - 2026-09-25" — the separator may be a hyphen, an en dash or an em dash; changelogs
  // switch between them over the years, and a missed one turns every later release into a sub-heading.
  var VERSION = /^\[([^\]]+)\](?:\s*[-–—]\s*(.*))?$/;

  // Split into one chunk per "## " section. The preamble before the first section and
  // reference-style link definitions are dropped; an empty section (typically "Unreleased") too.
  function sections(md) {
    var lines = md.replace(/\r/g, '').split('\n')
      .filter(function (l) { return !/^\[[^\]]+\]:\s+https?:\/\//.test(l); });
    var out = [];
    var cur = null;
    lines.forEach(function (l) {
      if (/^## /.test(l)) { cur = [l]; out.push(cur); } else if (cur) { cur.push(l); }
    });
    return out.filter(function (s) { return s.slice(1).some(function (l) { return /\S/.test(l); }); });
  }

  function isTableRow(l) { return /^\s*\|.*\|\s*$/.test(l); }
  function isTableRule(l) { return /^\s*\|[\s:|-]+\|\s*$/.test(l) && /-/.test(l); }
  function cells(l) { return l.trim().replace(/^\||\|$/g, '').split('|').map(function (c) { return c.trim(); }); }

  function renderSection(lines) {
    var out = [];
    var para = [];
    var stack = []; // open lists: {indent, tag}
    var item = null; // text of the list item being collected
    var fence = null; // lines of an open ``` block
    var table = null; // rows of a pipe table being collected

    function flushPara() {
      if (para.length) { out.push('<p>' + inline(para.join(' ')) + '</p>'); para = []; }
    }
    function flushItem() {
      if (item !== null) { out.push(inline(item)); item = null; }
    }
    function closeLists(toIndent) {
      flushItem();
      while (stack.length && stack[stack.length - 1].indent >= toIndent) {
        out.push('</li></' + stack.pop().tag + '>');
      }
    }
    function flushTable() {
      if (!table) return;
      var rows = table.filter(function (r) { return !isTableRule(r); }).map(cells);
      var head = table.length > 1 && isTableRule(table[1]) ? rows.shift() : null;
      var html = '<div class="cl-table"><table>';
      if (head) html += '<thead><tr>' + head.map(function (c) { return '<th>' + inline(c) + '</th>'; }).join('') + '</tr></thead>';
      html += '<tbody>' + rows.map(function (r) {
        return '<tr>' + r.map(function (c) { return '<td>' + inline(c) + '</td>'; }).join('') + '</tr>';
      }).join('') + '</tbody></table></div>';
      out.push(html);
      table = null;
    }

    lines.forEach(function (line) {
      if (fence) {
        if (/^\s*```/.test(line)) { out.push('<pre><code>' + esc(fence.join('\n')) + '</code></pre>'); fence = null; }
        else fence.push(line);
        return;
      }
      if (/^\s*```/.test(line)) { flushPara(); flushItem(); flushTable(); fence = []; return; }
      if (isTableRow(line)) { flushPara(); flushItem(); (table = table || []).push(line); return; }
      flushTable();

      var h = /^(#{2,6}) (.*)$/.exec(line);
      var li = /^( *)([-*]|\d+\.) (.*)$/.exec(line);
      if (h) {
        flushPara(); closeLists(0);
        var ver = VERSION.exec(h[2]);
        if (h[1].length === 2 && ver) {
          out.push('<h3 class="cl-version"><span>' + esc(ver[1]) + '</span>' +
            (ver[2] ? '<time>' + esc(ver[2]) + '</time>' : '') + '</h3>');
        } else if (h[1].length === 2) {
          out.push('<h3 class="cl-version"><span>' + inline(h[2]) + '</span></h3>');
        } else {
          out.push('<h4>' + inline(h[2]) + '</h4>');
        }
      } else if (li) {
        flushPara();
        var indent = li[1].length;
        var tag = /\d/.test(li[2]) ? 'ol' : 'ul';
        flushItem();
        var top = stack[stack.length - 1];
        if (!top || indent > top.indent) {
          out.push('<' + tag + '><li>');
          stack.push({ indent: indent, tag: tag });
        } else {
          closeLists(indent + 1);
          top = stack[stack.length - 1];
          if (top && top.indent === indent && top.tag === tag) {
            out.push('</li><li>');
          } else {
            if (top && top.indent === indent) out.push('</li></' + stack.pop().tag + '>');
            out.push('<' + tag + '><li>');
            stack.push({ indent: indent, tag: tag });
          }
        }
        item = li[3];
      } else if (/^\s*$/.test(line)) {
        flushPara();
        flushItem();
      } else if (stack.length && /^ +/.test(line)) {
        // Continuation of the current list item.
        if (item === null) { item = line.trim(); } else { item += ' ' + line.trim(); }
      } else {
        closeLists(0);
        para.push(line.trim());
      }
    });
    if (fence) out.push('<pre><code>' + esc(fence.join('\n')) + '</code></pre>');
    flushTable(); flushPara(); closeLists(0);
    return out.join('\n');
  }

  var api = { sections: sections, renderSection: renderSection, inline: inline, VERSION: VERSION,
    BATCH_FIRST: BATCH_FIRST, BATCH_MORE: BATCH_MORE };
  if (typeof document === 'undefined') {
    if (typeof module !== 'undefined') module.exports = api;
    return;
  }

  var dialog = document.getElementById('changelog');
  var body = document.getElementById('changelog-body');
  var moreLabel = document.getElementById('changelog-more');
  var loaded = false;
  var pending = [];

  function renderNext(n) {
    var old = body.querySelector('.cl-more');
    if (old) old.remove();
    var html = pending.splice(0, n).map(renderSection).join('\n');
    body.insertAdjacentHTML('beforeend', html);
    if (pending.length) {
      var b = document.createElement('button');
      b.type = 'button';
      b.className = 'cl-more';
      b.textContent = ((moreLabel && moreLabel.textContent) || 'Older versions') + ' (' + pending.length + ')';
      b.addEventListener('click', function () { renderNext(BATCH_MORE); });
      body.appendChild(b);
    }
  }

  function load() {
    if (loaded) return;
    body.setAttribute('aria-busy', 'true');
    fetch('changelog.md', { cache: 'no-cache' })
      .then(function (r) { if (!r.ok) throw new Error(r.status); return r.text(); })
      .then(function (md) {
        pending = sections(md);
        if (!pending.length) throw new Error('empty');
        body.innerHTML = '';
        renderNext(BATCH_FIRST);
        loaded = true;
      })
      .catch(function () {
        body.innerHTML = '<p>' + esc(document.getElementById('changelog-error').textContent) +
          ' <a href="https://github.com/pepperonas/brutus/blob/main/CHANGELOG.md" target="_blank" rel="noopener noreferrer">GitHub</a></p>';
      })
      .then(function () { body.removeAttribute('aria-busy'); });
  }

  document.getElementById('changelog-open').addEventListener('click', function () {
    if (typeof dialog.showModal !== 'function') {
      window.open('https://github.com/pepperonas/brutus/blob/main/CHANGELOG.md', '_blank', 'noopener');
      return;
    }
    dialog.showModal();
    load();
  });
  document.getElementById('changelog-close').addEventListener('click', function () { dialog.close(); });
  dialog.addEventListener('click', function (e) {
    if (e.target !== dialog) return;
    var r = dialog.getBoundingClientRect();
    if (e.clientX < r.left || e.clientX > r.right || e.clientY < r.top || e.clientY > r.bottom) dialog.close();
  });

})();
