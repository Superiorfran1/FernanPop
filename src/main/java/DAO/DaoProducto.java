package DAO;

import models.Producto;

import java.util.ArrayList;

/**
 * DaoProducto – Contrato de acceso a datos para la entidad Producto.
 */
public interface DaoProducto {

    /**
     * Inserta un producto nuevo, asociado al usuario que lo pone en venta.
     *
     * @param p           producto a insertar
     * @param ID_Vendedor ID del usuario vendedor
     * @return true si se insertó correctamente
     */
    boolean insertar(Producto p, String ID_Vendedor);

    /**
     * Actualiza los datos de un producto ya existente
     * (nombre, descripción, precio, estado e interesados).
     *
     * @param p producto con los datos actualizados
     * @return true si se actualizó correctamente
     */
    boolean actualizar(Producto p);

    /**
     * Elimina un producto de la base de datos (y a sus interesados en cascada).
     * Se usa, por ejemplo, cuando se cierra una venta o el usuario lo retira.
     *
     * @param ID_Producto ID del producto a eliminar
     * @return true si se eliminó correctamente
     */
    boolean eliminar(String ID_Producto);

    /**
     * Recupera todos los productos en venta de un usuario concreto.
     *
     * @param ID_Vendedor ID del usuario vendedor
     * @return lista de productos en venta de ese usuario
     */
    ArrayList<Producto> buscarPorVendedor(String ID_Vendedor);

    /**
     * Recupera todos los productos en venta de la aplicación, de todos los usuarios.
     *
     * @return lista completa de productos en venta
     */
    ArrayList<Producto> buscarTodos();
}