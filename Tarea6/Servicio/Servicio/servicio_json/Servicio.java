package servicio_json;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.QueryParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import java.sql.*;
import javax.sql.DataSource;
import javax.naming.Context;
import javax.naming.InitialContext;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.*;

@Path("ws")
public class Servicio {
    static DataSource pool = null;
    static {
        try {
            Context ctx = new InitialContext();
            pool = (DataSource) ctx.lookup("java:comp/env/jdbc/datasource_Servicio");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static Gson j = new GsonBuilder().registerTypeAdapter(byte[].class, new AdaptadorGsonBase64())
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").create();

    @POST
    @Path("captura_articulo")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response captura_articulo(String json) throws Exception {
        ParamCapturaArticulo p = (ParamCapturaArticulo) j.fromJson(json, ParamCapturaArticulo.class);
        Articulo articulo = p.articulo;

        Connection conexion = pool.getConnection();

        try {
            conexion.setAutoCommit(false);

            PreparedStatement stmt_1 = conexion.prepareStatement(
                    "INSERT INTO articulos(id_articulo,nombre,descripcion,precio,existencia,relevancia) VALUES (0,?,?,?,?,?)");

            try {
                stmt_1.setString(1, articulo.nombre);
                stmt_1.setString(2, articulo.descripcion);
                stmt_1.setFloat(3, (float) articulo.precio);
                stmt_1.setInt(4, (int) articulo.existencia);
                stmt_1.setInt(5, (int) articulo.relevancia);

                stmt_1.executeUpdate();
            } finally {
                stmt_1.close();
            }

            PreparedStatement stmt_2 = conexion.prepareStatement(
                    "INSERT INTO fotos_articulos(id_foto,foto,id_articulo) VALUES (0,?,LAST_INSERT_ID())");
            try {
                stmt_2.setBytes(1, articulo.foto);
                stmt_2.executeUpdate();
            } finally {
                stmt_2.close();
            }

            conexion.commit();
        } catch (Exception e) {
            conexion.rollback();
            return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
        } finally {
            conexion.setAutoCommit(true);
            conexion.close();
        }
        return Response.ok().build();
    }

    @POST
    @Path("busqueda_articulos")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response busqueda_articulos(String json) throws Exception {
        ParamBusquedaArticulo p = (ParamBusquedaArticulo) j.fromJson(json, ParamBusquedaArticulo.class);
        String palabraClave = p.palabraClave;
        int orden = p.orden; // el parámetro "orden" indica cómo se ordenarán los resultados de la consulta:
        // 1: por nombre
        // 2: de menor a mayor precio
        // 3: de mayor a menor precio
        // 4: por relevancia.

        Connection conexion = pool.getConnection();

        try {
            // notar que la instrucción SELECT no incluye ORDER BY
            // dependiendo del parámetro "orden" se deberá agregar el ORDER BY respectivo
            PreparedStatement stmt_1 = conexion.prepareStatement(
                    "SELECT a.id_articulo,a.nombre,a.descripcion,a.precio,a.existencia,b.foto FROM articulos a LEFT OUTER JOIN fotos_articulos b ON a.id_articulo=b.id_articulo WHERE a.nombre like ? or a.descripcion like ?");
            try {
                stmt_1.setString(1, "%" + palabraClave + "%");
                stmt_1.setString(2, "%" + palabraClave + "%");

                ResultSet rs = stmt_1.executeQuery();
                try {
                    ArrayList<ArticuloConsulta> lista = new ArrayList<ArticuloConsulta>();
                    while (rs.next()) {
                        ArticuloConsulta r = new ArticuloConsulta();
                        r.id_articulo = rs.getInt(1);
                        r.nombre = rs.getString(2);
                        r.descripcion = rs.getString(3);
                        r.precio = rs.getFloat(4);
                        r.existencia = rs.getInt(5);
                        r.foto = rs.getBytes(6);
                        lista.add(r);
                    }
                    return Response.ok().entity(j.toJson(lista)).build();
                } finally {
                    rs.close();
                }
            } finally {
                stmt_1.close();
            }
        } catch (Exception e) {
            return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
        } finally {
            conexion.close();
        }
    }

    @POST
    @Path("compra_articulo")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response compra_articulo(String json) {
        ParamCompraArticulo compra = j.fromJson(json, ParamCompraArticulo.class);
        Connection conexion = null;
        try {
            conexion = pool.getConnection();
            conexion.setAutoCommit(false);

            // Verificar la cantidad disponible del artículo
            PreparedStatement stmtVerif = conexion
                    .prepareStatement("SELECT existencia FROM articulos WHERE id_articulo = ?");
            stmtVerif.setInt(1, compra.getIdArticulo());
            ResultSet rs = stmtVerif.executeQuery();
            if (!rs.next()) {
                return Response.status(400).entity(j.toJson(new Error("El artículo no existe."))).build();
            }
            int existencia = rs.getInt(1);
            stmtVerif.close();

            if (compra.getCantidad() > existencia) {
                return Response.status(400)
                        .entity(j.toJson(new Error("No hay suficientes unidades disponibles del artículo."))).build();
            }

            // Insertar en la tabla de carrito_compra y actualizar la tabla de artículos
            // dentro de la misma transacción
            PreparedStatement stmtInsert = conexion
                    .prepareStatement("INSERT INTO carrito_compra (id_articulo, cantidad) VALUES (?, ?)");
            stmtInsert.setInt(1, compra.getIdArticulo());
            stmtInsert.setInt(2, compra.getCantidad());
            stmtInsert.executeUpdate();
            stmtInsert.close();

            PreparedStatement stmtUpdate = conexion
                    .prepareStatement("UPDATE articulos SET existencia = existencia - ? WHERE id_articulo = ?");
            stmtUpdate.setInt(1, compra.getCantidad());
            stmtUpdate.setInt(2, compra.getIdArticulo());
            stmtUpdate.executeUpdate();
            stmtUpdate.close();

            conexion.commit();
            return Response.ok().build();
        } catch (SQLException e) {
            if (conexion != null) {
                try {
                    conexion.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return Response.status(500).entity(j.toJson(new Error("Error interno del servidor."))).build();
        } finally {
            if (conexion != null) {
                try {
                    conexion.setAutoCommit(true);
                    conexion.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @POST
    @Path("ordenar_articulos")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response ordenar_articulos(String json) throws Exception {
        ParamOrdenaArticulo p = (ParamOrdenaArticulo) j.fromJson(json, ParamOrdenaArticulo.class);
        String columna;
        switch (p.orden) {
            case 1:
                columna = "a.nombre";
                break;
            case 2:
                columna = "a.precio";
                break;
            case 3:
                columna = "a.precio DESC";
                break;
            case 4:
                columna = "a.relevancia";
                break;
            default:
                columna = "a.id_articulo"; // Orden predeterminado
                break;
        }

        String consulta = "SELECT a.id_articulo, a.nombre, a.descripcion, a.precio, a.existencia, b.foto " +
                "FROM articulos a " +
                "LEFT OUTER JOIN fotos_articulos b ON a.id_articulo = b.id_articulo " +
                "ORDER BY " + columna;

        Connection conexion = pool.getConnection();

        try {
            PreparedStatement stmt_1 = conexion.prepareStatement(consulta);
            try {
                ResultSet rs = stmt_1.executeQuery();
                try {
                    ArrayList<ArticuloConsulta> lista = new ArrayList<ArticuloConsulta>();
                    while (rs.next()) {
                        ArticuloConsulta r = new ArticuloConsulta();
                        r.id_articulo = rs.getInt(1);
                        r.nombre = rs.getString(2);
                        r.descripcion = rs.getString(3);
                        r.precio = rs.getFloat(4);
                        r.existencia = rs.getInt(5);
                        r.foto = rs.getBytes(6);
                        lista.add(r);
                    }
                    return Response.ok().entity(j.toJson(lista)).build();
                } finally {
                    rs.close();
                }
            } finally {
                stmt_1.close();
            }
        } catch (Exception e) {
            return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
        } finally {
            conexion.close();
        }
    }

    @POST
    @Path("ver_carrito")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response ver_carrito() {
        Connection conexion = null;
        try {
            conexion = pool.getConnection();

            PreparedStatement stmt = conexion.prepareStatement(
                    "SELECT a.id_articulo, a.nombre, a.descripcion, a.precio, b.foto, c.cantidad " +
                            "FROM carrito_compra c " +
                            "JOIN articulos a ON c.id_articulo = a.id_articulo " +
                            "LEFT OUTER JOIN fotos_articulos b ON a.id_articulo = b.id_articulo");

            ResultSet rs = stmt.executeQuery();
            List<ArticuloCarrito> carrito = new ArrayList<>();
            while (rs.next()) {
                ArticuloCarrito item = new ArticuloCarrito();
                item.idArticulo = rs.getInt("id_articulo");
                item.nombre = rs.getString("nombre");
                item.descripcion = rs.getString("descripcion");
                item.precio = rs.getFloat("precio");
                item.cantidad = rs.getInt("cantidad");
                item.foto = rs.getBytes("foto");
                carrito.add(item);
            }
            rs.close();
            stmt.close();

            return Response.ok().entity(j.toJson(carrito)).build();
        } catch (SQLException e) {
            e.printStackTrace();
            return Response.status(500).entity(j.toJson(new Error("Error interno del servidor."))).build();
        } finally {
            if (conexion != null) {
                try {
                    conexion.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @POST
    @Path("eliminar_articulo_carrito")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response eliminar_articulo_carrito(String json) {
        ParamEliminarArticuloCarrito param = j.fromJson(json, ParamEliminarArticuloCarrito.class);
        int idArticulo = param.getIdArticulo();
        int cantidad = param.getCantidad();

        Connection conexion = null;
        try {
            conexion = pool.getConnection();
            conexion.setAutoCommit(false);

            // Verificar la cantidad en el carrito
            PreparedStatement stmtVerif = conexion
                    .prepareStatement("SELECT cantidad FROM carrito_compra WHERE id_articulo = ?");
            stmtVerif.setInt(1, idArticulo);
            ResultSet rs = stmtVerif.executeQuery();
            if (!rs.next()) {
                return Response.status(400).entity(j.toJson(new Error("El artículo no está en el carrito."))).build();
            }
            int cantidadEnCarrito = rs.getInt(1);
            stmtVerif.close();

            if (cantidad > cantidadEnCarrito) {
                return Response.status(400)
                        .entity(j.toJson(new Error("La cantidad a eliminar excede la cantidad en el carrito.")))
                        .build();
            }

            // Eliminar la cantidad especificada del carrito
            PreparedStatement stmtDelete = conexion
                    .prepareStatement("DELETE FROM carrito_compra WHERE id_articulo = ? AND cantidad = ?");
            stmtDelete.setInt(1, idArticulo);
            stmtDelete.setInt(2, cantidad);
            stmtDelete.executeUpdate();
            stmtDelete.close();

            // Sumar la cantidad eliminada a la cantidad disponible en los artículos
            PreparedStatement stmtUpdate = conexion
                    .prepareStatement("UPDATE articulos SET existencia = existencia + ? WHERE id_articulo = ?");
            stmtUpdate.setInt(1, cantidad);
            stmtUpdate.setInt(2, idArticulo);
            stmtUpdate.executeUpdate();
            stmtUpdate.close();

            conexion.commit();
            return Response.ok().build();
        } catch (SQLException e) {
            if (conexion != null) {
                try {
                    conexion.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return Response.status(500).entity(j.toJson(new Error("Error interno del servidor."))).build();
        } finally {
            if (conexion != null) {
                try {
                    conexion.setAutoCommit(true);
                    conexion.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}