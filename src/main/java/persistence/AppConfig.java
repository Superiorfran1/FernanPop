package persistence;

import utils.Utils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * AppConfig – Gestiona el archivo de configuración config.properties.
 *
 * Carga las propiedades al arrancar y expone métodos para leerlas y
 * escribirlas. También es el punto centralizado para leer/guardar el
 * último inicio de sesión de cada usuario.
 *
 * Solo debe existir UNA instancia de esta clase o se te irá a la mierda.
 */
public class AppConfig {

//ATRIBUTOS

    /**
     * Ruta del archivo de configuración.
     * Se construye de forma dinámica usando la ubicación real del .jar / del proyecto,
     * para que funcione tanto en IntelliJ como al ejecutar el .jar desde cualquier ruta.
     * La carpeta "FernanPop/data" se crea justo al lado del directorio de trabajo.
     *
     * TODO: Si el proyecto cambia de nombre o de estructura, REVISAR esta ruta.
     */
    private static final String RUTA_CONFIG = resolverRutaConfig();

    private static AppConfig instancia; //Instancia única de la clase. POR FAVOR NO CREAR MÁS
    private final Properties props; //Objeto Properties que contiene todas las claves y valores

//MÉTODOS

    //region CONSTRUCTOR
    /** Constructor privado: carga el archivo de propiedades al instanciarse. */
    private AppConfig() {
        props = new Properties();
        cargar();
    }

    //endregion

    //region GETTERS & SETTERS

    /**
     * Devuelve la instancia única de AppConfig.
     * Si aún no existe la crea (lazy initialization).
     *
     * @return la instancia única
     */
    public static AppConfig getInstance() {
        if (instancia == null) {
            instancia = new AppConfig();
        }
        return instancia;
    }

    //endregion

    //region RESOLUCIÓN DE RUTA

    /**
     * Calcula la ruta absoluta del archivo config.properties de forma robusta.
     *
     * Estrategia (en orden de preferencia):
     *   1. Busca en C:\Users\junio\IdeaProjects\ejRepaso1\FernanPop\FernanPop\data\config.properties
     *   2. Busca en dirTrabajo/FernanPop/FernanPop/data/config.properties
     *   3. Busca en dirTrabajo/FernanPop/data/config.properties
     *   4. Si está dentro de FernanPop, busca en ./data/config.properties
     *   5. Fallback: usa dirTrabajo/FernanPop/data/config.properties
     *
     * @return ruta absoluta del config.properties como String
     */
    private static String resolverRutaConfig() {
        String dirTrabajo = System.getProperty("user.dir");

        // Posibles ubicaciones del archivo (en orden de preferencia)
        String[] rutasPosibles = {
            // Ruta esperada cuando se ejecuta como aplicación en Tomcat
            "C:\\Users\\junio\\IdeaProjects\\ejRepaso1\\FernanPop\\FernanPop\\data\\config.properties",
            // Ruta cuando dirTrabajo es la carpeta padre (ej: C:\Users\junio\IdeaProjects\ejRepaso1\FernanPop)
            dirTrabajo + File.separator + "FernanPop" + File.separator + "data" + File.separator + "config.properties",
            // Ruta cuando dirTrabajo ya es FernanPop
            dirTrabajo + File.separator + "data" + File.separator + "config.properties"
        };

        for (String ruta : rutasPosibles) {
            File f = new File(ruta);
            if (f.exists() && f.isFile()) {
                System.out.println("[AppConfig] Encontrado config.properties en: " + ruta);
                return ruta;
            }
        }

        // Si no encuentra el archivo en ningún lado, usa fallback
        String fallback = dirTrabajo + File.separator + "FernanPop" + File.separator + "data" + File.separator + "config.properties";
        System.out.println("[AppConfig] No encontrado config.properties. Usando fallback: " + fallback);
        return fallback;
    }

    //endregion

    //region CARGA Y GUARDADO

