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

    String mensaje = null;
    String mensajeTipo = "info";

    if ("POST".equalsIgnoreCase(request.getMethod()) && "valorar".equals(request.getParameter("accion"))) {
        String idTrato = request.getParameter("idTrato");
        String puntuacionStr = request.getParameter("puntuacion");
        String comentario = request.getParameter("comentario");

        Trato t = u.getTratoCompras(idTrato);

        if (t == null) {
            mensaje = "No se encontró ese trato entre tus compras.";
            mensajeTipo = "error";
        } else if (!u.getValoracionesPendientes().contains(idTrato)) {
            mensaje = "Ese trato ya estaba valorado.";
            mensajeTipo = "error";
        } else {
            int puntuacion;
            try {
                puntuacion = Integer.parseInt(puntuacionStr);
            } catch (Exception e) {
                puntuacion = -1;
            }

            if (puntuacion < 0 || puntuacion > 5) {
                mensaje = "Elige una puntuación entre 0 y 5 estrellas.";
                mensajeTipo = "error";
            } else {
                t.setPuntuacion(puntuacion);
                if (comentario != null && !comentario.trim().isEmpty()) {
                    t.setComentario(comentario.trim());
                }

                app.borraValoracionPendiente(u, t.getID());
                app.guardarUnico(u);

                Usuario vendedor = app.buscaPorCorreoElectronico(t.getCorreoVendedor());
                if (vendedor != null) {
                    app.guardarUnico(vendedor);
                }

                mensaje = puntuacion == 0
                        ? "Valoración registrada."
                        : "¡Gracias por tu valoración de " + puntuacion + " estrella" + (puntuacion == 1 ? "" : "s") + "!";
            }
        }
    }

    ArrayList<Trato> pendientes = app.getValoracionesPendientes(u);

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
<title>Valoraciones pendientes · FernanPop</title>
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
    max-width: 720px;
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

  .all-done{
    background: var(--paper);
    border:1px solid var(--purple-100);
    border-radius:16px;
    padding:44px 24px;
    text-align:center;
  }
  [data-theme="dark"] .all-done{ border-color: var(--purple-300); }
  .all-done .icon{
    width:56px; height:56px;
    border-radius:50%;
    background: var(--ok-bg);
    color: var(--ok);
    display:flex;
    align-items:center;
    justify-content:center;
    margin:0 auto 16px;
  }
  .all-done h2{
    font-family: var(--serif);
    font-size:19px;
    margin:0 0 6px;
  }
  .all-done p{
    margin:0;
    font-size:13.5px;
    color: var(--ink-soft);
  }
  .all-done a{
    color: var(--purple-700);
    font-weight:600;
    text-decoration:none;
  }
  [data-theme="dark"] .all-done a{ color: var(--purple-500); }

  .trato-card{
    background: var(--paper);
    border:1px solid var(--purple-100);
    border-radius:14px;
    margin-bottom:14px;
    overflow:hidden;
  }
  [data-theme="dark"] .trato-card{ border-color: var(--purple-300); }

  .trato-head{
    display:flex;
    align-items:center;
    justify-content:space-between;
    gap:14px;
    padding:16px 18px;
  }
  .trato-head .info .name{
    font-size:15px;
    font-weight:600;
    color: var(--ink);
    margin:0 0 4px;
  }
  .trato-head .info .meta{
    font-size:12.5px;
    color: var(--ink-soft);
  }
  .trato-head .price{
    font-family: var(--serif);
    font-size:18px;
    font-weight:700;
    color: var(--purple-800);
    white-space:nowrap;
    flex-shrink:0;
  }
  [data-theme="dark"] .trato-head .price{ color: var(--purple-500); }

  .rate-panel{
    border-top:1px solid var(--purple-100);
    background: var(--cream);
    padding:18px;
  }
  [data-theme="dark"] .rate-panel{ border-color: var(--purple-300); }

  .star-picker{
    display:flex;
    gap:6px;
    margin-bottom:14px;
  }
  .star-picker input{
    position:absolute;
    opacity:0;
    width:1px; height:1px;
  }
  .star-picker label{
    cursor:pointer;
    color: var(--purple-100);
    font-size:30px;
    line-height:1;
    transition: color .15s ease;
  }
  [data-theme="dark"] .star-picker label{ color: var(--purple-300); }

  /* Resaltado progresivo: todas las estrellas hasta la elegida se iluminan,
     usando el truco de "elegir hacia atrás en el DOM" con ~ (general sibling)
     sobre radios en orden inverso. */
  .star-picker{ flex-direction:row-reverse; }
  .star-picker input:checked ~ label,
  .star-picker label:hover,
  .star-picker label:hover ~ label{
    color: var(--gold);
  }
  .star-picker input:focus-visible ~ label{
    outline:2px solid var(--purple-500);
    outline-offset:2px;
    border-radius:4px;
  }

  .scale-hint{
    font-size:12px;
    color: var(--ink-soft);
    margin:-35px 0 16px;
  }

  .field{ margin-bottom:14px; }
  .field label{
    display:block;
    font-size:12px;
    font-weight:600;
    color: var(--ink-soft);
    margin-bottom:6px;
  }
  .field textarea{
    width:100%;
    padding:10px 12px;
    border-radius:9px;
    border:1px solid var(--purple-100);
    background: var(--paper);
    color: var(--ink);
    font-size:14px;
    font-family: var(--sans);
    resize:vertical;
  }
  [data-theme="dark"] .field textarea{ border-color: var(--purple-300); }
  .field textarea:focus{
    outline:none;
    border-color: var(--purple-500);
    box-shadow: 0 0 0 3px var(--ring);
  }

  .btn-rate-submit{
    padding:10px 20px;
    border:1px solid var(--purple-700);
    border-radius:9px;
    background: var(--purple-700);
    color:#fff;
    font-size:13.5px;
    font-weight:600;
    font-family: var(--sans);
    cursor:pointer;
  }
  .btn-rate-submit:hover{ filter:brightness(1.06); }
  .btn-rate-submit:focus-visible{ outline:2px solid var(--purple-700); outline-offset:2px; }

  @media (prefers-reduced-motion: reduce){ *{ transition:none !important; } }

  @media (max-width: 480px){
    .trato-head{ flex-wrap:wrap; }
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
      <h1>Valoraciones pendientes</h1>
      <p>Cuenta cómo te fue con tus compras para ayudar a otros usuarios.</p>
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

    <% if (pendientes.isEmpty()) { %>

    <div class="all-done">
      <div class="icon">
        <svg width="26" height="26" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path d="M5 13l4 4L19 7" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </div>
      <h2>¡Todo al día!</h2>
      <p>No tienes ninguna valoración pendiente. <a href="buscar.jsp">Sigue explorando el mercadillo</a>.</p>
    </div>

    <% } else {
       for (Trato t : pendientes) {
         Usuario vendedor = app.buscaPorCorreoElectronico(t.getCorreoVendedor());
         String nombreVendedor = vendedor != null ? vendedor.getNombre() : t.getCorreoVendedor();
    %>
    <div class="trato-card">
      <div class="trato-head">
        <div class="info">
          <p class="name"><%= esc.apply(t.getProducto().getNombre()) %></p>
          <p class="meta">Comprado a <%= esc.apply(nombreVendedor) %> · <%= sdf.format(t.getFecha().getTime()) %></p>
        </div>
        <div class="price"><%= String.format("%.2f €", t.getPrecio()) %></div>
      </div>

      <div class="rate-panel">
        <form method="post" action="valoraciones.jsp">
          <input type="hidden" name="accion" value="valorar">
          <input type="hidden" name="idTrato" value="<%= t.getID() %>">

          <div class="star-picker" role="radiogroup" aria-label="Puntuación de 0 a 5 estrellas">
            <% for (int n = 5; n >= 0; n--) { %>
            <input type="radio" name="puntuacion" id="r<%= n %>-<%= t.getID() %>" value="<%= n %>" <%= n == 5 ? "required" : "" %>>
            <label for="r<%= n %>-<%= t.getID() %>" title="<%= n %> estrella<%= n == 1 ? "" : "s" %>"><%= n == 0 ? "✕" : "★" %></label>
            <% } %>
          </div>
          <p class="scale-hint">0 = Horrible · 5 = Excelente</p>

          <div class="field">
            <label for="comentario-<%= t.getID() %>">Comentario (opcional)</label>
            <textarea id="comentario-<%= t.getID() %>" name="comentario" rows="2" placeholder="¿Cómo fue la experiencia?"></textarea>
          </div>

          <button class="btn-rate-submit" type="submit">Enviar valoración</button>
        </form>
      </div>
    </div>
    <% } } %>

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





