package utils;

// ── JavaMail ──────────────────────────────────────────────────────────────────
// JAR: javax_mail-1_6_2.jar + javax_activation-1_2_0.jar
import javax.mail.*;
import javax.mail.internet.*;

// ── Apache PDFBox 3.x ─────────────────────────────────────────────────────────
// JAR: pdfbox-app-3_0_7.jar  (incluye fontbox y commons)
// IMPORTANTE: PDFBox trabaja a bajo nivel con coordenadas X/Y manuales.
//             NO tiene tablas ni layouts automáticos como OpenPDF.
//             La página A4 mide 595 x 842 puntos (pt). El (0,0) está abajo-izquierda.
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import java.util.logging.Level;
import java.util.logging.Logger;

// Añadir esto antes de llamar a cualquier función de PDFBox
// Logger.getLogger("org.apache.pdfbox").setLevel(Level.OFF);
// Logger.getLogger("org.apache.fontbox").setLevel(Level.OFF);
// Si no, te llenará el cmd con advertencias relacionadas a los formatos de todas las
// fuentes que hayan en tu ordenador.

// ── Modelos propios ───────────────────────────────────────────────────────────
import models.Producto;
import models.Trato;
import models.Usuario;
import persistence.AppConfig;

// ── Java estándar ─────────────────────────────────────────────────────────────
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Communications – Gestión de notificaciones y comunicaciones externas.
 *
 * Centraliza todos los canales de salida hacia el exterior de la aplicación.
 *
 * CANALES UTILIZADOS:
 * - Telegram (Bot API): alertas y logs rápidos al administrador.
 * - Gmail (SMTP / JavaMail): correos formales con adjuntos.
 * - PDF (Apache PDFBox 3.0.7): recibo de trato adjunto al correo.
 * - CSV (sin librerías externas): listado de productos en .csv, que Excel abre directamente.
 *
 * LIBRERÍAS REQUERIDAS (en /FernanPop/librerias):
 *   javax_mail-1_6_2.jar          → SMTP
 *   javax_activation-1_2_0.jar    → dependencia de javax.mail
 *   pdfbox-app-3_0_7.jar          → PDF (incluye fontbox y commons-logging)
 *
 * La exportación de productos usa CSV puro (sin librerías externas).
 *
 * Los datos sensibles (cuenta de Gmail y bot de Telegram) NO están en el código:
 * se leen de config.properties (claves gmail.remitente, gmail.clave,
 * telegram.token y telegram.chatId) a través de AppConfig.
 *
 * TODO: Si se cambia de cuenta de Gmail o de bot, basta con editar config.properties.
 */
public class Communications {

//ATRIBUTOS

    //Cuenta de Gmail remitente y contraseña de aplicación: se leen de config.properties.
    //Se consultan en cada envío (no se guardan en constantes) para que un cambio en el
    //config se aplique sin recompilar.
    private static String remitente()  { return AppConfig.getInstance().getGmailRemitente(); }
    private static String claveGmail() { return AppConfig.getInstance().getGmailClave(); }

    //Márgenes y medidas del PDF en puntos (1 pt = 1/72 pulgada)
    //Una página A4 tiene 595 pt de ancho y 842 pt de alto
    private static final float PDF_MARGEN_IZQ  = 50f;  //Margen izquierdo
    private static final float PDF_MARGEN_DER  = 50f;  //Margen derecho
    private static final float PDF_ANCHO_UTIL  = PDRectangle.A4.getWidth() - PDF_MARGEN_IZQ - PDF_MARGEN_DER; //Ancho útil de escritura
    private static final float PDF_Y_INICIO    = PDRectangle.A4.getHeight() - 60f; //Posición Y de la primera línea (desde arriba)
    private static final float PDF_INTERLINEA  = 18f; //Separación entre líneas normales
    private static final float PDF_INTERLINEA_GRANDE = 26f; //Separación entre secciones


//MÉTODOS

    //region TELEGRAM

