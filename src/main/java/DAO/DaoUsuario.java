package DAO;

import models.Usuario;

import java.util.ArrayList;

/**
 * DaoUsuario – Contrato de acceso a datos para la entidad Usuario.
 *
 * Define las operaciones que cualquier implementación (SQL, Mongo, lo que sea)
 * debe ofrecer para que el Controller pueda trabajar sin saber nada de la
 * tecnología de persistencia concreta.
 */
public interface DaoUsuario {

    /**
     * Inserta un usuario nuevo en la base de datos junto con todo lo que
     * lleva dentro (productos en venta, ventas, compras, valoraciones pendientes).
     *
     * @param u usuario a insertar
     * @return true si se insertó correctamente
     */
    boolean insertar(Usuario u);

    /**
     * Actualiza los datos personales de un usuario ya existente
     * (nombre, apellidos, correo, contraseña, teléfono).
     *
     * @param u usuario con los datos actualizados
     * @return true si se actualizó correctamente
     */
    boolean actualizar(Usuario u);

    /**
     * Elimina un usuario de la base de datos junto con todo lo que dependa de
     * él en cascada (productos en venta e interesados).
     * Los tratos NO se eliminan (quedan como historial).
     *
     * @param ID_Usuario ID del usuario a eliminar
     * @return true si se eliminó correctamente
     */
    boolean eliminar(String ID_Usuario);

    /**
     * Recupera un único usuario por su ID, con todos sus datos asociados
     * (productos en venta, ventas, compras, valoraciones pendientes) ya cargados.
     *
     * @param ID_Usuario ID del usuario a buscar
     * @return el Usuario encontrado, o null si no existe
     */
    Usuario buscarPorID(String ID_Usuario);

    /**
     * Recupera todos los usuarios de la base de datos, con todos sus datos
     * asociados ya cargados (productos en venta, ventas, compras, valoraciones pendientes).
     *
     * @return lista completa de usuarios
     */
    ArrayList<Usuario> buscarTodos();
}