    /**
     * Carga el archivo config.properties desde disco.
     * Si no existe, usa los valores por defecto definidos en el código
     * y crea el archivo (con su carpeta si hace falta).
     */
    private void cargar() {
        File f = new File(RUTA_CONFIG);
        System.out.println("[AppConfig] Buscando config en: " + f.getAbsolutePath());
        System.out.println("[AppConfig] ¿Existe?: " + f.exists());

        if (f.exists()) {
            try (InputStream in = new FileInputStream(f)) {
                props.load(in);
                System.out.println("[AppConfig] ✓ Cargado config.properties correctamente");
            } catch (IOException e) {
                System.out.println("[Config] Aviso: no se pudo leer config.properties. Se usarán valores por defecto.");
                System.out.println("[Config] Error: " + e.getMessage());
                Utils.pulsaEnter();
            }
        } else {
            System.out.println("[AppConfig] ✗ No encontrado config.properties. Usando valores por defecto.");
            //Valores por defecto si no hay archivo
            //Las rutas de datos y log también se resuelven de forma dinámica
            //para que funcionen desde cualquier directorio de trabajo
            String baseDir = new File(RUTA_CONFIG).getParentFile().getParent(); // .../FernanPop
            props.setProperty("ruta.datos",   baseDir + File.separator + "data" + File.separator + "bins");
            props.setProperty("ruta.log",     baseDir + File.separator + "data");
            props.setProperty("pagina.tamano","5");
            props.setProperty("acceso.invitado", "true");
            //Los datos sensibles NO se escriben en el código: se crean vacíos y
            //hay que rellenarlos a mano en config.properties.
            props.setProperty("admin.correoElectronico",  "");
            props.setProperty("admin.contrasenia","");
            //Datos de conexión a la base de datos en la nube. Por defecto apuntan
            //a una BBDD local de pruebas; cada equipo debe cambiar estos valores
            //por los de su propio servicio en la nube (ej. Clever Cloud, Railway, etc.)
            //para que el programa funcione SIN tener que recompilar nada.
            props.setProperty("db.url",  "jdbc:mysql://localhost:3306/fernanpop?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true");
            props.setProperty("db.user", "root");
            props.setProperty("db.pass", "");
            //Cuenta de Gmail (contraseña de aplicación) desde la que se envían los correos
            props.setProperty("gmail.remitente", "");
            props.setProperty("gmail.clave", "");
            //Bot de Telegram para los avisos al administrador
            props.setProperty("telegram.token", "");
            props.setProperty("telegram.chatId", "");
            guardar(); // Crea el archivo con los valores por defecto
        }
    }

    /**
     * Guarda el estado actual de las propiedades en config.properties.
     * Crea la carpeta destino si no existe.
     * Se llama automáticamente cuando se modifica algún valor.
     */
    public void guardar() {
        File archivo = new File(RUTA_CONFIG);
        // Creamos el directorio padre si no existe (data/)
        if (!archivo.getParentFile().exists()) {
            archivo.getParentFile().mkdirs();
        }
        try (OutputStream out = new FileOutputStream(archivo)) {
            props.store(out, "FernanPop – Configuracion de la aplicacion");
        } catch (IOException e) {
            System.out.println("[Config] Error: no se pudo guardar config.properties.");
            Utils.pulsaEnter();
        }
    }

    //endregion

    //region GETTERS DE CONFIGURACIÓN

    /**
     * Ruta absoluta donde se guardan los archivos .bin de datos.
     * Si la propiedad contiene una ruta relativa (legado), la devuelve tal cual;
     * si es absoluta, la devuelve directamente.
     *
     * @return ruta de datos
     */
    public String getRutaDatos() {
        return props.getProperty("ruta.datos",
                new File(RUTA_CONFIG).getParentFile().getParent()
                        + File.separator + "data" + File.separator + "bins");
    }

    /**
     * Ruta absoluta donde se guarda el log de actividad.
     * @return ruta del log
     */
    public String getRutaLog() {
        return props.getProperty("ruta.log",
                new File(RUTA_CONFIG).getParentFile().getParent()
                        + File.separator + "data");
    }

    /**
     * URL de conexión JDBC a la base de datos en la nube.
     * Formato esperado: jdbc:mysql://host:puerto/nombreBaseDeDatos
     * @return URL de conexión
     */
    public String getDbUrl() {
        return props.getProperty("db.url", "jdbc:mysql://localhost:3306/fernanpop?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true");
    }

    /**
     * Usuario de la base de datos.
     * @return usuario de conexión a la BBDD
     */
    public String getDbUser() {
        return props.getProperty("db.user", "root");
    }

