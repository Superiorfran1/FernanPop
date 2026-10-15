package data;

import controller.Controller;
import models.Producto;
import models.Usuario;

import java.util.ArrayList;

public class Testing {

    public static void mock(Controller app) {
        //Usuarios
        Usuario u1 = new Usuario("U00001", "Mihai Iosif",    "Koritar",   "mihaiiosifkoritar@gmail.com",   "Mihai_2101", 658658658);
        Usuario u2 = new Usuario("U00002", "Francisco",   "Cantero Maestro", "franciscocm1000@gmail.com",  "fcm1234", 623456789);


        //listas internas
        u1.setEnVenta(new ArrayList<>());
        u1.setVentas(new ArrayList<>());
        u1.setCompras(new ArrayList<>());
        u1.setValoracionesPendientes(new ArrayList<>());

        u2.setEnVenta(new ArrayList<>());
        u2.setVentas(new ArrayList<>());
        u2.setCompras(new ArrayList<>());
        u2.setValoracionesPendientes(new ArrayList<>());

        app.addUsuario(u1);
        app.addUsuario(u2);

        //Productos
        app.addProducto(u1, new Producto("P10001", "Bicicleta de montaña", "Poco uso, perfecta.", 150.00, "Nuevo"));
        app.addProducto(u1, new Producto("P10002", "Teclado mecánico", "Switch Red, retroiluminado.", 60.00, "Usado"));
        app.addProducto(u1, new Producto("P10007", "Portátil gaming ASUS", "RTX 3060, i7 11ª generación, 16GB RAM, SSD 512GB. Excelente estado, con caja original y accesorios.", 750.00, "Excelente"));
        app.addProducto(u1, new Producto("P10008", "Cámara Nikon D3500", "18-55mm, sensor DX 24MP, ideal para principiantes. Incluye mochila y protector de lentes.", 450.00, "Usado"));
        app.addProducto(u1, new Producto("P10009", "Micrófono Rode NT1", "Profesional para streaming, incluye soporte ajustable y cable XLR de 5 metros.", 85.00, "Nuevo"));

        app.addProducto(u2, new Producto("P10003", "Monitor 24 pulgadas", "Full HD, HDMI.", 90.00, "Usado"));
        app.addProducto(u2, new Producto("P10004", "Auriculares Bluetooth", "Sin estrenar.", 35.00, "Nuevo"));
        app.addProducto(u2, new Producto("P10010", "Escritorio gaming 150cm", "Estructura metálica negra, superficie laminada resistente, capacidad 50kg. Perfecto para setup completo.", 120.00, "Excelente"));
        app.addProducto(u2, new Producto("P10011", "Impresora Canon PIXMA", "Multifunción, imprime, escanea y copia. WiFi integrado, cartuchos casi nuevos incluidos.", 110.00, "Usado"));
        app.addProducto(u2, new Producto("P10012", "Webcam Logitech 4K", "Ultra HD 4K, con micrófono incorporado, perfecto para videoconferencias profesionales.", 95.00, "Nuevo"));

        //Se añaden al ArrayList de usuarios
        app.addUsuario(u1);
        app.addUsuario(u2);
        app.recalcularContadoresID();
    }
}
