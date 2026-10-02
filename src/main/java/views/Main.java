package views;

import controller.*;
import data.*;
import models.*;
import persistence.*;
import utils.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Scanner;

/**
 * Main – Punto de entrada y capa de vistas de FernanPop.
 * .
 * Contiene todos los menús y pantallas de la aplicación de consola.
 * La lógica de negocio está delegada en Controller; esta clase
 * solo se ocupa de pedir datos al usuario y mostrar resultados.
 * .
 * Al arrancar:
 *   1. Carga AppConfig (config.properties)
 *   2. Carga los datos desde disco (Persistence)
 *   3. Muestra el menú principal según si el acceso invitado está activo
 */
public class Main {

    //ATRIBUTOS
    private static final Scanner SCANNER = new Scanner(System.in);


//MÉTODOS
    /**
     * Metodo principal de arranque de la aplicación.
     * Llama a runApp() que contiene el bucle principal.
     */
    /*public static void main(String[] args) {
        runApp();
    }

    //BUCLE PRINCIPAL
    /**
     * Inicializa la aplicación y gestiona el bucle principal de navegación.
     *-
     * Flujo:
     *   1. Carga datos de disco (o inicia vacío si es la primera vez)
     *   2. Inserta datos de prueba (se puede quitar en producción)
     *   3. Muestra el menú adecuado según si hay sesión activa o no
     *   4. Guarda los datos al salir
     */
    static void runApp() {
        //Carga los usuarios guardados en disco
        Controller app = new Controller();


        /*Garantiza que el administrador definido en config.properties existe
        siempre como Usuario en el sistema, incluso en la primera ejecución
        o si alguien borró accidentalmente su .bin.
        Se lee el email y contraseña del config en cada arranque para que
        cualquier cambio manual en config.properties surta efecto sin tocar código.
        CUIDADO: Si cambias los datos del admin en config.properties mientras la app
        está corriendo, el cambio no se aplica hasta el próximo arranque.*/
        garantizarUsuarioAdmin(app);

        Usuario usuarioActivo = null;
        boolean salir = false;

        while (!salir) {
            if (usuarioActivo == null) {
                //Sin sesión: mostramos el menú público
                int opcion = menuSinSesion(app);
                switch (opcion) {
                    case 1 -> usuarioActivo = intentarLogin(app);
                    case 0 -> salir = true;
                    //Los casos 2, 3, 4 se gestionan dentro de menuSinSesion()
                }
            } else {
                //Con sesión activa: menú privado (puede devolver null para cerrar sesión)
                usuarioActivo = menuConSesion(app, usuarioActivo);
            }
        }

        //Guardado de TODOS datos antes de cerrar
        app.guardar();

        //Animación de cierre
        Utils.simularCierre("Saliendo de FernanPop");
        System.out.println("\n¡Hasta pronto!");
    }

    //region MENÚ SIN SESIÓN
    /**
     * Muestra el menú principal sin sesión iniciada y gestiona las opciones.
     * Las opciones 2, 3 y 4 se ejecutan directamente aquí.
     * Las opciones 0 y 1 se devuelven al bucle principal de runApp().
     * .
     * Si el acceso invitado está desactivado en config.properties,
     * las opciones 3 y 4 (ver productos) no estarán disponibles.
     *
     * @param app instancia del Controller
     * @return la opción elegida (0 = salir, 1 = login)
     */
    private static int menuSinSesion(Controller app) {
        Utils.limpiaPantalla();

        //Se lee si el acceso de invitado está habilitado en la configuración
        boolean accesoInvitado = AppConfig.getInstance().isAccesoInvitado();

        UI.pintaMenuSinSesion(accesoInvitado);

        //El rango de opciones cambia según si hay acceso invitado o no
        int maxOpcion = accesoInvitado ? 4 : 2;
        int opcion = Utils.pideDatoEntero("una opción: ", 0, maxOpcion);

        switch (opcion) {
            case 2 -> registrarUsuario(app); //Registro siempre disponible

            case 3 -> {
                //Buscar productos: solo si el acceso invitado está habilitado
                if (accesoInvitado) {
                    menuBuscarProductos(app, null); //null = sin usuario activo
                } else {
                    System.out.println("✗ Debes iniciar sesión para acceder.");
                    Utils.pulsaEnter();
                }
            }

            case 4 -> {
                //Ver todos los productos: solo si el acceso invitado está habilitado
                if (accesoInvitado) {
                    mostrarTodosLosProductos(app);
                } else {
                    System.out.println("✗ Debes iniciar sesión para acceder.");
                    Utils.pulsaEnter();
                }
            }
        }
        return opcion;
    }

    //LOGIN
    /**
     * Solicita email y contraseña al usuario e intenta autentificarlo.
     * .
     * Si el login tiene éxito:
     *   - Muestra mensaje de bienvenida
     *   - Muestra el último inicio de sesión (leído de config.properties)
     *   - Actualiza el último login en config.properties
     *   - Registra el evento en el log
     *   - Avisa si hay valoraciones pendientes
     *
     * @param app instancia del Controller
     * @return el Usuario autenticado, o null si el login falló
     */
    private static Usuario intentarLogin(Controller app) {
        Utils.limpiaPantalla();
        System.out.println("=== INICIAR SESIÓN ===");
        System.out.print("Correo Electrónico: ");
        String correoElectronico = SCANNER.nextLine().trim();
        System.out.print("Contraseña: ");
        String clave = SCANNER.nextLine().trim();

        Usuario u = app.login(correoElectronico, clave);

        if (u != null) {
            //Login correcto

            //Muestra el último inicio de sesión antes de actualizarlo
            String ultimoLogin = AppConfig.getInstance().getUltimoLogin(correoElectronico);
            if (ultimoLogin != null) {
                //Parseo "dd/MM/yyyy HH:mm" para mostrar un mensaje más natural. Odio gestionar las fechas así que esto lo gestionó la IA xd
                try {
                    String[] partes = ultimoLogin.split("[/ :]");
                    //partes: [0]=día, [1]=mes, [2]=año, [3]=hora, [4]=min
                    String[] meses = {"enero","febrero","marzo","abril","mayo","junio",
                            "julio","agosto","septiembre","octubre","noviembre","diciembre"};
                    int mesNum = Integer.parseInt(partes[1]) - 1;
                    System.out.println("\n  ℹ Usted inició sesión por última vez el " +
                            partes[0] + " de " + meses[mesNum] + " de " + partes[2] +
                            " a las " + partes[3] + ":" + partes[4] + ".");
                } catch (Exception ignored) {
                    //Si el formato falla, mostramos el texto tal cual. WTF, ¿cómo que fallar? Gemini, que decepción. Algún día me pasaré a Claude.
                    System.out.println("\n  ℹ Último inicio de sesión: " + ultimoLogin);
                }
                Utils.esperar(1800);
            }

            //Guardado de la fecha/hora de este login en config.properties
            AppConfig.getInstance().actualizarUltimoLogin(correoElectronico);

            //Registro del inicio de sesión en el log
            Log.registrarInicioSesion(correoElectronico);



        } else {
            //Login fallido
            System.out.println("\n✗ Correo electrónico o contraseña incorrectos.");
            Utils.pulsaEnter();
        }

        return u;
    }

