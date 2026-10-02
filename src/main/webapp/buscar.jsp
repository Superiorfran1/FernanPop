<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="controller.Controller" %>
<%@ page import="models.Usuario" %>
<%@ page import="models.Producto" %>
<%@ page import="utils.Communications" %>
<%@ page import="utils.UI" %>
<%@ page import="java.util.ArrayList" %>
<%
    Usuario u = (Usuario) session.getAttribute("usuarioActivo");

    Controller app = (Controller) session.getAttribute("app");
    if (app == null) {
        app = new Controller();
        session.setAttribute("app", app);
    }

    String mensaje = null;
    String mensajeTipo = "info";

    if ("POST".equalsIgnoreCase(request.getMethod()) && "interesar".equals(request.getParameter("accion"))) {
        if (u == null) {
            mensaje = "Inicia sesión para mostrar interés en un producto.";
            mensajeTipo = "error";
        } else {
            String idProducto = request.getParameter("idProducto");
            Producto p = app.buscaProductoId(idProducto);
            Usuario vendedor = app.buscaPropietarioProducto(idProducto);

            if (p == null || vendedor == null) {
                mensaje = "El producto ya no está disponible.";
                mensajeTipo = "error";
            } else if (vendedor.getCorreoElectronico().equalsIgnoreCase(u.getCorreoElectronico())) {
                mensaje = "No puedes interesarte en tu propio producto.";
                mensajeTipo = "error";
            } else if (p.getInteresados().contains(u.getCorreoElectronico())) {
                mensaje = "Ya habías mostrado interés en este producto.";
            } else {
                p.addInteresado(u.getCorreoElectronico());
                app.guardarUnico(vendedor);

                try {
                    String asunto = "¡Alguien está interesado en tu producto!";
                    String cuerpo = UI.generarEmailInteresado(vendedor.getNombre(), p.getNombre(),
                            u.getNombre(), u.getApellidos(), u.getCorreoElectronico());
                    Communications.enviarConGMail(vendedor.getCorreoElectronico(), asunto, cuerpo);
                } catch (Throwable ignored) {
                }

                mensaje = "Interés registrado. Hemos avisado al vendedor.";
            }
        }
    }

    boolean esFragmento = "1".equals(request.getParameter("ajax"));

    // Búsqueda por texto (en nombre Y descripción, delegado en Controller) o listado completo
    String textoBusqueda = request.getParameter("q");
    ArrayList<Producto> resultados;
    if (textoBusqueda != null && !textoBusqueda.trim().isEmpty()) {
        resultados = app.buscaProductosTexto(textoBusqueda.trim());
    } else {
        resultados = app.getAllProductos();
        textoBusqueda = "";
    }
    resultados.sort((a, b) -> Double.compare(a.getPrecio(), b.getPrecio()));

    java.util.function.Function<String, String> esc = (s) -> {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    };
    String textoMostrado = esc.apply(textoBusqueda);

    StringBuilder htmlResultados = new StringBuilder();

    if (!textoMostrado.isEmpty()) {
        htmlResultados.append("<p class=\"results-meta\">")
                .append(resultados.size()).append(resultados.size() == 1 ? " resultado para \"" : " resultados para \"")
                .append(textoMostrado).append("\" · <a href=\"buscar.jsp\">Ver todos los productos</a></p>");
    }
    if (u == null) {
        htmlResultados.append("<p class=\"results-meta\"><a href=\"login.jsp\">Inicia sesión</a> para mostrar interés en un producto.</p>");
    }

    htmlResultados.append("<div class=\"product-grid\">");
    if (resultados.isEmpty()) {
        htmlResultados.append("<div class=\"empty\">");
        if (!textoMostrado.isEmpty()) {
            htmlResultados.append("No se encontró nada para \"").append(textoMostrado)
                    .append("\". <a href=\"buscar.jsp\">Ver todos los productos</a>.");
        } else {
            htmlResultados.append("No hay productos en venta en este momento.");
        }
        htmlResultados.append("</div>");
    } else {
        for (Producto p : resultados) {
            Usuario vendedor = app.buscaPropietarioProducto(p.getID());
            boolean esPropio = u != null && vendedor != null && vendedor.getCorreoElectronico().equalsIgnoreCase(u.getCorreoElectronico());
            boolean yaInteresado = u != null && p.getInteresados().contains(u.getCorreoElectronico());
            String nombreP = esc.apply(p.getNombre());
            String descP = esc.apply(p.getDescripcion());
            String estadoP = esc.apply(p.getEstado());
            String nombreVendedor = vendedor != null ? esc.apply(vendedor.getNombre()) : "—";

            htmlResultados.append("<div class=\"product-card\">")
                    .append("<div class=\"head\"><p class=\"name\">").append(nombreP).append("</p>")
                    .append("<span class=\"price\">").append(String.format("%.2f €", p.getPrecio())).append("</span></div>");

            if (p.getDescripcion() != null && !p.getDescripcion().isEmpty()) {
                htmlResultados.append("<p class=\"desc\">").append(descP).append("</p>");
            }

            htmlResultados.append("<div class=\"meta-row\">");
            if (p.getEstado() != null && !p.getEstado().isEmpty()) {
                htmlResultados.append("<span class=\"badge\">").append(estadoP).append("</span>");
            }
            htmlResultados.append("<span class=\"seller\">Vende ").append(nombreVendedor).append("</span></div>");

            htmlResultados.append("<div class=\"actions\">");
            if (u == null) {
                htmlResultados.append("<a class=\"btn-interest\" style=\"text-decoration:none;\" href=\"login.jsp\">Inicia sesión para interesarte</a>");
            } else if (esPropio) {
                htmlResultados.append("<button class=\"btn-interest\" type=\"button\" disabled>Es tuyo</button>");
            } else if (yaInteresado) {
                htmlResultados.append("<button class=\"btn-interest already\" type=\"button\" disabled>"
                        + "<svg width=\"13\" height=\"13\" viewBox=\"0 0 24 24\" fill=\"none\" aria-hidden=\"true\"><path d=\"M5 13l4 4L19 7\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/></svg>"
                        + "Ya interesado</button>");
            } else {
                htmlResultados.append("<form method=\"post\" action=\"buscar.jsp\" style=\"flex:1; margin:0; display:flex;\">")
                        .append("<input type=\"hidden\" name=\"accion\" value=\"interesar\">")
                        .append("<input type=\"hidden\" name=\"idProducto\" value=\"").append(p.getID()).append("\">");
                if (!textoMostrado.isEmpty()) {
                    htmlResultados.append("<input type=\"hidden\" name=\"q\" value=\"").append(textoMostrado).append("\">");
                }
                htmlResultados.append("<button class=\"btn-interest\" type=\"submit\">Me interesa</button></form>");
            }
            htmlResultados.append("</div></div>");
        }
    }
    htmlResultados.append("</div>");

    if (esFragmento) {
        // MODO FRAGMENTO: respuesta a fetch() del buscador en vivo.
        // Solo el HTML de dentro de #resultsArea, nada de página completa.
        response.setContentType("text/html; charset=UTF-8");
        out.print(htmlResultados);
        out.flush();
        return;
    }
