package models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Usuario – Representa un usuario registrado en FernanPop.
 *
 * Almacena los datos personales del usuario y sus tres listas principales:
 *   - enVenta:               productos que tiene publicados actualmente
 *   - ventas:                tratos en los que él fue el vendedor
 *   - compras:               tratos en los que él fue el comprador
 *   - valoracionesPendientes: IDs de tratos que aún no ha valorado
 *
 * Es Serializable para guardarse en disco mediante Persistence.
 * Con solo guardar la lista de usuarios se preserva toda la información
 * de la aplicación (productos y tratos incluidos).
 */
public class Usuario implements Serializable{

//ATRIBUTOS

    private final String ID; //Identificador único del usuario (formato: U + dígitos, ej. U00001). Final, no cambia.
    private String nombre; //Nombre del usuario
    private String apellidos; //Apellidos del usuario
    private String correoElectronico; //Correo electrónico (se usa también como identificador de login)
    private String contrasenia; //Contraseña de acceso
    private int telefono; //Número de teléfono móvil
    private ArrayList<Producto> enVenta; //Lista de productos que el usuario tiene actualmente en venta
    private ArrayList<Trato> ventas; //Historial de tratos en los que este usuario fue el vendedor
    private ArrayList<Trato> compras; //Historial de tratos en los que este usuario fue el comprador
    private ArrayList<String> valoracionesPendientes; // Lista de IDs de tratos que el usuario aún no ha valorado.


//MÉTODOS

    //region CONSTRUCTORES

    /**
     * Crea un nuevo Usuario con todos sus datos personales.
     * Inicializa todas las listas vacías para evitar NullPointerException.
     *
     * @param ID                identificador único (generado por el Controller)
     * @param nombre            nombre de pila
     * @param apellidos         apellidos
     * @param correoElectronico correo electrónico (debe ser único en el sistema)
     * @param contrasenia       contraseña de acceso
     * @param telefono          número de móvil
     */
    public Usuario(String ID, String nombre, String apellidos, String correoElectronico, String contrasenia, int telefono) {
        this.ID = ID;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.correoElectronico = correoElectronico;
        this.contrasenia = contrasenia;
        this.telefono = telefono;

        // Inicialización explícita de todas las listas
        this.enVenta = new ArrayList<>();
        this.ventas = new ArrayList<>();
        this.compras = new ArrayList<>();
        this.valoracionesPendientes = new ArrayList<>();
    }

    //endregion

    //region GETTERS & SETTERS

    /**
     * Devuelve el ID único del usuario.
     * @return ID (ej. "U00001")
     */
    public String getID() {
        return ID;
    }

    /**
     * Devuelve el nombre del usuario.
     * @return nombre
     */
    public String getNombre() {
        return nombre;
    }

    /** @param nombre nuevo nombre */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Devuelve los apellidos del usuario.
     * @return apellidos
     */
    public String getApellidos() {
        return apellidos;
    }

    /** @param apellidos nuevos apellidos */
    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    /**
     * Devuelve el correo electrónico del usuario.
     * @return correo electrónico
     */
    public String getCorreoElectronico() {
        return correoElectronico;
    }

    /** @param correoElectronico nuevo correo electrónico */
    public void setCorreoElectronico(String correoElectronico) {
        this.correoElectronico = correoElectronico;
    }

    /**
     * Devuelve la contraseña del usuario.
     * @return contraseña
     */
    public String getContrasenia() {
        return contrasenia;
    }

    /** @param contrasenia nueva contraseña */
    public void setContrasenia(String contrasenia) {
        this.contrasenia = contrasenia;
    }

    /**
     * Devuelve el número de teléfono del usuario.
     * @return teléfono móvil
     */
    public int getTelefono() {
        return telefono;
    }

    /** @param telefono nuevo número de teléfono */
    public void setTelefono(int telefono) {
        this.telefono = telefono;
    }

    /**
     * Devuelve la lista de productos en venta del usuario.
     * @return lista de productos en venta
     */
    public ArrayList<Producto> getEnVenta() {
        return enVenta;
    }

    /** @param enVenta nueva lista de productos en venta */
    public void setEnVenta(ArrayList<Producto> enVenta) {
        this.enVenta = enVenta;
    }