    // REGISTRO DE USUARIO
    /**
     * Guía al usuario por el proceso de registro:
     *   1. Solicita nombre, apellidos, correo electrónico, contraseña y teléfono
     *   2. Valida el formato del correo y que no esté ya registrado
     *   3. Envía un código de verificación por correo
     *   4. Si el código es correcto, crea el usuario
     *   5. Notifica al admin por Telegram
     *
     * @param app instancia del Controller
     */
    private static void registrarUsuario(Controller app) {
        Utils.limpiaPantalla();
        System.out.println("=== REGISTRO DE NUEVO USUARIO ===");

        //Solicitud del nombre y apellidos
        System.out.print("Nombre: ");
        String nombre = SCANNER.nextLine().trim();
        System.out.print("Apellidos: ");
        String apellidos = SCANNER.nextLine().trim();

        //Solicitud del correo electrónico con validación de formato y unicidad
        String correoElectronico;
        do {
            System.out.print("Correo electrónico: ");
            correoElectronico = SCANNER.nextLine().trim();
            if (!Utils.emailValido(correoElectronico)) {
                System.out.println("  ✗ Formato inválido.");
                correoElectronico = "";
            } else if (app.buscaPorCorreoElectronico(correoElectronico) != null) {
                System.out.println("  ✗ Email ya registrado.");
                correoElectronico = "";
            }
        } while (correoElectronico.isEmpty());

        //Solicitud de la contraseña y teléfono
        System.out.print("Elige tu contraseña: ");
        String contrasenia = SCANNER.nextLine().trim();
        int telefono = Utils.pideDatoEntero("móvil (9 dígitos): ", 600000000, 799999999);

        //Generación y envío del código de verificación por correo
        String codigoVerificacion = Utils.generarClave(6);
        System.out.println("\nEnviando código de confirmación a " + correoElectronico + "...");

        try {
            String asunto = "Código de verificación FernanPop";
            String mensaje = generarEmailVerificacion(nombre, codigoVerificacion);
            Communications.enviarConGMail(correoElectronico, asunto, mensaje);

            System.out.println("✓ Código enviado. Por favor, revísalo.");
            System.out.print("Introduce el código recibido: ");
            String codigoIntroducido = SCANNER.nextLine().trim();

            //Verificación del código introducido
            if (codigoIntroducido.equalsIgnoreCase(codigoVerificacion)) {
                //Creación del usuario
                Usuario nuevoUsuario = new Usuario(app.generaID_Usuario(), nombre, apellidos, correoElectronico, contrasenia, telefono);
                //Añadir el usuario a la aplicación
                if (app.addUsuario(nuevoUsuario)) {
                    System.out.println("\n✓ ¡Cuenta verificada y creada con éxito!");

                    //Guardado del objeto que acabamos de crear en el disco
                    app.guardarUnico(nuevoUsuario);

                    //Notificación al admin por Telegram (Se ejecuta si ha ido bien)
                    try {
                        String msgTel = UI.msgNuevoUsuario(nombre, correoElectronico, contrasenia);
                        Communications.enviaMensajeTelegram(msgTel);
                    } catch (Throwable ignored) {}

                } else {
                    System.out.println("\n✗ Error al guardar el usuario en el sistema.");
                }
            } else {
                System.out.println("\n✗ Código incorrecto. El registro ha sido cancelado.");
            }
        } catch (Throwable e) {
            System.out.println("\n✗ Error al enviar el email de verificación.");
        }
        Utils.pulsaEnter();
    }

    //CLAVE DE VERIFICACIÓN
    /** Genera el HTML del email de verificación de cuenta */
    private static String generarEmailVerificacion(String nombre, String codigoVerificacion) {
        return UI.generaEmailVerificacion(nombre, codigoVerificacion);
    }

    //endregion

    // region MENÚ ADMINISTRADOR
    /**
     * Menú extendido exclusivo para el administrador del sistema.
     * Además de todas las opciones normales, el admin tiene:
     *   - Ver configuración del sistema
     *   - Enviar listado de productos por correo en CSV
     *   - Realizar copia de seguridad
     *
     * @param app instancia del Controller
     * @param u   usuario administrador
     * @return el usuario admin (para continuar sesión) o null (para cerrarla)
     */
    private static Usuario menuAdmin(Controller app, Usuario u) {
        UI.pintaMenuAdmin(u);
        int opcion = Utils.pideDatoEntero("una opción: ", 0, 12);

        switch (opcion) {
            //Opciones normales (igual que el menú estándar)
            case 1  -> { mostrarPerfil(u);                         return u; }
            case 2  -> { cambiarDatosPersonales(u);                return u; }
            case 3  -> { menuMisProductos(app, u);                 return u; }
            case 4  -> { introducirProducto(app, u);               return u; }
            case 5  -> { menuBuscarProductos(app, u);              return u; }
            case 6  -> { menuValoracionesPendientes(app, u);       return u; }
            case 7  -> { verHistorialTratos(u);                    return u; }
            case 8  -> { return borrarPerfil(app, u); }

            //Opciones exclusivas del admin
            case 10 -> { mostrarConfiguracionAdmin(app);           return u; }
            case 11 -> { enviarListadoProductosPorCorreo(app, u);  return u; }
            case 12 -> { realizarCopiaSeguridad();                 return u; }

            case 9  -> { cerrarSesion(u);                          return null; }
            case 0  -> {
                app.guardar();
                Utils.simularCierre("Saliendo de FernanPop");
                System.exit(0);
                return null;
            }
            default -> { return u; }
        }
    }

    /**
     * Muestra la configuración completa del sistema (solo admin).
     * Incluye todas las claves del config.properties y los últimos logins.
     *
     * @param app instancia del Controller
     */
    private static void mostrarConfiguracionAdmin(Controller app) {
        Utils.limpiaPantalla();
        int ancho = 100;

        // Helper: imprime una línea dentro de la caja con margen izquierdo de 2 espacios
        // "║  contenido<relleno>║"  — si contenido supera el ancho interior, se corta a la fuerza
        // (las líneas de esta pantalla están diseñadas para caber en ancho=100)

        // ── Cabecera ───────────────────────────────────────────────────────────────────────────────
        System.out.println("╔" + "═".repeat(ancho) + "╗");
        System.out.println(UI.centrarEnCaja("PANEL DE ADMINISTRACIÓN – CONFIGURACIÓN", ancho));
        System.out.println("╠" + "═".repeat(ancho) + "╣");

        // ── Propiedades generales (excluimos rutas y último login) ─────────────────────────────────
        String configRaw = AppConfig.getInstance().mostrarConfiguracion();
        String[] lineasConfig = configRaw.split("\n");
        int anchoTexto = ancho - 2; // margen izq. de 2 espacios ("║  ")

        for (String linea : lineasConfig) {
            linea = linea.trim();
            if (linea.isEmpty() || linea.startsWith("===")) continue;

            int sepIdx = linea.indexOf(" = ");
            if (sepIdx < 0) continue;
            String clave = linea.substring(0, sepIdx).trim();
            String valor = linea.substring(sepIdx + 3).trim();

            // Omitimos rutas y logins: van en sus propias secciones más abajo
            if (clave.startsWith("ruta.") || clave.startsWith("ultimo.login.")) continue;

            String texto = clave + " = " + valor;
            // Si no cabe, cortamos a la fuerza (no debería pasar con ancho=100)
            while (texto.length() > anchoTexto) {
                System.out.println("║  " + texto.substring(0, anchoTexto) + "║");
                texto = texto.substring(anchoTexto);
            }
            int relleno = anchoTexto - texto.length();
            System.out.println("║  " + texto + " ".repeat(relleno) + "║");
        }

        // ── Sección de rutas ───────────────────────────────────────────────────────────────────────
        System.out.println("╠" + "═".repeat(ancho) + "╣");
        System.out.println(UI.centrarEnCaja("RUTAS", ancho));
        System.out.println("╠" + "═".repeat(ancho) + "╣");

        for (String linea : lineasConfig) {
            linea = linea.trim();
            if (linea.isEmpty() || linea.startsWith("===")) continue;

            int sepIdx = linea.indexOf(" = ");
            if (sepIdx < 0) continue;
            String clave = linea.substring(0, sepIdx).trim();
            String valor = linea.substring(sepIdx + 3).trim();

            if (!clave.startsWith("ruta.")) continue;

            String texto = clave + " = " + valor;
            while (texto.length() > anchoTexto) {
                System.out.println("║  " + texto.substring(0, anchoTexto) + "║");
                texto = texto.substring(anchoTexto);
            }
            int relleno = anchoTexto - texto.length();
            System.out.println("║  " + texto + " ".repeat(relleno) + "║");
        }

        // ── Sección de últimas conexiones ──────────────────────────────────────────────────────────
        System.out.println("╠" + "═".repeat(ancho) + "╣");
        System.out.println(UI.centrarEnCaja("ÚLTIMAS CONEXIONES DE USUARIOS", ancho));
        System.out.println("╠" + "═".repeat(ancho) + "╣");

        for (Usuario u : app.getUsuarios()) {
            String email = u.getCorreoElectronico();
            String ultimoLogin = AppConfig.getInstance().getUltimoLogin(email);
            String loginStr = ultimoLogin != null ? ultimoLogin : "Nunca ha iniciado sesión";
            String texto = email + "  →  " + loginStr;
            while (texto.length() > anchoTexto) {
                System.out.println("║  " + texto.substring(0, anchoTexto) + "║");
                texto = texto.substring(anchoTexto);
            }
            int relleno = anchoTexto - texto.length();
            System.out.println("║  " + texto + " ".repeat(relleno) + "║");
        }

        System.out.println("╚" + "═".repeat(ancho) + "╝");
        Utils.pulsaEnter();
    }
    /**
     * Opción de administrador: genera un CSV con todos los productos en venta
     * del sistema y lo envía al correo del administrador como adjunto.
     * .
     * Flujo:
     *   1. Obtiene la lista de todos los usuarios (con sus productos dentro)
     *   2. Llama a Communications.enviarCSVProductosAlAdmin() que:
     *       a. Genera el .csv en memoria con Apache POI
     *       b. Lo adjunta a un correo y lo envía al email del admin
     *   3. Muestra feedback al administrador en pantalla
     *-
     *      IMPORTANTE: Si el sistema tiene muchos productos, la generación puede tardar.
     *
     * @param app   instancia del Controller (para obtener todos los usuarios)
     * @param admin usuario administrador (para saber su correo destino)
     */
    private static void enviarListadoProductosPorCorreo(Controller app, Usuario admin) {
        Utils.limpiaPantalla();
        System.out.println("=== EXPORTAR PRODUCTOS POR CORREO ===");
        System.out.println("  Se generará un CSV con todos los productos en venta");
        System.out.println("  y se enviará a: " + admin.getCorreoElectronico());
        System.out.println();

        //Conteo rápido de productos totales para informar al admin
        int totalProductos = app.getAllProductos().size();
        if (totalProductos == 0) {
            System.out.println("  ⚠ No hay productos en venta en este momento. No se generará el CSV.");
            Utils.pulsaEnter();
            return;
        }

        System.out.println("  Generando CSV con " + totalProductos + " producto(s)...");

        // Delegación a Communications: genera el CSV y lo envía por correo
        boolean enviado = Communications.enviarCSVProductosAlAdmin(
                app.getUsuarios(),
                admin.getCorreoElectronico()
        );

        if (enviado) {
            System.out.println("  ✓ CSV enviado correctamente a " + admin.getCorreoElectronico() + ".");
        } else {
            System.out.println("  ✗ No se pudo enviar el CSV. Comprueba la conexión y los logs.");
        }

        Utils.pulsaEnter();
    }

