<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="controller.Controller" %>
<%@ page import="models.Usuario" %>
<%@ page import="utils.Communications" %>
<%@ page import="utils.UI" %>
<%@ page import="utils.Utils" %>
<%@ page import="java.time.LocalDateTime" %>
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

    String mensaje = null;
    String mensajeTipo = "info";
    String accion = request.getParameter("accion");

    java.util.function.Function<String, String> esc = (s) -> {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    };

    if ("POST".equalsIgnoreCase(request.getMethod()) && "editarNombre".equals(accion)) {
        String nuevo = request.getParameter("valor");
        if (nuevo == null || nuevo.trim().isEmpty()) {
            mensaje = "El nombre no puede estar vacío.";
            mensajeTipo = "error";
        } else {
            String antes = u.getNombre();
            u.setNombre(nuevo.trim());
            app.guardarUnico(u);
            try { Communications.enviaMensajeTelegram(UI.msgCambioDatos(u, "Nombre", antes, u.getNombre())); }
            catch (Throwable ignored) { }
            mensaje = "Nombre actualizado.";
        }
    }

    if ("POST".equalsIgnoreCase(request.getMethod()) && "editarApellidos".equals(accion)) {
        String nuevo = request.getParameter("valor");
        if (nuevo == null || nuevo.trim().isEmpty()) {
            mensaje = "Los apellidos no pueden estar vacíos.";
            mensajeTipo = "error";
        } else {
            String antes = u.getApellidos();
            u.setApellidos(nuevo.trim());
            app.guardarUnico(u);
            try { Communications.enviaMensajeTelegram(UI.msgCambioDatos(u, "Apellidos", antes, u.getApellidos())); }
            catch (Throwable ignored) { }
            mensaje = "Apellidos actualizados.";
        }
    }

    if ("POST".equalsIgnoreCase(request.getMethod()) && "editarContrasenia".equals(accion)) {
        String nueva = request.getParameter("valor");
        if (nueva == null || nueva.trim().isEmpty()) {
            mensaje = "La contraseña no puede estar vacía.";
            mensajeTipo = "error";
        } else {
            u.setContrasenia(nueva.trim());
            app.guardarUnico(u);
            try { Communications.enviaMensajeTelegram(UI.msgCambioDatos(u, "Contraseña", "••••••", "••••••")); }
            catch (Throwable ignored) { }
            mensaje = "Contraseña actualizada.";
        }
    }

    if ("POST".equalsIgnoreCase(request.getMethod()) && "editarTelefono".equals(accion)) {
        String nuevoStr = request.getParameter("valor");
        int nuevo;
        try {
            nuevo = Integer.parseInt(nuevoStr.trim());
        } catch (Exception e) {
            nuevo = -1;
        }
        if (nuevo < 600000000 || nuevo > 799999999) {
            mensaje = "El móvil debe tener 9 dígitos y empezar por 6 o 7.";
            mensajeTipo = "error";
        } else {
            String antes = String.valueOf(u.getTelefono());
            u.setTelefono(nuevo);
            app.guardarUnico(u);
            try { Communications.enviaMensajeTelegram(UI.msgCambioDatos(u, "Teléfono", antes, String.valueOf(nuevo))); }
            catch (Throwable ignored) { }
            mensaje = "Teléfono actualizado.";
        }
    }

    String pasoBorrado = "inicial";
    if (session.getAttribute("borrado.pendiente") != null && !"confirmarBorrado".equals(accion)) {
        pasoBorrado = "codigo";
    }

    if ("POST".equalsIgnoreCase(request.getMethod()) && "confirmarBorrado".equals(accion)) {
        String confirmacion = request.getParameter("confirmacion");
        if (!"BORRAR".equals(confirmacion)) {
            mensaje = "Escribe BORRAR (en mayúsculas) para confirmar.";
            mensajeTipo = "error";
        } else {
            String codigoSeguridad = Utils.generarClave(6);
            session.setAttribute("borrado.pendiente", true);
            session.setAttribute("borrado.codigo", codigoSeguridad);
            session.setAttribute("borrado.expira", LocalDateTime.now().plusMinutes(10));

            try {
                String asunto = "Código de seguridad para eliminar cuenta - FernanPop";
                String cuerpo = UI.generarEmailEliminarCuenta(u.getNombre(), codigoSeguridad);
                Communications.enviarConGMail(u.getCorreoElectronico(), asunto, cuerpo);
            } catch (Throwable ignored) {
                // Si falla el envío, el código ya quedó en sesión y se puede reintentar
            }

            pasoBorrado = "codigo";
        }
    }

    if ("POST".equalsIgnoreCase(request.getMethod()) && "verificarBorrado".equals(accion)) {
        String codigoIntroducido = request.getParameter("codigo");
        String codigoEsperado = (String) session.getAttribute("borrado.codigo");
        LocalDateTime expira = (LocalDateTime) session.getAttribute("borrado.expira");

        if (codigoEsperado == null || expira == null) {
            response.sendRedirect("perfil.jsp");
            return;
        }

        boolean codigoCorrecto = codigoIntroducido != null && codigoIntroducido.trim().equalsIgnoreCase(codigoEsperado);
        boolean dentroDelTiempo = expira.isAfter(LocalDateTime.now());

        if (!dentroDelTiempo) {
            mensaje = "El código ha expirado (más de 10 minutos). Inténtalo de nuevo.";
            mensajeTipo = "error";
            session.removeAttribute("borrado.pendiente");
            session.removeAttribute("borrado.codigo");
            session.removeAttribute("borrado.expira");
            pasoBorrado = "inicial";
        } else if (!codigoCorrecto) {
            mensaje = "Código incorrecto.";
            mensajeTipo = "error";
            pasoBorrado = "codigo";
        } else {
            if (app.deleteUsuario(u)) {
                try { Communications.enviaMensajeTelegram(UI.msgUsuarioEliminado(u)); }
                catch (Throwable ignored) { }

                session.removeAttribute("borrado.pendiente");
                session.removeAttribute("borrado.codigo");
                session.removeAttribute("borrado.expira");
                session.removeAttribute("usuarioActivo");

                response.sendRedirect("index.jsp?cuentaEliminada=1");
                return;
            } else {
                mensaje = "No se pudo eliminar el perfil. Inténtalo de nuevo.";
                mensajeTipo = "error";
                pasoBorrado = "codigo";
            }
        }
    }

    if ("cancelarBorrado".equals(accion)) {
        session.removeAttribute("borrado.pendiente");
        session.removeAttribute("borrado.codigo");
        session.removeAttribute("borrado.expira");
        response.sendRedirect("perfil.jsp");
        return;
    }

    double media = u.notaMedia();
    String valoracionTexto = media == -1 ? "Sin valoraciones" : String.format("%.1f ★", media);
