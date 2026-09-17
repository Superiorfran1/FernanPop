<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="controller.Controller" %>
<%@ page import="models.Usuario" %>
<%@ page import="models.Producto" %>
<%@ page import="models.Trato" %>
<%@ page import="persistence.Log" %>
<%@ page import="utils.Communications" %>
<%@ page import="utils.UI" %>
<%@ page import="utils.Utils" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Calendar" %>
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
    String mensajeTipo = "info"; // "info" o "error"
    String accion = request.getParameter("accion");

    java.util.function.Function<String, String> esc = (s) -> {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    };

    if ("POST".equalsIgnoreCase(request.getMethod()) && "editar".equals(accion)) {
        String idProducto = request.getParameter("idProducto");
        Producto p = u.getProductoFromEnVenta(idProducto);

        if (p == null) {
            mensaje = "No se encontró el producto a editar.";
            mensajeTipo = "error";
        } else {
            String nuevoNombre = request.getParameter("nombre");
            String nuevaDesc   = request.getParameter("descripcion");
            String nuevoPrecioStr = request.getParameter("precio");

            boolean modificado = false;
            ArrayList<String> antiguosInteresados = new ArrayList<>(p.getInteresados());

            if (nuevoNombre != null && !nuevoNombre.trim().isEmpty() && !nuevoNombre.trim().equals(p.getNombre())) {
                String antes = p.getNombre();
                p.setNombre(nuevoNombre.trim());
                modificado = true;
                try { Communications.enviaMensajeTelegram(UI.msgProductoModificado(u, p, "Título", antes, p.getNombre())); }
                catch (Throwable ignored) { }
            }

            if (nuevaDesc != null && !nuevaDesc.trim().isEmpty() && !nuevaDesc.trim().equals(p.getDescripcion())) {
                String antes = p.getDescripcion();
                p.setDescripcion(nuevaDesc.trim());
                modificado = true;
                try { Communications.enviaMensajeTelegram(UI.msgProductoModificado(u, p, "Descripción", antes, p.getDescripcion())); }
                catch (Throwable ignored) { }
            }

            if (nuevoPrecioStr != null && !nuevoPrecioStr.trim().isEmpty()) {
                try {
                    double nuevoPrecio = Utils.redondearDosDecimales(
                            Double.parseDouble(nuevoPrecioStr.trim().replace(',', '.')));
                    if (nuevoPrecio >= 0 && nuevoPrecio != p.getPrecio()) {
                        String antes = p.getPrecio() + " €";
                        p.setPrecio(nuevoPrecio);
                        modificado = true;
                        try { Communications.enviaMensajeTelegram(UI.msgProductoModificado(u, p, "Precio", antes, p.getPrecio() + " €")); }
                        catch (Throwable ignored) { }
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            if (modificado) {
                if (!antiguosInteresados.isEmpty()) {
                    p.getInteresados().clear();
                    for (String emailInteresado : antiguosInteresados) {
                        try {
                            String asunto = "Modificación en: " + p.getNombre();
                            String cuerpo = UI.generaEmailModificacion(p.getNombre(), u);
                            Communications.enviarConGMail(emailInteresado, asunto, cuerpo);
                        } catch (Throwable ignored) { }
                    }
                }
                app.guardarUnico(u);
                mensaje = "Producto actualizado correctamente.";
            } else {
                mensaje = "No se realizaron cambios.";
            }
        }
    }

    if ("POST".equalsIgnoreCase(request.getMethod()) && "eliminar".equals(accion)) {
        String idProducto = request.getParameter("idProducto");
        Producto p = u.getProductoFromEnVenta(idProducto);

        if (p != null && u.deleteProducto(idProducto)) {
            app.guardarUnico(u);
            try { Communications.enviaMensajeTelegram(UI.msgProductoEliminado(u, idProducto, p.getNombre())); }
            catch (Throwable ignored) { }
            mensaje = "Producto eliminado.";
        } else {
            mensaje = "No se encontró el producto a eliminar.";
            mensajeTipo = "error";
        }
    }

    if ("POST".equalsIgnoreCase(request.getMethod()) && "cerrarVenta".equals(accion)) {
        String idProducto = request.getParameter("idProducto");
        String emailComprador = request.getParameter("comprador");
        Producto p = u.getProductoFromEnVenta(idProducto);

        if (p == null) {
            mensaje = "No se encontró el producto.";
            mensajeTipo = "error";
        } else if (emailComprador == null || emailComprador.trim().isEmpty()
                || !p.getInteresados().contains(emailComprador)) {
            mensaje = "Elige uno de los interesados de la lista.";
            mensajeTipo = "error";
        } else {
            Usuario comprador = app.buscaPorCorreoElectronico(emailComprador);
            if (comprador == null) {
                mensaje = "El comprador ya no existe en el sistema.";
                mensajeTipo = "error";
            } else {
                double precioFinal = p.getPrecio();
                Trato trato = app.cerrarVenta(u, p.getID(), comprador, precioFinal);

                if (trato == null) {
                    mensaje = "Error al registrar la venta.";
                    mensajeTipo = "error";
                } else {
                    app.guardarUnico(comprador);
                    app.guardarUnico(u);
                    Log.registrarVentaCerrada(u.getCorreoElectronico(), comprador.getCorreoElectronico());

                    try {
                        Communications.enviarReciboPDFTrato(trato, u, comprador);
                    } catch (Throwable ignored) { }

                    // Confirmación por correo a ambas partes del trato: al
                    // vendedor con generarEmailVenta(), al comprador con
                    // generarEmailCompra(). Cada uno recibe el correo a su
                    // propia dirección, no el mismo contenido para los dos.
                    try {
                        String precioStr = String.format("%.2f", precioFinal);

                        String asuntoVendedor = "Venta confirmada: " + p.getNombre();
                        String cuerpoVendedor = UI.generarEmailVenta(
                                u.getNombre(), p.getNombre(), precioStr,
                                comprador.getNombre(), comprador.getCorreoElectronico());
                        Communications.enviarConGMail(u.getCorreoElectronico(), asuntoVendedor, cuerpoVendedor);

                        String asuntoComprador = "Compra confirmada: " + p.getNombre();
                        String cuerpoComprador = UI.generarEmailCompra(
                                comprador.getNombre(), p.getNombre(), precioStr,
                                u.getNombre(), u.getCorreoElectronico());
                        Communications.enviarConGMail(comprador.getCorreoElectronico(), asuntoComprador, cuerpoComprador);
                    } catch (Throwable ignored) {
                        // Si fallan los correos, la venta ya quedó registrada igualmente
                    }

                    try {
                        java.text.SimpleDateFormat sdfTel = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
                        String fechaFormateada = sdfTel.format(Calendar.getInstance().getTime());
                        Communications.enviaMensajeTelegram(UI.msgVentaRealizada(comprador, u, p, fechaFormateada));
                    } catch (Throwable ignored) { }

                    mensaje = "Venta cerrada con " + comprador.getNombre() + " por "
                            + String.format("%.2f €", precioFinal) + ".";
                }
            }
        }
    }

    // Productos en venta, ordenados por precio (igual que ordenarPorPrecio() en consola)
    ArrayList<Producto> productos = new ArrayList<>(u.getEnVenta());
    productos.sort((a, b) -> Double.compare(a.getPrecio(), b.getPrecio()));

    // Panel a expandir tras la recarga (?panel=ID&modo=editar|vender)
    String panelAbierto = request.getParameter("panel");
    String modoAbierto = request.getParameter("modo");
%>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Mis productos · FernanPop</title>
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

  .brand{
      text-align:center;
      margin-bottom: 24px;
  }
  .brand .mark-logo{
      margin: -50px 0 -50px -220px;
      max-width:100px;
      height:auto;
      display:inline-block;
  }
  [data-theme="dark"] .brand .mark-logo{ filter: none; }

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

  .page-title{
    margin: 6px 0 24px;
  }
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
  .notice-banner.info{
    background: var(--ok-bg);
    color: var(--ok);
  }
  .notice-banner.error{
    background: color-mix(in srgb, var(--stamp) 12%, var(--paper));
    border:1px solid color-mix(in srgb, var(--stamp) 40%, transparent);
    color: var(--stamp);
  }
  .notice-banner svg{ flex-shrink:0; margin-top:1px; }

  .product-card{
    background: var(--paper);
    border:1px solid var(--purple-100);
    border-radius:14px;
    margin-bottom:14px;
    overflow:hidden;
  }
  [data-theme="dark"] .product-card{ border-color: var(--purple-300); }

  .product-main{
    display:flex;
    align-items:center;
    justify-content:space-between;
    gap:14px;
    padding:16px 18px;
  }
  .product-main .info .name{
    font-size:15px;
    font-weight:600;
    color: var(--ink);
    margin:0 0 4px;
  }
  .product-main .info .meta{
    font-size:12.5px;
    color: var(--ink-soft);
  }
  .product-main .price-col{
    display:flex;
    flex-direction:column;
    align-items:flex-end;
    gap:6px;
    flex-shrink:0;
  }
  .product-main .price{
    font-family: var(--serif);
    font-size:18px;
    font-weight:700;
    color: var(--purple-800);
    white-space:nowrap;
  }
  [data-theme="dark"] .product-main .price{ color: var(--purple-500); }

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
  }
  [data-theme="dark"] .badge{ color: var(--purple-500); }

  .badge.has-interest{ background: var(--ok-bg); color: var(--ok); }

  .actions-row{
    display:flex;
    gap:8px;
    padding:0 18px 16px;
  }
  .action-btn{
    flex:1;
    display:flex;
    align-items:center;
    justify-content:center;
    gap:6px;
    padding:8px 10px;
    border-radius:9px;
    border:1px solid var(--purple-100);
    background: var(--cream);
    color: var(--ink-soft);
    font-size:12.5px;
    font-weight:600;
    font-family: var(--sans);
    cursor:pointer;
    text-decoration:none;
    transition: border-color .2s ease, color .2s ease;
  }
  [data-theme="dark"] .action-btn{ border-color: var(--purple-300); }
  .action-btn:hover{ border-color: var(--purple-500); color: var(--purple-700); }
  [data-theme="dark"] .action-btn:hover{ color: var(--purple-500); }
  .action-btn.danger:hover{ border-color: var(--stamp); color: var(--stamp); }
  .action-btn.primary{
    background: var(--purple-700);
    border-color: transparent;
    color:#fff;
  }
  .action-btn.primary:hover{ color:#fff; filter:brightness(1.06); }
  .action-btn.disabled{ opacity:.45; cursor:not-allowed; pointer-events:none; }
  .action-btn:focus-visible{ outline:2px solid var(--purple-500); outline-offset:2px; }

  .panel{
    border-top:1px solid var(--purple-100);
    background: var(--cream);
    padding:18px;
  }
  [data-theme="dark"] .panel{ border-color: var(--purple-300); }
  .panel h3{
    font-size:13px;
    font-weight:700;
    text-transform:uppercase;
    letter-spacing:.15px;
    color: var(--purple-800);
    margin:0 0 14px;
  }
  [data-theme="dark"] .panel h3{ color: var(--purple-500); }

  .field{ margin-bottom:14px; }
  .field label{
    display:block;
    font-size:12px;
    font-weight:600;
    color: var(--ink-soft);
    margin-bottom:6px;
  }
  .field input,
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
  [data-theme="dark"] .field input,
  [data-theme="dark"] .field textarea{ border-color: var(--purple-300); }
  .field input:focus,
  .field textarea:focus{
    outline:none;
    border-color: var(--purple-500);
    box-shadow: 0 0 0 3px var(--ring);
  }
  .field .hint{ margin:5px 2px 0; font-size:11px; color: var(--ink-soft); }

  .panel-actions{ display:flex; gap:8px; margin-top:4px; }
  .btn-panel-submit{
    padding:10px 18px;
    border:1px solid var(--purple-700);
    border-radius:9px;
    background: var(--purple-700);
    color:#fff;
    font-size:13.5px;
    font-weight:600;
    font-family: var(--sans);
    cursor:pointer;
  }
  .btn-panel-submit:hover{ filter:brightness(1.06); }
  .btn-panel-cancel{
    padding:10px 18px;
    border-radius:9px;
    border:1px solid var(--purple-100);
    background: var(--paper);
    color: var(--ink-soft);
    font-size:13.5px;
    font-weight:600;
    text-decoration:none;
    display:flex;
    align-items:center;
  }
  [data-theme="dark"] .btn-panel-cancel{ border-color: var(--purple-300); }

  .buyer-option{
    display:flex;
    align-items:center;
    gap:10px;
    padding:10px 12px;
    border:1px solid var(--purple-100);
    border-radius:9px;
    margin-bottom:8px;
    background: var(--paper);
    cursor:pointer;
  }
  [data-theme="dark"] .buyer-option{ border-color: var(--purple-300); }
  .buyer-option input{ accent-color: var(--purple-700); width:16px; height:16px; flex-shrink:0; }
  .buyer-option label{
    font-size:13.5px;
    color: var(--ink);
    cursor:pointer;
    margin:0;
  }
  .buyer-option:has(input:checked){
    border-color: var(--purple-500);
    background: var(--purple-100);
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

  @media (max-width: 560px){
    .product-main{ flex-wrap:wrap; }
    .actions-row{ flex-wrap:wrap; }
    .action-btn{ flex:1 1 45%; }
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
          <div class="brand">
              <img class="mark-logo" src="https://raw.githubusercontent.com/Superiorfran1/imagenesFernanPop/main/ChatGPT_Image_Jun_27__2026__12_28_08_AM-removebg-preview.png" alt="FernanPop">
          </div>
      </div>
      <button class="theme-toggle" id="themeToggle" type="button" aria-label="Cambiar a modo oscuro" aria-pressed="false">
        <span class="knob" id="themeKnob">☾</span>
      </button>
    </div>

    <div class="page-title">
      <h1>Mis productos</h1>
      <p>Gestiona lo que tienes en venta: edítalo, retíralo o cierra una venta.</p>
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

    <% if (productos.isEmpty()) { %>

    <div class="empty">No tienes ningún producto en venta todavía. <a href="productos-nuevo.jsp">Publica el primero</a>.</div>

    <% } else {
       for (Producto p : productos) {
         boolean tieneInteresados = !p.getInteresados().isEmpty();
         boolean editarAbierto = "editar".equals(modoAbierto) && p.getID().equals(panelAbierto);
         boolean venderAbierto = "vender".equals(modoAbierto) && p.getID().equals(panelAbierto);
    %>
    <div class="product-card">
      <div class="product-main">
        <div class="info">
          <p class="name"><%= esc.apply(p.getNombre()) %></p>
          <p class="meta">
            <% if (p.getEstado() != null && !p.getEstado().isEmpty()) { %><%= esc.apply(p.getEstado()) %> · <% } %>
            <%= p.getInteresados().size() %> interesado<%= p.getInteresados().size() == 1 ? "" : "s" %>
          </p>
        </div>
        <div class="price-col">
          <div class="price"><%= String.format("%.2f €", p.getPrecio()) %></div>
          <span class="badge<%= tieneInteresados ? " has-interest" : "" %>">
            <%= tieneInteresados ? "Con interés" : "Sin interés" %>
          </span>
        </div>
      </div>

      <div class="actions-row">
        <a class="action-btn" href="mis-productos.jsp?panel=<%= p.getID() %>&modo=editar#p-<%= p.getID() %>">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
          Editar
        </a>
        <a class="action-btn<%= tieneInteresados ? "" : " disabled" %>"
           href="<%= tieneInteresados ? "mis-productos.jsp?panel=" + p.getID() + "&modo=vender#p-" + p.getID() : "#" %>"
           <%= tieneInteresados ? "" : "tabindex=\"-1\" aria-disabled=\"true\"" %>>
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M20 6L9 17l-5-5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
          Cerrar venta
        </a>
        <form method="post" action="mis-productos.jsp" style="flex:1; margin:0;"
              onsubmit="return confirm('¿Seguro que quieres eliminar este producto?');">
          <input type="hidden" name="accion" value="eliminar">
          <input type="hidden" name="idProducto" value="<%= p.getID() %>">
          <button class="action-btn danger" type="submit" style="width:100%;">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
            Eliminar
          </button>
        </form>
      </div>

      <% if (editarAbierto) { %>
      <div class="panel" id="p-<%= p.getID() %>">
        <h3>Editar producto</h3>
        <form method="post" action="mis-productos.jsp">
          <input type="hidden" name="accion" value="editar">
          <input type="hidden" name="idProducto" value="<%= p.getID() %>">

          <div class="field">
            <label for="nombre-<%= p.getID() %>">Nombre</label>
            <input type="text" id="nombre-<%= p.getID() %>" name="nombre" placeholder="<%= esc.apply(p.getNombre()) %>">
          </div>
          <div class="field">
            <label for="desc-<%= p.getID() %>">Descripción</label>
            <textarea id="desc-<%= p.getID() %>" name="descripcion" rows="2" placeholder="<%= esc.apply(p.getDescripcion()) %>"></textarea>
          </div>
          <div class="field">
            <label for="precio-<%= p.getID() %>">Precio</label>
            <input type="text" id="precio-<%= p.getID() %>" name="precio" placeholder="<%= String.format("%.2f", p.getPrecio()) %>" inputmode="decimal">
            <p class="hint">Deja un campo vacío para no cambiarlo.</p>
            <% if (tieneInteresados) { %>
            <p class="hint">Si cambias algo, se avisará a los <%= p.getInteresados().size() %> interesado<%= p.getInteresados().size() == 1 ? "" : "s" %> y se reiniciará esa lista.</p>
            <% } %>
          </div>

          <div class="panel-actions">
            <button class="btn-panel-submit" type="submit">Guardar cambios</button>
            <a class="btn-panel-cancel" href="mis-productos.jsp">Cancelar</a>
          </div>
        </form>
      </div>
      <% } %>

      <% if (venderAbierto && tieneInteresados) { %>
      <div class="panel" id="p-<%= p.getID() %>">
        <h3>Cerrar venta</h3>
        <form method="post" action="mis-productos.jsp">
          <input type="hidden" name="accion" value="cerrarVenta">
          <input type="hidden" name="idProducto" value="<%= p.getID() %>">

          <div class="field">
            <label>Elige al comprador</label>
            <% int idxComprador = 0;
               for (String correoInteresado : p.getInteresados()) {
                 idxComprador++;
                 Usuario interesado = app.buscaPorCorreoElectronico(correoInteresado);
                 String nombreInteresado = interesado != null ? interesado.getNombre() : correoInteresado;
                 String radioId = "comp-" + p.getID() + "-" + idxComprador;
            %>
            <div class="buyer-option">
              <input type="radio" name="comprador" id="<%= radioId %>" value="<%= esc.apply(correoInteresado) %>">
              <label for="<%= radioId %>"><%= esc.apply(nombreInteresado) %> · <%= esc.apply(correoInteresado) %></label>
            </div>
            <% } %>
            <p class="hint">El precio de venta será el precio actual: <%= String.format("%.2f €", p.getPrecio()) %>.</p>
          </div>

          <div class="panel-actions">
            <button class="btn-panel-submit" type="submit">Confirmar venta</button>
            <a class="btn-panel-cancel" href="mis-productos.jsp">Cancelar</a>
          </div>
        </form>
      </div>
      <% } %>

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






