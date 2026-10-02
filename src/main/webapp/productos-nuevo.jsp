<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="controller.Controller" %>
<%@ page import="models.Usuario" %>
<%@ page import="models.Producto" %>
<%@ page import="persistence.Log" %>
<%@ page import="utils.Communications" %>
<%@ page import="utils.UI" %>
<%@ page import="utils.Utils" %>
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

    String error = null;
    boolean errorNombre = false;
    boolean errorPrecio = false;
    boolean publicado = false;
    Producto productoCreado = null;

    if ("POST".equalsIgnoreCase(request.getMethod())) {
        String nombre      = request.getParameter("nombre");
        String descripcion = request.getParameter("descripcion");
        String precioStr   = request.getParameter("precio");
        String estado      = request.getParameter("estado");

        if (nombre == null || nombre.trim().isEmpty()) {
            error = "El nombre del producto no puede estar vacío.";
            errorNombre = true;
        } else {
            double precio;
            try {
                precio = Double.parseDouble(precioStr.trim().replace(',', '.'));
            } catch (Exception e) {
                precio = -1;
            }

            if (precio < 0) {
                error = "Introduce un precio válido (0 o más).";
                errorPrecio = true;
            } else {
                precio = Utils.redondearDosDecimales(precio);

                String ID_Producto = app.generaID_Producto();
                Producto nuevo = new Producto(
                        ID_Producto,
                        nombre.trim(),
                        descripcion == null ? "" : descripcion.trim(),
                        precio,
                        estado == null ? "" : estado.trim()
                );

                if (app.addProducto(u, nuevo)) {
                    app.guardarUnico(u);
                    Log.registrarNuevoProducto(ID_Producto, u.getCorreoElectronico());

                    try {
                        Communications.enviaMensajeTelegram(UI.msgNuevoProducto(u, nuevo));
                    } catch (Throwable ignored) { }

                    publicado = true;
                    productoCreado = nuevo;
                } else {
                    error = "No se pudo publicar el producto. Inténtalo de nuevo.";
                }
            }
        }
    }

    java.util.function.Function<String, String> esc = (s) -> {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    };
    String nombreMostrado      = esc.apply(request.getParameter("nombre"));
    String descripcionMostrada = esc.apply(request.getParameter("descripcion"));
    String precioMostrado      = esc.apply(request.getParameter("precio"));
    String estadoSeleccionado  = request.getParameter("estado") == null ? "" : request.getParameter("estado").trim();
