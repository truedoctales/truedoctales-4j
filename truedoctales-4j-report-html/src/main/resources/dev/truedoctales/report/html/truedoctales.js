(function () {
  var fallbackDefaultPage = '__DEFAULT_PAGE__';
  var currentPath = '';
  var sidebar = document.getElementById('sidebar');
  var navFlyout = document.getElementById('nav-flyout');
  var flyoutHideTimer = null;
  var activeChapterEl = null;
  var pageList = [];

  // ---------------------------------------------------------------
  // Navigation JSON loader — builds the sidebar from report-nav.json
  // ---------------------------------------------------------------
  function escHtml(s) {
    return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
  }

  function numberBadge(n) {
    return n != null ? '<span class="nav-number" aria-hidden="true">' + escHtml(n) + '</span>' : '';
  }

  function buildSidebarFromNav(nav) {
    var sb = '';
    var chapters = nav.chapters || [];
    chapters.forEach(function (ch) {
      var stories = ch.stories || [];
      if (!ch.dirName) {
        // Book-level entries (no chapter folder)
        sb += '<div class="nav-group-label">' + escHtml(ch.label) + '</div>\n';
        sb += '<ul>\n';
        stories.forEach(function (s) {
          var failedClass = s.status === 'FAILURE' || s.status === 'ERROR' ? 'nav-failed' : '';
          var badge = s.hasErrors ? '<span class="nav-error-badge" title="Has failures">\u26A0\uFE0F</span>' : '';
          sb += '<li><a href="#' + escHtml(s.htmlPath) + '" class="' + failedClass + '">' + escHtml(s.title) + badge + '</a></li>\n';
        });
        sb += '</ul>\n';
      } else {
        var num = ch.dirName ? parseInt(ch.dirName.split('_')[0], 10) : null;
        var badge = isNaN(num) || num === 0 ? '' : numberBadge(num);
        var chapterHasErrors = false;
        stories.forEach(function (s) {
          if (s.hasErrors) chapterHasErrors = true;
        });
        var chapterFailedClass = chapterHasErrors ? ' nav-failed' : '';
        var chapterBadge = chapterHasErrors ? '<span class="nav-error-badge" title="Has failures">\u26A0\uFE0F</span>' : '';
        sb += '<div class="nav-chapter">\n';
        sb += '  <div class="nav-chapter-row">';
        if (ch.introPage) {
          sb += '<a href="#' + escHtml(ch.introPage) + '" class="chapter-link' + chapterFailedClass + '">' + badge + escHtml(ch.label) + '</a>';
        } else {
          sb += '<span class="chapter-link' + chapterFailedClass + '">' + badge + escHtml(ch.label) + '</span>';
        }
        sb += chapterBadge;
        sb += '<span class="flyout-arrow" aria-hidden="true">&#x203A;</span>';
        sb += '</div>\n';
        sb += '  <ul class="chapter-stories">\n';
        stories.forEach(function (s, idx) {
          var storyNum = idx + 1;
          var failedClass = s.status === 'FAILURE' || s.status === 'ERROR' ? 'nav-failed' : '';
          var storyBadge = s.hasErrors ? '<span class="nav-error-badge" title="Has failures">\u26A0\uFE0F</span>' : '';
          sb += '    <li><a href="#' + escHtml(s.htmlPath) + '" class="' + failedClass + '">' + numberBadge(storyNum) + escHtml(s.title) + storyBadge + '</a></li>\n';
        });
        sb += '  </ul>\n';
        sb += '</div>\n';
      }
    });
    return sb;
  }

  // ---------------------------------------------------------------
  // Flat ordered page list — used for prev/next navigation
  // ---------------------------------------------------------------
  function buildPageList(nav) {
    var list = [];
    var chapters = nav.chapters || [];
    chapters.forEach(function (ch) {
      if (ch.introPage) {
        list.push({ htmlPath: ch.introPage, title: ch.label });
      }
      (ch.stories || []).forEach(function (s) {
        list.push({ htmlPath: s.htmlPath, title: s.title });
      });
    });
    return list;
  }

  function renderPageNav(path) {
    var pageNav = document.getElementById('page-nav');
    if (!pageNav || !pageList.length) { return; }
    var idx = -1;
    for (var i = 0; i < pageList.length; i++) {
      if (pageList[i].htmlPath === path) { idx = i; break; }
    }
    if (idx === -1) { pageNav.innerHTML = ''; return; }
    var prev = idx > 0 ? pageList[idx - 1] : null;
    var next = idx < pageList.length - 1 ? pageList[idx + 1] : null;
    var html = '<div class="page-nav-inner">';
    if (prev) {
      html += '<a href="#' + escHtml(prev.htmlPath) + '" class="page-nav-btn page-nav-prev">'
        + '<span class="page-nav-arrow" aria-hidden="true">&#8592;</span>'
        + '<span class="page-nav-label">'
        + '<span class="page-nav-hint">Previous</span>'
        + '<span class="page-nav-title">' + escHtml(prev.title) + '</span>'
        + '</span></a>';
    } else {
      html += '<span class="page-nav-btn page-nav-prev page-nav-disabled" aria-hidden="true"></span>';
    }
    if (next) {
      html += '<a href="#' + escHtml(next.htmlPath) + '" class="page-nav-btn page-nav-next">'
        + '<span class="page-nav-label">'
        + '<span class="page-nav-hint">Next</span>'
        + '<span class="page-nav-title">' + escHtml(next.title) + '</span>'
        + '</span>'
        + '<span class="page-nav-arrow" aria-hidden="true">&#8594;</span>'
        + '</a>';
    } else {
      html += '<span class="page-nav-btn page-nav-next page-nav-disabled" aria-hidden="true"></span>';
    }
    html += '</div>';
    pageNav.innerHTML = html;
  }

  // ---------------------------------------------------------------
  function resolvePath(base, rel) {
    if (!rel || rel.charAt(0) === '#' || rel.indexOf('//') !== -1 || rel.indexOf(':') !== -1) {
      return rel;
    }
    var stack = base.split('/');
    stack.pop();
    rel.split('/').forEach(function (s) {
      if (s === '..') { stack.pop(); } else if (s !== '.') { stack.push(s); }
    });
    return stack.join('/');
  }

  // ---------------------------------------------------------------
  // Active nav
  // ---------------------------------------------------------------
  function updateActiveNav(path) {
    document.querySelectorAll('.sidebar-content a').forEach(function (a) {
      a.classList.toggle('active', a.getAttribute('href') === '#' + path);
    });
    document.querySelectorAll('#nav-flyout a').forEach(function (a) {
      a.classList.toggle('active', a.getAttribute('href') === '#' + path);
    });
  }

  // ---------------------------------------------------------------
  // Mermaid helpers
  // ---------------------------------------------------------------
  function saveMermaidSources(container) {
    container.querySelectorAll('.mermaid').forEach(function (el) {
      if (!el.dataset.mermaidSource) {
        el.dataset.mermaidSource = el.textContent.trim();
      }
    });
  }

  function resetMermaidElements(container) {
    container.querySelectorAll('.mermaid').forEach(function (el) {
      if (el.dataset.mermaidSource) {
        el.removeAttribute('data-processed');
        el.textContent = el.dataset.mermaidSource;
      }
    });
  }

  // ---------------------------------------------------------------
  // Step status classification — adds CSS classes to step blockquotes
  // ---------------------------------------------------------------
  function classifyStepBlocks(container) {
    container.querySelectorAll('blockquote').forEach(function (bq) {
      var text = bq.textContent;
      if (text.indexOf('✅') !== -1) { bq.classList.add('step', 'step-success'); }
      else if (text.indexOf('❌') !== -1) { bq.classList.add('step', 'step-failure'); }
      else if (text.indexOf('⚠️') !== -1) { bq.classList.add('step', 'step-error'); }
      else if (text.indexOf('⏭️') !== -1) { bq.classList.add('step', 'step-skipped'); }
    });
  }

  // ---------------------------------------------------------------
  // Content loader
  // ---------------------------------------------------------------
  var defaultPage = fallbackDefaultPage;

  function loadContent(path) {
    var reqPath = path || defaultPage;
    currentPath = reqPath;
    fetch(reqPath)
      .then(function (r) {
        return r.ok
          ? r.text()
          : '<article><h1>Page not found</h1><p>Could not load <code>' + reqPath
              + '</code>. <a href="#' + defaultPage + '">Return to home</a></p></article>';
      })
      .then(function (html) {
        var container = document.getElementById('page-content');
        container.innerHTML = html;
        saveMermaidSources(container);
        classifyStepBlocks(container);
        updateActiveNav(currentPath);
        renderPageNav(currentPath);
        if (typeof mermaid !== 'undefined') {
          mermaid.run({ querySelector: '#page-content .mermaid' });
        }
        document.querySelector('.content').scrollTop = 0;
      });
  }

  // ---------------------------------------------------------------
  // Flyout menu
  // ---------------------------------------------------------------
  function positionAndShow(chapter) {
    clearTimeout(flyoutHideTimer);
    var source = chapter.querySelector('.chapter-stories');
    if (!source || !source.children.length) { return; }
    navFlyout.innerHTML = source.innerHTML;
    navFlyout.querySelectorAll('a').forEach(function (a) {
      a.classList.toggle('active', a.getAttribute('href') === '#' + currentPath);
    });
    activeChapterEl = chapter;
    sidebar.classList.add('flyout-open');
    var rowRect = chapter.querySelector('.nav-chapter-row').getBoundingClientRect();
    var sidebarWidth = parseFloat(
      getComputedStyle(document.documentElement).getPropertyValue('--sidebar-width')
    ) || 290;
    navFlyout.style.top = rowRect.top + 'px';
    navFlyout.style.left = sidebarWidth + 'px';
    navFlyout.classList.add('flyout-visible');
  }

  function hideFlyout() {
    clearTimeout(flyoutHideTimer);
    navFlyout.classList.remove('flyout-visible');
    sidebar.classList.remove('flyout-open');
    activeChapterEl = null;
  }

  function scheduleFlyoutHide() {
    flyoutHideTimer = setTimeout(hideFlyout, 100);
  }

  // ---------------------------------------------------------------
  // Bind flyout, keyboard, toggle events (called after nav is ready)
  // ---------------------------------------------------------------
  function bindNavEvents() {
    document.querySelectorAll('.nav-chapter').forEach(function (chapter) {
      chapter.querySelector('.nav-chapter-row').addEventListener('mouseenter', function () {
        positionAndShow(chapter);
      });
    });

    sidebar.addEventListener('mouseleave', function (e) {
      if (navFlyout.contains(e.relatedTarget)) { return; }
      scheduleFlyoutHide();
    });
    navFlyout.addEventListener('mouseenter', function () { clearTimeout(flyoutHideTimer); });
    navFlyout.addEventListener('mouseleave', function (e) {
      if (sidebar.contains(e.relatedTarget)) { return; }
      scheduleFlyoutHide();
    });

    sidebar.addEventListener('click', function (e) {
      if (e.target.closest('a[href]')) { hideFlyout(); }
    });
    navFlyout.addEventListener('click', function (e) {
      if (e.target.closest('a[href]')) { hideFlyout(); sidebar.classList.remove('open'); }
    });
  }

  // ---------------------------------------------------------------
  // Keyboard navigation
  // ---------------------------------------------------------------
  document.addEventListener('keydown', function (e) {
    var tag = document.activeElement ? document.activeElement.tagName : '';
    if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') { return; }

    if (e.key === ':') {
      e.preventDefault();
      var first = sidebar.querySelector('.nav-chapter-row .chapter-link, .sidebar-content li a');
      if (first) { first.focus(); }
      return;
    }

    var flyoutVisible = navFlyout.classList.contains('flyout-visible');

    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
      e.preventDefault();
      var items = flyoutVisible
        ? Array.from(navFlyout.querySelectorAll('a'))
        : Array.from(sidebar.querySelectorAll('.nav-chapter-row .chapter-link, .sidebar-content > ul > li > a'));
      if (!items.length) { return; }
      var idx = items.indexOf(document.activeElement);
      idx = e.key === 'ArrowDown' ? (idx + 1) % items.length : (idx - 1 + items.length) % items.length;
      items[idx].focus();
      return;
    }

    if (e.key === 'ArrowRight') {
      e.preventDefault();
      var chapter = document.activeElement && document.activeElement.closest('.nav-chapter');
      if (chapter) {
        positionAndShow(chapter);
        var fi = navFlyout.querySelector('a');
        if (fi) { fi.focus(); }
      }
      return;
    }

    if (e.key === 'ArrowLeft' && flyoutVisible) {
      e.preventDefault();
      var chLink = activeChapterEl && activeChapterEl.querySelector('.chapter-link');
      hideFlyout();
      if (chLink) { chLink.focus(); }
      return;
    }

    if (e.key === 'Escape') {
      hideFlyout();
      if (document.activeElement && sidebar.contains(document.activeElement)) {
        document.activeElement.blur();
      }
    }
  });

  // ---------------------------------------------------------------
  // Sidebar toggles
  // ---------------------------------------------------------------
  document.getElementById('sidebar-handle').addEventListener('click', function () {
    sidebar.classList.toggle('open');
  });
  document.getElementById('sidebar-toggle').addEventListener('click', function () {
    sidebar.classList.toggle('open');
  });

  // ---------------------------------------------------------------
  // Content link intercept
  // ---------------------------------------------------------------
  document.addEventListener('click', function (e) {
    var a = e.target.closest('#page-content a[href]');
    if (!a) { return; }
    var href = a.getAttribute('href');
    if (href && href.charAt(0) !== '#' && href.indexOf('//') === -1 && href.indexOf(':') === -1) {
      e.preventDefault();
      location.hash = '#' + resolvePath(currentPath, href);
    }
  });

  window.addEventListener('hashchange', function () {
    loadContent(location.hash.slice(1));
  });

  // ---------------------------------------------------------------
  // Theme toggle
  // ---------------------------------------------------------------
  var toggle = document.getElementById('theme-toggle');
  var htmlEl = document.documentElement;
  var stored = localStorage.getItem('truedoctales-theme');
  if (stored) {
    htmlEl.setAttribute('data-bs-theme', stored);
    toggle.textContent = stored === 'dark' ? '☀️' : '🌙';
  }
  toggle.addEventListener('click', function () {
    var cur = htmlEl.getAttribute('data-bs-theme') || 'light';
    var next = cur === 'dark' ? 'light' : 'dark';
    htmlEl.setAttribute('data-bs-theme', next);
    localStorage.setItem('truedoctales-theme', next);
    toggle.textContent = next === 'dark' ? '☀️' : '🌙';
    if (typeof mermaid !== 'undefined') {
      mermaid.initialize({ startOnLoad: false, theme: next === 'dark' ? 'dark' : 'default' });
      resetMermaidElements(document.getElementById('page-content'));
      mermaid.run({ querySelector: '#page-content .mermaid' });
    }
  });

  // ---------------------------------------------------------------
  // Mermaid init
  // ---------------------------------------------------------------
  if (typeof mermaid !== 'undefined') {
    mermaid.initialize({
      startOnLoad: false,
      theme: htmlEl.getAttribute('data-bs-theme') === 'dark' ? 'dark' : 'default'
    });
  }

  // ---------------------------------------------------------------
  // Bootstrap: load report-nav.json, build sidebar, then load page
  // ---------------------------------------------------------------
  fetch('report-nav.json')
    .then(function (r) { return r.ok ? r.json() : null; })
    .then(function (nav) {
      if (nav) {
        if (nav.defaultPage) { defaultPage = nav.defaultPage; }
        if (nav.title) {
          var bt = document.querySelector('.brand-title');
          if (bt) { bt.textContent = nav.title; }
        }
        var sc = document.getElementById('sidebar-content');
        if (sc) { sc.innerHTML = buildSidebarFromNav(nav); }
        pageList = buildPageList(nav);
      }
      bindNavEvents();
      loadContent(location.hash ? location.hash.slice(1) : null);
    })
    .catch(function () {
      bindNavEvents();
      loadContent(location.hash ? location.hash.slice(1) : null);
    });
}());
