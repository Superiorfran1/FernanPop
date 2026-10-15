package models;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Producto – Representa un artículo disponible para la venta en FernanPop.
 *
 * * Contiene la información detallada del artículo, su estado físico,
 * el precio de venta y el registro de usuarios interesados.
 */
public class Producto implements Serializable {

//ATRIBUTOS

    private final String ID; //Identificador único del producto (formato: P + dígitos, ej. P00001)
    private String nombre; //Nombre del producto (ej. "Bicicleta de montaña")
    private String descripcion; //Descripción detallada del producto
    private double precio; //Precio de venta en euros
    private String estado; //Estado del producto (ej. "Nuevo", "Usado", "Como nuevo")
    private ArrayList<String> interesados; //Lista de correos electrónicos de usuarios interesados (sin duplicados)


//MÉTODOS

    //region CONSTRUCTORES

    /**
     * Crea un nuevo Producto con todos sus datos.
     * Inicializa la lista de interesados vacía (lista nueva, no null).
     *
     * @param ID          identificador único (generado por el Controller)
     * @param nombre      nombre del artículo
     * @param descripcion descripción del artículo
     * @param precio      precio de venta en euros
     * @param estado      estado del artículo (ej. "Nuevo", "Usado")
     */
    public Producto(String ID, String nombre, String descripcion, double precio, String estado) {
        this.ID = ID;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.estado = estado;
        this.interesados = new ArrayList<>(); // Siempre inicializada, nunca null
    }

    //endregion

    //region GETTERS & SETTERS

    /**
     * Devuelve el ID único del producto.
     * @return ID del producto (ej. "P00001")
     */
    public String getID() {
        return ID;
    }

    /**
     * Devuelve el nombre del producto.
     * @return nombre del producto
     */
    public String getNombre() {
        return nombre;
    }

    /** @param nombre nuevo nombre del producto */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Devuelve la descripción del producto.
     * @return descripción
     */
    public String getDescripcion() {
        return descripcion;
    }

    /** @param descripcion nueva descripción del producto */
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    /**
     * Devuelve el precio del producto en euros.
     * @return precio
     */
    public double getPrecio() {
        return precio;
    }

    /** @param precio nuevo precio del producto */
    public void setPrecio(double precio) {
        this.precio = precio;
    }

    /**
     * Devuelve el estado del producto (ej. "Nuevo", "Usado").
     * @return estado
     */
    public String getEstado() {
        return estado;
    }

    /** @param estado nuevo estado del producto */
    public void setEstado(String estado) {
        this.estado = estado;
    }

    /**
     * Devuelve la lista de correos de usuarios interesados.
     * Nunca devuelve null: si no hay interesados, devuelve lista vacía.
     * @return lista de correos de interesados
     */
    public ArrayList<String> getInteresados() {
        if (interesados == null) interesados = new ArrayList<>();
        return interesados;
    }

    /** @param interesados nueva lista de interesados */
    public void setInteresados(ArrayList<String> interesados) {
        this.interesados = interesados;
    }

    //endregion

    //region OTROS MÉTODOS

    /**
     * Añade el correo de un usuario a la lista de interesados.
     * Si el correo ya estaba en la lista, no lo añade de nuevo (sin duplicados).
     *
     * @param correoElectronico correo del usuario interesado
     */
    public void addInteresado(String correoElectronico) {
        if (interesados == null) interesados = new ArrayList<>();
        if (!interesados.contains(correoElectronico)) {
            interesados.add(correoElectronico);
        }
    }

    //endregion
}