    /**
     * Opción de administrador: Copia de seguridad de la base de datos.
     * .
     * Desde que FernanPop usa una base de datos SQL en la nube, esta opción
     * ofrece dos posibilidades:
     *   1. Guardar backup: vuelca TODOS los datos de la base de datos en
     *      un único fichero en disco, en la ruta que indique el admin.
     *   2. Recuperar backup: borra la base de datos completa y la
     *      restaura a partir de un fichero de backup existente.
     */
    private static void realizarCopiaSeguridad() {
        Utils.limpiaPantalla();
        System.out.println("=== COPIA DE SEGURIDAD DE LA BASE DE DATOS ===");
        System.out.println();
        System.out.println("  1. Guardar un backup en disco");
        System.out.println("  2. Recuperar un backup de disco");
        System.out.println("  0. Cancelar");
        System.out.println();
        int opcion = Utils.pideDatoEntero("una opción: ", 0, 2);

        switch (opcion) {
            case 1 -> guardarBackupEnDisco();
            case 2 -> recuperarBackupDeDisco();
            default -> System.out.println("Operación cancelada.");
        }
        Utils.pulsaEnter();
    }

    /**
     * Pide la ruta del fichero destino y vuelca en él todos los datos
     * actuales de la base de datos.
     */
    private static void guardarBackupEnDisco() {
        System.out.println();
        System.out.println("  Ejemplos de rutas válidas:");
        System.out.println("    Windows:  C:\\Users\\TuUsuario\\Desktop\\backup\\fernanpop.bak");
        System.out.println("    Linux/Mac: /home/tuusuario/backup/fernanpop.bak");
        System.out.println("    Relativa:  backup/fernanpop.bak  (se creará en la carpeta del programa)");
        System.out.println();
        System.out.print("  Introduce la ruta del fichero de backup a crear: ");
        String ruta = SCANNER.nextLine().trim();

        if (ruta.isEmpty()) {
            System.out.println("✗ Ruta vacía. Operación cancelada.");
            return;
        }

        boolean ok = Persistence.guardarBackup(ruta);
        if (ok) {
            System.out.println("✓ Copia de seguridad realizada correctamente en: " + ruta);
        } else {
            System.out.println("✗ No se pudo completar la copia de seguridad.");
        }
    }

    /**
     * Pide la ruta del fichero de backup a restaurar, confirma con el admin
     * (la operación borra toda la base de datos actual) y, si confirma,
     * delega en Persistence.restaurarBackup().
     */
    private static void recuperarBackupDeDisco() {
        System.out.println();
        System.out.println("  ⚠ ATENCIÓN: esta operación BORRARÁ TODOS los datos actuales");
        System.out.println("    de la base de datos y los sustituirá por los del backup.");
        System.out.println();
        System.out.print("  Introduce la ruta del fichero de backup a recuperar: ");
        String ruta = SCANNER.nextLine().trim();

        if (ruta.isEmpty()) {
            System.out.println("✗ Ruta vacía. Operación cancelada.");
            return;
        }

        System.out.print("  Escribe 'RESTAURAR' para confirmar: ");
        String confirmacion = SCANNER.nextLine().trim();
        if (!confirmacion.equals("RESTAURAR")) {
            System.out.println("Operación cancelada.");
            return;
        }

        boolean ok = Persistence.restaurarBackup(ruta);
        if (ok) {
            System.out.println("✓ Base de datos restaurada correctamente desde: " + ruta);
            System.out.println("  Reinicia la aplicación para recargar los datos restaurados.");
        } else {
            System.out.println("✗ No se pudo completar la restauración del backup.");
        }
    }

    //endregion

    //region MENÚ CON SESIÓN
    /**
     * Muestra el menú principal con sesión activa y gestiona la opción elegida.
     * .
     * Si el usuario activo es el administrador (email coincide con admin. Email del config),
     * se mostrará un menú extendido con opciones de administración.
     *
     * @param app instancia del Controller
     * @param u   usuario con sesión activa
     * @return el mismo usuario (para continuar la sesión) o null (para cerrar sesión)
     */
    private static Usuario menuConSesion(Controller app, Usuario u) {
        //Comprobación de si el usuario activo es el administrador
        boolean esAdmin = u.getCorreoElectronico().equalsIgnoreCase(
                AppConfig.getInstance().getAdminCorreoElectronico());

        if (esAdmin) {
            //Menú especial para el administrador (opciones extendidas)
            return menuAdmin(app, u);
        }

        //Menú normal para usuarios estándar
        UI.pintaMenuConSesion(u);
        int opcion = Utils.pideDatoEntero("una opción: ", 0, 9);

        switch (opcion) {
            case 1 -> { mostrarPerfil(u);                          return u; }
            case 2 -> { cambiarDatosPersonales(u);                 return u; }
            case 3 -> { menuMisProductos(app, u);                  return u; }
            case 4 -> { introducirProducto(app, u);                return u; }
            case 5 -> { menuBuscarProductos(app, u);               return u; }
            case 6 -> { menuValoracionesPendientes(app, u);        return u; }
            case 7 -> { verHistorialTratos(u);                     return u; }
            case 8 -> { return borrarPerfil(app, u); }            // puede devolver null
            case 9 -> {
                //Cierre de sesión: registro en el log y vuelta al menú principal
                cerrarSesion(u);
                return null;
            }
            case 0 -> {
                //Salir del programa completamente
                app.guardar();
                Utils.simularCierre("Saliendo de FernanPop");
                System.exit(0);
                return null;
            }
            default -> { return u; }
        }
    }

