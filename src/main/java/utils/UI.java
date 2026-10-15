package utils;

import models.Producto;
import models.Usuario;

/**
 * UI – Clase de presentación visual de FernanPop.
 *
 * Centraliza todos los elementos visuales de consola (menús, perfiles)
 * y las plantillas HTML para correos electrónicos y mensajes de Telegram.
 *
 * Ningún método de esta clase lee del teclado ni accede a datos de negocio.
 * Solo recibe parámetros y pinta o devuelve Strings formateados.
 */
public class UI {

//MÉTODOS

    //region MENÚS DE CONSOLA
    /**
     * Pinta el menú principal sin sesión iniciada.
     * Si el acceso de invitado está desactivado, no muestra las opciones
     * de ver productos (3 y 4).
     *
     * @param accesoInvitado true si el acceso sin login está habilitado en config.properties
     */
    public static void pintaMenuSinSesion(boolean accesoInvitado) {
        Utils.limpiaPantalla();
        if (accesoInvitado) {
            //Menú completo con opción de ver productos sin sesión
            System.out.print("""
            ╔══════════════════════════════════════╗
            ║        Bienvenido a FernanPop        ║
            ╠══════════════════════════════════════╣
            ║                                      ║
            ║  1. Iniciar sesión                   ║
            ║  2. Registrarse                      ║
            ║  3. Buscar productos                 ║
            ║  4. Ver todos los productos          ║
            ║                                      ║
            ╠══════════════════════════════════════╣
            ║  0. Salir                            ║
            ╚══════════════════════════════════════╝
            """);
        } else {
            //Menú reducido: sin acceso al catálogo sin login
            System.out.print("""
            ╔══════════════════════════════════════╗
            ║        Bienvenido a FernanPop        ║
            ╠══════════════════════════════════════╣
            ║                                      ║
            ║  1. Iniciar sesión                   ║
            ║  2. Registrarse                      ║
            ║                                      ║
            ╠══════════════════════════════════════╣
            ║  0. Salir                            ║
            ╚══════════════════════════════════════╝
            """);
        }
    }

    /**
     * Pinta el menú principal para un usuario con sesión activa.
     * Muestra el nombre del usuario y si tiene valoraciones pendientes.
     *
     * @param u usuario con sesión activa
     */
    public static void pintaMenuConSesion(Usuario u) {
        Utils.limpiaPantalla();

        //Texto de aviso de valoraciones pendientes (o confirmación de que está al día)
        String avisoVal = (u.cantidadValoracionesPendientes() > 0)
                ? "  ⚠ " + u.cantidadValoracionesPendientes() + " valoración(es) pendiente(s)"
                : "  ✓ Sin valoraciones pendientes";

        System.out.printf("""
        ╔══════════════════════════════════════╗
        ║  Hola, %-30s║
        ║%-38s║
        ╠══════════════════════════════════════╣
        ║                                      ║
        ║  1. Mi perfil                        ║
        ║  2. Cambiar mis datos personales     ║
        ║  3. Ver mis productos                ║
        ║  4. Introducir un producto           ║
        ║  5. Buscar productos                 ║
        ║  6. Ver valoraciones pendientes      ║
        ║  7. Ver historial de tratos          ║
        ║  8. Borrar mi perfil                 ║
        ║                                      ║
        ╠══════════════════════════════════════╣
        ║                                      ║
        ║  9. Cerrar sesión                    ║
        ║  0. Salir del programa               ║
        ║                                      ║
        ╚══════════════════════════════════════╝
        """, u.getNombre() + "!", avisoVal);
    }

    /**
     * Pinta el menú extendido para el administrador del sistema.
     * Incluye todas las opciones del menú normal más las opciones de administración
     * numeradas como 10, 11 y 12.
     *
     * @param u usuario administrador con sesión activa
     */
    public static void pintaMenuAdmin(Usuario u) {
        Utils.limpiaPantalla();

        // Ancho interior fijo de 42 caracteres
        int ancho = 42;
        String saludo = "★ ADMINISTRADOR – " + u.getNombre() + "!";
        String aviso  = (u.cantidadValoracionesPendientes() > 0)
                ? "  ⚠ " + u.cantidadValoracionesPendientes() + " valoración(es) pendiente(s)"
                : "  ✓ Sin valoraciones pendientes";

        System.out.println("╔" + "═".repeat(ancho) + "╗");
        System.out.println(filaRawEnCaja(saludo, ancho));
        System.out.println(filaRawEnCaja(aviso,  ancho));
        System.out.println("╠" + "═".repeat(ancho) + "╣");
        System.out.println("║" + " ".repeat(ancho) + "║");
        System.out.println("║  --- OPCIONES DE USUARIO ---             ║");
        System.out.println("║  1.  Mi perfil                           ║");
        System.out.println("║  2.  Cambiar mis datos personales        ║");
        System.out.println("║  3.  Ver mis productos                   ║");
        System.out.println("║  4.  Introducir un producto              ║");
        System.out.println("║  5.  Buscar productos                    ║");
        System.out.println("║  6.  Ver valoraciones pendientes         ║");
        System.out.println("║  7.  Ver historial de tratos             ║");
        System.out.println("║  8.  Borrar mi perfil                    ║");
        System.out.println("║" + " ".repeat(ancho) + "║");
        System.out.println("╠" + "═".repeat(ancho) + "╣");
        System.out.println("║" + " ".repeat(ancho) + "║");
        System.out.println("║  --- OPCIONES DE ADMINISTRACIÓN ---      ║");
        System.out.println("║  10. Ver configuración del sistema       ║");
        System.out.println("║  11. Enviar listado de productos         ║");
        System.out.println("║  12. Backup / restaurar base de datos    ║");
        System.out.println("║" + " ".repeat(ancho) + "║");
        System.out.println("╠" + "═".repeat(ancho) + "╣");
        System.out.println("║" + " ".repeat(ancho) + "║");
        System.out.println("║  9.  Cerrar sesión                       ║");
        System.out.println("║  0.  Salir del programa                  ║");
        System.out.println("║" + " ".repeat(ancho) + "║");
        System.out.println("╚" + "═".repeat(ancho) + "╝");
        System.out.println();
    }

