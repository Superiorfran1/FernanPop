package persistence;

import DAO.DAOManager;
import DAO.DaoUsuarioSQL;
import models.Usuario;

import java.io.*;
import java.util.ArrayList;

/**
 * Persistence – Copia de seguridad y restauración de la base de datos.
 *
 * Desde que FernanPop usa una base de datos SQL en la nube, esta clase
 * YA NO serializa usuarios en archivos .bin individuales. Su única
 * responsabilidad ahora es:
 *
 *   - guardarBackup(ruta):    vuelca TODOS los datos de la base de datos
 *                             en un único fichero serializado en disco.
 *   - restaurarBackup(ruta):  borra la base de datos completa y la
 *                             reconstruye a partir de lo que había en
 *                             ese fichero de backup.
 *
 * El fichero de backup contiene un objeto BackupData (ver clase interna),
 * que es simplemente la lista completa de usuarios con todo lo que llevan
 * dentro (productos en venta, ventas, compras, valoraciones pendientes),
 * exactamente igual que se hacía antes con los .bin, solo que ahora es
 * UN ÚNICO fichero en vez de uno por usuario, y se usa solo para backups
 * manuales, no como almacenamiento principal de la aplicación.
 */
public class Persistence {

    //region COPIA DE SEGURIDAD (BBDD -> FICHERO)

    /**
     * Vuelca todos los datos de la base de datos en un único fichero en disco,
     * en la ubicación indicada.
     *
     * @param rutaDestino ruta completa del fichero de backup (ej. "./backups/backup.dat")
     * @return true si la copia se realizó correctamente
     */
    public static boolean guardarBackup(String rutaDestino) {
        try {
            ArrayList<Usuario> usuarios = new DaoUsuarioSQL().buscarTodos();

            File archivo = new File(rutaDestino);
            File carpetaPadre = archivo.getParentFile();
            if (carpetaPadre != null && !carpetaPadre.exists()) {
                carpetaPadre.mkdirs();
            }

            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(archivo))) {
                oos.writeObject(usuarios);
            }

            System.out.println("[Persistencia] Backup completado en: " + archivo.getAbsolutePath()
                    + " (" + usuarios.size() + " usuario(s)).");
            return true;
        } catch (Exception e) {
            System.out.println("[Persistencia] Error al generar el backup: " + e.getMessage());
            return false;
        }
    }

    //endregion

    //region RESTAURACIÓN (FICHERO -> BBDD)

    /**
     * Borra la base de datos completa y la restaura a partir de lo que había
     * en el fichero de backup indicado.
     *
     * @param rutaOrigen ruta completa del fichero de backup a restaurar
     * @return true si la restauración se realizó correctamente
     */
    @SuppressWarnings("unchecked")
    public static boolean restaurarBackup(String rutaOrigen) {
        File archivo = new File(rutaOrigen);
        if (!archivo.exists() || !archivo.isFile()) {
            System.out.println("[Persistencia] Error: el fichero de backup no existe: " + rutaOrigen);
            return false;
        }

        ArrayList<Usuario> usuarios;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(archivo))) {
            Object obj = ois.readObject();
            if (!(obj instanceof ArrayList<?>)) {
                System.out.println("[Persistencia] Error: el fichero de backup tiene un formato no reconocido.");
                return false;
            }
            usuarios = (ArrayList<Usuario>) obj;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[Persistencia] Error al leer el fichero de backup: " + e.getMessage());
            return false;
        }

        try {
            vaciarBaseDeDatos();

            DaoUsuarioSQL daoUsuario = new DaoUsuarioSQL();
            for (Usuario u : usuarios) {
                daoUsuario.insertar(u);
            }

            System.out.println("[Persistencia] Restauración completada: " + usuarios.size() + " usuario(s) recuperado(s).");
            return true;
        } catch (Exception e) {
            System.out.println("[Persistencia] Error al restaurar el backup: " + e.getMessage());
            return false;
        }
    }

    /**
     * Borra TODAS las filas de TODAS las tablas de la base de datos,
     * dejándola completamente vacía antes de restaurar un backup.
     * El orden de borrado respeta las dependencias por FOREIGN KEY:
     * primero las tablas "hijas", luego las "padres".
     */
    private static void vaciarBaseDeDatos() throws Exception {
        DAOManager manager = DAOManager.getSinglentonInstance();
        manager.open();
        try (var st = manager.getConn().createStatement()) {
            st.execute("DELETE FROM valoraciones_pendientes");
            st.execute("DELETE FROM tratos");
            st.execute("DELETE FROM interesados");
            st.execute("DELETE FROM productos");
            st.execute("DELETE FROM usuarios");
        }
    }

    //endregion
}