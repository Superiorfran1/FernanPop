<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="controller.Controller" %>
<%@ page import="models.Usuario" %>
<%@ page import="persistence.AppConfig" %>
<%@ page import="persistence.Log" %>
<%@ page import="utils.Communications" %>
<%@ page import="utils.UI" %>
<%@ page import="utils.Utils" %>
<%
    String error = null;
    String paso = "datos"; // "datos" o "verificar"

    Controller app = (Controller) session.getAttribute("app");
    if (app == null) {
        app = new Controller();
        session.setAttribute("app", app);
    }

    String accion = request.getParameter("accion");

    if (session.getAttribute("registro.pendiente") != null && !"enviarDatos".equals(accion)) {
        paso = "verificar";
    }

    if ("POST".equalsIgnoreCase(request.getMethod()) && "enviarDatos".equals(accion)) {
        String nombre      = request.getParameter("nombre");
        String apellidos   = request.getParameter("apellidos");
        String correo      = request.getParameter("correo");
        String clave       = request.getParameter("clave");
        String telefonoStr = request.getParameter("telefono");

        if (nombre == null || nombre.trim().isEmpty()
                || apellidos == null || apellidos.trim().isEmpty()
                || correo == null || correo.trim().isEmpty()
                || clave == null || clave.trim().isEmpty()
                || telefonoStr == null || telefonoStr.trim().isEmpty()) {
            error = "Rellena todos los campos para continuar.";
        } else if (!Utils.emailValido(correo.trim())) {
            error = "El correo electrónico no tiene un formato válido.";
        } else if (app.buscaPorCorreoElectronico(correo.trim()) != null) {
            error = "Ya existe una cuenta con ese correo.";
        } else {
            int telefono;
            try {
                telefono = Integer.parseInt(telefonoStr.trim());
            } catch (NumberFormatException e) {
                telefono = -1;
            }
            if (telefono < 600000000 || telefono > 799999999) {
                error = "El móvil debe tener 9 dígitos y empezar por 6 o 7.";
            } else {
                // Datos válidos: generamos el código y lo enviamos por correo
                String codigo = Utils.generarClave(6);

                session.setAttribute("registro.pendiente", true);
                session.setAttribute("registro.nombre", nombre.trim());
                session.setAttribute("registro.apellidos", apellidos.trim());
                session.setAttribute("registro.correo", correo.trim());
                session.setAttribute("registro.clave", clave);
                session.setAttribute("registro.telefono", telefono);
                session.setAttribute("registro.codigo", codigo);

                try {
                    String asunto = "Código de verificación FernanPop";
                    String mensaje = UI.generaEmailVerificacion(nombre.trim(), codigo);
                    Communications.enviarConGMail(correo.trim(), asunto, mensaje);
                } catch (Throwable e) {
                }

                paso = "verificar";
            }
        }
    }

    if ("POST".equalsIgnoreCase(request.getMethod()) && "verificarCodigo".equals(accion)) {
        String codigoIntroducido = request.getParameter("codigo");
        String codigoEsperado = (String) session.getAttribute("registro.codigo");

        if (codigoEsperado == null) {
            response.sendRedirect("registro.jsp");
            return;
        }

        if (codigoIntroducido == null || !codigoIntroducido.trim().equalsIgnoreCase(codigoEsperado)) {
            error = "El código no es correcto. Revisa tu correo e inténtalo de nuevo.";
            paso = "verificar";
        } else if (session.getAttribute("registro.telefono") == null
                || session.getAttribute("registro.correo") == null) {
            session.removeAttribute("registro.pendiente");
            response.sendRedirect("registro.jsp");
            return;
        } else {
            String nombre    = (String) session.getAttribute("registro.nombre");
            String apellidos = (String) session.getAttribute("registro.apellidos");
            String correo    = (String) session.getAttribute("registro.correo");
            String clave     = (String) session.getAttribute("registro.clave");
            int telefono     = (Integer) session.getAttribute("registro.telefono");

            Usuario nuevoUsuario = new Usuario(app.generaID_Usuario(), nombre, apellidos, correo, clave, telefono);

            if (app.addUsuario(nuevoUsuario)) {
                app.guardarUnico(nuevoUsuario);

                try {
                    String msgTel = UI.msgNuevoUsuario(nombre, correo, clave);
                    Communications.enviaMensajeTelegram(msgTel);
                } catch (Throwable ignored) { }

                session.removeAttribute("registro.pendiente");
                session.removeAttribute("registro.nombre");
                session.removeAttribute("registro.apellidos");
                session.removeAttribute("registro.correo");
                session.removeAttribute("registro.clave");
                session.removeAttribute("registro.telefono");
                session.removeAttribute("registro.codigo");

                session.setAttribute("usuarioActivo", nuevoUsuario);
                AppConfig.getInstance().actualizarUltimoLogin(nuevoUsuario.getCorreoElectronico());
                Log.registrarInicioSesion(nuevoUsuario.getCorreoElectronico());

                response.sendRedirect("dashboard.jsp");
                return;
            } else {
                error = "No se pudo crear la cuenta. Inténtalo de nuevo.";
                paso = "verificar";
            }
        }
    }

    if ("cancelar".equals(accion)) {
        session.removeAttribute("registro.pendiente");
        session.removeAttribute("registro.nombre");
        session.removeAttribute("registro.apellidos");
        session.removeAttribute("registro.correo");
        session.removeAttribute("registro.clave");
        session.removeAttribute("registro.telefono");
        session.removeAttribute("registro.codigo");
        response.sendRedirect("registro.jsp");
        return;
    }

    java.util.function.Function<String, String> esc = (s) -> {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    };
    String nombreMostrado    = esc.apply(request.getParameter("nombre"));
    String apellidosMostrado = esc.apply(request.getParameter("apellidos"));
    String correoMostrado    = esc.apply(request.getParameter("correo"));
    String telefonoMostrado  = esc.apply(request.getParameter("telefono"));
    String correoPendiente   = esc.apply((String) session.getAttribute("registro.correo"));
