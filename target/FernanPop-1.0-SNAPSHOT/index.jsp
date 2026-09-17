<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    boolean cuentaEliminada = "1".equals(request.getParameter("cuentaEliminada"));
%>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>FernanPop · El mercadillo del instituto</title>
<link rel="icon" href="https://raw.githubusercontent.com/Superiorfran1/imagenesFernanPop/main/ChatGPT_Image_Jun_27__2026__12_28_08_AM-removebg-preview.png" type="image/png">
<script>
  (function () {
    try {
      var saved = localStorage.getItem('fernanpop-theme');
      if (!saved) {
        saved = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
      }
      if (saved === 'dark') {
        document.documentElement.setAttribute('data-theme', 'dark');
      }
    } catch (e) {}
  })();
</script>
<style>

  :root{
    --cream:        #e8e4df;
    --paper:        #fdfcfa;
    --ink:          #3a2f37;
    --ink-soft:     #8a7a8e;
    --purple-950:   #2d1f38;
    --purple-800:   #4a2f5c;
    --purple-700:   #5c3a7a;
    --purple-500:   #7d5a9f;
    --purple-300:   #a896b8;
    --purple-100:   #d4c5de;
    --stamp:        #a84030;
    --stamp-solid:  #964028;
    --ring:         rgba(125,90,159,0.25);
    --shadow:       rgba(50,35,60,0.12);
    --serif:        Georgia, 'Times New Roman', serif;
    --sans:         -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
    --ok:           #277048;
    --ok-bg:        #e7f3ec;
    --btn-from:     #5c3a7a;
    --btn-to:       #5c3a7a;
    --accent-yellow: #b8902f;
  }

  [data-theme="dark"]{
    --cream:        #170f1c;
    --paper:        #241a30;
    --ink:          #f7f4fa;
    --ink-soft:     #b9a6c9;
    --purple-950:   #0d0712;
    --purple-800:   #4a2f5c;
    --purple-700:   #8f5fc4;
    --purple-500:   #b28ee0;
    --purple-300:   #5a3f74;
    --purple-100:   #2e2040;
    --stamp:        #ff9466;
    --ring:         rgba(178,142,224,0.35);
    --shadow:       rgba(0,0,0,0.55);
    --btn-from:     #5c3a7a;
    --btn-to:       #5c3a7a;
    --ok:           #6ee6a0;
    --ok-bg:        #123023;
    --accent-yellow: #d9ac53;
  }

  *{ box-sizing: border-box; }

  html,body{
    margin:0;
    min-height:100vh;
    font-family: var(--sans);
    background: var(--cream);
    color: var(--ink);
    transition: background-color .5s ease, color .5s ease;
  }

  body{
    display:flex;
    align-items:center;
    justify-content:center;
    padding: 40px 20px;
    position:relative;
    overflow-x:hidden;
  }



  .theme-toggle{
    position:fixed;
    top:24px; right:24px;
    width:52px; height:30px;
    border-radius:999px;
    border:1px solid var(--purple-300);
    background: var(--paper);
    cursor:pointer;
    z-index:20;
    transition: background-color .5s ease, border-color .5s ease;
  }
  .theme-toggle .knob{
    position:absolute;
    top:3px; left:4px;
    width:22px; height:22px;
    border-radius:50%;
    background: var(--purple-700);
    display:flex;
    align-items:center;
    justify-content:center;
    color:#fff;
    font-size:11px;
    transition: left .4s ease, background-color .5s ease;
  }
  [data-theme="dark"] .theme-toggle .knob{ left:25px; }
  .theme-toggle:focus-visible{ outline:2px solid var(--purple-500); outline-offset:3px; }

  .hero{
    text-align:center;
    max-width: 420px;
    display:flex;
    flex-direction:column;
    align-items:center;
    animation: rise .7s cubic-bezier(.25,.46,.45,.94) both;
  }

  @keyframes rise{
    from{ opacity:0; transform: translateY(16px); }
    to{ opacity:1; transform: translateY(0); }
  }

  .account-deleted-notice{
    display:flex;
    align-items:center;
    gap:8px;
    background: var(--ok-bg);
    color: var(--ok);
    font-size:13px;
    font-weight:600;
    padding:10px 16px;
    border-radius:999px;
    margin-bottom:22px;
  }

  .price-tag{
    position:relative;
    display:inline-flex;
    align-items:center;
    gap:6px;
    background: var(--paper);
    border:1px solid var(--purple-300);
    color: var(--purple-700);
    font-size:12px;
    font-weight:700;
    letter-spacing:.3px;
    text-transform:uppercase;
    padding:6px 14px 6px 12px;
    border-radius:4px;
    margin-bottom:22px;
    box-shadow: 0 4px 12px -6px var(--shadow);
  }
  [data-theme="dark"] .price-tag{ color: var(--purple-500); }
  .price-tag::before{
    content:"";
    width:7px; height:7px;
    border-radius:50%;
    background: var(--cream);
    border:1px solid var(--purple-300);
  }
  .price-tag .string{
    position:absolute;
    top:-16px; left:50%;
    width:1.5px; height:16px;
    background: var(--purple-300);
    transform: translateX(-50%);
  }

  .mark-logo{
    max-width:120px;
    height:auto;
    display:inline-block;
  }
  [data-theme="dark"] .mark-logo{ filter: none; }

  .brand-name{
    margin:-15px 0 15px 0;
    font-family: var(--serif);
    font-size:30px;
    font-weight:700;
    letter-spacing:.5px;
    color: var(--purple-700);
  }
  [data-theme="dark"] .brand-name{ color: var(--purple-500); }
  .brand-name .brand-pop{ color: var(--accent-yellow); }

  .tagline{
    margin:14px 0 36px;
    font-size:15.5px;
    color: var(--ink-soft);
    line-height:1.55;
    max-width:320px;
  }

  .btn-enter{
    display:inline-flex;
    align-items:center;
    gap:10px;
    padding:14px 32px;
    border:1px solid var(--purple-700);
    border-radius:8px;
    background: var(--purple-700);
    color:#fff;
    font-size:16px;
    font-weight:600;
    font-family: var(--sans);
    letter-spacing:.1px;
    text-decoration:none;
    cursor:pointer;
    transition: transform .2s ease, background-color .3s ease, box-shadow .3s ease;
    box-shadow: 0 6px 16px -6px var(--shadow);
  }
  .btn-enter:hover{ filter:brightness(1.06); transform: translateY(-2px); }
  .btn-enter:active{ transform: translateY(0); filter:brightness(.97); }
  .btn-enter:focus-visible{ outline:2px solid var(--purple-700); outline-offset:3px; }
  .btn-enter svg{ flex-shrink:0; transition: transform .2s ease; }
  .btn-enter:hover svg{ transform: translateX(3px); }

  .footnote{
    margin-top:30px;
    font-size:12px;
    color: var(--ink-soft);
    letter-spacing:.15px;
  }

  @media (prefers-reduced-motion: reduce){
    .hero{ animation:none; }
    *{ transition:none !important; }
  }

  @media (max-width:420px){
    .mark{ font-size:38px; }
  }