    /**
     * Contraseña de la base de datos.
     * @return contraseña de conexión a la BBDD
     */
    public String getDbPass() {
        return props.getProperty("db.pass", "");
    }

    /**
     * Número de productos por página en los listados.
     * @return tamaño de página (ej. 5)
     */
    public int getPaginaTamano() {
        try {
            return Integer.parseInt(props.getProperty("pagina.tamano", "5"));
        } catch (NumberFormatException e) {
            return 5;
        }
    }

    /**
     * Indica si el acceso sin login (modo invitado) está habilitado.
     * @return true si el acceso de invitado está permitido
     */
    public boolean isAccesoInvitado() {
        return Boolean.parseBoolean(props.getProperty("acceso.invitado", "true"));
    }

    /**
     * Email del administrador del sistema.
     * @return email del admin
     */
    public String getAdminCorreoElectronico() {
        return props.getProperty("admin.correoElectronico", "");
    }

    /**
     * Contraseña del administrador del sistema.
     * @return contraseña del admin
     */
    public String getAdminContrasenia() {
        return props.getProperty("admin.contrasenia", "");
    }

    /**
     * Correo de la cuenta de Gmail desde la que la aplicación envía los correos.
     * @return correo remitente (cadena vacía si no está configurado)
     */
    public String getGmailRemitente() {
        return props.getProperty("gmail.remitente", "").trim();
    }

    /**
     * Contraseña de aplicación de la cuenta de Gmail remitente
     * (no es la contraseña normal de la cuenta).
     * @return contraseña de aplicación (cadena vacía si no está configurada)
     */
    public String getGmailClave() {
        return props.getProperty("gmail.clave", "").trim();
    }

    /**
     * Token del bot de Telegram que envía los avisos al administrador.
     * @return token del bot (cadena vacía si no está configurado)
     */
    public String getTelegramToken() {
        return props.getProperty("telegram.token", "").trim();
    }

    /**
     * Identificador del chat de Telegram que recibe los avisos.
     * @return chat id (cadena vacía si no está configurado)
     */
    public String getTelegramChatId() {
        return props.getProperty("telegram.chatId", "").trim();
    }

    //endregion

    //region GESTIÓN DE ÚLTIMO LOGIN

    /**
     * Guarda la fecha y hora actual como último inicio de sesión del usuario indicado.
     * La clave en el archivo es: ultimo.login.<email>
     *
     * @param email correo electrónico del usuario que acaba de iniciar sesión
     */
    public void actualizarUltimoLogin(String email) {
        String fecha = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        // Reemplazo de '@' y '.' por '_' para que sea una clave válida en Properties. Si no da problemas
        String clave = "ultimo.login." + email.replace("@", "_at_").replace(".", "_");
        props.setProperty(clave, fecha);
        guardar();
    }

    /**
     * Recupera la fecha del último inicio de sesión de un usuario.
     * Si nunca ha iniciado sesión, devuelve null.
     *
     * @param email correo electrónico del usuario
     * @return fecha en formato "dd/MM/yyyy HH:mm", o null si no existe
     */
    public String getUltimoLogin(String email) {
        String clave = "ultimo.login." + email.replace("@", "_at_").replace(".", "_");
        return props.getProperty(clave, null);
    }

    //endregion

    //region OTROS MÉTODOS

    /**
     * Devuelve todas las propiedades del archivo como texto formateado.
     * Útil para el panel de administración.
     *
     * @return String con todas las claves y valores
     */
    public String mostrarConfiguracion() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== CONFIGURACIÓN ACTUAL ===\n");
        // Ordenamos para que sea más legible
        props.stringPropertyNames().stream().sorted().forEach(clave -> {
            // Ocultamos los datos secretos (contraseñas y token) por seguridad
            boolean esContrasenia = clave.equals("admin.contrasenia") || clave.equals("db.pass")
                    || clave.equals("gmail.clave") || clave.equals("telegram.token");
            String valor = esContrasenia
                    ? "*".repeat(props.getProperty(clave).length())
                    : props.getProperty(clave);
            sb.append(String.format("  %-40s = %s%n", clave, valor));
        });
        return sb.toString();
    }

    //endregion
}