    /**
     * Pinta la ficha de perfil del usuario con todos sus datos y estadísticas.
     *
     * @param u usuario cuyo perfil se va a mostrar
     */
    public static void pintaPerfil(Usuario u) {
        Utils.limpiaPantalla();

        double media = u.notaMedia();
        String estrellas = (media == -1) ? "Sin valoraciones" : String.format("%.1f ★", media);

        int ancho = 46; // ancho interior de la caja

        System.out.println("╔" + "═".repeat(ancho) + "╗");
        System.out.println(centrarEnCaja("MI PERFIL", ancho));
        System.out.println("╠" + "═".repeat(ancho) + "╣");
        System.out.println("║" + " ".repeat(ancho) + "║");
        System.out.println(filaEnCaja("ID:",          u.getID(), ancho));
        System.out.println(filaEnCaja("Nombre:",      u.getNombre() + " " + u.getApellidos(), ancho));
        System.out.println(filaEnCaja("Correo:",      u.getCorreoElectronico(), ancho));
        System.out.println(filaEnCaja("Teléfono:",    String.valueOf(u.getTelefono()), ancho));
        System.out.println(filaEnCaja("Valoración:",  estrellas, ancho));
        System.out.println("║" + " ".repeat(ancho) + "║");
        System.out.println("╠" + "═".repeat(ancho) + "╣");
        System.out.println("║" + " ".repeat(ancho) + "║");
        System.out.println(filaEnCaja("Productos en venta:", String.valueOf(u.getEnVenta().size()), ancho));
        System.out.println(filaEnCaja("Ventas realizadas:",  String.valueOf(u.getVentas().size()), ancho));
        System.out.println(filaEnCaja("Compras realizadas:", String.valueOf(u.getCompras().size()), ancho));
        System.out.println(filaEnCaja("Pendientes valorar:", String.valueOf(u.getValoracionesPendientes().size()), ancho));
        System.out.println("║" + " ".repeat(ancho) + "║");
        System.out.println("╚" + "═".repeat(ancho) + "╝");

        Utils.pulsaEnter();
    }

    /**
     * Pinta el submenú de gestión de mis productos.
     */
    public static void pintaMenuMisProductos() {
        Utils.limpiaPantalla();
        System.out.printf("""
        ╔═════════════════════════════════════════════╗
        ║                  MIS PRODUCTOS              ║
        ╠═════════════════════════════════════════════╣
        ║                                             ║
        ║  1. Ver mis productos en venta              ║
        ║  2. Editar un producto                      ║
        ║  3. Eliminar un producto                    ║
        ║  4. Cerrar una venta (marcar como vendido)  ║
        ║                                             ║
        ╠═════════════════════════════════════════════╣
        ║  0. Volver                                  ║
        ╚═════════════════════════════════════════════╝
        """);
    }

    /**
     * Pinta el submenú de búsqueda de productos.
     */
    public static void pintaMenuBuscarProductos() {
        Utils.limpiaPantalla();
        System.out.printf("""
        ╔══════════════════════════════════════════╗
        ║               BUSCAR PRODUCTOS           ║
        ╠══════════════════════════════════════════╣
        ║                                          ║
        ║  1. Ver todos los productos              ║
        ║  2. Buscar por texto                     ║
        ║  3. Ver detalle de un producto           ║
        ║  4. Comprar un producto                  ║
        ║                                          ║
        ╠══════════════════════════════════════════╣
        ║  0. Volver                               ║
        ╚══════════════════════════════════════════╝
        """);
    }

