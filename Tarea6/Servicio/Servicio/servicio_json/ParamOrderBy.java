package servicio_json;

public class ParamOrderBy {
    int id_articulo;
    String nombre;
    float precio;
    int relevancia;

    public ParamOrderBy(int id_articulo, String nombre, float precio, int relevancia) {
        this.id_articulo = id_articulo;
        this.nombre = nombre;
        this.precio = precio;
        this.relevancia = relevancia;
    }

    // Getters y Setters
    public int getId_articulo() {
        return id_articulo;
    }

    public void setId_articulo(int id_articulo) {
        this.id_articulo = id_articulo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public float getPrecio() {
        return precio;
    }

    public void setPrecio(float precio) {
        this.precio = precio;
    }

    public int getRelevancia() {
        return relevancia;
    }

    public void setRelevancia(int relevancia) {
        this.relevancia = relevancia;
    }
}