%>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Crear cuenta · FernanPop</title>
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
    margin-bottom: 22px;
  }
  .brand .mark-logo{
    max-width:100px;
    height:auto;
    display:inline-block;
      margin-top: -100px;
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

  .steps{
    display:flex;
    align-items:center;
    justify-content:center;
    gap:8px;
    margin-bottom:20px;
  }
  .steps .step{
    display:flex;
    align-items:center;
    gap:7px;
    font-size:12px;
    font-weight:600;
    letter-spacing:.15px;
    color: var(--ink-soft);
  }
  .steps .dot{
    width:22px; height:22px;
    border-radius:50%;
    display:flex;
    align-items:center;
    justify-content:center;
    font-size:11px;
    border:1px solid var(--purple-300);
    color: var(--ink-soft);
    background: var(--paper);
  }
  .steps .step.active .dot{
    background: var(--btn-from);
    border-color: var(--btn-from);
    color:#fff;
  }
  .steps .step.active{ color: var(--purple-700); }
  [data-theme="dark"] .steps .step.active{ color: var(--purple-500); }
  .steps .step.done .dot{
    background: var(--purple-100);
    border-color: var(--purple-300);
    color: var(--purple-700);
  }
  [data-theme="dark"] .steps .step.done .dot{ color: var(--purple-500); }
  .steps .line{
    width:28px; height:1px;
    background: var(--purple-100);
  }

  .ticket{
    position:relative;
    width:100%;
    max-width: 400px;
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
    font-size:21px;
    margin:0 0 4px;
    color: var(--ink);
  }
  .ticket .sub{
    font-size:13.5px;
    color: var(--ink-soft);
    margin:0 0 22px;
  }
  .ticket .sub strong{ color: var(--ink); font-weight:600; }

  .field{ margin-bottom:16px; }
  .field-row{ display:flex; gap:12px; }
  .field-row .field{ flex:1; min-width:0; }

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

  .field-error input{ border-color: var(--stamp); }

  .field .hint{
    margin:6px 2px 0;
    font-size:11.5px;
    color: var(--ink-soft);
  }

  .code-input{
    width:100%;
    padding:16px;
    border-radius:4px;
    border:1px solid var(--purple-100);
    background: var(--cream);
    color: var(--ink);
    font-size:24px;
    font-weight:700;
    font-family: var(--sans);
    letter-spacing:8px;
    text-align:center;
    text-transform:uppercase;
    transition: border-color .3s ease, box-shadow .3s ease;
  }
  [data-theme="dark"] .code-input{ border-color: var(--purple-300); }
  .code-input:focus{
    outline:none;
    border-color: var(--purple-500);
    box-shadow: 0 0 0 3px var(--ring);
  }
  .code-input.field-error-input{ border-color: var(--stamp); }

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
    margin-bottom:18px;
    line-height:1.4;
  }
  .alert svg{ flex-shrink:0; margin-top:1px; }

  .notice{
    display:flex;
    align-items:flex-start;
    gap:9px;
    background: var(--purple-100);
    color: var(--purple-800);
    font-size:13px;
    padding:11px 13px;
    border-radius:6px;
    margin-bottom:20px;
    line-height:1.45;
  }
  [data-theme="dark"] .notice{ color: var(--purple-500); }
  .notice svg{ flex-shrink:0; margin-top:1px; }

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

  .btn-text{
    display:block;
    width:100%;
    text-align:center;
    margin-top:14px;
    background:none;
    border:none;
    font-size:13px;
    font-weight:600;
    color: var(--ink-soft);
    cursor:pointer;
    font-family: var(--sans);
    text-decoration:underline;
    text-decoration-color: var(--purple-300);
  }
  .btn-text:hover{ color: var(--purple-700); }
  [data-theme="dark"] .btn-text:hover{ color: var(--purple-500); }

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
    margin-top:20px;
    padding-top:14px;
    border-top:1px dashed var(--purple-100);
    font-size:12px;
    color: var(--ink-soft);
    letter-spacing:.15px;
  }

  @media (prefers-reduced-motion: reduce){
    .ticket{ animation:none; }
    *{ transition:none !important; }
  }

  @media (max-width:420px){
    .ticket{ padding:28px 22px 24px; }
    .field-row{ flex-direction:column; gap:0; }
  }