%>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Vender algo · FernanPop</title>
<link rel="icon" href="https://raw.githubusercontent.com/Superiorfran1/FernanPop/main/imagenes/ChatGPT_Image_Jun_27__2026__12_28_08_AM-removebg-preview%281%29.png" type="image/png">
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
    /* ---- Paleta "FernanPop" (modo claro) ---- */
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

    /* Morados dedicados a botones principales: necesitan suficiente contraste
       con el texto blanco que llevan encima (≥4.5:1 WCAG AA), por eso van
       aparte de --purple-700/--purple-500 (que se usan también como acento
       de texto, donde conviene un morado más claro y vivo). */
    --btn-from:     #5c3a7a;
    --btn-to:       #5c3a7a;

    /* Verde de éxito, dedicado a la confirmación de publicación.
       Mismo criterio que --btn-from/--btn-to: necesita contraste alto
       contra texto blanco encima, así que va aparte de cualquier verde
       "decorativo" más vivo. */
    --ok:           #277048;
    --ok-bg:        #e7f3ec;
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

  /* Resplandor ambiental morado, muy sutil, en la esquina superior */


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

  .back-link{
    position:fixed;
    top:24px; left:24px;
    display:flex;
    align-items:center;
    gap:6px;
    font-size:13px;
    font-weight:600;
    color: var(--ink-soft);
    text-decoration:none;
    z-index:20;
  }
  .back-link:hover{ color: var(--purple-700); }
  [data-theme="dark"] .back-link:hover{ color: var(--purple-500); }
  .back-link:focus-visible{ outline:2px solid var(--purple-500); outline-offset:3px; border-radius:4px; }

  .brand{
    text-align:center;
    margin-bottom: 24px;
  }
  .brand .mark-logo{
      margin-top: -100px;
    max-width:120px;
    height:auto;
    display:inline-block;
  }
  [data-theme="dark"] .brand .mark-logo{ filter: none; }

  .ticket{
    position:relative;
    width:100%;
    max-width: 420px;
    background: var(--paper);
    border-radius: 2px;
    padding: 36px 34px 28px;
    box-shadow: 0 8px 24px -8px var(--shadow);
    animation: rise .7s cubic-bezier(.25,.46,.45,.94) both;
  }

  @keyframes rise{
    from{ opacity:0; transform: translateY(14px); }
    to{ opacity:1; transform: translateY(0); }
  }

  .ticket::before,
  .ticket::after{
    content:"";
    position:absolute;
    left:0; right:0;
    height:12px;
    background-image: radial-gradient(circle at 8px 8px, transparent 7px, var(--cream) 7.5px);
    background-size: 16px 16px;
    background-repeat: repeat-x;
  }
  .ticket::before{ top:-6px; }
  .ticket::after{ bottom:-6px; transform: scaleY(-1); }

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
    margin:0 0 24px;
  }

  .field{ margin-bottom:18px; }
  .field label{
    display:block;
    font-size:12.5px;
    font-weight:600;
    letter-spacing:.15px;
    color: var(--purple-800);
    margin-bottom:7px;
    text-transform:uppercase;
  }
  [data-theme="dark"] .field label{ color: var(--purple-500); }

  .field input,
  .field textarea{
    width:100%;
    padding:12px 14px;
    border-radius:4px;
    border:1px solid var(--purple-100);
    background: var(--cream);
    color: var(--ink);
    font-size:15px;
    font-family: var(--sans);
    transition: border-color .2s ease, box-shadow .2s ease;
    resize:vertical;
  }
  [data-theme="dark"] .field input,
  [data-theme="dark"] .field textarea{ border-color: var(--purple-300); }

  .field input::placeholder,
  .field textarea::placeholder{ color: var(--ink-soft); opacity:.65; }

  .field input:focus,
  .field textarea:focus{
    outline:none;
    border-color: var(--purple-500);
    box-shadow: 0 0 0 3px var(--ring);
  }

  .field-error input{ border-color: var(--stamp); }

  .field-price{ position:relative; }
  .field-price .currency{
    position:absolute;
    left:14px; top:67%;
    transform: translateY(-50%);
    color: var(--ink-soft);
    font-size:15px;
    pointer-events:none;
  }
  .field-price input{ padding-left:30px; }

  .chip-group{
    display:flex;
    flex-wrap:wrap;
    gap:8px;
  }
  .chip{
    position:relative;
  }
  .chip input{
    position:absolute;
    opacity:0;
    width:1px; height:1px;
  }
  .chip label{
    display:inline-flex;
    align-items:center;
    padding:8px 14px;
    border-radius:999px;
    border:1px solid var(--purple-100);
    background: var(--cream);
    color: var(--ink-soft);
    font-size:13px;
    font-weight:600;
    text-transform:none;
    letter-spacing:0;
    margin:0;
    cursor:pointer;
    transition: border-color .2s ease, background-color .2s ease, color .2s ease;
  }
  [data-theme="dark"] .chip label{ border-color: var(--purple-300); }
  .chip input:checked + label{
    background: var(--btn-from);
    border-color: var(--btn-from);
    color:#fff;
  }
  .chip input:focus-visible + label{
    outline:2px solid var(--purple-500);
    outline-offset:2px;
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
    border-radius:4px;
    margin-bottom:20px;
    line-height:1.4;
  }
  .alert svg{ flex-shrink:0; margin-top:1px; }

  .success-icon{
    width:56px; height:56px;
    border-radius:50%;
    background: var(--ok-bg);
    color: var(--ok);
    display:flex;
    align-items:center;
    justify-content:center;
    margin:0 auto 18px;
  }
  .ticket.success{ text-align:center; }
  .ticket.success h1{ text-align:center; }
  .ticket.success .sub{ text-align:center; }
  .product-summary{
    background: var(--cream);
    border:1px solid var(--purple-100);
    border-radius:8px;
    padding:16px 18px;
    margin:20px 0 24px;
    text-align:left;
  }
  [data-theme="dark"] .product-summary{ border-color: var(--purple-300); }
  .product-summary .row{
    display:flex;
    justify-content:space-between;
    align-items:baseline;
    font-size:14px;
    padding:5px 0;
  }
  .product-summary .row .k{ color: var(--ink-soft); }
  .product-summary .row .v{ font-weight:600; color: var(--ink); }
  .product-summary .row .v.price{
    font-family: var(--serif);
    color: var(--purple-800);
    font-size:18px;
  }
  [data-theme="dark"] .product-summary .row .v.price{ color: var(--purple-500); }

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
  .btn-submit:focus-visible{ outline:2px solid var(--purple-700); outline-offset:3px; }

  .btn-secondary{
    display:block;
    width:100%;
    padding:13px 16px;
    border-radius:4px;
    border:1px solid var(--purple-100);
    background: var(--paper);
    color: var(--purple-700);
    font-size:14px;
    font-weight:600;
    font-family: var(--sans);
    text-align:center;
    text-decoration:none;
    margin-top:10px;
    transition: border-color .2s ease;
  }
  [data-theme="dark"] .btn-secondary{ border-color: var(--purple-300); color: var(--purple-500); }
  .btn-secondary:hover{ border-color: var(--purple-500); }
  .btn-secondary:focus-visible{ outline:2px solid var(--purple-500); outline-offset:2px; }

  .price-stub{
    text-align:center;
    margin-top:23px;
    padding-top:16px;
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
    .ticket{ padding:30px 24px 24px; }
  }
