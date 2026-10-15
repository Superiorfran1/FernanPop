package DAO;

import persistence.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DAOManager – Gestiona la conexión a la base de datos en la nube y
 * garantiza que el esquema de tablas necesario existe.
 *
 * Sigue el patrón Singleton: solo existe una instancia y toda la app
 * comparte la misma conexión.
 *
 * Los datos de conexión (URL, usuario, contraseña) ya NO están escritos
 * a fuego en el código: se leen de config.properties a través de AppConfig,
 * así el mismo .jar funciona en cualquier equipo sin recompilar, solo
 * cambiando el config.properties (o dejando que cada admin meta sus credenciales
 * la primera vez que arranca el programa).
 */
public class DAOManager {

    private Connection conn;
    private static DAOManager singlenton; //Atributo estatico que guarda una referencia al DAO

    private DAOManager() { //Constructor privado para que no se pueda llamar las veces que se quiera
        this.conn = null;
    }

    public static DAOManager getSinglentonInstance() { //Metodo que devuelve el DAO, si el atributo estatico ya ha inicializado no devuelve nada
        if (singlenton == null) singlenton = new DAOManager();
        return singlenton;
    }

    public Connection getConn() {
        return conn;
    }

    /**
     * Abre la conexión con la base de datos en la nube usando los datos
     * de config.properties, y se asegura de que las tablas necesarias existan.
     * Si ya hay una conexión abierta y válida, no hace nada (evita reconectar
     * en cada operación).
     */
    public void open() throws Exception {
        try {
            if (conn != null && !conn.isClosed()) return; //Ya conectados, no hace falta nada más

            AppConfig cfg = AppConfig.getInstance();

            System.out.println("[DAOManager] Intentando cargar driver MySQL...");
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                System.out.println("[DAOManager] ✓ Driver MySQL cargado correctamente");
            } catch (ClassNotFoundException e) {
                System.out.println("[DAOManager] ✗ Error cargando driver: " + e.getMessage());
                throw e;
            }

            System.out.println("[DAOManager] Conectando a: " + cfg.getDbUrl());
            conn = DriverManager.getConnection(cfg.getDbUrl(), cfg.getDbUser(), cfg.getDbPass());
            System.out.println("[DAOManager] ✓ Conexión establecida correctamente");

            crearTablasSiNoExisten();
        } catch (Exception e) {
            System.out.println("[DAOManager] ✗ Error en open(): " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public void close() throws SQLException {
        try {
            if (this.conn != null)
                this.conn.close();
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Crea el esquema de tablas si todavía no existe en la base de datos.
     * Así, la primera vez que se ejecuta la aplicación contra una BBDD nueva
     * (o en otro equipo distinto) no hace falta ningún script manual:
     * el propio programa se monta su esquema.
     *
     * Orden de creación importante por las FOREIGN KEY:
     *   usuarios -> productos -> interesados / tratos -> valoraciones_pendientes
     */
    private void crearTablasSiNoExisten() throws SQLException {
        try (Statement st = conn.createStatement()) {

            st.execute("""
                CREATE TABLE IF NOT EXISTS usuarios (
                    id               VARCHAR(10)  PRIMARY KEY,
                    nombre           VARCHAR(100) NOT NULL,
                    apellidos        VARCHAR(100) NOT NULL,
                    correo           VARCHAR(150) NOT NULL UNIQUE,
                    contrasenia      VARCHAR(100) NOT NULL,
                    telefono         INT NOT NULL
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS productos (
                    id               VARCHAR(10)  PRIMARY KEY,
                    nombre           VARCHAR(150) NOT NULL,
                    descripcion      TEXT,
                    precio           DOUBLE NOT NULL,
                    estado           VARCHAR(50),
                    id_vendedor      VARCHAR(10) NOT NULL,
                    FOREIGN KEY (id_vendedor) REFERENCES usuarios(id) ON DELETE CASCADE
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS interesados (
                    id_producto      VARCHAR(10) NOT NULL,
                    correo_interesado VARCHAR(150) NOT NULL,
                    PRIMARY KEY (id_producto, correo_interesado),
                    FOREIGN KEY (id_producto) REFERENCES productos(id) ON DELETE CASCADE
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS tratos (
                    id               VARCHAR(10) PRIMARY KEY,
                    correo_comprador VARCHAR(150) NOT NULL,
                    correo_vendedor  VARCHAR(150) NOT NULL,
                    id_producto      VARCHAR(10) NOT NULL,
                    nombre_producto  VARCHAR(150) NOT NULL,
                    descripcion_producto TEXT,
                    estado_producto  VARCHAR(50),
                    fecha            DATETIME NOT NULL,
                    precio           DOUBLE NOT NULL,
                    comentario       TEXT,
                    puntuacion       INT NOT NULL DEFAULT -1
                )
            """);
            //NOTA: el producto se desnormaliza dentro del trato (nombre, descripcion, estado)
            //porque cuando se cierra una venta, el producto se borra de "productos"
            //(ya no está en venta), pero el Trato necesita seguir mostrando sus datos
            //en el historial. No se usa FOREIGN KEY a productos por este motivo.

            st.execute("""
                CREATE TABLE IF NOT EXISTS valoraciones_pendientes (
                    id_usuario       VARCHAR(10) NOT NULL,
                    id_trato         VARCHAR(10) NOT NULL,
                    PRIMARY KEY (id_usuario, id_trato),
                    FOREIGN KEY (id_usuario) REFERENCES usuarios(id) ON DELETE CASCADE,
                    FOREIGN KEY (id_trato) REFERENCES tratos(id) ON DELETE CASCADE
                )
            """);
        }
    }
}