package utils;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Utils {

    //ATRIBUTOS
    private static final Scanner SCANNER = new Scanner(System.in);


    //MÉTODOS
    public static void pulsaEnter() {
        System.out.print("Pulsa ENTER para continuar.");
        SCANNER.nextLine();
    }

    public static void limpiaPantalla() {
        for (int i = 0; i < 50; i++) {
            System.out.println();
        }
    }

    // Genera un String con carácteres aleatorios del String CARACTERES
    public static String generarClave(int longitud) {
        // Definimos todos los caracteres permitidos
        String CARACTERES = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder clave = new StringBuilder();

        for (int i = 0; i < longitud; i++) {
            // Elegimos una posición al azar dentro del String de caracteres
            int indiceAleatorio = (int) (Math.random() * CARACTERES.length());

            // Sacamos el carácter de esa posición y lo pegamos a nuestra clave
            clave.append(CARACTERES.charAt(indiceAleatorio));
        }

        return clave.toString();
    }

    // Pide un número entero entre un rango, maneja excepciones
    public static int pideDatoEntero(String mensaje, int min, int max) {
        // Usamos el SCANNER estático de la clase para no crear uno nuevo cada vez
        int numero = 0; //Inicializa fuera
        boolean esValido;

        do {
            esValido = false; // Reset de la bandera en cada intento de rango
            System.out.print("Introduzca " + mensaje);
            String teclado = SCANNER.nextLine();

            try {
                numero = Integer.parseInt(teclado);

                //Verifica si está en el rango
                if (numero >= min && numero <= max) {
                    esValido = true;
                } else {
                    System.out.println("Error: el número debe estar entre " + min + " y " + max + ".");
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: el valor introducido no es válido.");
            }

        } while (!esValido); //No saldrá hasta que sea un número Y esté en rango

        return numero;
    }

    // Igual que el anterior, pero double y sin rango máximo, solo checa que sea positivo.
    public static double pideDatoDouble(String mensaje) {
        double numero = 0;
        boolean esValido;

        do {
            esValido = false;
            System.out.print("Introduce " + mensaje);
            String teclado = SCANNER.nextLine();

            try {
                //Y AQUÍ LOGRÉ QUE ACEPTE COMAS Y PUNTOS
                String entradaNormalizada = teclado.replace(',', '.');

                numero = Double.parseDouble(entradaNormalizada);

                if (numero >= 0) {
                    esValido = true;
                } else {
                    System.out.println("Error: el valor no puede ser negativo.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: el valor introducido no es válido.");
            }

        } while (!esValido);

        return numero;
    }

    // Es como el sleep del Java pero más sencillo en su uso
    public static void esperar(int milisegundos) {
        try {
            TimeUnit.MILLISECONDS.sleep(milisegundos);
        } catch (InterruptedException e) {
            /* Esto es para restaurar el estado de interrupción
            Tengo entendido que si usamos esta función en un bucle
            y no usa, podemos hacerlos infinitos si salta la excepción
            No sé muy bien cómo, pero sé que no funciona si no se pone
            Regla del programador noº 1: si funciona, no lo toques*/
            Thread.currentThread().interrupt();
        }
    }

    //Simula un cierre de sesión, de programa o lo que quieras, OJO, SÓLO SIMULA, no cierra nada
    public static void simularCierre(String mensaje) {
        // Se pone el mensaje
        System.out.print(mensaje);

        //Se ponen los puntos
        for (int i = 0; i < 3; i++) {
            esperar(600);
            System.out.print(".");
        }
    }

    //Valida los correos electrónicos
    public static boolean emailValido(String email) {
        if (!email.contains("@") || !email.contains(".")) return false; //Esto es para evitar cualquier cosa que no contenga "@" y "."
        if (email.startsWith("@") || email.startsWith(".")) return false; //Pero que no empiece con "@" o "."
        if (email.endsWith("@") || email.endsWith(".")) return false; //Tampoco que acaben con "@" o "."
        return email.charAt(email.indexOf("@") + 1) != '.'; //Y que no estén juntos (no vale poner correo@.com)
    }

    //Devuelve la contraseña en asteriscos
    public static String contraseniaOculta(String contrasenia) {
        return "*".repeat(contrasenia.length());
    }

    /**
     * Redondea un double a exactamente 2 decimales.
     * Se usa al crear o editar precios para que nunca se almacenen más de 2 decimales.
     * Ejemplo: 12.3456 → 12.35
     *
     * @param valor el precio con posibles decimales de más
     * @return el mismo valor redondeado a 2 decimales
     */
    public static double redondearDosDecimales(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