</style>
</head>
<body>

  <a class="back-link" href="dashboard.jsp">
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M19 12H5M12 19l-7-7 7-7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
    </svg>
    Panel
  </a>

  <button class="theme-toggle" id="themeToggle" type="button" aria-label="Cambiar a modo oscuro" aria-pressed="false">
    <span class="knob" id="themeKnob">☾</span>
  </button>

  <main>
    <div class="brand">
      <img class="mark-logo" src="https://raw.githubusercontent.com/Superiorfran1/FernanPop/main/imagenes/ChatGPT_Image_Jun_27__2026__12_28_08_AM-removebg-preview%281%29.png" alt="FernanPop">
    </div>

    <% if (publicado) { %>

      <div class="ticket success">
        <span class="stub-label">Publicado</span>

        <div class="success-icon">
          <svg width="26" height="26" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M5 13l4 4L19 7" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>

        <h1>Ya está en venta</h1>
        <p class="sub">Tu producto ya es visible para todo el instituto.</p>

        <div class="product-summary">
          <div class="row">
            <span class="k">Producto</span>
            <span class="v"><%= esc.apply(productoCreado.getNombre()) %></span>
          </div>
          <div class="row">
            <span class="k">Precio</span>
            <span class="v price"><%= String.format("%.2f €", productoCreado.getPrecio()) %></span>
          </div>
        </div>

        <a class="btn-submit" style="display:block; text-align:center; text-decoration:none; line-height:1.4;" href="productos-nuevo.jsp">Publicar otro producto</a>
        <a class="btn-secondary" href="dashboard.jsp">Volver al panel</a>

        <div class="price-stub">FERNANPOP · TIQUE DE PUBLICACIÓN</div>
      </div>

    <% } else { %>

      <div class="ticket">
        <span class="stub-label">Venta</span>
        <h1>Pon algo en venta</h1>
        <p class="sub">Cuéntanos qué vendes y a qué precio.</p>

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

        <form method="post" action="productos-nuevo.jsp" novalidate>

          <div class="field<%= errorNombre ? " field-error" : "" %>">
            <label for="nombre">Nombre del producto</label>
            <input type="text" id="nombre" name="nombre" placeholder="Calculadora científica Casio"
                   value="<%= nombreMostrado %>" required autofocus>
          </div>

          <div class="field">
            <label for="descripcion">Descripción</label>
            <textarea id="descripcion" name="descripcion" rows="3"
                      placeholder="Apenas usada, con su funda original..."><%= descripcionMostrada %></textarea>
          </div>

          <div class="field field-price<%= errorPrecio ? " field-error" : "" %>">
            <label for="precio">Precio</label>
                <span class="currency" aria-hidden="true">€</span>
            <input type="text" id="precio" name="precio" placeholder="0,00"
                   value="<%= precioMostrado %>" inputmode="decimal" required>
          </div>

          <div class="field">
            <label>Estado</label>
            <div class="chip-group">
              <% String[] estados = { "Nuevo", "Como nuevo", "Buen estado", "Usado" };
                 for (int i = 0; i < estados.length; i++) {
                   String est = estados[i];
                   boolean marcado = est.equalsIgnoreCase(estadoSeleccionado);
              %>
              <span class="chip">
                <input type="radio" name="estado" id="estado<%= i %>" value="<%= est %>" <%= marcado ? "checked" : "" %>>
                <label for="estado<%= i %>"><%= est %></label>
              </span>
              <% } %>
            </div>
          </div>

          <button class="btn-submit" type="submit">Publicar producto</button>
        </form>

        <div class="price-stub">FERNANPOP · PUBLICAR NUEVO PRODUCTO</div>
      </div>

    <% } %>
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