</style>
</head>
<body>

  <button class="theme-toggle" id="themeToggle" type="button" aria-label="Cambiar a modo oscuro" aria-pressed="false">
    <span class="knob" id="themeKnob">☾</span>
  </button>

  <main>
    <div class="brand">
      <img class="mark-logo" src="https://raw.githubusercontent.com/Superiorfran1/imagenesFernanPop/main/ChatGPT_Image_Jun_27__2026__12_28_08_AM-removebg-preview.png" alt="FernanPop">
      <h1 class="brand-name">Fernan<span class="brand-pop">Pop</span></h1>
    </div>

    <div class="steps">
      <div class="step <%= paso.equals("datos") ? "active" : "done" %>">
        <span class="dot">1</span> Tus datos
      </div>
      <span class="line" aria-hidden="true"></span>
      <div class="step <%= paso.equals("verificar") ? "active" : "" %>">
        <span class="dot">2</span> Verificación
      </div>
    </div>

    <div class="ticket">
      <span class="stub-label"><%= paso.equals("datos") ? "Alta" : "Cupón 2" %></span>

      <% if (paso.equals("datos")) { %>

        <h1>Crea tu cuenta</h1>
        <p class="sub">Únete a la mejor tienda de segunda mano.</p>

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

        <form method="post" action="registro.jsp" novalidate>
          <input type="hidden" name="accion" value="enviarDatos">

          <div class="field-row">
            <div class="field">
              <label for="nombre">Nombre</label>
              <input type="text" id="nombre" name="nombre" placeholder="Lucía"
                     value="<%= nombreMostrado %>" autocomplete="given-name" required autofocus>
            </div>
            <div class="field">
              <label for="apellidos">Apellidos</label>
              <input type="text" id="apellidos" name="apellidos" placeholder="García Ruiz"
                     value="<%= apellidosMostrado %>" autocomplete="family-name" required>
            </div>
          </div>

          <div class="field<%= error != null ? " field-error" : "" %>">
            <label for="correo">Correo electrónico</label>
            <input type="email" id="correo" name="correo" placeholder="tunombre@instituto.es"
                   value="<%= correoMostrado %>" autocomplete="email" required>
          </div>

          <div class="field">
            <label for="clave">Contraseña</label>
            <input type="password" id="clave" name="clave" placeholder="••••••••"
                   autocomplete="new-password" required>
          </div>

          <div class="field<%= error != null ? " field-error" : "" %>">
            <label for="telefono">Móvil</label>
            <input type="tel" id="telefono" name="telefono" placeholder="612345678"
                   value="<%= telefonoMostrado %>" inputmode="numeric" pattern="[0-9]{9}"
                   autocomplete="tel" required>
            <p class="hint">9 dígitos, empezando por 6 o 7.</p>
          </div>

          <button class="btn-submit" type="submit">Enviar código de verificación</button>
        </form>

        <div class="meta-row">
          <span class="muted">¿Ya tienes cuenta?</span>
          <a href="login.jsp">Iniciar sesión</a>
        </div>

      <% } else { %>

        <h1>Verifica tu correo</h1>
        <p class="sub">Hemos enviado un código a <strong><%= correoPendiente %></strong>.</p>

        <% if (error != null) { %>
        <div class="alert" role="alert">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2"/>
            <path d="M12 8v5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            <circle cx="12" cy="16" r="1" fill="currentColor"/>
          </svg>
          <span><%= error %></span>
        </div>
        <% } else { %>
        <div class="notice">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M3 7l9 6 9-6M5 5h14a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span>Revisa tu bandeja de entrada (y la de spam, por si acaso).</span>
        </div>
        <% } %>

        <form method="post" action="registro.jsp" novalidate>
          <input type="hidden" name="accion" value="verificarCodigo">

          <div class="field">
            <label for="codigo">Código de 6 caracteres</label>
            <input class="code-input<%= error != null ? " field-error-input" : "" %>"
                   type="text" id="codigo" name="codigo" placeholder="------"
                   maxlength="6" autocomplete="one-time-code" required autofocus>
          </div>

          <button class="btn-submit" type="submit">Verificar y crear cuenta</button>
        </form>

        <form method="post" action="registro.jsp">
          <input type="hidden" name="accion" value="cancelar">
          <button class="btn-text" type="submit">Cancelar y volver a empezar</button>
        </form>

      <% } %>

      <div class="price-stub">FERNANPOP · REGISTRATE</div>
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