    //region 1 – MI PERFIL

    /**
     * Muestra los datos del perfil del usuario activo (nombre, email, valoración, etc.)
     * @param u usuario activo
     */
    private static void mostrarPerfil(Usuario u) {
        UI.pintaPerfil(u);
    }

    //endregion

    //region 2 – CAMBIAR DATOS PERSONALES

    /**
     * Submenú para editar los datos personales del usuario (nombre, apellidos,
     * contraseña y teléfono). Permanece activo hasta que el usuario elige "Volver".
     * Notifica cada cambio al admin por Telegram.
     *
     * @param u usuario activo cuyos datos se van a modificar
     */
    private static void cambiarDatosPersonales(Usuario u) {
        boolean volver = false;
        while (!volver) {
            UI.pintaMenuCambiarDatos(u);
            int op = Utils.pideDatoEntero("una opción: ", 0, 4);

            switch (op) {
                case 1 -> {
                    //Cambio de nombre
                    String antes = u.getNombre();
                    System.out.print("Nuevo nombre: ");
                    String n = SCANNER.nextLine().trim();
                    if (!n.isEmpty()) {
                        u.setNombre(n);
                        try { Communications.enviaMensajeTelegram(UI.msgCambioDatos(u, "Nombre", antes, n)); }
                        catch (Throwable ignored) {}
                    }
                    Utils.pulsaEnter();
                }
                case 2 -> {
                    //Cambio de apellidos
                    String antes = u.getApellidos();
                    System.out.print("Nuevos apellidos: ");
                    String a = SCANNER.nextLine().trim();
                    if (!a.isEmpty()) {
                        u.setApellidos(a);
                        try { Communications.enviaMensajeTelegram(UI.msgCambioDatos(u, "Apellidos", antes, a)); }
                        catch (Throwable ignored) {}
                    }
                    Utils.pulsaEnter();
                }
                case 3 -> {
                    //Cambio de contraseña
                    String antes = u.getContrasenia();
                    System.out.print("Nueva contraseña: ");
                    String c = SCANNER.nextLine().trim();
                    if (!c.isEmpty()) {
                        u.setContrasenia(c);
                        try { Communications.enviaMensajeTelegram(UI.msgCambioDatos(u, "Contraseña", antes, c)); }
                        catch (Throwable ignored) {}
                    }
                    Utils.pulsaEnter();
                }
                case 4 -> {
                    //Cambio de teléfono
                    String antes = String.valueOf(u.getTelefono());
                    int nuevoTelefono = Utils.pideDatoEntero("nuevo móvil (9 dígitos): ", 600000000, 799999999);
                    u.setTelefono(nuevoTelefono);
                    try { Communications.enviaMensajeTelegram(UI.msgCambioDatos(u, "Teléfono", antes, String.valueOf(nuevoTelefono))); }
                    catch (Throwable ignored) {}
                    Utils.pulsaEnter();
                }
                case 0 -> volver = true;
            }
        }
    }

    //endregion

    //region 3 – MIS PRODUCTOS
    /**
     * Submenú de gestión de los productos propios del usuario.
     * Permite ver, editar, eliminar productos y cerrar ventas.
     *
     * @param app instancia del Controller
     * @param u   usuario activo
     */
    private static void menuMisProductos(Controller app, Usuario u) {
        boolean volver = false;
        while (!volver) {
            UI.pintaMenuMisProductos();
            int op = Utils.pideDatoEntero("una opción: ", 0, 4);
            switch (op) {
                case 1 -> listarProductosUsuario(u);
                case 2 -> editarProducto(u);
                case 3 -> eliminarProducto(u);
                case 4 -> cerrarVenta(app, u);
                case 0 -> volver = true;
            }
        }
    }

    /**
     * Muestra todos los productos en venta del usuario, ordenados por precio.
     * Usa paginación definida en config.properties.
     *
     * @param u usuario activo
     */
    private static void listarProductosUsuario(Usuario u) {
        Utils.limpiaPantalla();
        System.out.println("=== MIS PRODUCTOS EN VENTA ===");
        ArrayList<Producto> productos = u.getEnVenta();

        if (productos == null || productos.isEmpty()) {
            System.out.println("No tienes productos en venta.");
        } else {
            //Orden de mayor de menor a mayor precio antes de paginar
            productos = ordenarPorPrecio(productos);
            mostrarConPaginacion(productos);
        }
        Utils.pulsaEnter();
    }

    /**
     * Permite al usuario editar el nombre, descripción y precio de un producto.
     * Si se modifica algo, notifica a los interesados por correo y resetea la lista de interesados.
     * También notifica al admin por Telegram.
     *
     * @param u usuario activo propietario del producto a editar
     */
    private static void editarProducto(Usuario u) {
        Utils.limpiaPantalla();
        ArrayList<Producto> productos = u.getEnVenta();

        if (productos == null || productos.isEmpty()) {
            System.out.println("No tienes productos para editar.");
            Utils.pulsaEnter();
            return;
        }

        System.out.println("=== EDITAR PRODUCTO ===");
        imprimirListaProductos(productos);
        System.out.print("Introduce el ID del producto a editar: ");
        String ID_Producto = SCANNER.nextLine().trim();
        Producto p = u.getProductoFromEnVenta(ID_Producto);

        if (p == null) {
            System.out.println("✗ No se encontró el producto.");
            Utils.pulsaEnter();
            return;
        }

        boolean modificado = false;
        System.out.println("(Deja en blanco para no modificar)");

        //Edición del título
        String antesTitulo = p.getNombre();
        System.out.print("Nuevo nombre [" + p.getNombre() + "]: ");
        String nombre = SCANNER.nextLine().trim();
        if (!nombre.isEmpty()) {
            p.setNombre(nombre);
            modificado = true;
            try { Communications.enviaMensajeTelegram(UI.msgProductoModificado(u, p, "Título", antesTitulo, nombre)); }
            catch (Throwable ignored) {}
        }

        //Edición de la descripción
        String antesDesc = p.getDescripcion();
        System.out.print("Nueva descripción [" + p.getDescripcion() + "]: ");
        String desc = SCANNER.nextLine().trim();
        if (!desc.isEmpty()) {
            p.setDescripcion(desc);
            modificado = true;
            try { Communications.enviaMensajeTelegram(UI.msgProductoModificado(u, p, "Descripción", antesDesc, desc)); }
            catch (Throwable ignored) {}
        }

        //Edición del precio
        System.out.print("¿Cambiar precio? (s/n): ");
        if (SCANNER.nextLine().trim().equalsIgnoreCase("s")) {
            String antesPrecio = p.getPrecio() + " €";
            double precio = Utils.redondearDosDecimales(Utils.pideDatoDouble("el nuevo precio (€): "));
            p.setPrecio(precio);
            modificado = true;
            try { Communications.enviaMensajeTelegram(UI.msgProductoModificado(u, p, "Precio", antesPrecio, precio + " €")); }
            catch (Throwable ignored) {}
        }

        //Si hubo cambios, notificación a los interesados y reseteamos la lista
        if (modificado) {
            ArrayList<String> antiguosInteresados = new ArrayList<>(p.getInteresados());

            if (!antiguosInteresados.isEmpty()) {
                //Limpieza de la lista de interesados del producto (ya que el producto ha cambiado)
                p.getInteresados().clear();

                System.out.println("\n↻ Informando a los interesados de los cambios...");
                for (String emailInteresado : antiguosInteresados) {
                    try {
                        String asunto = "Modificación en: " + p.getNombre();
                        String mensaje = generarEmailModificacion(p.getNombre(), u);
                        Communications.enviarConGMail(emailInteresado, asunto, mensaje);
                    } catch (Throwable ignored) {
                        //Si falla el envío de un correo concreto, seguimos con el siguiente
                    }
                }
                System.out.println("✓ Lista de interesados reseteada y correos enviados.");
            }
            System.out.println("✓ Producto actualizado.");
        } else {
            System.out.println("No se realizaron cambios.");
        }

        Utils.pulsaEnter();
    }