%>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Mi perfil · FernanPop</title>
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
    --ok:           #6ee6a0;
    --ok-bg:        #123023;
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
    max-width: 640px;
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
  .topbar .left{ display:flex; align-items:center; gap:14px; }
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
    margin:-50px 0 -50px 0;
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

  .profile-card{
    background: var(--paper);
    border:1px solid var(--purple-100);
    border-radius:16px;
    padding:24px;
    margin-bottom:28px;
    display:flex;
    align-items:center;
    gap:18px;
  }
  [data-theme="dark"] .profile-card{ border-color: var(--purple-300); }

  .profile-card .avatar{
    width:58px; height:58px;
    border-radius:50%;
    background: var(--purple-700);
    color:#fff;
    display:flex;
    align-items:center;
    justify-content:center;
    font-family: var(--serif);
    font-size:22px;
    font-weight:700;
    flex-shrink:0;
  }
  .profile-card .info h2{
    font-family: var(--serif);
    font-size:19px;
    margin:0 0 3px;
  }
  .profile-card .info .email{
    font-size:13px;
    color: var(--ink-soft);
    margin:0 0 8px;
  }
  .profile-card .info .stats{
    display:flex;
    gap:14px;
    font-size:12.5px;
    color: var(--ink-soft);
  }
  .profile-card .info .stats .rating{
    color: var(--gold);
    font-weight:700;
  }

  .section{ margin-bottom:14px; }
  .section h3{
    font-size:13px;
    font-weight:700;
    text-transform:uppercase;
    letter-spacing:.15px;
    color: var(--purple-800);
    margin:0 0 12px;
  }
  [data-theme="dark"] .section h3{ color: var(--purple-500); }

  .field-card{
    background: var(--paper);
    border:1px solid var(--purple-100);
    border-radius:8px;
    padding:14px 16px;
    margin-bottom:10px;
  }
  [data-theme="dark"] .field-card{ border-color: var(--purple-300); }

  .field-row{
    display:flex;
    align-items:center;
    justify-content:space-between;
    gap:14px;
  }
  .field-row .label{ font-size:12px; color: var(--ink-soft); margin:0 0 2px; }
  .field-row .value{ font-size:14.5px; font-weight:600; color: var(--ink); margin:0; }

  .btn-edit-toggle{
    flex-shrink:0;
    padding:7px 14px;
    border-radius:8px;
    border:1px solid var(--purple-100);
    background: var(--cream);
    color: var(--purple-700);
    font-size:12.5px;
    font-weight:600;
    font-family: var(--sans);
    cursor:pointer;
  }
  [data-theme="dark"] .btn-edit-toggle{ border-color: var(--purple-300); color: var(--purple-500); }
  .btn-edit-toggle:hover{ border-color: var(--purple-500); }
  .btn-edit-toggle:focus-visible{ outline:2px solid var(--purple-500); outline-offset:2px; }

  .edit-form{
    display:none;
    margin-top:12px;
    gap:8px;
  }
  .edit-form.open{ display:flex; }
  .edit-form input{
    flex:1;
    padding:9px 12px;
    border-radius:8px;
    border:1px solid var(--purple-100);
    background: var(--cream);
    color: var(--ink);
    font-size:13.5px;
    font-family: var(--sans);
  }
  [data-theme="dark"] .edit-form input{ border-color: var(--purple-300); }
  .edit-form input:focus{
    outline:none;
    border-color: var(--purple-500);
    box-shadow: 0 0 0 3px var(--ring);
  }
  .edit-form button{
    padding:9px 16px;
    border:1px solid var(--purple-700);
    border-radius:8px;
    background: var(--purple-700);
    color:#fff;
    font-size:13px;
    font-weight:600;
    font-family: var(--sans);
    cursor:pointer;
    flex-shrink:0;
  }
  .edit-form button:hover{ filter:brightness(1.06); }

  .danger-zone{
    margin-top:30px;
    border:1px solid color-mix(in srgb, var(--stamp) 35%, transparent);
    border-radius:14px;
    padding:20px;
    background: color-mix(in srgb, var(--stamp) 6%, var(--paper));
  }
  .danger-zone h3{
    font-size:14px;
    font-weight:700;
    color: var(--stamp);
    margin:0 0 6px;
  }
  .danger-zone p{
    font-size:13px;
    color: var(--ink-soft);
    margin:0 0 16px;
    line-height:1.5;
  }
  .danger-zone input[type="text"]{
    width:100%;
    padding:11px 13px;
    border-radius:9px;
    border:1px solid color-mix(in srgb, var(--stamp) 40%, var(--purple-100));
    background: var(--paper);
    color: var(--ink);
    font-size:14px;
    font-family: var(--sans);
    margin-bottom:12px;
  }
  .danger-zone input[type="text"]:focus{
    outline:none;
    box-shadow: 0 0 0 3px color-mix(in srgb, var(--stamp) 25%, transparent);
  }
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

  .code-input{
    width:100%;
    padding:14px;
    border-radius:9px;
    border:1px solid color-mix(in srgb, var(--stamp) 40%, var(--purple-100));
    background: var(--paper);
    color: var(--ink);
    font-size:22px;
    font-weight:700;
    font-family: var(--sans);
    letter-spacing:7px;
    text-align:center;
    text-transform:uppercase;
    margin-bottom:12px;
  }
  .code-input:focus{
    outline:none;
    box-shadow: 0 0 0 3px color-mix(in srgb, var(--stamp) 25%, transparent);
  }

  .btn-cancel-link{
    display:inline-block;
    margin-top:10px;
    font-size:12.5px;
    font-weight:600;
    color: var(--ink-soft);
    background:none;
    border:1px solid var(--purple-700);
    cursor:pointer;
    text-decoration:underline;
    font-family: var(--sans);
    padding:0;
  }
  .btn-cancel-link:hover{ color: var(--stamp); }

  @media (prefers-reduced-motion: reduce){ *{ transition:none !important; } }

  @media (max-width: 480px){
    .profile-card{ flex-direction:column; text-align:center; }
    .profile-card .info .stats{ justify-content:center; }
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
        <img class="mark-logo" src="https://raw.githubusercontent.com/Superiorfran1/imagenesFernanPop/main/ChatGPT_Image_Jun_27__2026__12_28_08_AM-removebg-preview.png" alt="FernanPop">
      </div>
      <button class="theme-toggle" id="themeToggle" type="button" aria-label="Cambiar a modo oscuro" aria-pressed="false">
        <span class="knob" id="themeKnob">☾</span>
      </button>
    </div>

    <div class="page-title">
      <h1>Mi perfil</h1>
      <p>Tus datos personales y la configuración de tu cuenta.</p>
    </div>

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

    <div class="profile-card">
      <div class="avatar"><%= esc.apply(u.getNombre().isEmpty() ? "?" : u.getNombre().substring(0,1).toUpperCase()) %></div>
      <div class="info">
        <h2><%= esc.apply(u.getNombre()) %> <%= esc.apply(u.getApellidos()) %></h2>
        <p class="email"><%= esc.apply(u.getCorreoElectronico()) %></p>
        <div class="stats">
          <span><%= u.getEnVenta().size() %> en venta</span>
          <span><%= u.getVentas().size() %> ventas</span>
          <span class="rating"><%= valoracionTexto %></span>
        </div>
      </div>
    </div>

    <div class="section">
      <h3>Datos personales</h3>

      <div class="field-card">
        <div class="field-row">
          <div><p class="label">Nombre</p><p class="value"><%= esc.apply(u.getNombre()) %></p></div>
          <button class="btn-edit-toggle" type="button" onclick="toggleEdit('f-nombre')">Editar</button>
        </div>
        <form class="edit-form" id="f-nombre" method="post" action="perfil.jsp">
          <input type="hidden" name="accion" value="editarNombre">
          <input type="text" name="valor" value="<%= esc.apply(u.getNombre()) %>" required>
          <button type="submit">Guardar</button>
        </form>
      </div>

      <div class="field-card">
        <div class="field-row">
          <div><p class="label">Apellidos</p><p class="value"><%= esc.apply(u.getApellidos()) %></p></div>
          <button class="btn-edit-toggle" type="button" onclick="toggleEdit('f-apellidos')">Editar</button>
        </div>
        <form class="edit-form" id="f-apellidos" method="post" action="perfil.jsp">
          <input type="hidden" name="accion" value="editarApellidos">
          <input type="text" name="valor" value="<%= esc.apply(u.getApellidos()) %>" required>
          <button type="submit">Guardar</button>
        </form>
      </div>

      <div class="field-card">
        <div class="field-row">
          <div><p class="label">Contraseña</p><p class="value">••••••••</p></div>
          <button class="btn-edit-toggle" type="button" onclick="toggleEdit('f-clave')">Editar</button>
        </div>
        <form class="edit-form" id="f-clave" method="post" action="perfil.jsp">
          <input type="hidden" name="accion" value="editarContrasenia">
          <input type="password" name="valor" placeholder="Nueva contraseña" autocomplete="new-password" required>
          <button type="submit">Guardar</button>
        </form>
      </div>

      <div class="field-card">
        <div class="field-row">
          <div><p class="label">Móvil</p><p class="value"><%= u.getTelefono() %></p></div>
          <button class="btn-edit-toggle" type="button" onclick="toggleEdit('f-telefono')">Editar</button>
        </div>
        <form class="edit-form" id="f-telefono" method="post" action="perfil.jsp">
          <input type="hidden" name="accion" value="editarTelefono">
          <input type="text" name="valor" value="<%= u.getTelefono() %>" inputmode="numeric" placeholder="9 dígitos, empieza por 6 o 7" required>
          <button type="submit">Guardar</button>
        </form>
      </div>
    </div>

    <div class="danger-zone">
      <% if (pasoBorrado.equals("inicial")) { %>

        <h3>Eliminar cuenta</h3>
        <p>Esta acción es irreversible. Se borrará tu perfil, tus productos en venta y tu acceso a FernanPop. Tu historial de ventas y compras se conserva para quienes hicieron tratos contigo.</p>

        <form method="post" action="perfil.jsp">
          <input type="hidden" name="accion" value="confirmarBorrado">
          <input type="text" name="confirmacion" placeholder="Escribe BORRAR para confirmar" required>
          <button class="btn-danger" type="submit">Eliminar mi cuenta</button>
        </form>

      <% } else { %>

        <h3>Confirma tu identidad</h3>
        <p>Te hemos enviado un código de 6 caracteres a tu correo. Caduca en 10 minutos.</p>

        <form method="post" action="perfil.jsp">
          <input type="hidden" name="accion" value="verificarBorrado">
          <input class="code-input" type="text" name="codigo" placeholder="------" maxlength="6" autocomplete="one-time-code" required autofocus>
          <button class="btn-danger" type="submit">Confirmar eliminación</button>
        </form>

        <form method="post" action="perfil.jsp" style="display:inline;">
          <input type="hidden" name="accion" value="cancelarBorrado">
          <button class="btn-cancel-link" type="submit">Cancelar</button>
        </form>

      <% } %>
    </div>

  </div>

  <script>
    function toggleEdit(id) {
      var form = document.getElementById(id);
      var isOpen = form.classList.contains('open');
      document.querySelectorAll('.edit-form.open').forEach(function (f) { f.classList.remove('open'); });
      if (!isOpen) {
        form.classList.add('open');
        var input = form.querySelector('input:not([type="hidden"])');
        if (input) input.focus();
      }
    }

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






