-- Tabla Articulo
CREATE TABLE articulos (
    id_articulo INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    descripcion TEXT,
    precio FLOAT NOT NULL,
    existencia INT NOT NULL,
    relevancia INT NOT NULL
);

-- Tabla Foto_Articulo
CREATE TABLE fotos_articulos (
    id_foto INT AUTO_INCREMENT PRIMARY KEY,
    foto LONGBLOB,
    id_articulo INT,
    FOREIGN KEY (id_articulo) REFERENCES articulos(id_articulo)
);

-- Tabla Carrito_Compra
CREATE TABLE carrito_compra (
    id_carrito INT AUTO_INCREMENT PRIMARY KEY,
    id_articulo INT,
    cantidad INT NOT NULL,
    FOREIGN KEY (id_articulo) REFERENCES articulos(id_articulo)
);