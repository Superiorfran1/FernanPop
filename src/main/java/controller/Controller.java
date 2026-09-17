package controller;

import DAO.DaoProductoSQL;
import DAO.DaoTratoSQL;
import DAO.DaoUsuarioSQL;
import data.Testing;
import models.Producto;
import models.Trato;
import models.Usuario;

import java.util.ArrayList;

/**
 * Controller – Núcleo lógico de la aplicación FernanPop.
 *
 * Gestiona la lista de usuarios y todas las operaciones de negocio:
 * búsquedas, altas, bajas, login, tratos, valoraciones...
 *
 * También centraliza la generación de IDs incrementales para
 * usuarios, productos y tratos, evitando recorrer todos los arrays.
 *
 * IMPORTANTE (cambio de la POT8): los datos YA NO se cargan todos en
 * memoria al arrancar desde unos ficheros .bin. Ahora viven en una base
 * de datos SQL en la nube, y se consultan a través de los DAO
 * (DaoUsuarioSQL, DaoProductoSQL, DaoTratoSQL) en el momento en que
 * se necesitan. La lista "usuarios" sigue existiendo en memoria como
 * CACHÉ de trabajo durante la sesión (para no tener que repetir consultas
 * SQL constantemente desde el Main), pero el origen de la verdad real
 * es siempre la base de datos: cada operación de alta/baja/modificación
 * se reescribe inmediatamente en el DAO correspondiente.
 */
public class Controller {

// ATRIBUTOS

    private ArrayList<Usuario> usuarios; //Caché en memoria de todos los usuarios registrados en la aplicación

    private final DaoUsuarioSQL daoUsuario;
    private final DaoProductoSQL daoProducto;
    private final DaoTratoSQL daoTrato;

    /**
     * Contadores para la generación de IDs únicos e incrementales.
     * Se inicializan a 0 y se incrementan cada vez que se necesita una nueva ID.
     * Formato final: Prefijo + número rellenado con ceros a la izquierda.
     *
     * Ejemplo con longitud 5:  U00001, U00002, ..., U99999
     *
     * Lo malo es que sólo se pueden hacer 99999 de cada cosa. Pero meh, esto es un entorno de pruebas
     */
    private int ultimaID_Usuario;
    private int ultimaID_Producto;
    private int ultimaID_Trato;


//MÉTODOS

    //region CONSTRUCTORES

    /**
     * Crea el Controller cargando todos los usuarios (y lo que llevan dentro)
     * desde la base de datos SQL en la nube a través de DaoUsuarioSQL.
     * Calcula los contadores de ID buscando el máximo ya existente en los datos,
     * para que al añadir nuevos elementos no haya colisiones.
     */
    public Controller() {
        this.daoUsuario = new DaoUsuarioSQL();
        this.daoProducto = new DaoProductoSQL();
        this.daoTrato = new DaoTratoSQL();

        this.usuarios = daoUsuario.buscarTodos();
        // Si no hay datos guardados, se insertan datos de prueba para desarrollo
        if (this.usuarios.isEmpty()) {
            insertarDatosPrueba(this);
        }
        //Recalcula los contadores de ID a partir de los datos cargados
        recalcularContadoresID();
    }

    /**
     * Inserta datos de prueba en el sistema para facilitar el desarrollo y testing.
     * Solo se llama cuando no hay datos guardados en la base de datos.
     *
     * @param app instancia del Controller
     */
    private void insertarDatosPrueba(Controller app) {
        Testing.mock(app);
    }

    //endregion

    //region GETTERS & SETTERS

    /**
     * Devuelve la lista completa de usuarios del sistema (caché en memoria).
     * @return lista de usuarios
     */
    public ArrayList<Usuario> getUsuarios() {
        return usuarios;
    }

    /** @param usuarios nueva lista de usuarios */
    public void setUsuarios(ArrayList<Usuario> usuarios) {
        this.usuarios = usuarios;
    }

    //endregion

    //region GENERACIÓN DE IDs INCREMENTALES

    /**
     * Recorre los datos cargados y fija los contadores al máximo ID encontrado.
     * Así, si el sistema se reinicia con datos guardados, las nuevas IDs
     * continúan desde donde se dejó (sin colisiones).
     *
     * La parte numérica de cada ID es el número después del prefijo (U/P/T).
     */
    public void recalcularContadoresID() {
        ultimaID_Usuario = 0;
        ultimaID_Producto = 0;
        ultimaID_Trato = 0;

        for (Usuario u : usuarios) {
            //Extracción del número de la ID del usuario (formato U00001 → 1)
            int numUser = extraerNumeroDeID(u.getID());
            if (numUser > ultimaID_Usuario) ultimaID_Usuario = numUser;

            for (Producto p : u.getEnVenta()) {
                int numProd = extraerNumeroDeID(p.getID());
                if (numProd > ultimaID_Producto) ultimaID_Producto = numProd;
            }

            for (Trato t : u.getVentas()) {
                int numTrato = extraerNumeroDeID(t.getID());
                if (numTrato > ultimaID_Trato) ultimaID_Trato = numTrato;
            }
        }
    }

