<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="controller.Controller" %>
<%@ page import="models.Usuario" %>
<%@ page import="models.Producto" %>
<%@ page import="models.Trato" %>
<%@ page import="persistence.AppConfig" %>
<%@ page import="persistence.Log" %>
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

    if ("cerrarSesion".equals(request.getParameter("accion"))) {
        Log.registrarCierreSesion(u.getCorreoElectronico());
        session.removeAttribute("usuarioActivo");
        response.sendRedirect("login.jsp");
        return;
    }

    boolean esAdmin = u.getCorreoElectronico().equalsIgnoreCase(
            AppConfig.getInstance().getAdminCorreoElectronico());


    ArrayList<Producto> enVenta = u.getEnVenta();
    ArrayList<Trato> ventas = u.getVentas();
    ArrayList<Trato> compras = u.getCompras();
    int pendientes = u.getValoracionesPendientes().size();

    java.util.function.Function<String, String> esc = (s) -> {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    };

    SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy", new java.util.Locale("es", "ES"));

    String inicial = (u.getNombre() != null && !u.getNombre().isEmpty())
            ? u.getNombre().substring(0, 1).toUpperCase()
            : "?";
%>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Tu panel · FernanPop</title>
<link rel="icon" href="https://raw.githubusercontent.com/Superiorfran1/imagenesFernanPop/main/ChatGPT_Image_Jun_27__2026__12_28_08_AM-removebg-preview.png" type="image/png">
<script>
  // Se aplica ANTES del CSS para evitar el parpadeo del tema claro
  // al cargar la página si el usuario tenía guardado el modo oscuro.
  (function () {
    try {
      var saved = localStorage.getItem('fernanpop-theme');
      if (!saved) {
        saved = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
      }
      if (saved === 'dark') {
        document.documentElement.setAttribute('data-theme', 'dark');
      }
    } catch (e) { /* localStorage no disponible: se queda en modo claro */ }
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
    --btn-from:     #5c3a7a;
    --btn-to:       #5c3a7a;
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
    position:relative;
    overflow-x:hidden;
    padding-bottom: 60px;
  }

  /* Resplandor ambiental morado, muy sutil, en la esquina superior */
  body::before{
    content:"";
    position:fixed;
    top:-25%; left:50%;
    width:140vmax; height:140vmax;
    transform: translateX(-50%);
    background: radial-gradient(circle at 50% 0%, var(--purple-300) 0%, transparent 45%);
    opacity:.16;
    pointer-events:none;
    transition: opacity .35s ease;
  }
  [data-theme="dark"] body::before{ opacity:.12; }

  .wrap{
    position:relative;
    max-width: 920px;
    margin: 0 auto;
    padding: 28px 24px 0;
  }

  .topbar{
    display:flex;
    align-items:center;
    justify-content:space-between;
    gap:16px;
    margin-bottom: 30px;
  }
  .topbar .mark-logo{
    max-width:100px;
    height:auto;
    margin: -50px 0 -50px 0;
    display:inline-block;
  }
  [data-theme="dark"] .topbar .mark-logo{ filter: none; }

  .topbar-right{
    display:flex;
    align-items:center;
    gap:14px;
  }

  .theme-toggle{
    position:relative;
    width:52px; height:30px;
    border-radius:999px;
    border:1px solid var(--purple-300);
    background: var(--paper);
    cursor:pointer;
    flex-shrink:0;
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

  .avatar-chip{
    display:flex;
    align-items:center;
    gap:10px;
    padding:6px 14px 6px 6px;
    background: var(--paper);
    border:1px solid var(--purple-100);
    border-radius:999px;
    text-decoration:none;
    cursor:pointer;
    transition: border-color .2s ease;
  }
  .avatar-chip:hover{ border-color: var(--purple-500); }
  .avatar-chip:focus-visible{ outline:2px solid var(--purple-500); outline-offset:2px; }
  [data-theme="dark"] .avatar-chip{ border-color: var(--purple-300); }
  .avatar-chip .initials{
    width:30px; height:30px;
    border-radius:50%;
    background: var(--purple-700);
    color:#fff;
    display:flex;
    align-items:center;
    justify-content:center;
    font-size:12.5px;
    font-weight:700;
  }
  .avatar-chip .name{
    font-size:13.5px;
    font-weight:600;
    color: var(--ink);
  }

  .btn-logout{
    background:none;
    border:1px solid var(--purple-100);
    color: var(--ink-soft);
    font-family: var(--sans);
    font-size:13px;
    font-weight:600;
    padding:8px 14px;
    border-radius:4px;
    cursor:pointer;
    display:flex;
    align-items:center;
    gap:6px;
    transition: border-color .2s ease, color .2s ease;
  }
  [data-theme="dark"] .btn-logout{ border-color: var(--purple-300); }
  .btn-logout:hover{ border-color: var(--stamp); color: var(--stamp); }
  .btn-logout:focus-visible{ outline:2px solid var(--purple-500); outline-offset:2px; }

  .admin-banner{
    display:flex;
    align-items:center;
    gap:10px;
    background: var(--stamp-solid);
    color:#fff;
    font-size:13px;
    font-weight:600;
    padding:10px 16px;
    border-radius:4px;
    margin-bottom:24px;
    text-decoration:none;
    cursor:pointer;
    transition: filter .2s ease;
  }
  .admin-banner:hover{ filter:brightness(1.08); }
  .admin-banner:focus-visible{ outline:2px solid #fff; outline-offset:2px; }

  .welcome h1{
    font-family: var(--serif);
    font-size:26px;
    font-weight:700;
    margin:0 0 6px;
    color: var(--ink);
  }
  .welcome p{
    margin:0 0 28px;
    font-size:14.5px;
    color: var(--ink-soft);
  }

  .stats{
    display:grid;
    grid-template-columns: repeat(4, 1fr);
    gap:14px;
    margin-bottom: 32px;
  }
  .stat-card{
    background: var(--paper);
    border:1px solid var(--purple-100);
    border-radius:14px;
    padding:18px 16px;
  }
  [data-theme="dark"] .stat-card{ border-color: var(--purple-300); }
  .stat-card .num{
    font-family: var(--serif);
    font-size:30px;
    font-weight:700;
    color: var(--purple-800);
    line-height:1;
  }
  [data-theme="dark"] .stat-card .num{ color: var(--purple-500); }
  .stat-card.alert-card .num{ color: var(--stamp); }
  .stat-card .label{
    margin-top:6px;
    font-size:12.5px;
    color: var(--ink-soft);
    letter-spacing:.1px;
  }

  .section{
    margin-bottom: 32px;
  }
  .section-head{
    display:flex;
    align-items:baseline;
    justify-content:space-between;
    margin-bottom:14px;
  }
  .section-head h2{
    font-family: var(--serif);
    font-size:18px;
    font-weight:700;
    margin:0;
    color: var(--ink);
  }
  .section-head a{
    font-size:13px;
    font-weight:600;
    color: var(--purple-700);
    text-decoration:none;
  }
  [data-theme="dark"] .section-head a{ color: var(--purple-500); }
  .section-head a:hover{ text-decoration:underline; }

  .quick-grid{
    display:grid;
    grid-template-columns: repeat(4, 1fr);
    gap:14px;
  }
  .quick-card{
    background: var(--paper);
    border:1px solid var(--purple-100);
    border-radius:14px;
    padding:20px 18px;
    text-decoration:none;
    display:flex;
    flex-direction:column;
    gap:10px;
    transition: border-color .2s ease, transform .15s ease, box-shadow .2s ease;
  }
  [data-theme="dark"] .quick-card{ border-color: var(--purple-300); }
  .quick-card:hover{
    border-color: var(--purple-500);
    transform: translateY(-2px);
    box-shadow: 0 14px 26px -14px var(--shadow);
  }
  .quick-card:focus-visible{ outline:2px solid var(--purple-500); outline-offset:2px; }
  .quick-card .icon{
    width:36px; height:36px;
    border-radius:4px;
    background: var(--purple-100);
    color: var(--purple-700);
    display:flex;
    align-items:center;
    justify-content:center;
  }
  [data-theme="dark"] .quick-card .icon{ color: var(--purple-500); }
  .quick-card .title{
    font-size:14.5px;
    font-weight:600;
    color: var(--ink);
  }
  .quick-card .desc{
    font-size:12.5px;
    color: var(--ink-soft);
    line-height:1.4;
  }

  .product-list{
    background: var(--paper);
    border:1px solid var(--purple-100);
    border-radius:14px;
    overflow:hidden;
  }
  [data-theme="dark"] .product-list{ border-color: var(--purple-300); }
  .product-row{
    display:flex;
    align-items:center;
    justify-content:space-between;
    gap:14px;
    padding:14px 18px;
    border-bottom:1px solid var(--purple-100);
  }
  [data-theme="dark"] .product-row{ border-color: var(--purple-300); }
  .product-row:last-child{ border-bottom:none; }
  .product-row .info .name{
    font-size:14.5px;
    font-weight:600;
    color: var(--ink);
    margin:0 0 3px;
  }
  .product-row .info .meta{
    font-size:12.5px;
    color: var(--ink-soft);
  }
  .product-row .price{
    font-family: var(--serif);
    font-size:17px;
    font-weight:700;
    color: var(--purple-800);
    white-space:nowrap;
  }
  [data-theme="dark"] .product-row .price{ color: var(--purple-500); }

  .badge{
    display:inline-block;
    font-size:11px;
    font-weight:700;
    letter-spacing:.15px;
    text-transform:uppercase;
    padding:3px 9px;
    border-radius:999px;
    background: var(--purple-100);
    color: var(--purple-800);
    margin-top:4px;
  }
  [data-theme="dark"] .badge{ color: var(--purple-500); }

  .pending-banner{
    display:flex;
    align-items:center;
    justify-content:space-between;
    gap:16px;
    background: color-mix(in srgb, var(--stamp) 10%, var(--paper));
    border:1px solid color-mix(in srgb, var(--stamp) 35%, transparent);
    border-radius:14px;
    padding:16px 20px;
    margin-bottom:32px;
  }
  .pending-banner .text{ font-size:14px; color: var(--stamp); font-weight:600; }
  .pending-banner .sub{ font-size:12.5px; color: var(--ink-soft); font-weight:400; margin-top:2px; }
  .pending-banner a{
    flex-shrink:0;
    background: var(--stamp-solid);
    color:#fff;
    font-size:13px;
    font-weight:600;
    padding:9px 16px;
    border-radius:4px;
    text-decoration:none;
  }

  .empty{
    background: var(--paper);
    border:1px dashed var(--purple-300);
    border-radius:14px;
    padding:28px 20px;
    text-align:center;
    color: var(--ink-soft);
    font-size:13.5px;
  }

  @media (prefers-reduced-motion: reduce){
    *{ transition:none !important; }
  }

  @media (max-width: 760px){
    .stats{ grid-template-columns: repeat(2, 1fr); }
    .quick-grid{ grid-template-columns: repeat(2, 1fr); }
  }

  @media (max-width: 480px){
    .topbar{ flex-wrap:wrap; }
    .avatar-chip .name{ display:none; }
    .avatar-chip{ gap:0; padding:3px; }
    .stats{ grid-template-columns: 1fr 1fr; }
    .quick-grid{ grid-template-columns: 1fr; }
    .product-row{ flex-wrap:wrap; }
  }
</style>
</head>
<body>

  <div class="wrap">

    <div class="topbar">
      <img class="mark-logo" src="https://raw.githubusercontent.com/Superiorfran1/imagenesFernanPop/main/ChatGPT_Image_Jun_27__2026__12_28_08_AM-removebg-preview.png" alt="FernanPop">
      <div class="topbar-right">
        <button class="theme-toggle" id="themeToggle" type="button" aria-label="Cambiar a modo oscuro" aria-pressed="false">
          <span class="knob" id="themeKnob">☾</span>
        </button>
        <a class="avatar-chip" href="perfil.jsp" title="Ver mi perfil">
          <span class="initials"><%= esc.apply(inicial) %></span>
          <span class="name"><%= esc.apply(u.getNombre()) %></span>
        </a>
        <form method="post" action="dashboard.jsp" style="margin:0;">
          <input type="hidden" name="accion" value="cerrarSesion">
          <button class="btn-logout" type="submit">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            Salir
          </button>
        </form>
      </div>
    </div>

    <% if (esAdmin) { %>
    <a class="admin-banner" href="admin.jsp">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M12 2l8 4v6c0 5-3.5 8.5-8 10-4.5-1.5-8-5-8-10V6l8-4z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      Sesión de administrador · Ir al panel
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true" style="margin-left:auto;">
        <path d="M9 6l6 6-6 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    </a>
    <% } %>

    <div class="welcome">
      <h1>Hola, <%= esc.apply(u.getNombre()) %></h1>
      <p>Esto es lo que está pasando en tu cuenta de FernanPop.</p>
    </div>

    <div class="stats">
      <div class="stat-card">
        <div class="num"><%= enVenta.size() %></div>
        <div class="label">En venta</div>
      </div>
      <div class="stat-card">
        <div class="num"><%= ventas.size() %></div>
        <div class="label">Vendidos</div>
      </div>
      <div class="stat-card">
        <div class="num"><%= compras.size() %></div>
        <div class="label">Comprados</div>
      </div>
      <div class="stat-card<%= pendientes > 0 ? " alert-card" : "" %>">
        <div class="num"><%= pendientes %></div>
        <div class="label">Por valorar</div>
      </div>
    </div>

    <% if (pendientes > 0) { %>
    <div class="pending-banner">
      <div>
        <div class="text">Tienes <%= pendientes %> compra<%= pendientes == 1 ? "" : "s" %> por valorar</div>
        <div class="sub">Cuenta cómo te fue para ayudar a otros compradores.</div>
      </div>
      <a href="valoraciones.jsp">Valorar ahora</a>
    </div>
    <% } %>

    <div class="section">
      <div class="section-head"><h2>Accesos rápidos</h2></div>
      <div class="quick-grid">
        <a class="quick-card" href="productos-nuevo.jsp">
          <span class="icon" aria-hidden="true">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
          </span>
          <span class="title">Vender algo</span>
          <span class="desc">Publica un producto nuevo en el mercadillo.</span>
        </a>
        <a class="quick-card" href="mis-productos.jsp">
          <span class="icon" aria-hidden="true">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M4 7h16M4 12h16M4 17h10" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
          </span>
          <span class="title">Mis productos</span>
          <span class="desc">Edita o retira lo que tienes en venta.</span>
        </a>
        <a class="quick-card" href="buscar.jsp">
          <span class="icon" aria-hidden="true">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none"><circle cx="11" cy="11" r="7" stroke="currentColor" stroke-width="2"/><path d="M21 21l-4.3-4.3" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
          </span>
          <span class="title">Buscar productos</span>
          <span class="desc">Explora lo que vende el resto del instituto.</span>
        </a>
        <a class="quick-card" href="historial.jsp">
          <span class="icon" aria-hidden="true">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M12 8v4l3 3M3 12a9 9 0 1 0 3-6.7L3 8" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </span>
          <span class="title">Historial</span>
          <span class="desc">Repasa tus compras y ventas pasadas.</span>
        </a>
      </div>
    </div>

    <div class="section">
      <div class="section-head">
        <h2>Tus productos en venta</h2>
        <% if (!enVenta.isEmpty()) { %><a href="mis-productos.jsp">Ver todos</a><% } %>
      </div>

      <% if (enVenta.isEmpty()) { %>
      <div class="empty">Todavía no tienes nada en venta. <a href="productos-nuevo.jsp" style="color:var(--purple-700); font-weight:600; text-decoration:none;">Publica tu primer producto</a>.</div>
      <% } else { %>
      <div class="product-list">
        <% int mostrados = 0;
           for (Producto p : enVenta) {
             if (mostrados >= 5) break;
             mostrados++;
        %>
        <div class="product-row">
          <div class="info">
            <p class="name"><%= esc.apply(p.getNombre()) %></p>
            <p class="meta">
              <%= p.getInteresados().size() %> interesado<%= p.getInteresados().size() == 1 ? "" : "s" %>
              <% if (p.getEstado() != null && !p.getEstado().isEmpty()) { %>
                · <span class="badge"><%= esc.apply(p.getEstado()) %></span>
              <% } %>
            </p>
          </div>
          <div class="price"><%= String.format("%.2f €", p.getPrecio()) %></div>
        </div>
        <% } %>
      </div>
      <% } %>
    </div>

    <% if (!ventas.isEmpty() || !compras.isEmpty()) { %>
    <div class="section">
      <div class="section-head">
        <h2>Última actividad</h2>
        <a href="historial.jsp">Ver historial completo</a>
      </div>
      <div class="product-list">
        <%
          ArrayList<Trato> recientes = new ArrayList<>();
          recientes.addAll(ventas);
          recientes.addAll(compras);
          recientes.sort((a, b) -> b.getFecha().compareTo(a.getFecha()));
          int mostradosT = 0;
          for (Trato t : recientes) {
            if (mostradosT >= 5) break;
            mostradosT++;
            boolean fueVenta = t.getCorreoVendedor().equalsIgnoreCase(u.getCorreoElectronico());
        %>
        <div class="product-row">
          <div class="info">
            <p class="name"><%= esc.apply(t.getProducto().getNombre()) %></p>
            <p class="meta">
              <%= fueVenta ? "Vendido" : "Comprado" %> el <%= sdf.format(t.getFecha().getTime()) %>
            </p>
          </div>
          <div class="price"><%= String.format("%.2f €", t.getPrecio()) %></div>
        </div>
        <% } %>
      </div>
    </div>
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