    /**
     * Pinta el submenú de cambio de datos personales.
     * Muestra el valor actual de cada campo entre corchetes.
     *
     * @param u usuario cuyos datos se van a mostrar como referencia
     */
    public static void pintaMenuCambiarDatos(Usuario u) {
        Utils.limpiaPantalla();
        int ancho = 42;
        System.out.println("╔" + "═".repeat(ancho) + "╗");
        System.out.println(centrarEnCaja("CAMBIAR DATOS PERSONALES", ancho));
        System.out.println("╠" + "═".repeat(ancho) + "╣");
        System.out.println("║" + " ".repeat(ancho) + "║");
        System.out.println(filaEnCaja("1. Nombre:",     u.getNombre(), ancho));
        System.out.println(filaEnCaja("2. Apellidos:",  u.getApellidos(), ancho));
        System.out.println(filaEnCaja("3. Contraseña:", Utils.contraseniaOculta(u.getContrasenia()), ancho));
        System.out.println(filaEnCaja("4. Teléfono:",   String.valueOf(u.getTelefono()), ancho));
        System.out.println("║" + " ".repeat(ancho) + "║");
        System.out.println("╠" + "═".repeat(ancho) + "╣");
        System.out.println("║  0. Volver                               ║");
        System.out.println("╚" + "═".repeat(ancho) + "╝");
        System.out.println();
    }

    /**
     * Pinta el submenú de valoraciones pendientes.
     */
    public static void pintaMenuValoraciones() {
        System.out.printf("""
        ╔══════════════════════════════════════════╗
        ║               VALORACIONES               ║
        ╠══════════════════════════════════════════╣
        ║  1. Valorar un trato                     ║
        ╠══════════════════════════════════════════╣
        ║  0. Volver                               ║
        ╚══════════════════════════════════════════╝
        """);
    }

    //region MÉTODOS AUXILIARES DE CAJA PARA CONSOLA

    /**
     * Genera una línea centrada dentro de los bordes de la caja.
     * Ejemplo: centrarEnCaja("HOLA", 20) → "║         HOLA         ║"
     *
     * @param texto texto a centrar
     * @param ancho ancho interior de la caja (sin contar los bordes ║)
     * @return línea completa con bordes
     */
    public static String centrarEnCaja(String texto, int ancho) {
        int espacios = ancho - texto.length();
        if (espacios < 0) espacios = 0;
        int izq = espacios / 2;
        int der = espacios - izq;
        return "║" + " ".repeat(izq) + texto + " ".repeat(der) + "║";
    }

    /**
     * Genera una línea con texto libre dentro de la caja, rellenando con espacios hasta el ancho.
     * Si el texto es más largo que el ancho interior, se trunca con "…".
     * A diferencia de filaEnCaja, no divide etiqueta/valor: pone el texto tal cual desde la columna 0.
     *
     * @param texto texto a insertar (sin ║)
     * @param ancho ancho interior de la caja
     * @return línea completa con bordes
     */
    public static String filaRawEnCaja(String texto, int ancho) {
        if (texto.length() > ancho) texto = texto.substring(0, ancho - 1) + "…";
        int relleno = ancho - texto.length();
        return "║" + texto + " ".repeat(relleno) + "║";
    }

    /**
     * Genera una línea con etiqueta y valor alineados dentro de la caja.
     * Si el texto combinado supera el ancho, el valor se trunca con "…".
     * Ejemplo: filaEnCaja("Nombre:", "Paco García", 30) → "║  Nombre:  Paco García         ║"
     *
     * @param etiqueta texto de la etiqueta (ej. "Correo:")
     * @param valor    valor a mostrar a su derecha
     * @param ancho    ancho interior de la caja
     * @return línea completa con bordes
     */
    public static String filaEnCaja(String etiqueta, String valor, int ancho) {
        // Margen izquierdo fijo de 2 espacios, separación entre etiqueta y valor de 1 espacio
        int espacioDisponible = ancho - 2 - etiqueta.length() - 1;
        if (espacioDisponible < 1) espacioDisponible = 1;
        // Truncar el valor si no cabe
        String valorMostrado = valor.length() > espacioDisponible
                ? valor.substring(0, espacioDisponible - 1) + "…"
                : valor;
        String linea = "  " + etiqueta + " " + valorMostrado;
        // Rellenamos hasta el ancho interior
        int relleno = ancho - linea.length();
        if (relleno < 0) relleno = 0;
        return "║" + linea + " ".repeat(relleno) + "║";
    }

    /**
     * Muestra la lista de tratos pendientes de valorar dentro de una caja,
     * numerados por posición. Muestra ID del trato, nombre del producto y precio.
     *
     * @param pendientes lista de tratos pendientes de valorar
     */
    public static void pintaListaValoracionesPendientes(java.util.ArrayList<models.Trato> pendientes) {
        int ancho = 62;
        System.out.println("╔" + "═".repeat(ancho) + "╗");
        System.out.println(centrarEnCaja("TRATOS PENDIENTES DE VALORAR", ancho));
        System.out.println("╠" + "═".repeat(ancho) + "╣");
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
        for (int i = 0; i < pendientes.size(); i++) {
            models.Trato t = pendientes.get(i);
            String fecha = sdf.format(t.getFecha().getTime());
            // Línea 1: número, ID trato y fecha
            String l1 = String.format("  [%d] %s  –  %s", (i + 1), t.getID(), fecha);
            // Línea 2: nombre del producto y precio
            String l2 = String.format("      Producto: %-28s %.2f€",
                    t.getProducto().getNombre(), t.getPrecio());
            // Línea 3: vendedor
            String l3 = String.format("      Vendedor: %s", t.getCorreoVendedor());

            System.out.println(filaRawEnCaja(l1, ancho));
            System.out.println(filaRawEnCaja(l2, ancho));
            System.out.println(filaRawEnCaja(l3, ancho));
            if (i < pendientes.size() - 1) {
                System.out.println("║" + "·".repeat(ancho) + "║");
            }
        }
        System.out.println("╚" + "═".repeat(ancho) + "╝");
    }