    /**
     * Envía un mensaje de texto al bot de Telegram configurado.
     * Usa Markdown para el formato. Silencioso ante errores para no interrumpir el flujo.
     *
     * @param mensaje texto del mensaje (puede usar Markdown)
     * @return true si el envío fue correcto, false si hubo algún error
     */
    public static boolean enviaMensajeTelegram(String mensaje) {
        AppConfig cfg = AppConfig.getInstance();
        String token  = cfg.getTelegramToken();
        String chatId = cfg.getTelegramChatId();
        //Si el bot no está configurado en config.properties no se envía nada (silencioso)
        if (token.isEmpty() || chatId.isEmpty()) return false;
        String fijo = "https://api.telegram.org/bot" + token + "/sendMessage?chat_id=" + chatId + "&parse_mode=Markdown&text=";
        boolean dev = false;

        try {
            String mensajeCodificado = URLEncoder.encode(mensaje, StandardCharsets.UTF_8);
            URL url = new URL(fijo + mensajeCodificado);
            URLConnection con = url.openConnection();

            //Lectura del stream necesaria para que la conexión se complete y cierre bien
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                // (vacío a propósito)
            }
            dev = true;
        } catch (IOException e) {
            //NO queremos que notifique de este error al usuario
        }
        return dev;
    }

    //endregion

    //region GMAIL

    /**
     * Construye y devuelve la Session de JavaMail con la configuración SMTP de Gmail.
     * Extraído aquí para no repetir la configuración en cada función de envío.
     *
     * @return Session lista para usarse con Transport
     */
    private static Session crearSessionGmail() {
        Properties props = new Properties();
        props.put("mail.smtp.host",            "smtp.gmail.com");
        props.put("mail.smtp.user",            remitente());
        props.put("mail.smtp.password",        claveGmail());
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.port",            "587");
        return Session.getDefaultInstance(props);
    }

    /**
     * Envía un correo con cuerpo HTML sin adjunto, mediante SMTP de Gmail.
     *
     * @param destinatario dirección de correo del receptor
     * @param asunto       asunto del mensaje
     * @param cuerpo       cuerpo del mensaje en formato HTML
     * @return true si el envío fue exitoso, false si hubo algún error
     */
    public static boolean enviarConGMail(String destinatario, String asunto, String cuerpo) {
        Session session = crearSessionGmail();

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(remitente()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            message.setSubject(asunto);
            message.setContent(cuerpo, "text/html; charset=utf-8");

            Transport transport = session.getTransport("smtp");
            transport.connect("smtp.gmail.com", remitente(), claveGmail());
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();

            return true;

        } catch (Exception e) {
            System.out.println("[Communications] Error al enviar correo a " + destinatario + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Envía un correo con cuerpo HTML y un archivo adjunto, mediante SMTP de Gmail.
     * Usa MimeMultipart para combinar ambas partes en el mismo mensaje.
     *
     * @param destinatario  dirección de correo del receptor
     * @param asunto        asunto del mensaje
     * @param cuerpoHTML    cuerpo del mensaje en formato HTML
     * @param adjuntoBytes  contenido del adjunto como array de bytes
     * @param nombreAdjunto nombre del archivo tal como aparece en el correo (ej. "recibo_T00001.pdf")
     * @param tipoMIME      tipo MIME del adjunto (ej. "application/pdf")
     * @return true si el envío fue exitoso, false si hubo algún error
     */
    public static boolean enviarConGMailAdjunto(String destinatario, String asunto, String cuerpoHTML,
                                                byte[] adjuntoBytes, String nombreAdjunto, String tipoMIME) {
        Session session = crearSessionGmail();

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(remitente()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            message.setSubject(asunto);

            //Parte 1: cuerpo HTML del correo
            MimeBodyPart parteCuerpo = new MimeBodyPart();
            parteCuerpo.setContent(cuerpoHTML, "text/html; charset=utf-8");

            //Parte 2: archivo adjunto en bytes
            MimeBodyPart parteAdjunto = new MimeBodyPart();
            parteAdjunto.setContent(adjuntoBytes, tipoMIME);
            //Codificamos el nombre en Base64 para que tildes y caracteres especiales no rompan nada
            parteAdjunto.setFileName(MimeUtility.encodeText(nombreAdjunto, "utf-8", "B"));

            //Unimos ambas partes en un Multipart
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(parteCuerpo);
            multipart.addBodyPart(parteAdjunto);
            message.setContent(multipart);

            Transport transport = session.getTransport("smtp");
            transport.connect("smtp.gmail.com", remitente(), claveGmail());
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();

            return true;

        } catch (Exception e) {
            System.out.println("[Communications] Error al enviar correo con adjunto a " + destinatario + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    //endregion

    //region PDF – RECIBO DE TRATO

    /**
     * Genera en memoria un PDF con el resumen completo del trato realizado.
     *
     * LIBRERÍA: Apache PDFBox 3.0.7 → paquete org.apache.pdfbox
     *
     * IMPORTANTE SOBRE PDFBOX:
     *   PDFBox trabaja a bajo nivel. No tiene tablas ni layouts automáticos.
     *   El texto se escribe con coordenadas X/Y absolutas en puntos (pt).
     *   El eje Y crece hacia ARRIBA (0 = borde inferior de la página).
     *   Para escribir "de arriba a abajo" hay que ir RESTANDO a Y en cada línea.
     *   A4 = 595 x 842 pt. Nuestros márgenes dejan X desde 50 hasta 545.
     *
     * Estructura del PDF generado:
     *   [TITULO]         FernanPop — Confirmacion de Transaccion
     *   [LINEA]          ─────────────────────────────────────────
     *   [SECCION]        Datos del Trato
     *                    ID | Fecha | Precio
     *   [SECCION]        Vendedor
     *                    Nombre | Correo | Telefono
     *   [SECCION]        Comprador
     *                    Nombre | Correo | Telefono
     *   [SECCION]        Producto
     *                    ID | Nombre | Descripcion | Estado
     *   [LINEA]          ─────────────────────────────────────────
     *   [PIE]            Texto informativo
     *
     * TODO: Si en el futuro el contenido no cabe en una página (muchos datos),
     *       habría que detectar cuando Y cae por debajo del margen inferior
     *       y llamar a doc.addPage() + abrir un nuevo PDPageContentStream.
     *
     * @param trato     objeto Trato con los datos de la transacción
     * @param vendedor  objeto Usuario vendedor
     * @param comprador objeto Usuario comprador
     * @return array de bytes con el PDF generado, o null si hubo error
     */
    public static byte[] generarPDFTrato(Trato trato, Usuario vendedor, Usuario comprador) {
        Logger.getLogger("org.apache.pdfbox").setLevel(Level.OFF);
        Logger.getLogger("org.apache.fontbox").setLevel(Level.OFF);
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDPage pagina = new PDPage(PDRectangle.A4);
            doc.addPage(pagina);

            //── FUENTES DISPONIBLES EN PDFBox sin archivos externos ──────────────
            //PDType1Font son las 14 fuentes estándar de PDF, siempre disponibles.
            PDType1Font fuenteNegrita  = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fuenteNormal   = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fuenteItalica  = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

            //Abrimos el stream de contenido de la página
            //AppendMode.OVERWRITE porque es una página nueva vacía
            try (PDPageContentStream cs = new PDPageContentStream(
                    doc, pagina, PDPageContentStream.AppendMode.OVERWRITE, false)) {

                float y = PDF_Y_INICIO; //Posición Y actual (empieza cerca del borde superior)

                //── TITULO ───────────────────────────────────────────────────────
                y = escribirTexto(cs, "FernanPop", fuenteNegrita, 22, PDF_MARGEN_IZQ, y, true);
                y = escribirTexto(cs, "Confirmacion de Transaccion", fuenteItalica, 11, PDF_MARGEN_IZQ, y - 4, true);

                //── LINEA SEPARADORA ─────────────────────────────────────────────
                y -= 8;
                dibujarLinea(cs, PDF_MARGEN_IZQ, PDRectangle.A4.getWidth() - PDF_MARGEN_DER, y);
                y -= PDF_INTERLINEA_GRANDE;

                //── SECCION: DATOS DEL TRATO ─────────────────────────────────────
                String fechaStr = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(trato.getFecha().getTime());

                y = escribirSeccion(cs, "Datos del Trato", fuenteNegrita, fuenteNormal, y,
                        new String[][]{
                                {"ID del trato:", trato.getID()},
                                {"Fecha:",        fechaStr},
                                {"Precio final:", String.format("%.2f EUR", trato.getPrecio())}
                        }
                );

                //── SECCION: VENDEDOR ─────────────────────────────────────────────
                y = escribirSeccion(cs, "Vendedor", fuenteNegrita, fuenteNormal, y,
                        new String[][]{
                                {"Nombre:",    vendedor.getNombre() + " " + vendedor.getApellidos()},
                                {"Correo:",    vendedor.getCorreoElectronico()},
                                {"Telefono:",  String.valueOf(vendedor.getTelefono())}
                        }
                );

                //── SECCION: COMPRADOR ────────────────────────────────────────────
                y = escribirSeccion(cs, "Comprador", fuenteNegrita, fuenteNormal, y,
                        new String[][]{
                                {"Nombre:",    comprador.getNombre() + " " + comprador.getApellidos()},
                                {"Correo:",    comprador.getCorreoElectronico()},
                                {"Telefono:",  String.valueOf(comprador.getTelefono())}
                        }
                );

                //── SECCION: PRODUCTO ─────────────────────────────────────────────
                //OJO!!: El producto se guarda por referencia en el Trato (Serializable).
                //      Si alguna vez se borra el .bin del vendedor y se recarga de otro sitio,
                //      podría quedar como null. De momento no es problema, pero ojo con refactorizaciones.
                String[][] datosProducto;
                if (trato.getProducto() != null) {
                    datosProducto = new String[][]{
                            {"ID:",          trato.getProducto().getID()},
                            {"Nombre:",      trato.getProducto().getNombre()},
                            {"Descripcion:", trato.getProducto().getDescripcion()},
                            {"Estado:",      trato.getProducto().getEstado()}
                    };
                } else {
                    datosProducto = new String[][]{{"Producto:", "(datos no disponibles)"}};
                }
                y = escribirSeccion(cs, "Producto", fuenteNegrita, fuenteNormal, y, datosProducto);

                //── LINEA SEPARADORA FINAL ────────────────────────────────────────
                y -= 4;
                dibujarLinea(cs, PDF_MARGEN_IZQ, PDRectangle.A4.getWidth() - PDF_MARGEN_DER, y);
                y -= PDF_INTERLINEA;

                //── PIE DE PAGINA ─────────────────────────────────────────────────
                //PDFBox no tiene salto de línea automático, así que el pie va en una sola línea
                escribirTexto(cs,
                        "Documento generado automaticamente por FernanPop. Conservalo como justificante.",
                        fuenteItalica, 8, PDF_MARGEN_IZQ, y, false);
            }

            doc.save(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            System.out.println("[PDF] Error al generar el PDF del trato: " + e.getMessage());
            return null;
        }
    }

    /**
     * Envía a vendedor y comprador un correo con el PDF del trato como adjunto.
     * Si el PDF no se pudo generar, envía igualmente los correos pero sin adjunto.
     *
     * Se llama desde Main.cerrarVenta() justo después de que app.cerrarVenta() tenga exito.
     *
     * IMPORTANTE: Si se quiere mandar el recibo tambien al admin, añadir aquí una tercera llamada
     *       con AppConfig.getInstance().getAdminEmail() como destinatario.
     *
     * @param trato     objeto Trato recien creado
     * @param vendedor  objeto Usuario vendedor
     * @param comprador objeto Usuario comprador
     */
    public static void enviarReciboPDFTrato(Trato trato, Usuario vendedor, Usuario comprador) {
        Logger.getLogger("org.apache.pdfbox").setLevel(Level.OFF);
        Logger.getLogger("org.apache.fontbox").setLevel(Level.OFF);
        byte[] pdfBytes   = generarPDFTrato(trato, vendedor, comprador);
        String nombrePDF  = "recibo_" + trato.getID() + ".pdf";
        String nombreProd = trato.getProducto() != null ? trato.getProducto().getNombre() : trato.getID();

        //Asuntos personalizados para vendedor y comprador
        String asuntoVendedor  = "Recibo de venta - " + nombreProd;
        String asuntoComprador = "Recibo de compra - " + nombreProd;

        //Cuerpos HTML del correo (breve, el detalle está en el PDF adjunto)
        String cuerpoVendedor  = generarCuerpoCorreoRecibo(vendedor.getNombre(),  "venta",  trato);
        String cuerpoComprador = generarCuerpoCorreoRecibo(comprador.getNombre(), "compra", trato);

        if (pdfBytes != null) {
            //Envio con PDF adjunto a ambas partes
            enviarConGMailAdjunto(vendedor.getCorreoElectronico(),  asuntoVendedor,  cuerpoVendedor,
                    pdfBytes, nombrePDF, "application/pdf");
            enviarConGMailAdjunto(comprador.getCorreoElectronico(), asuntoComprador, cuerpoComprador,
                    pdfBytes, nombrePDF, "application/pdf");
        } else {
            //Si el PDF fallo, al menos enviamos el correo de texto sin adjunto
            System.out.println("  [!] No se pudo generar el PDF del recibo. Se enviaran correos sin adjunto.");
            enviarConGMail(vendedor.getCorreoElectronico(),  asuntoVendedor,  cuerpoVendedor);
            enviarConGMail(comprador.getCorreoElectronico(), asuntoComprador, cuerpoComprador);
        }
    }

    /**
     * Genera el cuerpo HTML del correo de recibo para un usuario concreto.
     *
     * @param nombreUsuario nombre del destinatario
     * @param tipoOperacion "venta" o "compra"
     * @param trato         datos del trato realizado
     * @return String con el HTML listo para enviar
     */
    private static String generarCuerpoCorreoRecibo(String nombreUsuario, String tipoOperacion, Trato trato) {
        String nombreProducto = trato.getProducto() != null ? trato.getProducto().getNombre() : "(desconocido)";
        String precio         = String.format("%.2f EUR", trato.getPrecio());

        return "<html><body style='font-family:Arial,sans-serif;color:#333;'>"
                + "<h2 style='color:#1e50a0;'>FernanPop - Confirmacion de " + tipoOperacion + "</h2>"
                + "<p>Hola, <strong>" + nombreUsuario + "</strong>.</p>"
                + "<p>Tu " + tipoOperacion + " ha sido registrada correctamente.</p>"
                + "<ul>"
                + "  <li><strong>Producto:</strong> " + nombreProducto + "</li>"
                + "  <li><strong>Importe:</strong> "  + precio          + "</li>"
                + "  <li><strong>ID del trato:</strong> " + trato.getID() + "</li>"
                + "</ul>"
                + "<p>Adjuntamos el recibo en PDF con todos los detalles.</p>"
                + "<hr/>"
                + "<p style='font-size:11px;color:#888;'>Correo generado automaticamente por FernanPop. "
                + "No respondas a este mensaje.</p>"
                + "</body></html>";
    }

    //endregion

    //region AUXILIARES PDF (PDFBox)

    /**
     * Escribe una línea de texto en el PDPageContentStream en la posición indicada.
     * Devuelve la nueva Y tras escribir (Y - tamaño de la fuente), lista para la siguiente línea.
     *
     * @param cs        stream de contenido de la página
     * @param texto     texto a escribir
     * @param fuente    fuente PDType1Font a usar
     * @param tamano    tamaño de la fuente en puntos
     * @param x         posición X (desde el borde izquierdo)
     * @param y         posición Y (desde el borde inferior — PDFBox usa eje Y invertido)
     * @param centrado  si true, calcula X para centrar el texto en el ancho útil
     * @return nueva Y calculada (y - interlinea normal) para encadenar la siguiente llamada
     */
    private static float escribirTexto(PDPageContentStream cs, String texto,
                                       PDType1Font fuente, int tamano,
                                       float x, float y, boolean centrado) throws IOException {
        if (centrado) {
            //Calculamos el ancho del texto en pt para centrarlo
            float anchoTexto = fuente.getStringWidth(texto) / 1000 * tamano;
            x = (PDRectangle.A4.getWidth() - anchoTexto) / 2;
        }

        cs.beginText();
        cs.setFont(fuente, tamano);
        cs.newLineAtOffset(x, y);
        cs.showText(texto);
        cs.endText();

        return y - PDF_INTERLINEA; //Devolvemos la Y lista para la siguiente línea
    }

    /**
     * Dibuja una línea horizontal entre dos puntos X en la Y indicada.
     * Se usa para los separadores de sección del PDF.
     *
     * @param cs stream de contenido de la página
     * @param x1 X de inicio de la línea
     * @param x2 X de fin de la línea
     * @param y  Y donde se dibuja la línea
     */
    private static void dibujarLinea(PDPageContentStream cs, float x1, float x2, float y) throws IOException {
        cs.setLineWidth(0.8f);
        cs.moveTo(x1, y);
        cs.lineTo(x2, y);
        cs.stroke();
    }

    /**
     * Escribe un bloque de sección en el PDF: título de sección en negrita
     * y una lista de pares [etiqueta, valor] en dos columnas.
     *
     * La columna de etiqueta ocupa 1/3 del ancho útil y la de valor, 2/3.
     * Devuelve la Y resultante tras escribir el bloque.
     *
     * @param cs           stream de contenido de la página
     * @param titulo       texto del título de sección (ej. "Vendedor")
     * @param fuenteNeg    fuente negrita para el título y las etiquetas
     * @param fuenteNorm   fuente normal para los valores
     * @param y            Y de inicio del bloque
     * @param filas        array de pares {"etiqueta", "valor"} a escribir
     * @return Y tras el último par escrito más el espacio de separación entre secciones
     */
    private static float escribirSeccion(PDPageContentStream cs,
                                         String titulo,
                                         PDType1Font fuenteNeg,
                                         PDType1Font fuenteNorm,
                                         float y,
                                         String[][] filas) throws IOException {
        //Título de la sección en negrita, tamaño 12
        cs.beginText();
        cs.setFont(fuenteNeg, 12);
        cs.newLineAtOffset(PDF_MARGEN_IZQ, y);
        cs.showText(titulo);
        cs.endText();
        y -= PDF_INTERLINEA;

        //Columna de etiqueta: de PDF_MARGEN_IZQ hasta 1/3 del ancho util
        float xEtiqueta = PDF_MARGEN_IZQ;
        float xValor    = PDF_MARGEN_IZQ + PDF_ANCHO_UTIL / 3f;

        for (String[] fila : filas) {
            String etiqueta = fila[0];
            String valor    = fila.length > 1 && fila[1] != null ? fila[1] : "";

            //Etiqueta en negrita
            cs.beginText();
            cs.setFont(fuenteNeg, 10);
            cs.newLineAtOffset(xEtiqueta, y);
            cs.showText(etiqueta);
            cs.endText();

            //Valor en normal
            cs.beginText();
            cs.setFont(fuenteNorm, 10);
            cs.newLineAtOffset(xValor, y);
            cs.showText(valor);
            cs.endText();

            y -= PDF_INTERLINEA;
        }

        return y - 6f; //Pequeño espacio extra entre secciones
    }

    //endregion

    //region CSV – LISTADO DE PRODUCTOS

    /**
     * Genera en memoria un archivo CSV con todos los productos en venta
     * de todos los usuarios del sistema.
     *
     * Sin librerías externas: se construye manualmente con un StringBuilder.
     *
     * Formato del CSV:
     *   - Separador: punto y coma (;) en vez de coma, para que Excel español
     *     lo abra directamente sin tener que configurar nada.
     *   - Codificación: UTF-8 con BOM (los tres bytes 0xEF 0xBB 0xBF al inicio).
     *     El BOM le indica a Excel que el archivo es UTF-8, evitando que
     *     las tildes y la ñ aparezcan como caracteres raros al abrirlo.
     *   - Los campos que puedan contener el separador (;) o comillas se envuelven
     *     en comillas dobles y las comillas internas se escapan duplicándolas ("").
     *     Esto sigue el estándar RFC 4180 de CSV.
     *
     * Cabecera: ID Producto;Nombre;Descripcion;Precio (EUR);Estado;Vendedor;Email Vendedor
     *
     * @param usuarios lista de todos los usuarios (con sus productos dentro)
     * @return array de bytes con el CSV generado en UTF-8 con BOM, o null si hubo error
     */
    public static byte[] generarCSVProductos(ArrayList<Usuario> usuarios) {
        try {
            StringBuilder sb = new StringBuilder();

            //── CABECERA ──────────────────────────────────────────────────────────
            sb.append("ID Producto;Nombre;Descripcion;Precio (EUR);Estado;Vendedor;Email Vendedor\n");

            //── FILAS DE DATOS ────────────────────────────────────────────────────
            for (Usuario u : usuarios) {
                for (Producto p : u.getEnVenta()) {
                    sb.append(escaparCSV(p.getID())).append(";");
                    sb.append(escaparCSV(p.getNombre())).append(";");
                    sb.append(escaparCSV(p.getDescripcion())).append(";");
                    //Precio con punto decimal (no coma) para que sea portable entre locales
                    sb.append(String.format("%.2f", p.getPrecio())).append(";");
                    sb.append(escaparCSV(p.getEstado())).append(";");
                    sb.append(escaparCSV(u.getNombre() + " " + u.getApellidos())).append(";");
                    sb.append(escaparCSV(u.getCorreoElectronico())).append("\n");
                }
            }

            //── BOM UTF-8 + CONTENIDO ─────────────────────────────────────────────
            //El BOM (Byte Order Mark) son 3 bytes que le dicen a Excel que el archivo
            //es UTF-8. Sin ellos, las tildes y la ñ aparecen mal al abrirlo en Windows.
            byte[] bom          = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
            byte[] contenido    = sb.toString().getBytes(StandardCharsets.UTF_8);
            byte[] resultado    = new byte[bom.length + contenido.length];

            System.arraycopy(bom,       0, resultado, 0,           bom.length);
            System.arraycopy(contenido, 0, resultado, bom.length,  contenido.length);

            return resultado;

        } catch (Exception e) {
            System.out.println("[CSV] Error al generar el CSV de productos: " + e.getMessage());
            return null;
        }
    }

    /**
     * Escapa un campo para el formato CSV según RFC 4180.
     *
     * Reglas aplicadas:
     *   - Si el campo contiene ; (separador), " (comillas) o salto de línea,
     *     se envuelve entre comillas dobles.
     *   - Cualquier comilla doble interior se duplica ("").
     *   - Si el campo es null, se devuelve cadena vacía.
     *
     * @param campo texto del campo a escapar
     * @return campo escapado y listo para escribir en el CSV
     */
    private static String escaparCSV(String campo) {
        if (campo == null) return "";

        //Si contiene caracteres problemáticos, lo envolvemos en comillas
        if (campo.contains(";") || campo.contains("\"") || campo.contains("\n")) {
            return "\"" + campo.replace("\"", "\"\"") + "\"";
        }
        return campo;
    }

    /**
     * Genera el CSV de todos los productos y lo envia por correo al administrador.
     * Se llama desde Main.enviarListadoProductosPorCorreo().
     *
     * @param usuarios   lista de todos los usuarios del sistema
     * @param emailAdmin correo del administrador destino
     * @return true si el correo se envio correctamente
     */
    public static boolean enviarCSVProductosAlAdmin(ArrayList<Usuario> usuarios, String emailAdmin) {
        byte[] csvBytes = generarCSVProductos(usuarios);

        if (csvBytes == null) {
            System.out.println("  [X] No se pudo generar el archivo CSV.");
            return false;
        }

        String fechaStr     = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date());
        String fechaArchivo = new SimpleDateFormat("yyyyMMdd_HHmm").format(new java.util.Date());

        String asunto = "FernanPop - Listado de productos " + fechaStr;

        String cuerpo = "<html><body style='font-family:Arial,sans-serif;'>"
                + "<h2 style='color:#1e50a0;'>FernanPop - Listado de productos en venta</h2>"
                + "<p>Hola, administrador.</p>"
                + "<p>Adjuntamos el listado completo de productos actualmente en venta en formato CSV.</p>"
                + "<p>Total de usuarios en el sistema: <strong>" + usuarios.size() + "</strong></p>"
                + "<p style='font-size:12px;color:#555;'>El archivo se puede abrir directamente con Excel. "
                + "Si los caracteres no se ven bien, importalo como UTF-8 con separador punto y coma (;).</p>"
                + "<hr/>"
                + "<p style='font-size:11px;color:#888;'>Generado automaticamente por FernanPop el " + fechaStr + ".</p>"
                + "</body></html>";

        String nombreArchivo = "productos_" + fechaArchivo + ".csv";
        String tipoMIME      = "text/csv; charset=utf-8";

        return enviarConGMailAdjunto(emailAdmin, asunto, cuerpo, csvBytes, nombreArchivo, tipoMIME);
    }

    //endregion
}