    /** Genera el HTML del email que avisa a un interesado de que el producto fue modificado */
    private static String generarEmailModificacion(String tituloProducto, Usuario u) {
        return UI.generaEmailModificacion(tituloProducto, u);
    }

    /**
     * Permite al usuario eliminar uno de sus productos en venta.
     * Notifica al admin por Telegram.
     *
     * @param u usuario activo
     */
    private static void eliminarProducto(Usuario u) {
        Utils.limpiaPantalla();
        ArrayList<Producto> productos = u.getEnVenta();

        if (productos == null || productos.isEmpty()) {
            System.out.println("No tienes productos para eliminar.");
            Utils.pulsaEnter();
            return;
        }

        System.out.println("=== ELIMINAR PRODUCTO ===");
        imprimirListaProductos(productos);
        System.out.print("Introduce el ID del producto a eliminar: ");

        try {
            String ID_Producto = SCANNER.nextLine().trim();
            Producto p = u.getProductoFromEnVenta(ID_Producto); //Guardado de ref antes de borrar

            if (p != null && u.deleteProducto(ID_Producto)) {
                System.out.println("✓ Producto eliminado.");
                try { Communications.enviaMensajeTelegram(UI.msgProductoEliminado(u, ID_Producto, p.getNombre())); }
                catch (Throwable ignored) {}
            } else {
                System.out.println("✗ No se encontró el producto.");
            }
        } catch (NumberFormatException e) {
            System.out.println("✗ ID no válido.");
        }
        Utils.pulsaEnter();
    }

    /**
     * Cierra una venta: el vendedor elige un comprador de los interesados y
     * se registra la transacción.
     *-
     * Flujo:
     *   1. El vendedor elige el producto vendido
     *   2. Elige al comprador de la lista de interesados
     *   3. Se crea UN único Trato (Controller.cerrarVenta) que se añade
     *      a las ventas del vendedor Y a las compras del comprador
     *   4. Ambos reciben valoración pendiente
     *   5. El producto se elimina de "en venta"
     *   6. Se envían correos de confirmación a ambos y notificación Telegram
     *   7. Se registra en el log
     *
     * @param app instancia del Controller
     * @param u   usuario vendedor
     */
    private static void cerrarVenta(Controller app, Usuario u) {
        Utils.limpiaPantalla();
        System.out.println("=== CERRAR UNA VENTA ===");
        ArrayList<Producto> productos = u.getEnVenta();

        if (productos == null || productos.isEmpty()) {
            System.out.println("No tienes productos en venta.");
            Utils.pulsaEnter();
            return;
        }

        // Filtrar solo los productos que tienen al menos un interesado
        ArrayList<Producto> conInteresados = new ArrayList<>();
        for (Producto p : productos) {
            if (p.getInteresados() != null && !p.getInteresados().isEmpty()) {
                conInteresados.add(p);
            }
        }

        if (conInteresados.isEmpty()) {
            System.out.println("⚠ Ninguno de tus productos tiene compradores interesados todavía.");
            Utils.pulsaEnter();
            return;
        }

        // Mostrar lista de productos con interesados en formato caja
        UI.pintaListaProductosConInteresados(conInteresados);
        System.out.println();
        System.out.println("  [0] Cancelar y volver");
        System.out.println();

        int selProd = Utils.pideDatoEntero("el número del producto vendido (0 para cancelar): ", 0, conInteresados.size());
        if (selProd == 0) {
            System.out.println("Operación cancelada.");
            Utils.pulsaEnter();
            return;
        }

        Producto p = conInteresados.get(selProd - 1);

        // Mostrar lista de interesados del producto seleccionado en formato caja
        ArrayList<String> interesados = p.getInteresados();
        System.out.println();
        UI.pintaListaInteresados(interesados);
        System.out.println();
        System.out.println("  [0] Cancelar y volver");
        System.out.println();

        int selComp = Utils.pideDatoEntero("el número del comprador (0 para cancelar): ", 0, interesados.size());
        if (selComp == 0) {
            System.out.println("Operación cancelada.");
            Utils.pulsaEnter();
            return;
        }

        String emailComprador = interesados.get(selComp - 1);
        Usuario comprador = app.buscaPorCorreoElectronico(emailComprador);

        if (comprador == null) {
            System.out.println("✗ Error: El comprador ya no existe en el sistema.");
            Utils.pulsaEnter();
            return;
        }

        double precioFinal = p.getPrecio();

        // CREACIÓN DEL TRATO
        Trato trato = app.cerrarVenta(u, p.getID(), comprador, precioFinal);

        if (trato == null) {
            System.out.println("✗ Error al registrar la venta.");
            Utils.pulsaEnter();
            return;
        }

        // Guardado en disco
        app.guardarUnico(comprador);
        app.guardarUnico(u);

        // Registro en el log
        Log.registrarVentaCerrada(u.getCorreoElectronico(), comprador.getCorreoElectronico());

        System.out.println("\nEnviando recibos de confirmación con PDF adjunto...");
        try {
            Communications.enviarReciboPDFTrato(trato, u, comprador);
            System.out.println("✓ Recibos enviados a ambas partes.");
        } catch (Throwable e) {
            System.out.println("⚠ No se pudieron enviar los recibos por correo. El trato sí quedó registrado.");
        }

        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
            String fechaFormateada = sdf.format(Calendar.getInstance().getTime());
            Communications.enviaMensajeTelegram(UI.msgVentaRealizada(comprador, u, p, fechaFormateada));
        } catch (Throwable ignored) {}

        System.out.printf("%n✓ Venta cerrada (ID Trato: %s) con %s por %.2f€.%n",
                trato.getID(), comprador.getNombre(), precioFinal);