%>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Buscar productos · FernanPop</title>
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
    max-width: 960px;
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

  .page-title{ margin: 6px 0 20px; }
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

  .search-bar{
    display:flex;
    gap:10px;
    margin-bottom:22px;
  }
  .search-bar .input-wrap{
    position:relative;
    flex:1;
  }
  .search-bar .input-wrap svg{
    position:absolute;
    left:14px; top:50%;
    transform: translateY(-50%);
    color: var(--ink-soft);
    pointer-events:none;
  }
  .search-bar input{
    width:100%;
    padding:12px 14px 13px 40px;
    border-radius:8px;
    border:1px solid var(--purple-100);
    background: var(--paper);
    color: var(--ink);
    font-size:15px;
    font-family: var(--sans);
  }
  [data-theme="dark"] .search-bar input{ border-color: var(--purple-300); }
  .search-bar input::placeholder{ color: var(--ink-soft); opacity:.65; }
  .search-bar input:focus{
    outline:none;
    border-color: var(--purple-500);
    box-shadow: 0 0 0 3px var(--ring);
  }
  .search-bar button{
    padding:13px 22px;
    border:1px solid var(--purple-700);
    border-radius:8px;
    background: var(--purple-700);
    color:#fff;
    font-size:14.5px;
    font-weight:600;
    font-family: var(--sans);
    cursor:pointer;
    flex-shrink:0;
  }
  .search-bar button:hover{ filter:brightness(1.06); }
  .search-bar button:focus-visible{ outline:2px solid var(--purple-700); outline-offset:2px; }

  .results-meta{
    font-size:13px;
    color: var(--ink-soft);
    margin-bottom:18px;
  }
  .results-meta a{ color: var(--purple-700); text-decoration:none; font-weight:600; }
  [data-theme="dark"] .results-meta a{ color: var(--purple-500); }

  .notice-banner{
    display:flex;
    align-items:flex-start;
    gap:9px;
    font-size:13.5px;
    padding:12px 14px;
    border-radius:4px;
    margin-bottom:20px;
    line-height:1.4;
  }
  .notice-banner.info{ background: var(--ok-bg); color: var(--ok); }
  .notice-banner.error{
    background: color-mix(in srgb, var(--stamp) 12%, var(--paper));
    border:1px solid color-mix(in srgb, var(--stamp) 40%, transparent);
    color: var(--stamp);
  }
  .notice-banner svg{ flex-shrink:0; margin-top:1px; }

  .product-grid{
    display:grid;
    grid-template-columns: repeat(2, 1fr);
    gap:16px;
  }
  .product-card{
    background: var(--paper);
    border:1px solid var(--purple-100);
    border-radius:14px;
    padding:18px;
    display:flex;
    flex-direction:column;
    gap:10px;
  }
  [data-theme="dark"] .product-card{ border-color: var(--purple-300); }

  .product-card .head{
    display:flex;
    align-items:flex-start;
    justify-content:space-between;
    gap:10px;
  }
  .product-card .name{
    font-size:15.5px;
    font-weight:700;
    color: var(--ink);
    margin:0;
    line-height:1.3;
  }
  .product-card .price{
    font-family: var(--serif);
    font-size:19px;
    font-weight:700;
    color: var(--purple-800);
    white-space:nowrap;
    flex-shrink:0;
  }
  [data-theme="dark"] .product-card .price{ color: var(--purple-500); }

  .product-card .desc{
    font-size:13px;
    color: var(--ink-soft);
    line-height:1.5;
    margin:0;
    display:-webkit-box;
    -webkit-line-clamp:2;
    -webkit-box-orient:vertical;
    overflow:hidden;
  }

  .product-card .meta-row{
    display:flex;
    align-items:center;
    gap:8px;
    margin-top:auto;
  }
  .badge{
    display:inline-block;
    font-size:10.5px;
    font-weight:700;
    letter-spacing:.15px;
    text-transform:uppercase;
    padding:3px 9px;
    border-radius:999px;
    background: var(--purple-100);
    color: var(--purple-800);
  }
  [data-theme="dark"] .badge{ color: var(--purple-500); }
  .seller{
    font-size:12px;
    color: var(--ink-soft);
  }

  .product-card .actions{
    display:flex;
    gap:8px;
    margin-top:4px;
  }
  .btn-interest{
    flex:1;
    padding:10px 12px;
    border:1px solid var(--purple-700);
    border-radius:9px;
    background: var(--purple-700);
    color:#fff;
    font-size:13px;
    font-weight:600;
    font-family: var(--sans);
    cursor:pointer;
    display:flex;
    align-items:center;
    justify-content:center;
    gap:6px;
  }
  .btn-interest:hover{ filter:brightness(1.06); }
  .btn-interest:focus-visible{ outline:2px solid var(--purple-700); outline-offset:2px; }
  .btn-interest:disabled{
    opacity:.55;
    cursor:not-allowed;
    filter:none;
  }
  .btn-interest.already{
    background: var(--ok-bg);
    color: var(--ok);
  }

  .empty{
    grid-column: 1 / -1;
    background: var(--paper);
    border:1px dashed var(--purple-300);
    border-radius:14px;
    padding:36px 20px;
    text-align:center;
    color: var(--ink-soft);
    font-size:13.5px;
  }
  .empty a{ color: var(--purple-700); font-weight:600; text-decoration:none; }
  [data-theme="dark"] .empty a{ color: var(--purple-500); }

  @media (prefers-reduced-motion: reduce){ *{ transition:none !important; } }

  @media (max-width: 720px){
    .product-grid{ grid-template-columns: 1fr; }
  }
  @media (max-width: 480px){
    .search-bar{ flex-direction:column; }
    .search-bar button{ width:100%; }
  }