    /**
     * Muestra una lista de productos en formato caja con numeración por posición.
     * Solo muestra productos que tengan al menos un interesado.
     *
     * @param productosConInteresados lista de productos filtrada (ya solo contiene los que tienen interesados)
     */
    public static void pintaListaProductosConInteresados(java.util.ArrayList<Producto> productosConInteresados) {
        int ancho = 62;
        System.out.println("╔" + "═".repeat(ancho) + "╗");
        System.out.println(centrarEnCaja("PRODUCTOS CON COMPRADORES INTERESADOS", ancho));
        System.out.println("╠" + "═".repeat(ancho) + "╣");
        for (int i = 0; i < productosConInteresados.size(); i++) {
            Producto p = productosConInteresados.get(i);
            String linea = String.format("  [%d] %-8s %-24s %.2f€  %d interesado(s)",
                    (i + 1), p.getID(), p.getNombre(), p.getPrecio(), p.getInteresados().size());
            System.out.println(filaRawEnCaja(linea, ancho));
        }
        System.out.println("╚" + "═".repeat(ancho) + "╝");
    }

    /**
     * Muestra una lista de interesados en un producto dentro de una caja,
     * numerados por posición.
     *
     * @param interesados lista de correos de usuarios interesados
     */
    public static void pintaListaInteresados(java.util.ArrayList<String> interesados) {
        int ancho = 52;
        System.out.println("╔" + "═".repeat(ancho) + "╗");
        System.out.println(centrarEnCaja("COMPRADORES INTERESADOS", ancho));
        System.out.println("╠" + "═".repeat(ancho) + "╣");
        for (int i = 0; i < interesados.size(); i++) {
            String linea = String.format("  [%d] %s", (i + 1), interesados.get(i));
            if (linea.length() > ancho) linea = linea.substring(0, ancho - 1) + "…";
            int relleno = ancho - linea.length();
            System.out.println("║" + linea + " ".repeat(relleno) + "║");
        }
        System.out.println("╚" + "═".repeat(ancho) + "╝");
    }

    /**
     * Muestra una lista genérica de productos en formato caja.
     * Muestra posición, nombre y precio. Se usa en listas de selección.
     *
     * @param titulo título de la caja
     * @param productos lista de productos a mostrar
     */
    public static void pintaListaProductosCaja(String titulo, java.util.ArrayList<Producto> productos) {
        int ancho = 52;
        System.out.println("╔" + "═".repeat(ancho) + "╗");
        System.out.println(centrarEnCaja(titulo, ancho));
        System.out.println("╠" + "═".repeat(ancho) + "╣");
        for (int i = 0; i < productos.size(); i++) {
            Producto p = productos.get(i);
            String linea = String.format("  %-10s %-26s %.2f€",
                    p.getID(), p.getNombre(), p.getPrecio());
            if (linea.length() > ancho) linea = linea.substring(0, ancho - 1) + "…";
            int relleno = ancho - linea.length();
            System.out.println("║" + linea + " ".repeat(relleno) + "║");
        }
        System.out.println("╚" + "═".repeat(ancho) + "╝");
    }

    //endregion

    //endregion

    //region PLANTILLAS HTML PARA CORREOS ELECTRÓNICOS

    //region PLANTILLA BASE (diseño FernanPop)

