package DAO;

import models.Producto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * DaoProductoSQL – Implementación de DaoProducto contra una base de datos SQL,
 * usando JDBC.
 *
 * La lista de interesados de cada producto se guarda en la tabla "interesados"
 * (relación N:M entre producto y correo electrónico).
 */
public class DaoProductoSQL implements DaoProducto {

    //region INSERTAR

    @Override
    public boolean insertar(Producto p, String ID_Vendedor) {
        String sql = "INSERT INTO productos (id, nombre, descripcion, precio, estado, id_vendedor) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            Connection conn = conectar();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getID());
                ps.setString(2, p.getNombre());
                ps.setString(3, p.getDescripcion());
                ps.setDouble(4, p.getPrecio());
                ps.setString(5, p.getEstado());
                ps.setString(6, ID_Vendedor);
                ps.executeUpdate();
            }
            guardarInteresados(p);
            return true;
        } catch (Exception e) {
            System.out.println("[DaoProductoSQL] Error al insertar producto " + p.getID() + ": " + e.getMessage());
            return false;
        }
    }

    //endregion

    //region ACTUALIZAR

    @Override
    public boolean actualizar(Producto p) {
        String sql = "UPDATE productos SET nombre=?, descripcion=?, precio=?, estado=? WHERE id=?";
        try {
            Connection conn = conectar();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getNombre());
                ps.setString(2, p.getDescripcion());
                ps.setDouble(3, p.getPrecio());
                ps.setString(4, p.getEstado());
                ps.setString(5, p.getID());
                ps.executeUpdate();
            }
            guardarInteresados(p);
            return true;
        } catch (Exception e) {
            System.out.println("[DaoProductoSQL] Error al actualizar producto " + p.getID() + ": " + e.getMessage());
            return false;
        }
    }

    //endregion

    //region ELIMINAR

    @Override
    public boolean eliminar(String ID_Producto) {
        String sql = "DELETE FROM productos WHERE id=?";
        try {
            Connection conn = conectar();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, ID_Producto);
                ps.executeUpdate();
                //Los interesados de este producto se eliminan en cascada
                //gracias al ON DELETE CASCADE definido en el esquema.
                return true;
            }
        } catch (SQLException e) {
            System.out.println("[DaoProductoSQL] Error al eliminar producto " + ID_Producto + ": " + e.getMessage());
            return false;
        }
    }

    //endregion

    //region BÚSQUEDAS

    @Override
    public ArrayList<Producto> buscarPorVendedor(String ID_Vendedor) {
        ArrayList<Producto> productos = new ArrayList<>();
        String sql = "SELECT * FROM productos WHERE id_vendedor=?";
        try {
            Connection conn = conectar();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, ID_Vendedor);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        productos.add(construirProducto(rs));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[DaoProductoSQL] Error al buscar productos del vendedor " + ID_Vendedor + ": " + e.getMessage());
        }
        return productos;
    }

    @Override
    public ArrayList<Producto> buscarTodos() {
        ArrayList<Producto> productos = new ArrayList<>();
        String sql = "SELECT * FROM productos";
        try {
            Connection conn = conectar();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    productos.add(construirProducto(rs));
                }
            }
        } catch (Exception e) {
            System.out.println("[DaoProductoSQL] Error al recuperar todos los productos: " + e.getMessage());
        }
        return productos;
    }

    //endregion

    //region MÉTODOS AUXILIARES

    /**
     * Reconstruye un Producto a partir de una fila de la tabla "productos",
     * cargando también su lista de interesados.
     */
    private Producto construirProducto(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        Producto p = new Producto(
                id,
                rs.getString("nombre"),
                rs.getString("descripcion"),
                rs.getDouble("precio"),
                rs.getString("estado")
        );
        p.setInteresados(cargarInteresados(id));
        return p;
    }

    /**
     * Recupera la lista de correos interesados en un producto.
     */
    private ArrayList<String> cargarInteresados(String ID_Producto) throws SQLException {
        ArrayList<String> interesados = new ArrayList<>();
        String sql = "SELECT correo_interesado FROM interesados WHERE id_producto=?";
        Connection conn = conectar();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ID_Producto);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    interesados.add(rs.getString("correo_interesado"));
                }
            }
        }
        return interesados;
    }

    /**
     * Sincroniza la tabla "interesados" con la lista actual del producto:
     * borra todos los que tenía y vuelve a insertar los vigentes.
     */
    private void guardarInteresados(Producto p) throws SQLException {
        Connection conn = conectar();
        try (PreparedStatement del = conn.prepareStatement("DELETE FROM interesados WHERE id_producto=?")) {
            del.setString(1, p.getID());
            del.executeUpdate();
        }
        String sqlIns = "INSERT INTO interesados (id_producto, correo_interesado) VALUES (?, ?)";
        try (PreparedStatement ins = conn.prepareStatement(sqlIns)) {
            for (String correo : p.getInteresados()) {
                ins.setString(1, p.getID());
                ins.setString(2, correo);
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