    /**
     * Devuelve el historial de ventas del usuario.
     * @return lista de tratos en los que fue vendedor
     */
    public ArrayList<Trato> getVentas() {
        return ventas;
    }

    /** @param ventas nueva lista de ventas */
    public void setVentas(ArrayList<Trato> ventas) {
        this.ventas = ventas;
    }

    /**
     * Devuelve el historial de compras del usuario.
     * @return lista de tratos en los que fue comprador
     */
    public ArrayList<Trato> getCompras() {
        return compras;
    }

    /** @param compras nueva lista de compras */
    public void setCompras(ArrayList<Trato> compras) {
        this.compras = compras;
    }

    /**
     * Devuelve la lista de IDs de tratos pendientes de valorar,
     * ORDENADA POR ANTIGÜEDAD real (comparando la fecha de cada trato).
     * * Solo busca en la lista de 'compras', ya que en FernanPop
     * solo el comprador puede valorar la transacción.
     *
     * @return nueva lista con los IDs ordenados cronológicamente
     */
    public ArrayList<String> getValoracionesPendientes() {
        if (valoracionesPendientes.isEmpty()) return new ArrayList<>();

        ArrayList<Trato> listaTratos = new ArrayList<>();

        //Buscamos los objetos Trato únicamente en la lista de compras
        for (String id : valoracionesPendientes) {
            Trato t = getTratoCompras(id);
            if (t != null) {
                listaTratos.add(t);
            }
        }

        //Ordenación natural por fecha (gracias a Comparable en Trato)
        listaTratos.sort(null); //Se le pasa null porque YA DEFINÍ en tratos que la forma por defecto de ordenarse es por fecha de antigüedad.
        //Le podría pasar Integer (lo hará de menor a mayor) o String (por órden alfabético). Pero no es lo que queremos, así que, null.

        //Reconvertimos a lista de IDs
        ArrayList<String> IDsOrdenados = new ArrayList<>();
        for (Trato t : listaTratos) {
            IDsOrdenados.add(t.getID());
        }

        return IDsOrdenados;
    }

    /** @param valoracionesPendientes nueva lista de valoraciones pendientes */
    public void setValoracionesPendientes(ArrayList<String> valoracionesPendientes) {
        this.valoracionesPendientes = valoracionesPendientes;
    }

    //endregion

    //region MÉTODOS RELACIONADOS CON "enVenta"

    /**
     * Devuelve el número de productos que el usuario tiene en venta.
     * @return número de productos en venta
     */
    public int cantidadProductosEnVenta() {
        return enVenta.size();
    }

    /**
     * Añade un producto a la lista de productos en venta del usuario.
     * @param p producto a añadir
     */
    public void addProducto(Producto p) {
        enVenta.add(p);
    }

    /**
     * Elimina un producto de la lista en venta usando su ID.
     * Usa removeIf para mayor claridad y eficiencia.
     *
     * @param ID_Producto ID del producto a eliminar
     * @return true si encontró y eliminó el producto; false si no existía
     */
    public boolean deleteProducto(String ID_Producto) {
        if (enVenta == null) return false;
        return enVenta.removeIf(p -> p.getID().equals(ID_Producto));
    }

    /**
     * Busca y devuelve un producto de la lista en venta por su ID.
     *
     * @param ID_Producto ID del producto a buscar
     * @return el Producto encontrado, o null si no existe
     */
    public Producto getProductoFromEnVenta(String ID_Producto) {
        for (Producto p : enVenta) {
            if (p.getID().equals(ID_Producto)) {
                return p;
            }
        }
        return null;
    }

    //endregion

    //region MÉTODOS RELACIONADOS CON "valoracionesPendientes"

    /**
     * Devuelve cuántas valoraciones tiene pendientes el usuario.
     * @return número de valoraciones pendientes
     */
    public int cantidadValoracionesPendientes() {
        return valoracionesPendientes.size();
    }

    /**
     * Añade el ID de un trato a la lista de valoraciones pendientes.
     *
     * @param ID_Trato ID del trato que queda pendiente de valorar
     */
    public void addValoracionPendiente(String ID_Trato) {
        valoracionesPendientes.add(ID_Trato);
    }