    /**
     * Envuelve el contenido de un correo con la plantilla visual de FernanPop:
     * cabecera morada con el logo, tarjeta blanca central a modo de "tique"
     * y pie de página con la firma del mercadillo.
     *
     * Todo el CSS va inline y la estructura usa tablas (no flex/grid), porque
     * los clientes de correo (Gmail, Outlook, Apple Mail...) no soportan CSS
     * moderno ni variables; las tablas con estilos inline son lo único que
     * se renderiza de forma fiable en todos ellos. Por el mismo motivo no hay
     * Google Fonts: Georgia/Arial son tipografías "web-safe" que ya existen
     * en cualquier dispositivo, así que el correo se ve igual en todas partes.
     *
     * @param tituloOjo  texto pequeño en mayúsculas sobre el título (ej. "VERIFICACIÓN", "ALERTA DE VENTA")
     * @param colorAcento color hexadecimal del título y los detalles de esta plantilla concreta
     *                    (morado para avisos normales, rojo para alertas de seguridad, verde para confirmaciones)
     * @param contenidoHtml HTML ya construido del cuerpo específico de cada correo
     * @return HTML completo del correo, listo para enviar
     */
    private static String plantillaCorreo(String tituloOjo, String colorAcento, String contenidoHtml) {
        return """
        <!DOCTYPE html>
        <html lang="es">
        <head>
          <meta charset="UTF-8"/>
          <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
          <title>FernanPop</title>
        </head>
        <body style="margin:0; padding:0; background-color:#faf6f2; font-family:Arial,Helvetica,sans-serif;">
          <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#faf6f2; padding:32px 16px;">
            <tr>
              <td align="center">
                <table role="presentation" width="520" cellpadding="0" cellspacing="0" style="max-width:520px; width:100%;">

                  <!-- Cabecera con el logo -->
                  <tr>
                    <td align="center" style="padding-bottom:22px;">
                      <span style="font-family:Georgia,'Times New Roman',serif; font-size:26px; font-weight:bold; color:#3d1f5c; letter-spacing:0.5px;">
                        Fernan<span style="color:#b53620;">Pop</span>
                      </span>
                      <div style="font-family:Arial,sans-serif; font-size:11px; color:#6b5d78; letter-spacing:0.4px; margin-top:2px;">
                        EL MERCADILLO DEL INSTITUTO
                      </div>
                    </td>
                  </tr>

                  <!-- Tarjeta principal a modo de "tique" -->
                  <tr>
                    <td style="background-color:#ffffff; border-radius:14px; border:1px solid #efe3fa; overflow:hidden;">
                      <table role="presentation" width="100%" cellpadding="0" cellspacing="0">

                        <!-- Franja superior de color, simula el borde del tique -->
                        <tr>
                          <td style="background-color:""" + colorAcento + """
; height:6px; line-height:6px; font-size:1px;">&nbsp;</td>
                        </tr>

                        <tr>
                          <td style="padding:34px 36px 30px;">
                            <div style="font-family:Arial,sans-serif; font-size:11.5px; font-weight:bold; letter-spacing:1.2px; color:""" + colorAcento + """
; text-transform:uppercase; margin-bottom:10px;">""" + tituloOjo + """
</div>
""" + contenidoHtml + """
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>

                  <!-- Pie de página -->
                  <tr>
                    <td align="center" style="padding-top:22px; font-family:Arial,sans-serif; font-size:11.5px; color:#6b5d78; letter-spacing:0.3px;">
                      FERNANPOP · TIQUE DIGITAL · NO RESPONDER A ESTE CORREO
                    </td>
                  </tr>

                </table>
              </td>
            </tr>
          </table>
        </body>
        </html>
        """;
    }

    /**
     * Construye el bloque visual de un código grande (verificación / seguridad):
     * número o texto destacado, centrado, con tracking amplio, dentro de una
     * caja con fondo suave del color de acento indicado.
     *
     * @param codigo      código a mostrar
     * @param colorAcento color hexadecimal del texto y el borde de la caja
     * @param colorFondo  color hexadecimal de fondo de la caja (debe ser una versión clara de colorAcento)
     * @return HTML del bloque de código, listo para insertar en el cuerpo de un correo
     */
    private static String bloqueCodigo(String codigo, String colorAcento, String colorFondo) {
        return """
                            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin:18px 0;">
                              <tr>
                                <td align="center" style="background-color:""" + colorFondo + """
; border:1.5px solid\s""" + colorAcento + """
; border-radius:10px; padding:18px;">
                                  <span style="font-family:Arial,sans-serif; font-size:30px; font-weight:bold; letter-spacing:8px; color:""" + colorAcento + """
;">""" + codigo + """
</span>
                                </td>
                              </tr>
                            </table>
        """;
    }

    /**
     * Construye una fila de "ficha de datos" (etiqueta a la izquierda, valor
     * en negrita a la derecha), usada para mostrar producto/precio/persona
     * en los correos de venta, compra e interés.
     *
     * @param etiqueta texto descriptivo (ej. "Producto", "Precio")
     * @param valor    valor correspondiente, ya formateado
     * @return HTML de la fila, listo para insertar dentro de una tabla de ficha
     */
    private static String filaFicha(String etiqueta, String valor) {
        return """
                                <tr>
                                  <td style="padding:8px 0; border-bottom:1px solid #efe3fa; font-family:Arial,sans-serif; font-size:13px; color:#6b5d78;">""" + etiqueta + """
</td>
                                  <td style="padding:8px 0; border-bottom:1px solid #efe3fa; font-family:Arial,sans-serif; font-size:13.5px; color:#2a1f33; font-weight:bold; text-align:right;">""" + valor + """
</td>
                                </tr>
        """;
    }

    /**
     * Envuelve un conjunto de filaFicha() en la tabla contenedora de ficha
     * de datos, con fondo suave y bordes redondeados.
     *
     * @param filasHtml HTML ya concatenado de varias llamadas a filaFicha()
     * @return HTML de la ficha completa, lista para insertar en el cuerpo de un correo
     */
    private static String fichaDatos(String filasHtml) {
        return """
                            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#faf6f2; border-radius:10px; margin:16px 0;">
                              <tr>
                                <td style="padding:4px 16px;">
                                  <table role="presentation" width="100%" cellpadding="0" cellspacing="0">
""" + filasHtml + """
                                  </table>
                                </td>
                              </tr>
                            </table>
        """;
    }

