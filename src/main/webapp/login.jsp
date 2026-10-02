<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="controller.Controller" %>
<%@ page import="models.Usuario" %>
<%@ page import="persistence.AppConfig" %>
<%@ page import="persistence.Log" %>
<%
    String error = null;

    Controller app = (Controller) session.getAttribute("app");
    if (app == null) {
        app = new Controller();
        session.setAttribute("app", app);
    }

    if ("POST".equalsIgnoreCase(request.getMethod())) {
        String correo = request.getParameter("correo");
        String clave  = request.getParameter("clave");

        if (correo == null || correo.trim().isEmpty() || clave == null || clave.trim().isEmpty()) {
            error = "Introduce tu correo y tu contraseña.";
        } else {
            Usuario u = app.login(correo.trim(), clave);

            if (u != null) {
                session.setAttribute("usuarioActivo", u);
                AppConfig.getInstance().actualizarUltimoLogin(u.getCorreoElectronico());
                Log.registrarInicioSesion(u.getCorreoElectronico());
                response.sendRedirect("dashboard.jsp");
                return;
            } else {
                error = "Correo electrónico o contraseña incorrectos.";
            }
        }
    }

    String correoMostrado = request.getParameter("correo");
    if (correoMostrado == null) correoMostrado = "";
    correoMostrado = correoMostrado
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
%>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Iniciar sesión · FernanPop</title>
<link rel="icon" href="https://raw.githubusercontent.com/Superiorfran1/FernanPop/main/imagenes/ChatGPT_Image_Jun_27__2026__12_28_08_AM-removebg-preview%281%29.png" type="image/png">
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
    --radius:       18px;
    --serif:        Georgia, 'Times New Roman', serif;
    --sans:         -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
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
  .theme-toggle:focus-visible{ outline:2px solid var(--purple-700); outline-offset:3px; }

  .brand{
    text-align:center;
    margin-bottom: 26px;
  }
  /* Logo como imagen en lugar de texto */
  .brand .mark-logo{
      max-width:120px;
    height:auto;
    display:inline-block;
  }
  [data-theme="dark"] .brand .mark-logo{ filter: none; }
  .brand .brand-name{
    margin:-15px 0 15px 0;
    font-family: var(--serif);
    font-size:26px;
    font-weight:700;
    letter-spacing:.5px;
    color: var(--purple-700);
  }
  [data-theme="dark"] .brand .brand-name{ color: var(--purple-500); }
  .brand .brand-name .brand-pop{ color: var(--accent-yellow); }
  .brand .tag{
    margin-top:4px;
    font-size:13px;
    color: var(--ink-soft);
    letter-spacing:.2px;
  }

  .ticket{
    position:relative;
    width:100%;
    max-width: 380px;
    background: var(--paper);
    border-radius: 2px;
    padding: 40px 36px 32px;
    box-shadow: 0 8px 24px -8px var(--shadow);
    animation: rise .7s cubic-bezier(.25,.46,.45,.94) both;
  }

  @keyframes rise{
    from{ opacity:0; transform: translateY(16px); }
    to{ opacity:1; transform: translateY(0); }
  }

  .ticket::before,
  .ticket::after{
    content:"";
    position:absolute;
    left:0; right:0;
    height:1px;
    background: var(--purple-100);
  }
  .ticket::before{ top:24px; }
  .ticket::after{ bottom:24px; }

  .ticket .stub-label{
    position:absolute;
    top:18px; right:-1px;
    background: var(--stamp-solid);
    color:#fff;
    font-size:10.5px;
    font-weight:700;
    letter-spacing:1.2px;
    text-transform:uppercase;
    padding:5px 10px 5px 14px;
    border-radius: 5px 0 0 5px;
    box-shadow: -1px 1px 4px var(--shadow);
  }

  .ticket h1{
    font-family: var(--serif);
    font-size:22px;
    margin:0 0 4px;
    color: var(--ink);
  }
  .ticket .sub{
    font-size:13.5px;
    color: var(--ink-soft);
    margin:0 0 26px;
  }

  .field{
    margin-bottom:20px;
  }
  .field label{
    display:block;
    font-size:12px;
    font-weight:600;
    letter-spacing:.15px;
    color: var(--purple-700);
    margin-bottom:8px;
    text-transform:uppercase;
  }
  [data-theme="dark"] .field label{ color: var(--purple-500); }

  .field input{
    width:100%;
    padding:12px 14px;
    border-radius:4px;
    border:1px solid var(--purple-100);
    background: var(--cream);
    color: var(--ink);
    font-size:15px;
    font-family: var(--sans);
    transition: border-color .3s ease, box-shadow .3s ease, background-color .5s ease;
  }
  [data-theme="dark"] .field input{ border-color: var(--purple-300); }

  .field input::placeholder{ color: var(--ink-soft); opacity:.6; }

  .field input:focus{
    outline:none;
    border-color: var(--purple-500);
    box-shadow: 0 0 0 3px var(--ring);
  }

  .field-error input{
    border-color: var(--stamp);
  }

  .alert{
    display:flex;
    align-items:flex-start;
    gap:9px;
    background: color-mix(in srgb, var(--stamp) 12%, var(--paper));
    border:1px solid color-mix(in srgb, var(--stamp) 40%, transparent);
    color: var(--stamp);
    font-size:13.5px;
    padding:11px 13px;
    border-radius:6px;
    margin-bottom:20px;
    line-height:1.4;
  }
  .alert svg{ flex-shrink:0; margin-top:1px; }

  .btn-submit{
    width:100%;
    padding:13px 16px;
    border:1px solid var(--purple-700);
    border-radius:4px;
    background: var(--purple-700);
    color:#fff;
    font-size:15px;
    font-weight:600;
    font-family: var(--sans);
    letter-spacing:.1px;
    cursor:pointer;
    transition: transform .2s ease, background-color .3s ease, box-shadow .3s ease;
    box-shadow: 0 4px 12px -4px var(--shadow);
  }
  .btn-submit:hover{ background: #6b4589; transform: translateY(-1px); }
  .btn-submit:active{ transform: translateY(0); background: #5c3a7a; }
  .btn-submit:focus-visible{ outline:2px solid var(--purple-500); outline-offset:3px; }

  .meta-row{
    display:flex;
    justify-content:space-between;
    align-items:center;
    margin-top:18px;
    font-size:13px;
  }
  .meta-row a{
    color: var(--purple-700);
    text-decoration:none;
    font-weight:600;
  }
  [data-theme="dark"] .meta-row a{ color: var(--purple-500); }
  .meta-row a:hover{ text-decoration:underline; }
  .meta-row .muted{ color: var(--ink-soft); }

  .price-stub{
    text-align:center;
    margin-top:22px;
    padding-top:16px;
    border-top:1px dashed var(--purple-100);
    font-size:11px;
    color: var(--ink-soft);
    letter-spacing:.15px;
  }

  @media (prefers-reduced-motion: reduce){
    .ticket{ animation:none; }
    *{ transition:none !important; }
  }

  @media (max-width:420px){
    .ticket{ padding:30px 24px 26px; }
  }
</style>
</head>
<body>

  <button class="theme-toggle" id="themeToggle" type="button" aria-label="Cambiar a modo oscuro" aria-pressed="false">
    <span class="knob" id="themeKnob">☾</span>
  </button>

  <main>
    <div class="brand">
      <img class="mark-logo" src="https://raw.githubusercontent.com/Superiorfran1/FernanPop/main/imagenes/ChatGPT_Image_Jun_27__2026__12_28_08_AM-removebg-preview%281%29.png" alt="FernanPop">
      <h1 class="brand-name">Fernan<span class="brand-pop">Pop</span></h1>
    </div>

    <div class="ticket">
      <span class="stub-label">Acceso</span>
      <h1>Hola de nuevo</h1>
      <p class="sub">Entra con tu correo para comprar y vender.</p>

      <% if (error != null) { %>
      <div class="alert" role="alert">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2"/>
          <path d="M12 8v5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          <circle cx="12" cy="16" r="1" fill="currentColor"/>
        </svg>
        <span><%= error %></span>
      </div>
      <% } %>

      <form method="post" action="login.jsp" novalidate>
        <div class="field<%= error != null ? " field-error" : "" %>">
          <label for="correo">Correo electrónico</label>
          <input
            type="email"
            id="correo"
            name="correo"
            placeholder="tunombre@instituto.es"
            value="<%= correoMostrado %>"
            autocomplete="username"
            required
            autofocus>
        </div>

        <div class="field<%= error != null ? " field-error" : "" %>">
          <label for="clave">Contraseña</label>
          <input
            type="password"
            id="clave"
            name="clave"
            placeholder="••••••••"
            autocomplete="current-password"
            required>
        </div>

        <button class="btn-submit" type="submit">Entrar a FernanPop</button>
      </form>

      <div class="meta-row">
        <span class="muted">¿Primera vez aquí?</span>
        <a href="registro.jsp">Crear cuenta</a>
      </div>

      <div class="price-stub">FERNANPOP · LOGIN · </div>
    </div>
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


