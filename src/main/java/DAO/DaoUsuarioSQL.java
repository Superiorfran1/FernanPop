package DAO;

import models.Trato;
import models.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * DaoUsuarioSQL – Implementación de DaoUsuario contra una base de datos SQL
 * (MySQL / MariaDB), usando JDBC.
 *
 * Cada usuario se reconstruye con TODA su información asociada:
 *   - Productos en venta            (delegado en DaoProductoSQL)
 *   - Ventas y compras (Tratos)     (delegado en DaoTratoSQL)
 *   - Valoraciones pendientes       (tabla valoraciones_pendientes)
 *
 * Así el Controller puede pedir un Usuario y recibirlo "completo",
 * igual que antes con la deserialización del .bin.
 */
public class DaoUsuarioSQL implements DaoUsuario {

    private final DaoProductoSQL daoProducto = new DaoProductoSQL();
    private final DaoTratoSQL daoTrato = new DaoTratoSQL();

    //region INSERTAR

    @Override
    public boolean insertar(Usuario u) {
        String sql = "INSERT INTO usuarios (id, nombre, apellidos, correo, contrasenia, telefono) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            Connection conn = conectar();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, u.getID());
                ps.setString(2, u.getNombre());
                ps.setString(3, u.getApellidos());
                ps.setString(4, u.getCorreoElectronico());
                ps.setString(5, u.getContrasenia());
                ps.setInt(6, u.getTelefono());
                ps.executeUpdate();
            }

            //Insertamos también todo lo que el usuario ya llevara dentro (normalmente
            //listas vacías al crearse, pero por seguridad lo cubrimos igualmente)
            for (var p : u.getEnVenta()) {
                daoProducto.insertar(p, u.getID());
            }
            for (Trato t : u.getVentas()) {
                daoTrato.insertar(t);
            }
            guardarValoracionesPendientes(u);