</style>
</head>
<body>

  <button class="theme-toggle" id="themeToggle" type="button" aria-label="Cambiar a modo oscuro" aria-pressed="false">
    <span class="knob" id="themeKnob">☾</span>
  </button>

  <main class="hero">
    <% if (cuentaEliminada) { %>
    <div class="account-deleted-notice">
      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M5 13l4 4L19 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      Tu cuenta se ha eliminado correctamente
    </div>
    <% } %>

    <img class="mark-logo" src="https://raw.githubusercontent.com/Superiorfran1/imagenesFernanPop/main/ChatGPT_Image_Jun_27__2026__12_28_08_AM-removebg-preview.png" alt="FernanPop" aria-hidden="false">
    <h1 class="brand-name">Fernan<span class="brand-pop">Pop</span></h1>
    <p class="tagline">Compra y vende. Rápido, cercano y sin comisiones.</p>

    <span class="price-tag">
      Segunda mano · Sin gastos
    </span>

    <a class="btn-enter" href="login.jsp">
      Acceder
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M5 12h14M13 6l6 6-6 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    </a>

    <p class="footnote">Hecho por Francisco Cantero Maestro</p>
  </main>

  <script>
    (function () {
      var root = document.documentElement;
      var toggle = document.getElementById('themeToggle');
      var knob = document.getElementById('themeKnob');
      var STORAGE_KEY = 'fernanpop-theme';

      function syncToggle() {
        var isDark = root.getAttribute('data-theme') === 'dark';
        knob.textContent = isDark ? '☼' : '☾';
        toggle.setAttribute('aria-pressed', isDark ? 'true' : 'false');
        toggle.setAttribute('aria-label', isDark ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro');
      }
      syncToggle();

      toggle.addEventListener('click', function () {
        var isDark = root.getAttribute('data-theme') === 'dark';
        if (isDark) {
          root.removeAttribute('data-theme');
          localStorage.setItem(STORAGE_KEY, 'light');
        } else {
          root.setAttribute('data-theme', 'dark');
          localStorage.setItem(STORAGE_KEY, 'dark');
        }
        syncToggle();
      });
    })();
  </script>

</body>
</html>
