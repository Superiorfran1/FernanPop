<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="controller.Controller" %>
<%@ page import="models.Usuario" %>
<%@ page import="persistence.AppConfig" %>
<%@ page import="persistence.Persistence" %>
<%@ page import="utils.Communications" %>
<%@ page import="java.io.File" %>
<%@ page import="java.nio.file.Files" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.UUID" %>
<%@ page import="jakarta.servlet.http.Part" %>
<%
    Usuario u = (Usuario) session.getAttribute("usuarioActivo");
    if (u == null) {
        response.sendRedirect("login.jsp");
        return;
    }

    boolean esAdmin = u.getCorreoElectronico().equalsIgnoreCase(
            AppConfig.getInstance().getAdminCorreoElectronico());
    if (!esAdmin) {
        response.sendRedirect("dashboard.jsp");
        return;
    }

    Controller app = (Controller) session.getAttribute("app");
    if (app == null) {
        app = new Controller();
        session.setAttribute("app", app);
    }

    String mensaje = null;
    String mensajeTipo = "info";
    String accion = request.getParameter("accion");

    java.util.function.Function<String, String> esc = (s) -> {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    };

    if ("POST".equalsIgnoreCase(request.getMethod()) && "exportarCSV".equals(accion)) {
        int totalProductos = app.getAllProductos().size();
        if (totalProductos == 0) {
            mensaje = "No hay productos en venta en este momento. No se generó el CSV.";
            mensajeTipo = "error";
        } else {
            boolean enviado = Communications.enviarCSVProductosAlAdmin(app.getUsuarios(), u.getCorreoElectronico());
            mensaje = enviado
                    ? "CSV con " + totalProductos + " producto(s) enviado a " + u.getCorreoElectronico() + "."
                    : "No se pudo enviar el CSV. Comprueba la conexión y los logs.";
            mensajeTipo = enviado ? "info" : "error";
        }
    }

    if ("GET".equalsIgnoreCase(request.getMethod()) && "descargarBackup".equals(accion)) {
        File temp = File.createTempFile("fernanpop-backup-", ".dat");
        temp.deleteOnExit();

        boolean ok = Persistence.guardarBackup(temp.getAbsolutePath());
        if (ok) {
            byte[] datos = Files.readAllBytes(temp.toPath());
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"fernanpop-backup.dat\"");
            response.setContentLength(datos.length);
            response.getOutputStream().write(datos);
            response.getOutputStream().flush();
            temp.delete();
            return;
        } else {
            temp.delete();
            mensaje = "No se pudo generar la copia de seguridad.";
            mensajeTipo = "error";
        }
    }

    if ("POST".equalsIgnoreCase(request.getMethod()) && "restaurarBackup".equals(accion)) {
        String confirmacion = request.getParameter("confirmacion");
        if (!"RESTAURAR".equals(confirmacion)) {
            mensaje = "Escribe RESTAURAR (en mayúsculas) para confirmar.";
            mensajeTipo = "error";
        } else {
            Part filePart = null;
            boolean errorLectura = false;
            try {
                filePart = request.getPart("ficheroBackup");
            } catch (IllegalStateException | java.io.IOException | jakarta.servlet.ServletException ex) {
                //Fichero demasiado grande (límite de web.xml) o petición que no es multipart
                errorLectura = true;
            }

            if (errorLectura) {
                mensaje = "No se pudo leer el fichero. Comprueba que sea un backup válido y que no supere los 50 MB.";
                mensajeTipo = "error";
            } else if (filePart == null || filePart.getSize() == 0) {
                mensaje = "Selecciona un fichero de backup antes de restaurar.";
                mensajeTipo = "error";
            } else {
                File temp = File.createTempFile("fernanpop-restore-", ".dat");
                temp.deleteOnExit();
                try (java.io.InputStream in = filePart.getInputStream()) {
                    Files.copy(in, temp.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }

                boolean ok = Persistence.restaurarBackup(temp.getAbsolutePath());
                temp.delete();

                if (ok) {
                    // La base de datos cambió por completo: la caché en memoria
                    // del Controller de esta sesión ya no refleja la realidad,
                    // así que se descarta para que se recargue desde cero.
                    session.removeAttribute("app");
                    mensaje = "Base de datos restaurada correctamente. La sesión se ha actualizado.";
                } else {
                    mensaje = "No se pudo restaurar el backup. Comprueba que el fichero sea válido.";
                    mensajeTipo = "error";
                }
            }
        }
    }

    if (session.getAttribute("app") == null) {
        app = new Controller();
        session.setAttribute("app", app);
    }

    AppConfig cfg = AppConfig.getInstance();
%>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Administración · FernanPop</title>
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
    --ok:           #277048;
    --ok-bg:        #e7f3ec;
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
    --ok:           #6ee6a0;
    --ok-bg:        #123023;
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
    max-width: 820px;
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

  .page-title{ margin: 6px 0 8px; }
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

  .admin-pill{
    display:inline-flex;
    align-items:center;
    gap:6px;
    background: var(--stamp-solid);
    color:#fff;
    font-size:11.5px;
    font-weight:700;
    letter-spacing:.15px;
    text-transform:uppercase;
    padding:4px 12px;
    border-radius:999px;
    margin-bottom:20px;
  }

  .notice-banner{
    display:flex;
    align-items:flex-start;
    gap:9px;
    font-size:13.5px;
    padding:12px 14px;
    border-radius:4px;
    margin-bottom:22px;
    line-height:1.4;
  }
  .notice-banner.info{ background: var(--ok-bg); color: var(--ok); }
  .notice-banner.error{
    background: color-mix(in srgb, var(--stamp) 12%, var(--paper));
    border:1px solid color-mix(in srgb, var(--stamp) 40%, transparent);
    color: var(--stamp);
  }
  .notice-banner svg{ flex-shrink:0; margin-top:1px; }

  .panel-card{
    background: var(--paper);
    border:1px solid var(--purple-100);
    border-radius:16px;
    padding:22px;
    margin-bottom:18px;
  }
  [data-theme="dark"] .panel-card{ border-color: var(--purple-300); }

  .panel-card h2{
    font-family: var(--serif);
    font-size:17px;
    margin:0 0 4px;
    display:flex;
    align-items:center;
    gap:9px;
  }
  .panel-card h2 .icon{
    width:30px; height:30px;
    border-radius:9px;
    background: var(--purple-100);
    color: var(--purple-700);
    display:flex;
    align-items:center;
    justify-content:center;
    flex-shrink:0;
  }
  [data-theme="dark"] .panel-card h2 .icon{ color: var(--purple-500); }
  .panel-card .desc{
    font-size:13px;
    color: var(--ink-soft);
    margin:0 0 16px;
    line-height:1.5;
  }

  .kv-table{
    border:1px solid var(--purple-100);
    border-radius:4px;
    overflow:hidden;
    margin-bottom:14px;
  }
  [data-theme="dark"] .kv-table{ border-color: var(--purple-300); }
  .kv-row{
    display:flex;
    justify-content:space-between;
    gap:14px;
    padding:9px 14px;
    border-bottom:1px solid var(--purple-100);
    font-size:13px;
  }
  [data-theme="dark"] .kv-row{ border-color: var(--purple-300); }
  .kv-row:last-child{ border-bottom:none; }
  .kv-row:nth-child(even){ background: var(--cream); }
  .kv-row .k{ color: var(--ink-soft); flex-shrink:0; }
  .kv-row .v{ color: var(--ink); font-weight:600; text-align:right; word-break:break-all; }

  .kv-group-label{
    font-size:11.5px;
    font-weight:700;
    text-transform:uppercase;
    letter-spacing:.2px;
    color: var(--purple-800);
    margin:18px 0 8px;
  }
  [data-theme="dark"] .kv-group-label{ color: var(--purple-500); }
  .kv-group-label:first-child{ margin-top:0; }

  .btn-primary{
    padding:11px 20px;
    border:1px solid var(--purple-700);
    border-radius:4px;
    background: var(--purple-700);
    color:#fff;
    font-size:14px;
    font-weight:600;
    font-family: var(--sans);
    cursor:pointer;
    text-decoration:none;
    display:inline-flex;
    align-items:center;
    gap:8px;
  }
  .btn-primary:hover{ filter:brightness(1.06); }
  .btn-primary:focus-visible{ outline:2px solid var(--purple-700); outline-offset:2px; }

  .btn-danger{
    padding:11px 18px;
    border:1px solid var(--purple-700);
    border-radius:9px;
    background: var(--stamp-solid);
    color:#fff;
    font-size:13.5px;
    font-weight:600;
    font-family: var(--sans);
    cursor:pointer;
  }
  .btn-danger:hover{ filter:brightness(1.08); }
  .btn-danger:focus-visible{ outline:2px solid var(--stamp); outline-offset:2px; }

  .danger-box{
    margin-top:18px;
    border:1px solid color-mix(in srgb, var(--stamp) 35%, transparent);
    border-radius:8px;
    padding:18px;
    background: color-mix(in srgb, var(--stamp) 6%, var(--paper));
  }
  .danger-box h3{
    font-size:13.5px;
    font-weight:700;
    color: var(--stamp);
    margin:0 0 6px;
  }
  .danger-box p{
    font-size:12.5px;
    color: var(--ink-soft);
    margin:0 0 14px;
    line-height:1.5;
  }
  .file-input-wrap{
    margin-bottom:12px;
  }
  .file-input-wrap input[type="file"]{
    width:100%;
    padding:9px;
    border-radius:8px;
    border:1px solid color-mix(in srgb, var(--stamp) 40%, var(--purple-100));
    background: var(--paper);
    color: var(--ink);
    font-size:13px;
    font-family: var(--sans);
  }
  .danger-box input[type="text"]{
    width:100%;
    padding:10px 12px;
    border-radius:8px;
    border:1px solid color-mix(in srgb, var(--stamp) 40%, var(--purple-100));
    background: var(--paper);
    color: var(--ink);
    font-size:13.5px;
    font-family: var(--sans);
    margin-bottom:12px;
  }
  .danger-box input[type="text"]:focus{
    outline:none;
    box-shadow: 0 0 0 3px color-mix(in srgb, var(--stamp) 25%, transparent);
  }

  @media (prefers-reduced-motion: reduce){ *{ transition:none !important; } }

  @media (max-width: 480px){
    .kv-row{ flex-direction:column; gap:2px; }
    .kv-row .v{ text-align:left; }
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
      <h1>Administración</h1>
      <p>Configuración del sistema, exportación de datos y copias de seguridad.</p>
    </div>
    <span class="admin-pill">
      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M12 2l8 4v6c0 5-3.5 8.5-8 10-4.5-1.5-8-5-8-10V6l8-4z" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      Solo administrador
    </span>

    <% if (mensaje != null) { %>
    <div class="notice-banner <%= mensajeTipo %>">
      <% if (mensajeTipo.equals("error")) { %>
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2"/>
        <path d="M12 8v5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        <circle cx="12" cy="16" r="1" fill="currentColor"/>
      </svg>
      <% } else { %>
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M5 13l4 4L19 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <% } %>
      <span><%= esc.apply(mensaje) %></span>
    </div>
    <% } %>

    <!-- ============ CONFIGURACIÓN ============ -->
    <div class="panel-card">
      <h2>
        <span class="icon">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" aria-hidden="true"><circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="2"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" stroke="currentColor" stroke-width="1.5"/></svg>
        </span>
        Configuración
      </h2>
      <p class="desc">Propiedades cargadas desde config.properties.</p>

      <div class="kv-group-label">General</div>
      <div class="kv-table">
        <div class="kv-row"><span class="k">acceso.invitado</span><span class="v"><%= cfg.isAccesoInvitado() %></span></div>
        <div class="kv-row"><span class="k">admin.correoElectronico</span><span class="v"><%= esc.apply(cfg.getAdminCorreoElectronico()) %></span></div>
        <div class="kv-row"><span class="k">admin.contrasenia</span><span class="v">••••••</span></div>
        <div class="kv-row"><span class="k">pagina.tamano</span><span class="v"><%= cfg.getPaginaTamano() %></span></div>
      </div>

      <div class="kv-group-label">Base de datos</div>
      <div class="kv-table">
        <div class="kv-row"><span class="k">db.url</span><span class="v"><%= esc.apply(cfg.getDbUrl()) %></span></div>
        <div class="kv-row"><span class="k">db.user</span><span class="v"><%= esc.apply(cfg.getDbUser()) %></span></div>
        <div class="kv-row"><span class="k">db.pass</span><span class="v">••••••</span></div>
      </div>

      <div class="kv-group-label">Rutas</div>
      <div class="kv-table">
        <div class="kv-row"><span class="k">ruta.datos</span><span class="v"><%= esc.apply(cfg.getRutaDatos()) %></span></div>
        <div class="kv-row"><span class="k">ruta.log</span><span class="v"><%= esc.apply(cfg.getRutaLog()) %></span></div>
      </div>

      <div class="kv-group-label">Últimas conexiones</div>
      <div class="kv-table">
        <% for (Usuario usuarioListado : app.getUsuarios()) {
             String correoListado = usuarioListado.getCorreoElectronico();
             String ultimoLogin = AppConfig.getInstance().getUltimoLogin(correoListado);
             String loginStr = ultimoLogin != null ? ultimoLogin : "Nunca ha iniciado sesión";
        %>
        <div class="kv-row">
          <span class="k"><%= esc.apply(correoListado) %></span>
          <span class="v"><%= esc.apply(loginStr) %></span>
        </div>
        <% } %>
      </div>
    </div>

    <!-- ============ EXPORTAR PRODUCTOS ============ -->
    <div class="panel-card">
      <h2>
        <span class="icon">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M7 10l5 5 5-5M12 15V3" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </span>
        Exportar productos
      </h2>
      <p class="desc">Genera un CSV con todos los productos en venta del sistema (<%= app.getAllProductos().size() %> actualmente) y lo envía a tu correo: <strong><%= esc.apply(u.getCorreoElectronico()) %></strong>.</p>

      <form method="post" action="admin.jsp">
        <input type="hidden" name="accion" value="exportarCSV">
        <button class="btn-primary" type="submit">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
          Enviar CSV por correo
        </button>
      </form>
    </div>

    <!-- ============ BACKUP / RESTAURAR ============ -->
    <div class="panel-card">
      <h2>
        <span class="icon">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" aria-hidden="true"><ellipse cx="12" cy="5" rx="9" ry="3" stroke="currentColor" stroke-width="2"/><path d="M3 5v14c0 1.7 4 3 9 3s9-1.3 9-3V5M3 12c0 1.7 4 3 9 3s9-1.3 9-3" stroke="currentColor" stroke-width="2"/></svg>
        </span>
        Copia de seguridad de la base de datos
      </h2>
      <p class="desc">Descarga un volcado completo de la base de datos, o restaura uno desde un fichero anterior.</p>

      <a class="btn-primary" href="admin.jsp?accion=descargarBackup">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M7 10l5 5 5-5M12 15V3" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
        Descargar copia de seguridad
      </a>

      <div class="danger-box">
        <h3>Restaurar desde un fichero</h3>
        <p>Esta acción <strong>borra todos los datos actuales</strong> de la base de datos y los sustituye por los del fichero que subas. No se puede deshacer.</p>

        <form method="post" action="admin.jsp" enctype="multipart/form-data">
          <input type="hidden" name="accion" value="restaurarBackup">
          <div class="file-input-wrap">
            <input type="file" name="ficheroBackup" accept=".dat" required>
          </div>
          <input type="text" name="confirmacion" placeholder="Escribe RESTAURAR para confirmar" required>
          <button class="btn-danger" type="submit">Restaurar base de datos</button>
        </form>
      </div>
    </div>

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