            return true;
        } catch (Exception e) {
            System.out.println("[DaoUsuarioSQL] Error al insertar usuario " + u.getID() + ": " + e.getMessage());
            return false;
        }
    }

    //endregion

    //region ACTUALIZAR

    @Override
    public boolean actualizar(Usuario u) {
        String sql = "UPDATE usuarios SET nombre=?, apellidos=?, correo=?, contrasenia=?, telefono=? WHERE id=?";
        try {
            Connection conn = conectar();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, u.getNombre());
                ps.setString(2, u.getApellidos());
                ps.setString(3, u.getCorreoElectronico());
                ps.setString(4, u.getContrasenia());
                ps.setInt(5, u.getTelefono());
                ps.setString(6, u.getID());
                ps.executeUpdate();
            }

            //Sincronización completa de productos en venta: se borran los que ya
            //no están y se insertan/actualizan los actuales. Es más simple y seguro
            //que comparar diferencias campo a campo.
            for (var pExistente : daoProducto.buscarPorVendedor(u.getID())) {
                if (u.getProductoFromEnVenta(pExistente.getID()) == null) {
                    daoProducto.eliminar(pExistente.getID());
                }
            }
            for (var p : u.getEnVenta()) {
                if (daoProducto.buscarPorVendedor(u.getID()).stream().anyMatch(x -> x.getID().equals(p.getID()))) {
                    daoProducto.actualizar(p);
                } else {
                    daoProducto.insertar(p, u.getID());
                }
            }

            //Sincronización de valoraciones pendientes (se reemplazan todas)
            guardarValoracionesPendientes(u);

            return true;
        } catch (Exception e) {
            System.out.println("[DaoUsuarioSQL] Error al actualizar usuario " + u.getID() + ": " + e.getMessage());
            return false;
        }
    }

    //endregion

    //region ELIMINAR

    @Override
    public boolean eliminar(String ID_Usuario) {
        String sql = "DELETE FROM usuarios WHERE id=?";
        try {
            Connection conn = conectar();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, ID_Usuario);
                ps.executeUpdate();
                //Los productos e interesados de este usuario se eliminan en
                //cascada gracias al ON DELETE CASCADE definido en el esquema.
                //Los tratos NO se tocan: quedan como historial.
                return true;
            }
        } catch (SQLException e) {
            System.out.println("[DaoUsuarioSQL] Error al eliminar usuario " + ID_Usuario + ": " + e.getMessage());
            return false;
        }
    }

    //endregion

    //region BÚSQUEDAS

    @Override
    public Usuario buscarPorID(String ID_Usuario) {
        String sql = "SELECT * FROM usuarios WHERE id=?";
        try {
            Connection conn = conectar();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, ID_Usuario);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return construirUsuarioCompleto(rs);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[DaoUsuarioSQL] Error al buscar usuario " + ID_Usuario + ": " + e.getMessage());
        }
        return null;
    }

    @Override
    public ArrayList<Usuario> buscarTodos() {
        ArrayList<Usuario> usuarios = new ArrayList<>();
        String sql = "SELECT * FROM usuarios";
        try {
            Connection conn = conectar();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    usuarios.add(construirUsuarioCompleto(rs));
                }
            }
        } catch (Exception e) {
            System.out.println("[DaoUsuarioSQL] Error al recuperar todos los usuarios: " + e.getMessage());
        }
        return usuarios;
    }

    //endregion

    //region MÉTODOS AUXILIARES

    /**
     * Reconstruye un Usuario completo a partir de una fila de la tabla "usuarios",
     * cargando además todo lo que depende de él: productos en venta, ventas,
     * compras y valoraciones pendientes.
     *
     * @param rs ResultSet posicionado en la fila del usuario a construir
     * @return el Usuario reconstruido con todos sus datos
     */
    private Usuario construirUsuarioCompleto(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String correo = rs.getString("correo");

        Usuario u = new Usuario(
                id,
                rs.getString("nombre"),
                rs.getString("apellidos"),
                correo,
                rs.getString("contrasenia"),
                rs.getInt("telefono")
        );

        //Productos en venta
        u.setEnVenta(daoProducto.buscarPorVendedor(id));

        //Ventas y compras (el mismo objeto Trato no se puede "compartir" tal
        //cual entre dos consultas distintas, pero el Controller solo necesita
        //que ambos lados tengan los DATOS correctos, no la misma referencia
        //en memoria; eso es solo relevante mientras la app vive en RAM antes de
        //volver a guardar).
        ArrayList<Trato> ventas = daoTrato.buscarPorVendedor(correo);
        ArrayList<Trato> compras = daoTrato.buscarPorComprador(correo);
        u.setVentas(ventas);
        u.setCompras(compras);

        //Valoraciones pendientes
        u.setValoracionesPendientes(cargarValoracionesPendientes(id));

        return u;
    }

    /**
     * Recupera la lista de IDs de tratos pendientes de valorar por un usuario.
     */
    private ArrayList<String> cargarValoracionesPendientes(String ID_Usuario) throws SQLException {
        ArrayList<String> pendientes = new ArrayList<>();
        String sql = "SELECT id_trato FROM valoraciones_pendientes WHERE id_usuario=?";
        Connection conn = conectar();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ID_Usuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pendientes.add(rs.getString("id_trato"));
                }
            }
        }
        return pendientes;
    }

    /**
     * Sincroniza la tabla valoraciones_pendientes con la lista actual del
     * objeto Usuario: borra todas las que tenía y vuelve a insertar las
     * vigentes. Es más simple que calcular diferencias, y la lista
     * suele ser pequeña.
     */
    private void guardarValoracionesPendientes(Usuario u) throws SQLException {
        Connection conn = conectar();
        try (PreparedStatement del = conn.prepareStatement("DELETE FROM valoraciones_pendientes WHERE id_usuario=?")) {
            del.setString(1, u.getID());
            del.executeUpdate();
        }
        String sqlIns = "INSERT INTO valoraciones_pendientes (id_usuario, id_trato) VALUES (?, ?)";
        try (PreparedStatement ins = conn.prepareStatement(sqlIns)) {
            for (String idTrato : u.getValoracionesPendientes()) {
                ins.setString(1, u.getID());
                ins.setString(2, idTrato);
                ins.executeUpdate();
            }
        }
    }

    /**
     * Abre (si hace falta) y devuelve la conexión compartida del DAOManager.
     * Cualquier error de conexión se envuelve en SQLException para no obligar
     * a todos los métodos de este DAO a declarar "throws Exception".
     */
    private Connection conectar() throws SQLException {
        try {
            DAOManager.getSinglentonInstance().open();
            return DAOManager.getSinglentonInstance().getConn();
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException("No se pudo conectar a la base de datos: " + e.getMessage(), e);
        }
    }

    //endregion
}