</style>
</head>
<body>

  <div class="wrap">

    <div class="topbar">
      <div class="left">
        <a class="back-link" href="<%= u != null ? "dashboard.jsp" : "index.jsp" %>">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M19 12H5M12 19l-7-7 7-7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <%= u != null ? "Panel" : "Inicio" %>
        </a>
        <img class="mark-logo" src="https://raw.githubusercontent.com/Superiorfran1/FernanPop/main/imagenes/ChatGPT_Image_Jun_27__2026__12_28_08_AM-removebg-preview%281%29.png" alt="FernanPop">
      </div>
      <button class="theme-toggle" id="themeToggle" type="button" aria-label="Cambiar a modo oscuro" aria-pressed="false">
        <span class="knob" id="themeKnob">☾</span>
      </button>
    </div>

    <div class="page-title">
      <h1>Buscar productos</h1>
      <p>Explora lo que vende el resto del instituto.</p>
    </div>

    <form class="search-bar" method="get" action="buscar.jsp" id="searchForm">
      <div class="input-wrap">
        <svg width="17" height="17" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <circle cx="11" cy="11" r="7" stroke="currentColor" stroke-width="2"/>
          <path d="M21 21l-4.3-4.3" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        </svg>
        <input type="text" name="q" id="searchInput" placeholder="Busca por nombre o descripción..." value="<%= textoMostrado %>" autocomplete="off">
      </div>
    </form>

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

    <div id="resultsArea"><%= htmlResultados %></div>

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

  <script>
    (function () {
      var form = document.getElementById('searchForm');
      var input = document.getElementById('searchInput');
      var resultsArea = document.getElementById('resultsArea');
      if (!form || !input || !resultsArea) return;

      var DEBOUNCE_MS = 350;
      var temporizador = null;
      var peticionActual = null; // AbortController de la búsqueda en curso

      function buscar(texto) {
        if (peticionActual) {
          peticionActual.abort();
        }
        peticionActual = new AbortController();

        var url = 'buscar.jsp?ajax=1&q=' + encodeURIComponent(texto);

        fetch(url, { signal: peticionActual.signal })
          .then(function (resp) {
            if (!resp.ok) throw new Error('Respuesta no válida');
            return resp.text();
          })
          .then(function (html) {
            resultsArea.innerHTML = html;
            var nuevaUrl = texto ? ('buscar.jsp?q=' + encodeURIComponent(texto)) : 'buscar.jsp';
            window.history.replaceState(null, '', nuevaUrl);
          })
          .catch(function (err) {
            if (err.name !== 'AbortError') {
              console.error('Error en la búsqueda en vivo:', err);
            }
          });
      }

      input.addEventListener('input', function () {
        clearTimeout(temporizador);
        var texto = input.value;
        temporizador = setTimeout(function () {
          buscar(texto);
        }, DEBOUNCE_MS);
      });

      // El submit normal (Enter o clic en "Buscar") sigue funcionando igual
      // que antes (navegación clásica), por si JavaScript no está disponible
      // o el usuario prefiere no esperar al debounce.
    })();
  </script>

</body>
</html>