    /**
     * Elimina una valoración pendiente por el ID del trato.
     * Se llama cuando el usuario finalmente valora ese trato.
     * Usa removeIf para mayor claridad y eficiencia.
     *
     * @param ID_Trato ID del trato que ya fue valorado
     */
    public void deleteValoracionPendiente(String ID_Trato) {
        valoracionesPendientes.removeIf(id -> id.equals(ID_Trato));
    }

    //endregion

    //region MÉTODOS RELACIONADOS CON "ventas" Y "compras"

    /**
     * Crea y añade un nuevo Trato a la lista de ventas del usuario.
     * Este mismo objeto Trato será también añadido a las compras del comprador
     * (ver Controller.cerrarVenta), por lo que solo se crea UNA instancia.
     * POR FAVOR, RESPETAR EL ORDEN:
     * 1. SE CREA EL TRATO EN VENTAS, LUEGO SE AÑADE A COMPRAS.
     * Si lo haces al revés no funcionará porque no se habrá instanciado.
     *
     * @param ID_Venta      ID único del trato (generado por el Controller)
     * @param ID_Producto   ID del producto vendido
     * @param correoComprador correo del comprador
     * @param precioFinal   precio final de la transacción
     * @return el Trato recién creado (para que el Controller pueda añadirlo también al comprador)
     */
    public Trato addTratoVenta(String ID_Venta, String ID_Producto, String correoComprador, double precioFinal) {
        Trato nuevoTrato = new Trato(
                ID_Venta,
                correoComprador,          //comprador
                this.correoElectronico,   //vendedor (este usuario)
                getProductoFromEnVenta(ID_Producto),
                Calendar.getInstance(),
                precioFinal
        );
        ventas.add(nuevoTrato);
        return nuevoTrato; //Devolvemos el objeto para que el comprador lo pueda referenciar
    }

    /**
     * Añade un trato existente a la lista de compras del usuario.
     * Se llama con el mismo objeto Trato que ya está en las ventas del vendedor.
     *
     * @param trato objeto Trato a añadir a las compras
     */
    public void addTratoCompra(Trato trato) {
        compras.add(trato);
    }

    /**
     * Busca y devuelve un trato de la lista de ventas por su ID.
     *
     * @param ID_Trato ID del trato a buscar
     * @return el Trato encontrado, o null si no existe en ventas
     */
    public Trato getTratoVentas(String ID_Trato) {
        for (Trato t : ventas) {
            if (t.getID().equals(ID_Trato)) {
                return t;
            }
        }
        return null;
    }

    /**
     * Busca y devuelve un trato de la lista de compras por su ID.
     *
     * @param ID_Trato ID del trato a buscar
     * @return el Trato encontrado, o null si no existe en compras
     */
    public Trato getTratoCompras(String ID_Trato) {
        for (Trato t : compras) {
            if (t.getID().equals(ID_Trato)) {
                return t;
            }
        }
        return null;
    }

    //endregion

    //region OTROS MÉTODOS

    /**
     * Verifica si el email y la contraseña coinciden con los de este usuario.
     * Usado por el Controller para autentificar el login.
     *
     * @param email    correo electrónico introducido
     * @param password contraseña introducida
     * @return true si las credenciales son correctas
     */
    public boolean login(String email, String password) {
        return this.correoElectronico.equals(email) && this.contrasenia.equals(password);
    }

    /**
     * Calcula la nota media de las valoraciones recibidas en las ventas del usuario.
     * Solo se tienen en cuenta los tratos con puntuación mayor o igual que 0.
     * Si no hay ninguna valoración, devuelve 0.0.
     *
     * Nota: el resultado puede tener muchos decimales; aplica formato al mostrarlo.
     *
     * @return media de las puntuaciones recibidas, o 0.0 si no hay valoraciones
     */
    public double notaMedia() {
        ArrayList<Trato> tratosValorados = new ArrayList<>();
        for (Trato t : ventas) {
            if (t.getPuntuacion() >= 0) tratosValorados.add(t);
        }
        if (tratosValorados.isEmpty()) return -1;

        double suma = 0;
        for (Trato t : tratosValorados) {
            suma += t.getPuntuacion();
        }
        return suma / tratosValorados.size();
    }

    //endregion
}