    /**
     * Construye un botón de llamada a la acción (CTA), estilo "tique de aviso",
     * usado en los correos para invitar a volver a FernanPop.
     *
     * @param texto texto del botón
     * @param url   enlace de destino
     * @return HTML del botón, listo para insertar en el cuerpo de un correo
     */
    private static String botonCTA(String texto, String url) {
        return """
                            <table role="presentation" cellpadding="0" cellspacing="0" style="margin:22px 0 4px;">
                              <tr>
                                <td style="background-color:#5b2a86; border-radius:9px;">
                                  <a href=\"""" + url + """
\" style="display:inline-block; padding:12px 26px; font-family:Arial,sans-serif; font-size:14px; font-weight:bold; color:#ffffff; text-decoration:none;">""" + texto + """
</a>
                                </td>
                              </tr>
                            </table>
        """;
    }

    /**
     * Párrafo de texto secundario, en gris suave, para avisos legales o
     * notas al pie de cada correo (ej. "Si no fuiste tú, ignora este correo").
     *
     * @param texto contenido del párrafo
     * @return HTML del párrafo, listo para insertar en el cuerpo de un correo
     */
    private static String notaSecundaria(String texto) {
        return """
                            <p style="font-family:Arial,sans-serif; font-size:12px; color:#6b5d78; line-height:1.5; margin:18px 0 0;">""" + texto + """
</p>
        """;
    }

    //endregion

    /**
     * Genera el HTML del correo de verificación de cuenta para un nuevo usuario.
     *
     * @param nombre              nombre del usuario que se está registrando
     * @param codigoVerificacion  código de 6 caracteres generado aleatoriamente
     * @return HTML listo para enviar como cuerpo del correo
     */
    public static String generaEmailVerificacion(String nombre, String codigoVerificacion) {
        String contenido = """
                            <h1 style="font-family:Georgia,'Times New Roman',serif; font-size:21px; color:#2a1f33; margin:0 0 8px;">¡Bienvenido/a a FernanPop!</h1>
                            <p style="font-family:Arial,sans-serif; font-size:14px; color:#2a1f33; line-height:1.5; margin:0 0 4px;">Hola <strong>""" + nombre + """
</strong>, gracias por registrarte. Para activar tu cuenta, introduce este código en la página de registro:</p>
""" + bloqueCodigo(codigoVerificacion, "#5b2a86", "#efe3fa") + """
""" + notaSecundaria("Si no solicitaste este registro, puedes ignorar este correo.");

        return plantillaCorreo("VERIFICACIÓN DE CUENTA", "#5b2a86", contenido);
    }

    /**
     * Genera el HTML del correo que avisa al vendedor de que alguien está interesado en su producto.
     *
     * @param nombreVendedor    nombre del vendedor destinatario
     * @param tituloProducto    nombre del producto en cuestión
     * @param nombreComprador   nombre del usuario interesado
     * @param apellidosComprador apellidos del usuario interesado
     * @param emailComprador    correo del usuario interesado
     * @return HTML del correo
     */
    public static String generarEmailInteresado(String nombreVendedor, String tituloProducto,
                                                String nombreComprador, String apellidosComprador,
                                                String emailComprador) {
        String iniciales = (nombreComprador.isEmpty() ? "?" : String.valueOf(nombreComprador.charAt(0)).toUpperCase())
                + (apellidosComprador.isEmpty() ? "" : String.valueOf(apellidosComprador.charAt(0)).toUpperCase());

        String contenido = """
                            <h1 style="font-family:Georgia,'Times New Roman',serif; font-size:21px; color:#2a1f33; margin:0 0 8px;">¡Alguien quiere tu producto!</h1>
                            <p style="font-family:Arial,sans-serif; font-size:14px; color:#2a1f33; line-height:1.5; margin:0 0 4px;">Hola <strong>""" + nombreVendedor + """
</strong>, tienes un nuevo interesado en uno de tus productos.</p>
""" + fichaDatos(
                filaFicha("Producto", tituloProducto) +
                filaFicha("Interesado/a", iniciales + " · " + nombreComprador + " " + apellidosComprador) +
                filaFicha("Contacto", "<a href=\"mailto:" + emailComprador + "\" style=\"color:#5b2a86; text-decoration:none;\">" + emailComprador + "</a>")
        ) + """
""" + notaSecundaria("Responde directamente a su correo si quieres cerrar la venta con él/ella.");

        return plantillaCorreo("NUEVO INTERÉS", "#5b2a86", contenido);
    }

