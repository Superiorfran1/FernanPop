package models;

import java.io.Serializable;
import java.util.Calendar;

/**
 * Trato – Representa una transacción (venta o compra) realizada en FernanPop.
 *
 * Un Trato es el registro de un intercambio entre dos usuarios.
 * El mismo objeto Trato se añade a la lista "ventas" del vendedor
 * y a la lista "compras" del comprador: solo se crea UNA instancia
 * por transacción, no dos copias como se hizo en la POT5.
 */
public class Trato implements Serializable, Comparable<Trato> {

//ATRIBUTOS

    private final String ID; //Identificador único del trato (formato: T + dígitos, ej. T00001)
    private String correoComprador; //Correo electrónico del comprador
    private String correoVendedor; //Correo electrónico del vendedor
    private Producto producto; //Producto que fue objeto de la transacción
    private Calendar fecha; //Fecha y hora en que se cerró el trato
    private double precio; //Precio final al que se realizó la transacción
    private String comentario; //Comentario de valoración dejado por el usuario (puede ser null si no valoró)
    private int puntuacion; //Puntuación de valoración (1-5). Se usa -1 como valor centinela para indicar "sin valorar todavía".
    // No se usa 0 porque podría confundirse con una puntuación real.


//MÉTODOS

    //region CONSTRUCTORES

    /**
     * Crea un nuevo Trato con todos los datos de la transacción.
     * La puntuación se inicializa a -1 (pendiente de valorar).
     * El comentario se inicializa a null (pendiente de comentar).
     *
     * @param ID              identificador único del trato
     * @param correoComprador correo del comprador
     * @param correoVendedor  correo del vendedor
     * @param producto        producto que se vendió
     * @param fecha           fecha y hora del cierre del trato
     * @param precio          precio final de la transacción
     */
    public Trato(String ID, String correoComprador, String correoVendedor,
                 Producto producto, Calendar fecha, double precio) {
        this.ID = ID;
        this.correoComprador = correoComprador;
        this.correoVendedor = correoVendedor;
        this.producto = producto;
        this.fecha = fecha;
        this.precio = precio;
        this.comentario = null;
        this.puntuacion = -1; // -1 significa "pendiente de valorar"
    }

    //Quizás nos haga falta un constructor copia idk. lo dejaré puesto como un TODO por si acaso

    //endregion

    //region GETTERS & SETTERS

    /**
     * Devuelve el ID único del trato.
     * @return ID del trato (ej. "T00001")
     */
    public String getID() {
        return ID;
    }

    /**
     * Devuelve el correo del comprador.
     * @return correo electrónico del comprador
     */
    public String getCorreoComprador() {
        return correoComprador;
    }

    /**@param correoComprador nuevo correo del comprador */
    public void setCorreoComprador(String correoComprador) {
        this.correoComprador = correoComprador;
    }

    /**
     * Devuelve el correo del vendedor.
     * @return correo electrónico del vendedor
     */
    public String getCorreoVendedor() {
        return correoVendedor;
    }

    /** @param correoVendedor nuevo correo del vendedor */
    public void setCorreoVendedor(String correoVendedor) {
        this.correoVendedor = correoVendedor;
    }

    /**
     * Devuelve el producto asociado al trato.
     * @return objeto Producto de la transacción
     */
    public Producto getProducto() {
        return producto;
    }

    /** @param producto nuevo producto asociado al trato */
    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    /**
     * Devuelve la fecha y hora del cierre del trato.
     * @return Calendar con la fecha/hora
     */
    public Calendar getFecha() {
        return fecha;
    }

    /** @param fecha nueva fecha/hora del trato */
    public void setFecha(Calendar fecha) {
        this.fecha = fecha;
    }

    /**
     * Devuelve el precio final de la transacción.
     * @return precio en euros
     */
    public double getPrecio() {
        return precio;
    }

    /** @param precio nuevo precio de la transacción */
    public void setPrecio(double precio) {
        this.precio = precio;
    }

    /**
     * Devuelve el comentario de valoración.
     * Puede ser null si el usuario aún no ha valorado.
     * @return comentario o null
     */
    public String getComentario() {
        return comentario;
    }

    /** @param comentario nuevo comentario de valoración */
    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    /**
     * Devuelve la puntuación de valoración (1-5).
     * Devuelve -1 si el trato aún no ha sido valorado.
     * @return puntuación o -1 si pendiente
     */
    public int getPuntuacion() {
        return puntuacion;
    }

    /** @param puntuacion nueva puntuación (1-5) */
    public void setPuntuacion(int puntuacion) {
        this.puntuacion = puntuacion;
    }

    //endregion

    //region OTROS MÉTODOS

    /**
     * Indica si el trato ya ha sido valorado por algún usuario.
     * Un trato se considera valorado si su puntuación es mayor que 0.
     *
     * @return true si el trato tiene una valoración asignada
     */
    public boolean estaValorado() {
        return puntuacion >= 0;
    }

    /**
     * Permite comparar dos tratos cronológicamente.
     * @param otro El otro trato con el que comparar.
     * @return un entero negativo, cero o positivo si este trato es
     * anterior, igual o posterior al otro.
     */
    @Override
    public int compareTo(Trato otro) {
        return this.fecha.compareTo(otro.getFecha());
        //Dato curioso: no sé por qué si le cambias el nombre a la función a algo tipo:
        //compararPorFecha(Trato otro) peta. Supongo que para la libería necesita que se llamen sí o si compareTo.
        //Pero, ¿cómo distingo si quiero comparar la fecha, el título o cualquier otra cosa? Lo que se le pasa por parámetro es el objeto trato, no un atributo suyo.
        //Ok, según la IA el compareTo de Java sólo permite comparar por una cosa. Si quiero por más, sería la librería "Comparator". Interesante.
        //Espero no tener que tocar más comparaciones o me tocará hacer las comparaciones manualmente.
    }

    //endregion
}