    /**
     * Extrae la parte numérica de una ID (ej. "U00042" → 42).
     * Si el formato no es el esperado, devuelve 0.
     *
     * @param ID la ID completa (ej. "T00007")
     * @return el número extraído, o 0 si no se pudo parsear
     */
    private int extraerNumeroDeID(String ID) {
        if (ID == null || ID.length() < 6) return 0;
        try {
            // El prefijo siempre es 1 carácter (U, P, T), lo demás es el número
            return Integer.parseInt(ID.substring(1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Formatea un número como ID completa con el prefijo y ceros a la izquierda.
     *
     * Ejemplo: prefijo="P", numero=7, longitud=5 → "P00007"
     *
     * La longitud de la parte numérica del número.
     *
     * @param prefijo  carácter prefijo ("U", "P" o "T")
     * @param numero   tamaño del apartado numérico de las ID
     * @return ID completa formateada
     */
    private String formatearID(String prefijo, int numero) {
        int longitud = 5;
        // %0Nd rellena con ceros a la izquierda hasta N dígitos
        return prefijo + String.format("%0" + longitud + "d", numero);
    }

    /**
     * Genera y devuelve la siguiente ID disponible para un nuevo Usuario.
     * Incrementa el contador interno para que la siguiente sea distinta.
     *
     * Formato: U + N dígitos (ej. U00001)
     *
     * @return nueva ID de usuario lista para usar
     */
    public String generaID_Usuario() {
        ultimaID_Usuario++;
        return formatearID("U", ultimaID_Usuario);
    }

    /**
     * Genera y devuelve la siguiente ID disponible para un nuevo Producto.
     * Incrementa el contador interno para que la siguiente sea distinta.
     *
     * Formato: P + N dígitos (ej. P00001)
     *
     * @return nueva ID de producto lista para usar
     */
    public String generaID_Producto() {
        ultimaID_Producto++;
        return formatearID("P", ultimaID_Producto);
    }

    /**
     * Genera y devuelve la siguiente ID disponible para un nuevo Trato.
     * Incrementa el contador interno para que la siguiente sea distinta.
     *
     * Formato: T + N dígitos (ej. T00001)
     *
     * @return nueva ID de trato lista para usar
     */
    public String generaID_Trato() {
        ultimaID_Trato++;
        return formatearID("T", ultimaID_Trato);
    }

    //endregion

    //region PERSISTENCIA

    /**
     * Guarda el estado actual de TODOS los usuarios en la base de datos.
     * Delega en DaoUsuarioSQL.actualizar() para cada usuario de la caché
     * en memoria, sincronizando también sus productos en venta y
     * valoraciones pendientes.
     *
     * @return true si todos los usuarios se guardaron correctamente
     */
    public boolean guardar() {
        boolean exitoTotal = true;
        for (Usuario u : usuarios) {
            if (!daoUsuario.actualizar(u)) {
                exitoTotal = false;
            }
        }
        return exitoTotal;
    }

    /**
     * Guarda un sólo usuario en la base de datos.
     * Si el usuario ya existe en la BBDD se actualiza; si no existe
     * (usuario recién creado) se inserta.
     *
     * @return true si el guardado fue exitoso
     */
    public boolean guardarUnico(Usuario u) {
        if (daoUsuario.buscarPorID(u.getID()) != null) {
            return daoUsuario.actualizar(u);
        }
        return daoUsuario.insertar(u);
    }

    //endregion

    //region GESTIÓN DE USUARIOS

    /**
     * Busca un usuario por su correo electrónico.
     *
     * @param correoElectronico correo a buscar
     * @return el Usuario encontrado, o null si no existe
     */
    public Usuario buscaPorCorreoElectronico(String correoElectronico) {
        for (Usuario u : usuarios) {
            if (u.getCorreoElectronico().equals(correoElectronico)) {
                return u;
            }
        }
        return null;
    }

    /**
     * Añade un nuevo usuario al sistema si no existía previamente.
     * Se inserta tanto en la caché en memoria como en la base de datos.
     *
     * @param u usuario a añadir
     * @return true si se añadió correctamente; false si ya existía o hubo error
     */
    public boolean addUsuario(Usuario u) {
        if (buscaUsuario(u)) {
            return false; //El usuario ya existe, no se puede añadir duplicado
        }
        try {
            usuarios.add(u);
            daoUsuario.insertar(u);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Comprueba si un usuario ya existe en la lista (por referencia de objeto).
     *
     * @param u usuario a buscar
     * @return true si el usuario está en la lista
     */
    public boolean buscaUsuario(Usuario u) {
        return usuarios.contains(u);
    }

    /**
     * Elimina un usuario del sistema, tanto de la caché en memoria como
     * de la base de datos (sus productos en venta e interesados se
     * eliminan en cascada; sus tratos permanecen como historial).
     *
     * @param u usuario a eliminar
     * @return true si se encontró y eliminó; false si no existía o hubo error
     */
    public boolean deleteUsuario(Usuario u) {
        String ID_cuentaBorrada = u.getID();
        try {
            if (usuarios.remove(u)) {
                daoUsuario.eliminar(ID_cuentaBorrada);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifica las credenciales de login y devuelve el usuario si son correctas.
     *
     * @param correo    correo electrónico introducido
     * @param contrasenia contraseña introducida
     * @return el Usuario autenticado, o null si las credenciales son incorrectas
     */
    public Usuario login(String correo, String contrasenia) {
        if (correo == null || contrasenia == null) {
            return null; //Credenciales nulas no se procesan
        }
        for (Usuario u : usuarios) {
            if (u.login(correo, contrasenia)) {
                return u; //Credenciales correctas
            }
        }
        return null; //No se encontró ningún usuario con esas credenciales
    }

    //endregion

    //region GESTIÓN DE PRODUCTOS

    /**
     * Añade un producto a la lista en venta de un usuario, tanto en la
     * caché en memoria como en la base de datos.
     *
     * @param u usuario que pone el producto en venta
     * @param p producto a añadir
     * @return true si se añadió correctamente
     */
    public boolean addProducto(Usuario u, Producto p) {
        try {
            u.addProducto(p);
            daoProducto.insertar(p, u.getID());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Devuelve la lista de productos en venta de un usuario, buscando por su correo electronico.
     *
     * @param correo correo del usuario
     * @return lista de productos en venta, o null si no se encontró el usuario
     */
    public ArrayList<Producto> getProductosUser(String correo) {
        Usuario u = buscaPorCorreoElectronico(correo);
        if (u != null) {
            return u.getEnVenta();
        }
        return null;
    }

    /**
     * Devuelve todos los productos en venta de todos los usuarios.
     * El Main se encarga de paginar esta lista para la visualización.
     *
     * @return lista completa de todos los productos disponibles
     */
    public ArrayList<Producto> getAllProductos() {
        ArrayList<Producto> productos = new ArrayList<>();
        for (Usuario u : usuarios) {
            productos.addAll(u.getEnVenta());
        }
        return productos;
    }

    /**
     * Busca un producto en toda la aplicación por su ID.
     *
     * @param ID_Producto ID del producto a buscar
     * @return el Producto encontrado, o null si no existe
     */
    public Producto buscaProductoId(String ID_Producto) {
        for (Usuario u : usuarios) {
            for (Producto p : u.getEnVenta()) {
                if (p.getID().equals(ID_Producto)) {
                    return p;
                }
            }
        }
        return null;
    }

    /**
     * Busca productos cuyo nombre o descripción contengan el texto indicado.
     * Da igual si es mayúscula o minúscula.
     *
     * @param texto texto a buscar
     * @return lista de productos que coinciden con la búsqueda
     */
    public ArrayList<Producto> buscaProductosTexto(String texto) {
        ArrayList<Producto> resultado = new ArrayList<>();
        for (Usuario u : usuarios) {
            for (Producto p : u.getEnVenta()) {
                if (p.getNombre().toLowerCase().contains(texto.toLowerCase()) ||
                        p.getDescripcion().toLowerCase().contains(texto.toLowerCase())) {
                    resultado.add(p);
                }
            }
        }
        return resultado;
    }

    /**
     * Encuentra el usuario propietario de un producto buscando por ID de producto.
     *
     * @param ID_Producto ID del producto
     * @return el Usuario propietario, o null si no se encontró
     */
    public Usuario buscaPropietarioProducto(String ID_Producto) {
        for (Usuario u : usuarios) {
            if (u.getProductoFromEnVenta(ID_Producto) != null) {
                return u;
            }
        }
        return null;
    }

    //endregion

    //region GESTIÓN DE TRATOS

    /**
     * Cierra una venta creando UN único objeto Trato y añadiéndolo tanto
     * a las ventas del vendedor como a las compras del comprador.
     *
     * De esta forma ambos usuarios referencian el MISMO objeto (no dos copias),
     * lo que garantiza que una valoración en un lado sea visible en el otro
     * mientras dure la sesión en memoria. El trato se inserta en la base de
     * datos UNA sola vez (no dos), y el producto vendido se elimina de la
     * tabla "productos" porque ya no está en venta (sus datos quedan
     * conservados dentro de la propia fila del trato).
     *
     * @param vendedor        usuario que vende
     * @param ID_Producto     ID del producto vendido
     * @param comprador       usuario que compra
     * @param precioFinal     precio acordado de la transacción
     * @return el Trato creado, o null si hubo algún error
     */
    public Trato cerrarVenta(Usuario vendedor, String ID_Producto, Usuario comprador, double precioFinal) {
        String ID_Trato = generaID_Trato();

        //addTratoVenta() crea el objeto Trato, lo añade a ventas del vendedor y lo devuelve
        Trato trato = vendedor.addTratoVenta(ID_Trato, ID_Producto, comprador.getCorreoElectronico(), precioFinal);

        if (trato == null) return null;

        //Se añade el MISMO objeto Trato a las compras del comprador
        comprador.addTratoCompra(trato);

        //Se añade la valoración pendiente al comprador
        comprador.addValoracionPendiente(ID_Trato);

        //Se elimina el producto de la lista en venta del vendedor
        vendedor.deleteProducto(ID_Producto);

        //Persistencia en la base de datos: se inserta el trato una sola vez
        //y se elimina el producto de la tabla "productos"
        daoTrato.insertar(trato);
        daoProducto.eliminar(ID_Producto);

        return trato;
    }

    /**
     * Busca un trato en toda la aplicación por su ID.
     * Primero busca en las ventas, luego en las compras de cada usuario.
     * Como el mismo objeto está en ambas listas, al encontrarlo en ventas ya es suficiente.
     * AUNQUE, POR SI SE ELIMINA EL COMPRADOR, SI SE ENCUENTRA NULL EN VENTAS, SE MIRA EN COMPRAS.
     *
     * @param ID_Trato ID del trato a buscar
     * @return el Trato encontrado, o null si no existe
     */
    public Trato buscaTratoId(String ID_Trato) {
        for (Usuario u : usuarios) {
            //Se mira en ventas
            Trato tVenta = u.getTratoVentas(ID_Trato);
            if (tVenta != null) return tVenta;

            //Si no está en ventas, se mira en compras del MISMO usuario antes de pasar al siguiente
            Trato tCompra = u.getTratoCompras(ID_Trato);
            if (tCompra != null) return tCompra;
        }
        return null;
    }

    //endregion

    //region GESTIÓN DE VALORACIONES PENDIENTES

    /**
     * Devuelve los tratos pendientes de valorar por el usuario indicado.
     * La lista está ORDENADA POR ANTIGÜEDAD (los más antiguos primero),
     * ya que getValoracionesPendientes() preserva el orden de inserción.
     *
     * @param u usuario del que se quieren las valoraciones pendientes
     * @return lista de Trato pendientes de valorar, en orden de antigüedad
     */
    public ArrayList<Trato> getValoracionesPendientes(Usuario u) {
        ArrayList<Trato> valoracionesPendientes = new ArrayList<>();

        //Se recorre los IDs pendientes (ya vienen en orden de antigüedad)
        for (String ID_Trato : u.getValoracionesPendientes()) {
            //Búsqueda del trato en compras
            Trato t = u.getTratoCompras(ID_Trato);
            if (t != null) valoracionesPendientes.add(t);
        }

        return valoracionesPendientes;
    }

    /**
     * Marca una valoración pendiente como completada para el usuario indicado,
     * y persiste tanto la valoración (puntuación + comentario) del trato como
     * la eliminación de la valoración pendiente en la base de datos.
     * .
     * IMPORTANTE: el trato se busca en las COMPRAS del propio usuario (no con
     * buscaTratoId), porque en la base de datos las listas "ventas" y "compras"
     * de cada usuario se reconstruyen con consultas independientes: el objeto
     * Trato del lado del vendedor y el objeto Trato del lado del comprador
     * NO son la misma referencia en memoria (a diferencia de cuando todo vivía
     * en RAM con los .bin). Solo el lado del comprador puede valorar, así que
     * es ahí donde está el objeto realmente modificado con la nueva puntuación.
     *
     * @param u        usuario que completó la valoración
     * @param ID_Trato ID del trato que fue valorado
     * @return true si se eliminó correctamente
     */
    public boolean borraValoracionPendiente(Usuario u, String ID_Trato) {
        try {
            u.deleteValoracionPendiente(ID_Trato);
            Trato t = u.getTratoCompras(ID_Trato);
            if (t != null) {
                daoTrato.actualizar(t); //Persiste puntuación y comentario
            }
            guardarUnico(u); //Persiste la eliminación de la valoración pendiente
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    //endregion
}