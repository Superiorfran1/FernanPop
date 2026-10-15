package persistence;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Log – Sistema de registro de actividad de la aplicación.
 *
 * Graba en un archivo "log.txt" los eventos más relevantes de la app:
 * inicios/cierres de sesión, nuevos productos y ventas cerradas.
 *
 * Cada línea del log sigue el formato:
 *   "TipoEvento";dato1;dato2;...;fecha/hora
 *
 * La ruta del archivo se obtiene de AppConfig (config.properties).
 * AppConfig ya devuelve rutas absolutas, así que NO hay que añadir ".".
 *
 * TODO: Si tenías un log.txt en la ruta antigua (con "."), muévelo manualmente
 *       a la nueva ubicación o perderás el historial anterior.
 */
public class Log {

    //ATRIBUTOS
    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss"); //Formato de fecha/hora usado en cada entrada del log.


//MÉTODOS
    //region MÉTODOS PÚBLICOS DE REGISTRO

    /**
     * Registra un inicio de sesión en el log.
     * Formato: "Inicio de sesión";correoUser;fecha/hora
     *
     * @param correoUsuario correo del usuario que inició sesión
     */
    public static void registrarInicioSesion(String correoUsuario) {
        escribir("\"Inicio de sesion\";" + correoUsuario + ";" + ahora());
    }

    /**
     * Registra un cierre de sesión en el log.
     * Formato: "Cierre de sesión";correoUser;fecha/hora
     *
     * @param correoUsuario correo del usuario que cerró sesión
     */
    public static void registrarCierreSesion(String correoUsuario) {
        escribir("\"Cierre de sesion\";" + correoUsuario + ";" + ahora());
    }

    /**
     * Registra la publicación de un nuevo producto en el log.
     * Formato: "Nuevo producto en venta";ID_Producto;correoUser;fecha/hora
     *
     * @param ID_Producto identificador del producto publicado
     * @param correoUsuario correo del vendedor
     */
    public static void registrarNuevoProducto(String ID_Producto, String correoUsuario) {
        escribir("\"Nuevo producto en venta\";" + ID_Producto + ";" + correoUsuario + ";" + ahora());
    }

    /**
     * Registra el cierre de una venta en el log.
     * Formato: "Venta Cerrada";correoVendedor;correoComprador;fecha/hora
     *
     * @param correoVendedor  correo del vendedor
     * @param correoComprador correo del comprador
     */
    public static void registrarVentaCerrada(String correoVendedor, String correoComprador) {
        escribir("\"Venta Cerrada\";" + correoVendedor + ";" + correoComprador + ";" + ahora());
    }

    //endregion

    //region MÉTODOS PRIVADOS AUXILIARES

    /**
     * Devuelve la fecha y hora actual formateada para el log.
     *
     * @return String con la fecha/hora actual
     */
    private static String ahora() {
        return SDF.format(new Date());
    }

    /**
     * Escribe una línea en el archivo log.txt.
     * Si el archivo o la carpeta no existen, los crea.
     * Usa modo "append" para no sobreescribir entradas anteriores.
     *
     * @param linea texto de la línea a añadir al log
     */
    private static void escribir(String linea) {
        // Obtenemos la ruta del log desde la configuración (ya es absoluta)
        String ruta = AppConfig.getInstance().getRutaLog();
        File directorio = new File(ruta);

        // Creamos el directorio si no existe
        if (!directorio.exists()) {
            directorio.mkdirs();
        }

        File archivoLog = new File(directorio, "log.txt");

        // Abrimos el log modo append (true) para no borrar entradas anteriores
        try (PrintWriter pw = new PrintWriter(new FileWriter(archivoLog, true))) {
            pw.println(linea);
        } catch (IOException e) {
            // No interrumpimos la aplicación si el log falla, solo avisamos
            System.out.println("[Log] Aviso: no se pudo escribir en el log. " + e.getMessage());
        }
    }

    //endregion
}
