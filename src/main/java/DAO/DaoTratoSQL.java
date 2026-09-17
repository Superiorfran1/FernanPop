package DAO;

import models.Producto;
import models.Trato;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * DaoTratoSQL – Implementación de DaoTrato contra una base de datos SQL,
 * usando JDBC.
 *
 * Los datos del producto vendido se guardan "desnormalizados" directamente
 * en la fila del trato (nombre_producto, descripcion_producto, estado_producto),
 * ya que el Producto original se borra de la tabla "productos" en cuanto
 * se cierra la venta (deja de estar en venta), pero el Trato necesita
 * conservar esa información para el historial.
 */
public class DaoTratoSQL implements DaoTrato {

    //region INSERTAR

    @Override
    public boolean insertar(Trato t) {
        String sql = """
            INSERT INTO tratos
                (id, correo_comprador, correo_vendedor, id_producto, nombre_producto,
                 descripcion_producto, estado_producto, fecha, precio, comentario, puntuacion)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try {
            Connection conn = conectar();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                Producto p = t.getProducto();
                ps.setString(1, t.getID());
                ps.setString(2, t.getCorreoComprador());
                ps.setString(3, t.getCorreoVendedor());
                ps.setString(4, p.getID());
                ps.setString(5, p.getNombre());
                ps.setString(6, p.getDescripcion());
                ps.setString(7, p.getEstado());
                ps.setTimestamp(8, new Timestamp(t.getFecha().getTimeInMillis()));
                ps.setDouble(9, t.getPrecio());
                ps.setString(10, t.getComentario());
                ps.setInt(11, t.getPuntuacion());
                ps.executeUpdate();
            }
            return true;
        } catch (Exception e) {
            System.out.println("[DaoTratoSQL] Error al insertar trato " + t.getID() + ": " + e.getMessage());
            return false;
        }
    }

    //endregion

    //region ACTUALIZAR

    @Override
    public boolean actualizar(Trato t) {
        String sql = "UPDATE tratos SET comentario=?, puntuacion=? WHERE id=?";
        try {
            Connection conn = conectar();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, t.getComentario());
                ps.setInt(2, t.getPuntuacion());
                ps.setString(3, t.getID());
                ps.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            System.out.println("[DaoTratoSQL] Error al actualizar trato " + t.getID() + ": " + e.getMessage());
            return false;
        }
    }

    //endregion

    //region BÚSQUEDAS

    @Override
    public Trato buscarPorID(String ID_Trato) {
        String sql = "SELECT * FROM tratos WHERE id=?";
        try {
            Connection conn = conectar();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, ID_Trato);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return construirTrato(rs);
                }
            }
        } catch (Exception e) {
            System.out.println("[DaoTratoSQL] Error al buscar trato " + ID_Trato + ": " + e.getMessage());
        }
        return null;
    }

    @Override
    public ArrayList<Trato> buscarPorVendedor(String correoVendedor) {
        return buscarPorCampo("correo_vendedor", correoVendedor);
    }

    @Override
    public ArrayList<Trato> buscarPorComprador(String correoComprador) {
        return buscarPorCampo("correo_comprador", correoComprador);
    }

    //endregion

    //region MÉTODOS AUXILIARES

    /**
     * Devuelve los tratos donde el campo indicado (correo_vendedor o
     * correo_comprador) coincide con el correo dado. Evita duplicar el
     * mismo código de consulta dos veces.
     */
    private ArrayList<Trato> buscarPorCampo(String nombreCampo, String correo) {
        ArrayList<Trato> tratos = new ArrayList<>();
        String sql = "SELECT * FROM tratos WHERE " + nombreCampo + "=?";
        try {
            Connection conn = conectar();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, correo);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        tratos.add(construirTrato(rs));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[DaoTratoSQL] Error al buscar tratos por " + nombreCampo + ": " + e.getMessage());
        }
        return tratos;
    }

    /**
     * Reconstruye un Trato a partir de una fila de la tabla "tratos",
     * recreando también el Producto desnormalizado que contiene.
     */
    private Trato construirTrato(ResultSet rs) throws SQLException {
        Producto p = new Producto(
                rs.getString("id_producto"),
                rs.getString("nombre_producto"),
                rs.getString("descripcion_producto"),
                rs.getDouble("precio"), //El precio de venta final es el mismo que el del trato
                rs.getString("estado_producto")
        );

        Calendar fecha = Calendar.getInstance();
        fecha.setTimeInMillis(rs.getTimestamp("fecha").getTime());

        Trato t = new Trato(
                rs.getString("id"),
                rs.getString("correo_comprador"),
                rs.getString("correo_vendedor"),
                p,
                fecha,
                rs.getDouble("precio")
        );
        t.setComentario(rs.getString("comentario"));
        t.setPuntuacion(rs.getInt("puntuacion"));
        return t;
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