        Utils.pulsaEnter();
    }

    /** Genera el HTML del email de confirmación de venta para el vendedor */
    private static String generarEmailVenta(String nombreVendedor, String titulo, double precio,
                                            String nombreComprador, String emailComprador) {
        return UI.generarEmailVenta(nombreVendedor, titulo, String.valueOf(precio), nombreComprador, emailComprador);
    }

    /** Genera el HTML del email de confirmación de compra para el comprador */
    private static String generarEmailCompra(String nombreComprador, String titulo, double precio,
                                             String nombreVendedor, String emailVendedor) {
        return UI.generarEmailCompra(nombreComprador, titulo, String.valueOf(precio), nombreVendedor, emailVendedor);
    }

    //endregion

    //region 4 – INTRODUCIR UN PRODUCTO

    /**
     * Formulario para publicar un nuevo producto en venta.
     * Solicita nombre, descripción, precio y estado, luego lo añade al usuario.
     * Notifica al admin por Telegram y registra el evento en el log.
     *
     * @param app instancia del Controller
     * @param u   usuario vendedor
     */
    private static void introducirProducto(Controller app, Usuario u) {
        Utils.limpiaPantalla();
        System.out.println("=== PONER PRODUCTO EN VENTA ===");

        //Solicitud de los datos del producto
        System.out.print("Nombre del producto: ");
        String nombre = SCANNER.nextLine().trim();
        if (nombre.isEmpty()) {
            System.out.println("✗ El nombre no puede estar vacío.");
            Utils.pulsaEnter();
            return;
        }

        System.out.print("Descripción: ");
        String descripcion = SCANNER.nextLine().trim();
        double precio = Utils.redondearDosDecimales(Utils.pideDatoDouble("el precio (€): "));
        System.out.print("Estado: ");
        String estado = SCANNER.nextLine().trim();

        //Generación de la ID del producto y su creación
        String ID_Producto = app.generaID_Producto();
        Producto nuevo = new Producto(ID_Producto, nombre, descripcion, precio, estado);

        if (app.addProducto(u, nuevo)) {
            System.out.println("\n✓ Producto añadido correctamente.");

            //Guardado en disco
            app.guardarUnico(u);

            //Registro en el log
            Log.registrarNuevoProducto(ID_Producto, u.getCorreoElectronico());

            //Notificación por Telegram
            try { Communications.enviaMensajeTelegram(UI.msgNuevoProducto(u, nuevo)); }
            catch (Throwable ignored) {}
        } else {
            System.out.println("\n✗ No se pudo añadir el producto.");
        }
        Utils.pulsaEnter();
    }

    //endregion

    //region 5 – BUSCAR PRODUCTOS

    /**
     * Submenú de búsqueda y consulta de productos.
     * Si se llama sin usuario activo (u == null), la opción de comprar estará bloqueada
     * (acceso de invitado: solo consulta).
     *
     * @param app instancia del Controller
     * @param u   usuario activo, o null si es un invitado sin sesión
     */
    private static void menuBuscarProductos(Controller app, Usuario u) {
        boolean volver = false;
        while (!volver) {
            UI.pintaMenuBuscarProductos();
            int op = Utils.pideDatoEntero("una opción: ", 0, 4);

            switch (op) {
                case 1 -> mostrarTodosLosProductos(app);
                case 2 -> buscarPorTexto(app);
                case 3 -> verDetalleProducto(app);
                case 4 -> {
                    //La compra requiere sesión iniciada
                    if (u == null) {
                        System.out.println("✗ Debes iniciar sesión para comprar productos.");
                        Utils.pulsaEnter();
                    } else {
                        solicitarCompra(app, u);
                    }
                }
                case 0 -> volver = true;
            }
        }
    }

    /**
     * Muestra todos los productos en venta con paginación.
     * El tamaño de página se lee de config.properties.
     *
     * @param app instancia del Controller
     */
    private static void mostrarTodosLosProductos(Controller app) {
        Utils.limpiaPantalla();
        System.out.println("=== TODOS LOS PRODUCTOS EN VENTA ===");
        ArrayList<Producto> todos = app.getAllProductos();

        if (todos == null || todos.isEmpty()) {
            System.out.println("No hay productos disponibles en este momento.");
            Utils.pulsaEnter();
        } else {
            //Orden por precio antes de paginar
            todos = ordenarPorPrecio(todos);
            mostrarConPaginacion(todos);
        }
    }

    /**
     * Busca productos por texto (en nombre Y descripción) y muestra los resultados paginados.
     *
     * @param app instancia del Controller
     */
    private static void buscarPorTexto(Controller app) {
        Utils.limpiaPantalla();
        System.out.print("Introduce el texto a buscar (¡cuidado con las tildes!): ");
        String texto = SCANNER.nextLine().trim();

        // Delegamos en Controller, que busca en nombre Y descripción
        ArrayList<Producto> resultado = app.buscaProductosTexto(texto);

        System.out.println("\n=== RESULTADOS PARA \"" + texto + "\" ===");
        if (resultado.isEmpty()) {
            System.out.println("No se encontraron productos con ese texto.");
            Utils.pulsaEnter();
        } else {
            resultado = ordenarPorPrecio(resultado);
            mostrarConPaginacion(resultado);
        }
    }

    /**
     * Muestra el detalle completo de un producto buscado por ID,
     * incluyendo los datos del vendedor.
     *
     * @param app instancia del Controller
     */
    private static void verDetalleProducto(Controller app) {
        Utils.limpiaPantalla();
        System.out.print("Introduce el ID del producto: ");
        try {
            String ID_Producto = SCANNER.nextLine().trim();
            Producto p = app.buscaProductoId(ID_Producto);
            if (p != null) {
                imprimirDetalleProducto(p, app);
            } else {
                System.out.println("✗ Producto no encontrado.");
            }
        } catch (NumberFormatException e) {
            System.out.println("✗ ID no válido.");
        }
        Utils.pulsaEnter();
    }

    /**
     * Registra el interés de un usuario en comprar un producto.
     * Notifica al vendedor por correo con los datos del comprador.
     * El comprador se añade a la lista de interesados del producto.
     *
     * @param app instancia del Controller
     * @param u   usuario comprador
     */
    private static void solicitarCompra(Controller app, Usuario u) {
        Utils.limpiaPantalla();
        System.out.println("=== COMPRAR UN PRODUCTO ===");
        System.out.print("Introduce el ID del producto que deseas comprar: ");

        try {
            String ID_Producto = SCANNER.nextLine().trim();
            Producto p = app.buscaProductoId(ID_Producto);

            if (p == null) {
                System.out.println("✗ Producto no encontrado.");
                Utils.pulsaEnter();
                return;
            }

            //Localizar al vendedor
            Usuario vendedor = app.buscaPropietarioProducto(ID_Producto);
            if (vendedor == null) {
                System.out.println("✗ Error al localizar al vendedor.");
                Utils.pulsaEnter();
                return;
            }

            //Un usuario no puede comprar su propio producto
            if (vendedor.getCorreoElectronico().equals(u.getCorreoElectronico())) {
                System.out.println("✗ No puedes comprar tu propio producto.");
                Utils.pulsaEnter();
                return;
            }

            //Añadir al comprador a la lista de interesados del producto
            p.addInteresado(u.getCorreoElectronico());

            //Envío de notificación al vendedor por correo
            System.out.println("\nEnviando notificación al vendedor...");
            try {
                String asunto = "¡Alguien está interesado en tu producto!";
                String mensaje = generarEmailInteresado(vendedor.getNombre(), p.getNombre(),
                        u.getNombre(), u.getApellidos(), u.getCorreoElectronico());
                Communications.enviarConGMail(vendedor.getCorreoElectronico(), asunto, mensaje);
                System.out.println("✓ Notificación enviada al correo del vendedor.");
            } catch (Throwable e) {
                System.out.println("⚠ No se pudo enviar el correo al vendedor, pero la solicitud quedó registrada.");
            }

            System.out.println("\n✓ ¡Intención de compra registrada!");
            System.out.println("Le hemos avisado al vendedor. Cuando acepte el trato, podrás valorarlo.");

        } catch (NumberFormatException e) {
            System.out.println("✗ ID no válido.");
        }
        Utils.pulsaEnter();
    }

    /** Genera el HTML del email que se envía al vendedor cuando alguien se interesa en su producto */
    private static String generarEmailInteresado(String nombreVendedor, String tituloProducto,
                                                 String nombreComprador, String apellidosComprador,
                                                 String emailComprador) {
        return UI.generarEmailInteresado(nombreVendedor, tituloProducto, nombreComprador, apellidosComprador, emailComprador);
    }

    //endregion

    //region 6 – VALORACIONES PENDIENTES

    /**
     * Submenú de valoraciones pendientes.
     * Muestra la lista de tratos sin valorar (ordenados por antigüedad)
     * y permite valorar uno de ellos con una puntuación de 1 a 5.
     *
     * @param app instancia del Controller
     * @param u   usuario activo
     */
    private static void menuValoracionesPendientes(Controller app, Usuario u) {
        boolean volver = false;
        while (!volver) {
            Utils.limpiaPantalla();
            System.out.println("=== VALORACIONES PENDIENTES ===\n");

            ArrayList<Trato> pendientes = app.getValoracionesPendientes(u);

            if (pendientes.isEmpty()) {
                System.out.println("No tienes valoraciones pendientes. ¡Todo al día!");
                Utils.pulsaEnter();
                return;
            }

            // Mostrar la lista en formato caja antes del menú de opciones
            UI.pintaListaValoracionesPendientes(pendientes);
            System.out.println();

            UI.pintaMenuValoraciones();
            int op = Utils.pideDatoEntero("una opción: ", 0, 1);
            if (op == 1) {
                valorarTrato(app, u, pendientes);
            } else {
                volver = true;
            }
        }
    }

    /**
     * Solicita al usuario que elija un trato de los pendientes y le asigne una puntuación.
     * Elimina la valoración pendiente una vez puntuada y guarda los cambios.
     *
     * @param app       instancia del Controller
     * @param u         usuario que valora
     * @param pendientes lista de tratos pendientes de valorar
     */
    private static void valorarTrato(Controller app, Usuario u, ArrayList<Trato> pendientes) {
        //Selección por posición usando pideDatoEntero (robusto, con rango validado)
        int num = Utils.pideDatoEntero(
                "el número del trato a valorar: ", 1, pendientes.size()) - 1;

        Trato t = pendientes.get(num);

        //Mostrar escala de valoración para orientar al usuario
        System.out.println();
        System.out.println("  Escala de valoración:");
        System.out.println("  0 – Horrible        1 – Muy malo       2 – Malo");
        System.out.println("  3 – Regular         4 – Bueno          5 – Excelente");
        System.out.println();

        int estrellas = Utils.pideDatoEntero("tu valoración (0-5 estrellas): ", 0, 5);

        //Guardamos la puntuación en el trato (objeto compartido: el vendedor lo verá automáticamente)
        t.setPuntuacion(estrellas);
        app.borraValoracionPendiente(u, t.getID());

        //Persistir comprador (tiene la lista de valoraciones pendientes actualizada)
        app.guardarUnico(u);

        //Persistir también al vendedor para que su historial refleje la puntuación recibida
        Usuario vendedor = app.buscaPorCorreoElectronico(t.getCorreoVendedor());
        if (vendedor != null) {
            app.guardarUnico(vendedor);
        }

        String msg = estrellas == 0 ? "Valoración horrible registrada." : "¡Gracias por tu valoración de " + estrellas + " estrella(s)!";
        System.out.println("✓ " + msg);
        Utils.pulsaEnter();
    }

    //endregion

    //region 7 – HISTORIAL DE TRATOS

    /**
     * Muestra el historial completo de ventas y compras del usuario.
     * Para cada trato muestra ID, producto, precio y si fue valorado.
     *
     * @param u usuario activo
     */
    private static void verHistorialTratos(Usuario u) {
        Utils.limpiaPantalla();
        int ancho = 58;

        System.out.println("╔" + "═".repeat(ancho) + "╗");
        System.out.println(UI.centrarEnCaja("MI HISTORIAL DE TRATOS", ancho));
        System.out.println("╠" + "═".repeat(ancho) + "╣");
        System.out.println(UI.centrarEnCaja("── VENTAS ──", ancho));
        System.out.println("╠" + "═".repeat(ancho) + "╣");

        if (u.getVentas() == null || u.getVentas().isEmpty()) {
            System.out.println(UI.centrarEnCaja("Aún no has realizado ninguna venta.", ancho));
        } else {
            for (Trato t : u.getVentas()) {
                imprimirResumenTrato(t, ancho);
            }
        }

        System.out.println("╠" + "═".repeat(ancho) + "╣");
        System.out.println(UI.centrarEnCaja("── COMPRAS ──", ancho));
        System.out.println("╠" + "═".repeat(ancho) + "╣");

        if (u.getCompras() == null || u.getCompras().isEmpty()) {
            System.out.println(UI.centrarEnCaja("Aún no has realizado ninguna compra.", ancho));
        } else {
            for (Trato t : u.getCompras()) {
                imprimirResumenTrato(t, ancho);
            }
        }

        System.out.println("╚" + "═".repeat(ancho) + "╝");
        Utils.pulsaEnter();
    }

    //endregion

    //region 8 – BORRAR PERFIL

    /**
     * Proceso de eliminación de cuenta con doble confirmación por seguridad:
     *   1. El usuario escribe "BORRAR" para confirmar su intención
     *   2. Se envía un código de seguridad al correo del usuario
     *   3. El usuario debe introducir el código en menos de 10 minutos
     *   4. Si todo es correcto, se elimina la cuenta
     *
     * @param app instancia del Controller
     * @param u   usuario que quiere eliminar su cuenta
     * @return null si la cuenta fue eliminada; el mismo usuario si la operación fue cancelada
     */
    private static Usuario borrarPerfil(Controller app, Usuario u) {
        Utils.limpiaPantalla();
        System.out.println("=== BORRAR MI PERFIL ===");
        System.out.println("⚠ ATENCIÓN: Esta acción es irreversible.");
        System.out.print("¿Estás seguro de que quieres eliminar tu cuenta? (escribe 'BORRAR' para confirmar): ");
        String confirmacion = SCANNER.nextLine().trim();

        if (!confirmacion.equals("BORRAR")) {
            System.out.println("Operación cancelada.");
            Utils.pulsaEnter();
            return u;
        }

        //El usuario confirmó su intención: envío de código por seguridad
        String codigoSeguridad = Utils.generarClave(6);
        System.out.println("\nPor seguridad, enviando un código de confirmación a tu correo...");
        LocalDateTime inicioTemporizador = LocalDateTime.now(); //Se inicia un temporizador de 10 min

        try {
            String asunto = "Código de seguridad para eliminar cuenta - FernanPop";
            String mensaje = generarEmailEliminarCuenta(u.getNombre(), codigoSeguridad);
            Communications.enviarConGMail(u.getCorreoElectronico(), asunto, mensaje);

            System.out.println("✓ Código enviado. Por favor, revísalo.");
            System.out.print("Introduce el código recibido: ");
            String codigoIntroducido = SCANNER.nextLine().trim();

            //Verificación de que el código sea correcto y que no hayan pasado más de 10 minutos
            boolean codigoCorrecto = codigoIntroducido.equalsIgnoreCase(codigoSeguridad);
            boolean dentroDelTiempo = inicioTemporizador.plusMinutes(10).isAfter(LocalDateTime.now());

            if (codigoCorrecto && dentroDelTiempo) {
                //Correcto: eliminamos el usuario
                if (app.deleteUsuario(u)) { //Se guardan los cambios en disco también con esta función
                    System.out.println("✓ Tu perfil ha sido eliminado. Hasta pronto.");

                    //Notificamos al admin por Telegram
                    try { Communications.enviaMensajeTelegram(UI.msgUsuarioEliminado(u)); }
                    catch (Throwable ignored) {}

                    Utils.esperar(1500);
                    return null; //null = cerrar sesión (la cuenta ya no existe)

                } else {
                    System.out.println("\n✗ No se pudo eliminar el perfil en el sistema.");
                    Utils.pulsaEnter();
                    return u;
                }

            } else if (!codigoCorrecto) {
                System.out.println("\n✗ Código incorrecto. Operación de borrado cancelada.");
                Utils.pulsaEnter();
                return u;

            } else {
                System.out.println("\n✗ El código ha expirado (más de 10 minutos). Operación cancelada.");
                Utils.pulsaEnter();
                return u;
            }

        } catch (Throwable e) {
            System.out.println("\n✗ Error al enviar el código por correo. No podemos validar tu identidad.");
            Utils.pulsaEnter();
            return u;
        }
    }

    /** Genera el HTML del email con el código de seguridad para eliminar la cuenta */
    private static String generarEmailEliminarCuenta(String nombre, String codigoSeguridad) {
        return UI.generarEmailEliminarCuenta(nombre, codigoSeguridad);
    }

    //endregion

    //region 9 – CERRAR SESIÓN

    /**
     * Cierra la sesión del usuario activo.
     * Muestra animación de cierre, registra el evento en el log.
     *
     * @param u usuario que cierra sesión
     */
    private static void cerrarSesion(Usuario u) {
        //Registro del cierre de sesión en el log
        Log.registrarCierreSesion(u.getCorreoElectronico());
        Utils.simularCierre("Cerrando sesión de " + u.getNombre());
        System.out.println();
        Utils.esperar(500);
    }

    //endregion

    //region MÉTODOS AUXILIARES DE VISUALIZACIÓN

    /**
     * Muestra una lista de productos en formato tabla con paginación.
     * El tamaño de cada página se obtiene de config.properties (pagina.tamano).
     * El usuario puede navegar entre páginas con ENTER (siguiente) o escribir 'v' para volver.
     *
     * @param lista lista de productos a mostrar (debe estar ya ordenada)
     */
    private static void mostrarConPaginacion(ArrayList<Producto> lista) {
        int tamPagina = AppConfig.getInstance().getPaginaTamano();
        int total = lista.size();
        int paginaActual = 0;
        int totalPaginas = (int) Math.ceil((double) total / tamPagina);

        boolean seguir = true;
        while (seguir) {
            //Se calculan los índices de inicio y fin de la página actual
            int inicio = paginaActual * tamPagina;
            int fin = Math.min(inicio + tamPagina, total);

            //Se imprime la cabecera con el número de página
            System.out.printf("%n  Página %d de %d (%d producto(s) en total)%n%n",
                    paginaActual + 1, totalPaginas, total);

            //Se imprimen los productos de esta página
            imprimirListaProductos(new ArrayList<>(lista.subList(inicio, fin)));

            //Se muestran las opciones de navegación disponibles
            System.out.println();
            if (paginaActual < totalPaginas - 1) {
                System.out.print("  [ENTER] Siguiente página  ");
            }
            if (paginaActual > 0) {
                System.out.print("  [a] Página anterior  ");
            }
            System.out.print("  [0] Volver  → ");

            String input = SCANNER.nextLine().trim().toLowerCase();

            if (input.equals("0")) {
                //El usuario quiere volver al menú anterior
                seguir = false;
            } else if (input.equals("a") && paginaActual > 0) {
                //Retroceder una página
                paginaActual--;
            } else if (input.isEmpty() && paginaActual < totalPaginas - 1) {
                //Avanzar a la siguiente página (ENTER)
                paginaActual++;
            } else if (paginaActual == totalPaginas - 1) {
                //Estamos en la última página, cualquier tecla que no sea 'a' vuelve
                seguir = false;
            }
        }
    }

    /**
     * Imprime una lista de productos en formato caja.
     * Muestra ID, nombre y precio de cada producto.
     *
     * @param lista lista de productos a imprimir
     */
    private static void imprimirListaProductos(ArrayList<Producto> lista) {
        UI.pintaListaProductosCaja("PRODUCTOS", lista);
    }

    /**
     * Imprime el detalle completo de un producto, incluyendo los datos del vendedor.
     *
     * @param p   producto a mostrar
     * @param app instancia del Controller (para buscar al vendedor)
     */
    private static void imprimirDetalleProducto(Producto p, Controller app) {
        int ancho = 54;
        // Margen izquierdo para el contenido dentro de la caja
        int margen = 2;
        int anchoContenido = ancho - margen; // espacio disponible para texto

        System.out.println("╔" + "═".repeat(ancho) + "╗");
        System.out.println(UI.centrarEnCaja("DETALLE DEL PRODUCTO", ancho));
        System.out.println("╠" + "═".repeat(ancho) + "╣");
        System.out.println(UI.filaEnCaja("Nombre:",  p.getNombre(),  ancho));
        System.out.println(UI.filaEnCaja("Precio:",  String.format("%.2f€", p.getPrecio()), ancho));
        System.out.println(UI.filaEnCaja("Estado:",  p.getEstado(),  ancho));
        System.out.println(UI.filaEnCaja("ID:",      p.getID(),      ancho));

        // Descripción: word-wrap por palabras completas, margen izquierdo fijo
        System.out.println("╠" + "═".repeat(ancho) + "╣");
        String desc = p.getDescripcion();
        String margenDesc = "  "; // 2 espacios de margen izquierdo en todas las líneas
        int anchoTextoDesc = ancho - margenDesc.length();
        String[] palabrasDesc = desc.split(" ");
        StringBuilder lineaActualDesc = new StringBuilder();
        for (String palabra : palabrasDesc) {
            if (lineaActualDesc.length() == 0) {
                // Si una sola palabra supera el ancho, forzamos corte
                while (palabra.length() > anchoTextoDesc) {
                    String trozo = palabra.substring(0, anchoTextoDesc);
                    int relleno = anchoTextoDesc - trozo.length();
                    System.out.println("║" + margenDesc + trozo + " ".repeat(Math.max(0, relleno)) + "║");
                    palabra = palabra.substring(anchoTextoDesc);
                }
                lineaActualDesc.append(palabra);
            } else if (lineaActualDesc.length() + 1 + palabra.length() <= anchoTextoDesc) {
                lineaActualDesc.append(" ").append(palabra);
            } else {
                // No cabe: volcamos la línea actual
                String texto = lineaActualDesc.toString();
                int relleno = anchoTextoDesc - texto.length();
                System.out.println("║" + margenDesc + texto + " ".repeat(Math.max(0, relleno)) + "║");
                lineaActualDesc = new StringBuilder(palabra);
            }
        }
        if (lineaActualDesc.length() > 0) {
            String texto = lineaActualDesc.toString();
            int relleno = anchoTextoDesc - texto.length();
            System.out.println("║" + margenDesc + texto + " ".repeat(Math.max(0, relleno)) + "║");
        }

        Usuario vendedor = app.buscaPropietarioProducto(p.getID());
        if (vendedor != null) {
            System.out.println("╠" + "═".repeat(ancho) + "╣");
            System.out.println(UI.filaEnCaja("Vendedor:", vendedor.getNombre() + " " + vendedor.getApellidos(), ancho));
            System.out.println(UI.filaEnCaja("Contacto:", vendedor.getCorreoElectronico() + " | " + vendedor.getTelefono(), ancho));
        }
        System.out.println("╚" + "═".repeat(ancho) + "╝");
    }

    private static void imprimirResumenTrato(Trato t, int ancho) {
        String valoracion = t.getPuntuacion() >= 0 ? t.getPuntuacion() + "★" : "Pendiente";
        String linea = String.format("  %s | %-20s | %.2f€ | %s",
                t.getID(), t.getProducto().getNombre(), t.getPrecio(), valoracion);
        if (linea.length() > ancho) linea = linea.substring(0, ancho - 1) + "…";
        int relleno = ancho - linea.length();
        System.out.println("║" + linea + " ".repeat(relleno) + "║");
    }

    /**
     * Ordena una lista de productos de menor a mayor precio usando burbuja.
     * Opera sobre una COPIA de la lista para no modificar la original.
     *
     * @param lista lista de productos a ordenar
     * @return nueva lista ordenada por precio ascendente
     */
    private static ArrayList<Producto> ordenarPorPrecio(ArrayList<Producto> lista) {
        ArrayList<Producto> copia = new ArrayList<>(lista);
        int n = copia.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - 1 - i; j++) {
                if (copia.get(j).getPrecio() > copia.get(j + 1).getPrecio()) {
                    Producto temp = copia.get(j);
                    copia.set(j, copia.get(j + 1));
                    copia.set(j + 1, temp);
                }
            }
        }
        return copia;
    }

    //endregion

    //region DATOS DE PRUEBA

    /**
     * Comprueba si el usuario administrador definido en config.properties
     * ya existe en el sistema. Si no existe, lo crea y lo guarda en disco.
     * .
     * Esto resuelve dos problemas:
     *   1. Primera ejecución: no hay ningún usuario, el admin no podría hacer login.
     *   2. El .bin del admin fue borrado por error: se regenera automáticamente.
     * .
     * El admin se crea con:
     *   - Correo y contraseña leídos de config.properties (admin.correoElectronico / admin.contrasenia)
     *   - Nombre fijo "Administrador" y apellido "Sistema" (no se muestra al exterior)
     *   - Teléfono ficticio 600000000 (el admin no necesita uno real)
     *   - ID generada por el Controller igual que cualquier otro usuario
     *
     * @param app instancia del Controller ya inicializada con los usuarios cargados
     */
    private static void garantizarUsuarioAdmin(Controller app) {
        AppConfig cfg = AppConfig.getInstance();
        String correoAdmin = cfg.getAdminCorreoElectronico();
        String contraseniaAdmin = cfg.getAdminContrasenia();

        //Si en config.properties no hay correo o contraseña de admin, no se crea ningún administrador
        if (correoAdmin == null || correoAdmin.trim().isEmpty()
                || contraseniaAdmin == null || contraseniaAdmin.isEmpty()) {
            System.out.println("[Sistema] Falta admin.correoElectronico / admin.contrasenia en config.properties.");
            return;
        }

        //Comprobación de si ya existe un usuario con ese correo en el sistema
        if (app.buscaPorCorreoElectronico(correoAdmin) != null) {
            return; //Ya existe, no hay nada que hacer
        }

        //No existe: se crea con los datos del config
        String idAdmin = app.generaID_Usuario();
        Usuario admin  = new Usuario(idAdmin, "Administrador", "Sistema", correoAdmin, contraseniaAdmin, 600000000);

        app.addUsuario(admin);
        app.guardarUnico(admin); //Se guarda en disco para que no se repita en el próximo arranque

        System.out.println("[Sistema] Usuario administrador creado automáticamente (" + correoAdmin + ").");
    }

    //endregion

    //endregion
}