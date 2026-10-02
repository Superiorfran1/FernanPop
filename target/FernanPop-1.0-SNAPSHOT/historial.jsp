<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="controller.Controller" %>
<%@ page import="models.Usuario" %>
<%@ page import="models.Trato" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%
    Usuario u = (Usuario) session.getAttribute("usuarioActivo");
    if (u == null) {
        response.sendRedirect("login.jsp");
        return;
    }

    Controller app = (Controller) session.getAttribute("app");
    if (app == null) {
        app = new Controller();
        session.setAttribute("app", app);
    }

    ArrayList<Trato> ventas = new ArrayList<>(u.getVentas());
    ArrayList<Trato> compras = new ArrayList<>(u.getCompras());
    ventas.sort((a, b) -> b.getFecha().compareTo(a.getFecha()));
    compras.sort((a, b) -> b.getFecha().compareTo(a.getFecha()));

    String tab = request.getParameter("tab");
    if (tab == null || (!tab.equals("ventas") && !tab.equals("compras"))) {
        tab = "ventas";
    }

    java.util.function.Function<String, String> esc = (s) -> {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    };

    SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy", new java.util.Locale("es", "ES"));
%>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Historial · FernanPop</title>
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
    } catch (e) { }
  })();
</script>
<style>

  :root{
    --cream:        #e8e4df;
    --paper:        #fdfcfa;
    --ink:          #3a2f37;
    --ink-soft:     #8a7a8e;
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
    --btn-from:     #5c3a7a;
    --btn-to:       #5c3a7a;
    --gold:         #8a5f00;
    --gold-bg:      #fdf3dd;
  }

  [data-theme="dark"]{
    --cream:        #170f1c;
    --paper:        #241a30;
    --ink:          #f7f4fa;
    --ink-soft:     #b9a6c9;
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
    --gold:         #f0c34c;
    --gold-bg:      #2f2208;
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

  body{ position:relative; overflow-x:hidden; padding-bottom:60px; }

  body::before{
    content:"";
    position:fixed;
    top:-25%; left:50%;
    width:140vmax; height:140vmax;
    transform: translateX(-50%);
    background: radial-gradient(circle at 50% 0%, var(--purple-300) 0%, transparent 45%);
    opacity:.16;
    pointer-events:none;
  }
  [data-theme="dark"] body::before{ opacity:.12; }

  .wrap{
    position:relative;
    max-width: 760px;
    margin: 0 auto;
    padding: 28px 24px 0;
  }

  .topbar{
    display:flex;
    align-items:center;
    justify-content:space-between;
    gap:16px;
    margin-bottom: 24px;
  }
  .topbar .left{ margin-left: 75px; display:flex; align-items:center; gap:14px; }
  .back-link{
    display:flex;
    align-items:center;
    gap:6px;
    font-size:13px;
    font-weight:600;
    color: var(--ink-soft);
    text-decoration:none;
  }
  .back-link:hover{ color: var(--purple-700); }
  [data-theme="dark"] .back-link:hover{ color: var(--purple-500); }
  .back-link:focus-visible{ outline:2px solid var(--purple-500); outline-offset:3px; border-radius:4px; }

  .mark-logo{
    max-width:100px;
    height:auto;
    display:inline-block;
    margin:-50px 0 -50px -160px;
  }
  [data-theme="dark"] .mark-logo{ filter: none; }

  .theme-toggle{
    position:relative;
    width:52px; height:30px;
    border-radius:999px;
    border:1px solid var(--purple-300);
    background: var(--paper);
    cursor:pointer;
    flex-shrink:0;
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
    transition: left .4s ease;
  }
  [data-theme="dark"] .theme-toggle .knob{ left:25px; }
  .theme-toggle:focus-visible{ outline:2px solid var(--purple-500); outline-offset:3px; }

  .page-title{ margin: 6px 0 22px; }
  .page-title h1{
    font-family: var(--serif);
    font-size:24px;
    font-weight:700;
    margin:0 0 4px;
  }
  .page-title p{
    margin:0;
    font-size:13.5px;
    color: var(--ink-soft);
  }

  .tabs{
    display:flex;
    gap:6px;
    background: var(--purple-100);
    border-radius:8px;
    padding:4px;
    margin-bottom:22px;
  }
  .tabs a{
    flex:1;
    text-align:center;
    padding:10px 14px;
    border-radius:9px;
    font-size:13.5px;
    font-weight:600;
    color: var(--purple-800);
    text-decoration:none;
    transition: background-color .2s ease, color .2s ease;
  }
  [data-theme="dark"] .tabs a{ color: var(--purple-500); }
  .tabs a.active{
    background: var(--paper);
    color: var(--purple-700);
    box-shadow: 0 4px 10px -4px var(--shadow);
  }
  [data-theme="dark"] .tabs a.active{ color: var(--purple-500); }
  .tabs a:focus-visible{ outline:2px solid var(--purple-500); outline-offset:2px; }

  .trato-card{
    background: var(--paper);
    border:1px solid var(--purple-100);
    border-radius:14px;
    padding:16px 18px;
    margin-bottom:12px;
    display:flex;
    align-items:center;
    justify-content:space-between;
    gap:14px;
  }
  [data-theme="dark"] .trato-card{ border-color: var(--purple-300); }

  .trato-card .info .name{
    font-size:15px;
    font-weight:600;
    color: var(--ink);
    margin:0 0 4px;
  }
  .trato-card .info .meta{
    font-size:12.5px;
    color: var(--ink-soft);
  }
  .trato-card .info .meta .id{
    font-family: monospace;
    font-size:11.5px;
  }

  .trato-card .right{
    display:flex;
    flex-direction:column;
    align-items:flex-end;
    gap:6px;
    flex-shrink:0;
  }
  .trato-card .price{
    font-family: var(--serif);
    font-size:17px;
    font-weight:700;
    color: var(--purple-800);
    white-space:nowrap;
  }
  [data-theme="dark"] .trato-card .price{ color: var(--purple-500); }

  .rating{
    display:inline-flex;
    align-items:center;
    gap:4px;
    font-size:12px;
    font-weight:700;
    padding:3px 10px;
    border-radius:999px;
    background: var(--gold-bg);
    color: var(--gold);
  }
  .rating.pending{
    background: color-mix(in srgb, var(--stamp) 12%, var(--paper));
    color: var(--stamp);
  }
  .rating.pending a{
    color: inherit;
    text-decoration: underline;
  }

  .empty{
    background: var(--paper);
    border:1px dashed var(--purple-300);
    border-radius:14px;
    padding:32px 20px;
    text-align:center;
    color: var(--ink-soft);
    font-size:13.5px;
  }
  .empty a{ color: var(--purple-700); font-weight:600; text-decoration:none; }
  [data-theme="dark"] .empty a{ color: var(--purple-500); }

  @media (prefers-reduced-motion: reduce){ *{ transition:none !important; } }

  @media (max-width: 480px){
    .trato-card{ flex-wrap:wrap; }
    .trato-card .right{ align-items:flex-start; width:100%; flex-direction:row; justify-content:space-between; }
  }
</style>
</head>
<body>

  <div class="wrap">

    <div class="topbar">
      <div class="left">
        <a class="back-link" href="dashboard.jsp">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M19 12H5M12 19l-7-7 7-7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          Panel
        </a>
        <img class="mark-logo" src="https://raw.githubusercontent.com/Superiorfran1/FernanPop/main/imagenes/ChatGPT_Image_Jun_27__2026__12_28_08_AM-removebg-preview%281%29.png" alt="FernanPop">
      </div>
      <button class="theme-toggle" id="themeToggle" type="button" aria-label="Cambiar a modo oscuro" aria-pressed="false">
        <span class="knob" id="themeKnob">☾</span>
      </button>
    </div>

    <div class="page-title">
      <h1>Mi historial</h1>
      <p>Todo lo que has vendido y comprado en FernanPop.</p>
    </div>

    <div class="tabs">
      <a href="historial.jsp?tab=ventas" class="<%= tab.equals("ventas") ? "active" : "" %>">Ventas (<%= ventas.size() %>)</a>
      <a href="historial.jsp?tab=compras" class="<%= tab.equals("compras") ? "active" : "" %>">Compras (<%= compras.size() %>)</a>
    </div>

    <% if (tab.equals("ventas")) { %>

      <% if (ventas.isEmpty()) { %>
      <div class="empty">Aún no has realizado ninguna venta. <a href="productos-nuevo.jsp">Publica algo</a> para empezar a vender.</div>
      <% } else {
         for (Trato t : ventas) {
           Usuario comprador = app.buscaPorCorreoElectronico(t.getCorreoComprador());
           String nombreComprador = comprador != null ? comprador.getNombre() : t.getCorreoComprador();
      %>
      <div class="trato-card">
        <div class="info">
          <p class="name"><%= esc.apply(t.getProducto().getNombre()) %></p>
          <p class="meta">
            <span class="id"><%= esc.apply(t.getID()) %></span> ·
            Vendido a <%= esc.apply(nombreComprador) %> · <%= sdf.format(t.getFecha().getTime()) %>
          </p>
        </div>
        <div class="right">
          <span class="price"><%= String.format("%.2f €", t.getPrecio()) %></span>
          <% if (t.getPuntuacion() >= 0) { %>
          <span class="rating">★ <%= t.getPuntuacion() %>/5</span>
          <% } else { %>
          <span class="rating pending">Sin valorar aún</span>
          <% } %>
        </div>
      </div>
      <% } } %>

    <% } else { %>

      <% if (compras.isEmpty()) { %>
      <div class="empty">Aún no has comprado nada. <a href="buscar.jsp">Explora el mercadillo</a>.</div>
      <% } else {
         for (Trato t : compras) {
           Usuario vendedor = app.buscaPorCorreoElectronico(t.getCorreoVendedor());
           String nombreVendedor = vendedor != null ? vendedor.getNombre() : t.getCorreoVendedor();
           boolean pendienteValorar = u.getValoracionesPendientes().contains(t.getID());
      %>
      <div class="trato-card">
        <div class="info">
          <p class="name"><%= esc.apply(t.getProducto().getNombre()) %></p>
          <p class="meta">
            <span class="id"><%= esc.apply(t.getID()) %></span> ·
            Comprado a <%= esc.apply(nombreVendedor) %> · <%= sdf.format(t.getFecha().getTime()) %>
          </p>
        </div>
        <div class="right">
          <span class="price"><%= String.format("%.2f €", t.getPrecio()) %></span>
          <% if (t.getPuntuacion() >= 0) { %>
          <span class="rating">★ <%= t.getPuntuacion() %>/5</span>
          <% } else if (pendienteValorar) { %>
          <span class="rating pending"><a href="valoraciones.jsp">Valorar ahora</a></span>
          <% } else { %>
          <span class="rating pending">Sin valorar</span>
          <% } %>
        </div>
      </div>
      <% } } %>

    <% } %>

  </div>

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





