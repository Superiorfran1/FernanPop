package DAO;

import models.Trato;

import java.util.ArrayList;

/**
 * DaoTrato – Contrato de acceso a datos para la entidad Trato.
 */
public interface DaoTrato {

    /**
     * Inserta un nuevo trato en la base de datos.
     * Los datos del producto se guardan "desnormalizados" dentro del propio
     * trato (nombre, descripción, estado), ya que el producto original
     * se elimina de "productos" al venderse, pero el historial del trato
     * debe conservar esa información igualmente.
     *
     * @param t trato a insertar
     * @return true si se insertó correctamente
     */
    boolean insertar(Trato t);

    /**
     * Actualiza un trato ya existente. Se usa principalmente para
     * registrar la valoración (puntuación + comentario) que deja el comprador.
     *
     * @param t trato con los datos actualizados
     * @return true si se actualizó correctamente
     */
    boolean actualizar(Trato t);

    /**
     * Recupera un único trato por su ID.
     *
     * @param ID_Trato ID del trato a buscar
     * @return el Trato encontrado, o null si no existe
     */
    Trato buscarPorID(String ID_Trato);

    /**
     * Recupera todos los tratos en los que el correo indicado participó
     * como VENDEDOR.
     *
     * @param correoVendedor correo del vendedor
     * @return lista de tratos vendidos por ese usuario
     */
    ArrayList<Trato> buscarPorVendedor(String correoVendedor);

    /**
     * Recupera todos los tratos en los que el correo indicado participó
     * como COMPRADOR.
     *
     * @param correoComprador correo del comprador
     * @return lista de tratos comprados por ese usuario
     */
    ArrayList<Trato> buscarPorComprador(String correoComprador);
}