    /**
     * Genera el HTML del correo de confirmación de venta para el vendedor.
     *
     * @param nombreVendedor  nombre del vendedor
     * @param titulo          nombre del producto vendido
     * @param precio          precio de la transacción
     * @param nombreComprador nombre del comprador
     * @param emailComprador  correo del comprador
     * @return HTML del correo
     */
    public static String generarEmailVenta(String nombreVendedor, String titulo, String precio,
                                           String nombreComprador, String emailComprador) {
        String contenido = """
                            <h1 style="font-family:Georgia,'Times New Roman',serif; font-size:21px; color:#2a1f33; margin:0 0 8px;">✓ Venta confirmada</h1>
                            <p style="font-family:Arial,sans-serif; font-size:14px; color:#2a1f33; line-height:1.5; margin:0 0 4px;">Hola <strong>""" + nombreVendedor + """
</strong>, tu venta ha sido registrada correctamente.</p>
""" + fichaDatos(
                filaFicha("Producto", titulo) +
                filaFicha("Precio", precio + " €") +
                filaFicha("Comprador", nombreComprador + " (" + emailComprador + ")")
        ) + """
""" + notaSecundaria("Recuerda valorar el trato desde tu cuenta cuando el comprador confirme la recogida.");

        return plantillaCorreo("VENTA REALIZADA", "#277048", contenido);
    }

    /**
     * Genera el HTML del correo de confirmación de compra para el comprador.
     *
     * @param nombreComprador nombre del comprador
     * @param titulo          nombre del producto comprado
     * @param precio          precio de la transacción
     * @param nombreVendedor  nombre del vendedor
     * @param emailVendedor   correo del vendedor
     * @return HTML del correo
     */
    public static String generarEmailCompra(String nombreComprador, String titulo, String precio,
                                            String nombreVendedor, String emailVendedor) {
        String contenido = """
                            <h1 style="font-family:Georgia,'Times New Roman',serif; font-size:21px; color:#2a1f33; margin:0 0 8px;">✓ Compra confirmada</h1>
                            <p style="font-family:Arial,sans-serif; font-size:14px; color:#2a1f33; line-height:1.5; margin:0 0 4px;">Hola <strong>""" + nombreComprador + """
</strong>, tu compra ha sido registrada.</p>
""" + fichaDatos(
                filaFicha("Producto", titulo) +
                filaFicha("Precio", precio + " €") +
                filaFicha("Vendedor", nombreVendedor + " (" + emailVendedor + ")")
        ) + """
""" + notaSecundaria("Recuerda valorar el trato desde tu cuenta cuando recibas el producto.");

        return plantillaCorreo("COMPRA REALIZADA", "#277048", contenido);
    }

    /**
     * Genera el HTML del correo de modificación de producto para los interesados.
     *
     * @param tituloProducto nombre del producto modificado
     * @param u              usuario vendedor que realizó la modificación
     * @return HTML del correo
     */
    public static String generaEmailModificacion(String tituloProducto, Usuario u) {
        String contenido = """
                            <h1 style="font-family:Georgia,'Times New Roman',serif; font-size:21px; color:#2a1f33; margin:0 0 8px;">Producto modificado</h1>
                            <p style="font-family:Arial,sans-serif; font-size:14px; color:#2a1f33; line-height:1.5; margin:0 0 4px;">El producto <strong>""" + tituloProducto + """
</strong> que te interesaba ha sido modificado por el vendedor.</p>
""" + fichaDatos(
                filaFicha("Vendedor", u.getNombre() + " " + u.getApellidos())
        ) + """
                            <p style="font-family:Arial,sans-serif; font-size:14px; color:#2a1f33; line-height:1.5; margin:6px 0 0;">Accede a FernanPop para ver los cambios y, si sigues interesado/a, vuelve a solicitarlo.</p>
""";

        return plantillaCorreo("AVISO DE CAMBIO", "#5b2a86", contenido);
    }

    /**
     * Genera el HTML del correo con el código de seguridad para eliminar la cuenta.
     *
     * @param nombre          nombre del usuario que quiere eliminar su cuenta
     * @param codigoSeguridad código de 6 caracteres generado aleatoriamente
     * @return HTML del correo
     */
    public static String generarEmailEliminarCuenta(String nombre, String codigoSeguridad) {
        String contenido = """
                            <h1 style="font-family:Georgia,'Times New Roman',serif; font-size:21px; color:#2a1f33; margin:0 0 8px;">Confirmación de eliminación de cuenta</h1>
                            <p style="font-family:Arial,sans-serif; font-size:14px; color:#2a1f33; line-height:1.5; margin:0 0 4px;">Hola <strong>""" + nombre + """
</strong>, recibimos una solicitud para eliminar tu cuenta. Tu código de seguridad (válido 10 minutos) es:</p>
""" + bloqueCodigo(codigoSeguridad, "#b53620", "#fbf3f2") + """
""" + notaSecundaria("Si no solicitaste esto, ignora este correo: tu cuenta está segura y no se eliminará.");

        return plantillaCorreo("ELIMINAR CUENTA", "#b53620", contenido);
    }

    //endregion

    //region MENSAJES DE TELEGRAM

    /**
     * Genera el mensaje de Telegram para notificar un nuevo usuario registrado.
     *
     * @param nombre     nombre del nuevo usuario
     * @param email      correo del nuevo usuario
     * @param contrasenia contraseña elegida (se muestra en el aviso admin)
     * @return String formateado para Telegram con Markdown
     */
    public static String msgNuevoUsuario(String nombre, String email, String contrasenia) {
        return "*Nuevo usuario registrado*\n\n" +
                "Nombre — `" + nombre + "`\n" +
                "Correo electrónico — `" + email + "`\n" +
                "Contraseña — `" + contrasenia + "`\n\n" +
                "_Cuenta verificada y activa_";
    }

    /**
     * Genera el mensaje de Telegram para notificar que un usuario eliminó su cuenta.
     *
     * @param user usuario eliminado
     * @return String formateado para Telegram con Markdown
     */
    public static String msgUsuarioEliminado(Usuario user) {
        return "*Usuario eliminado*\n\n" +
                "Nombre — `" + user.getNombre() + " " + user.getApellidos() + "`\n" +
                "Correo electrónico — `" + user.getCorreoElectronico() + "`\n" +
                "ID — `#" + user.getID() + "`\n\n" +
                "_Baja definitiva procesada_";
    }

    /**
     * Genera el mensaje de Telegram para notificar un cambio en los datos de un usuario.
     *
     * @param user  usuario que modificó sus datos
     * @param campo nombre del campo modificado (ej. "Nombre", "Contraseña")
     * @param antes valor anterior del campo
     * @param ahora nuevo valor del campo
     * @return String formateado para Telegram con Markdown
     */
    public static String msgCambioDatos(Usuario user, String campo, String antes, String ahora) {
        return "*Perfil modificado*\n\n" +
                "Usuario — `" + user.getNombre() + "` \\(" + user.getCorreoElectronico() + "\\)\n" +
                "Campo — `" + campo + "`\n\n" +
                "Antes → `" + antes + "`\n" +
                "Ahora → `" + ahora + "`";
    }

    /**
     * Genera el mensaje de Telegram para notificar la publicación de un nuevo producto.
     *
     * @param vendedor usuario que publicó el producto
     * @param p        producto publicado
     * @return String formateado para Telegram con Markdown
     */
    public static String msgNuevoProducto(Usuario vendedor, Producto p) {
        return "*Nuevo producto publicado*\n\n" +
                "Vendedor — `" + vendedor.getNombre() + "`\n" +
                "Producto — `" + p.getNombre() + "`\n" +
                "ID — `#" + p.getID() + "`\n" +
                "Precio — `" + p.getPrecio() + " €`\n\n" +
                "_Visible y disponible en FernanPop_";
    }

    /**
     * Genera el mensaje de Telegram para notificar la modificación de un producto.
     *
     * @param vendedor usuario que modificó el producto
     * @param p        producto modificado
     * @param campo    campo que fue modificado
     * @param antes    valor anterior
     * @param ahora    nuevo valor
     * @return String formateado para Telegram con Markdown
     */
    public static String msgProductoModificado(Usuario vendedor, Producto p,
                                               String campo, String antes, String ahora) {
        return "*Producto modificado*\n\n" +
                "Vendedor — `" + vendedor.getNombre() + "`\n" +
                "Producto — `" + p.getNombre() + "` \\(#" + p.getID() + "\\)\n" +
                "Campo — `" + campo + "`\n\n" +
                "Antes → `" + antes + "`\n" +
                "Ahora → `" + ahora + "`";
    }

    /**
     * Genera el mensaje de Telegram para notificar que un producto fue retirado.
     *
     * @param vendedor      usuario propietario del producto
     * @param ID_Producto   ID del producto eliminado
     * @param nombreProducto nombre del producto eliminado
     * @return String formateado para Telegram con Markdown
     */
    public static String msgProductoEliminado(Usuario vendedor, String ID_Producto, String nombreProducto) {
        return "*Producto retirado*\n\n" +
                "Vendedor — `" + vendedor.getNombre() + "`\n" +
                "Producto — `" + nombreProducto + "`\n" +
                "ID — `#" + ID_Producto + "`\n\n" +
                "_Eliminado por el propietario_";
    }

    /**
     * Genera el mensaje de Telegram para notificar el cierre de una venta.
     *
     * @param comprador usuario comprador
     * @param vendedor  usuario vendedor
     * @param p         producto vendido
     * @param fecha     fecha y hora del cierre en formato "dd/MM/yyyy HH:mm"
     * @return String formateado para Telegram con Markdown
     */
    public static String msgVentaRealizada(Usuario comprador, Usuario vendedor, Producto p, String fecha) {
        return "*Transacción completada*\n\n" +
                "Vendedor — `" + vendedor.getNombre() + "`\n" +
                "Comprador — `" + comprador.getNombre() + "`\n" +
                "Producto — `" + p.getNombre() + "`\n" +
                "Total — `" + p.getPrecio() + " €`\n" +
                "Fecha — `" + fecha + "`\n\n" +
                "_Operación finalizada con éxito_";
    }

    //endregion


}