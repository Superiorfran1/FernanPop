<div align="center">

<img src="https://raw.githubusercontent.com/Superiorfran1/FernanPop/main/imagenes/ChatGPT_Image_Jun_27__2026__12_28_08_AM-removebg-preview%281%29.png" alt="FernanPop" width="140">

# FernanPop

**El mercadillo de segunda mano del instituto.**
Compra y vende entre compañeros. Rápido, cercano y sin comisiones.

![Java](https://img.shields.io/badge/Java-15%2B-orange)
![Jakarta EE](https://img.shields.io/badge/Jakarta%20Servlet-6.1-blue)
![MySQL](https://img.shields.io/badge/MySQL-8-4479A1)
![Maven](https://img.shields.io/badge/Maven-WAR-C71A36)

</div>

---

## Índice

1. [¿Qué es FernanPop?](#qué-es-fernanpop)
2. [Funcionalidades](#funcionalidades)
3. [Tecnologías](#tecnologías)
4. [Estructura del proyecto](#estructura-del-proyecto)
5. [Arquitectura](#arquitectura)
6. [Modelo de datos](#modelo-de-datos)
7. [Páginas de la aplicación web](#páginas-de-la-aplicación-web)
8. [Puesta en marcha](#puesta-en-marcha)
9. [Configuración (`config.properties`)](#configuración-configproperties)
10. [Administración](#administración)
11. [Notificaciones y documentos generados](#notificaciones-y-documentos-generados)
12. [Flujo de una venta](#flujo-de-una-venta)
13. [Notas de seguridad y mejoras pendientes](#notas-de-seguridad-y-mejoras-pendientes)
14. [Autor](#autor)

---

## ¿Qué es FernanPop?

FernanPop es una aplicación de compraventa de artículos de segunda mano pensada para una comunidad cerrada (un instituto). Los usuarios se registran con verificación por correo, publican productos, muestran interés en los de otros y cierran ventas; después, el comprador valora al vendedor.

El proyecto nació como aplicación de **consola** en Java y evolucionó a una **aplicación web** (JSP sobre Tomcat) que reutiliza la misma capa de negocio (`Controller`, modelos y DAO). Por eso en el código conviven las dos vistas: `views/Main.java` (menús de consola) y las páginas `.jsp`.

## Funcionalidades

**Usuarios**
- Registro en dos pasos con **código de verificación de 6 caracteres enviado por correo**.
- Inicio y cierre de sesión, con registro de la **última conexión** de cada usuario.
- Edición de perfil: nombre, apellidos, contraseña y teléfono (móvil de 9 dígitos que empiece por 6 o 7).
- **Eliminación de cuenta** protegida: hay que escribir `BORRAR` y confirmar con un código enviado por correo que caduca a los 10 minutos.

**Productos**
- Publicar productos con nombre, descripción, precio y estado (*Nuevo, Como nuevo, Buen estado, Usado*).
- Editar o eliminar los propios productos.
- **Buscador en vivo** por nombre y descripción (con *debounce* y cancelación de peticiones), ordenado por precio.
- Botón **«Me interesa»**: se registra el interés y se avisa por correo al vendedor.

**Tratos y valoraciones**
- El vendedor cierra la venta eligiendo entre los interesados.
- Se genera un **recibo en PDF** y se envían correos de confirmación al vendedor y al comprador.
- El comprador valora la compra de 0 a 5 estrellas con comentario opcional.
- Historial de ventas y compras con su valoración, y nota media del vendedor.

**Administración**
- Visualización de la configuración cargada y de las últimas conexiones de todos los usuarios.
- **Exportación del listado de productos a CSV** enviada por correo.
- **Copia de seguridad y restauración** completa de la base de datos.

**Extras**
- Modo claro/oscuro con persistencia y respeto a la preferencia del sistema.
- Registro de actividad en `log.txt` (sesiones, nuevos productos, ventas).
- Avisos por **Telegram** al administrador (altas, cambios, ventas...).
- Datos de prueba automáticos si la base de datos está vacía.

## Tecnologías

| Capa | Tecnología |
|---|---|
| Lenguaje | Java (compilación con `source/target 15`; se recomienda **JDK 17+**) |
| Web | JSP + Jakarta Servlet 6.1 (Tomcat 10.1 / 11) |
| Build | Maven (con *wrapper* `mvnw`), empaquetado **WAR** |
| Base de datos | MySQL 8 vía JDBC (`mysql-connector-java 8.0.33`) |
| Correo | JavaMail 1.6.2 (SMTP de Gmail) |
| PDF | Apache PDFBox 3.0.7 |
| Notificaciones | Telegram Bot API |
| Tests | JUnit Jupiter 5.13 (dependencias declaradas) |
| Front | HTML + CSS + JS propio, sin frameworks |

## Estructura del proyecto

```
FernanPop/
├── pom.xml
├── mvnw / mvnw.cmd
├── FernanPop/
│   └── data/
│       ├── config.properties      # Configuración de la aplicación
│       ├── log.txt                # Registro de actividad
│       └── bins/                  # Restos del almacenamiento antiguo en .bin
└── src/main/
    ├── java/
    │   ├── controller/Controller.java     # Lógica de negocio
    │   ├── models/                        # Usuario, Producto, Trato
    │   ├── DAO/                           # Acceso a datos (JDBC)
    │   │   ├── DAOManager.java            #   conexión + creación de tablas
    │   │   ├── DaoUsuario[SQL].java
    │   │   ├── DaoProducto[SQL].java
    │   │   └── DaoTrato[SQL].java
    │   ├── persistence/
    │   │   ├── AppConfig.java             # Lectura/escritura de config.properties
    │   │   ├── Persistence.java           # Backup y restauración de la BBDD
    │   │   └── Log.java                   # Registro en log.txt
    │   ├── utils/
    │   │   ├── Communications.java        # Gmail, Telegram, PDF y CSV
    │   │   ├── UI.java                    # Menús de consola y plantillas HTML de correo
    │   │   └── Utils.java                 # Utilidades (validación, entrada, IDs...)
    │   ├── data/Testing.java              # Datos de prueba
    │   └── views/Main.java                # Vista de consola
    └── webapp/
        ├── index.jsp                      # Portada
        ├── login.jsp / registro.jsp
        ├── dashboard.jsp                  # Panel del usuario
        ├── buscar.jsp                     # Buscador y «Me interesa»
        ├── productos-nuevo.jsp / mis-productos.jsp
        ├── historial.jsp / valoraciones.jsp
        ├── perfil.jsp
        ├── admin.jsp                      # Solo administrador
        └── WEB-INF/web.xml
```

## Arquitectura

```
   JSP (webapp)  ─┐
                  ├──►  Controller  ──►  DAO (JDBC)  ──►  MySQL
 Main (consola)  ─┘         │
                            ├──►  models (Usuario · Producto · Trato)
                            └──►  utils/Communications ──► Gmail · Telegram · PDF · CSV
```

- **Vistas:** cada JSP gestiona su propio formulario (patrón *post-and-render*) y delega en el `Controller`.
- **`Controller`:** contiene la lógica de negocio y mantiene una **caché en memoria** de los usuarios (con sus productos, ventas, compras y valoraciones pendientes). La base de datos es siempre la fuente de verdad: cada alta, baja o modificación se reescribe en el DAO correspondiente.
- **Sesión web:** cada sesión HTTP guarda su propio `Controller` (`session.getAttribute("app")`) y el usuario activo (`usuarioActivo`).
- **DAO:** patrón DAO con interfaces y su implementación SQL. `DAOManager` es un *singleton* que abre la conexión y **crea las tablas automáticamente** si no existen (`CREATE TABLE IF NOT EXISTS`).
- **IDs legibles:** `U00001` (usuarios), `P10001` (productos), `T00001` (tratos), calculados al arrancar a partir del máximo existente.

## Modelo de datos

El esquema se crea solo al primer arranque; no hace falta ningún script manual.

| Tabla | Descripción | Claves |
|---|---|---|
| `usuarios` | Datos personales y credenciales | PK `id`; `correo` único |
| `productos` | Productos actualmente en venta | PK `id`; FK `id_vendedor → usuarios` (`ON DELETE CASCADE`) |
| `interesados` | Usuarios interesados en un producto | PK `(id_producto, correo_interesado)`; FK → `productos` |
| `tratos` | Ventas cerradas (con copia de los datos del producto) | PK `id`; `puntuacion = -1` significa «sin valorar» |
| `valoraciones_pendientes` | Compras que el comprador aún no ha valorado | PK `(id_usuario, id_trato)`; FKs → `usuarios`, `tratos` |

> Los datos del producto se copian dentro de `tratos` a propósito: así el historial se conserva aunque el producto desaparezca de `productos` al venderse.

## Páginas de la aplicación web

| Página | Acceso | Descripción |
|---|---|---|
| `index.jsp` | Público | Portada. Muestra aviso al eliminar la cuenta (`?cuentaEliminada=1`). |
| `login.jsp` | Público | Inicio de sesión. |
| `registro.jsp` | Público | Alta de cuenta en dos pasos (datos → código por correo). |
| `buscar.jsp` | Público* | Buscador en vivo y «Me interesa» (requiere sesión para interesarse). |
| `dashboard.jsp` | Usuario | Resumen: productos en venta, ventas, compras, valoraciones pendientes. |
| `productos-nuevo.jsp` | Usuario | Publicar un producto. |
| `mis-productos.jsp` | Usuario | Editar, eliminar o cerrar la venta de un producto. |
| `historial.jsp` | Usuario | Ventas y compras con sus valoraciones. |
| `valoraciones.jsp` | Usuario | Valorar compras pendientes (0–5 estrellas). |
| `perfil.jsp` | Usuario | Editar datos y eliminar cuenta. |
| `admin.jsp` | Administrador | Configuración, exportar CSV, backup y restauración. |

\* `buscar.jsp` no consulta actualmente la opción `acceso.invitado`, por lo que el listado es visible sin iniciar sesión.

## Puesta en marcha

### Requisitos

- **JDK 17 o superior**
- **Maven** (o usar el `mvnw` incluido)
- **MySQL 8** (local o en la nube: Clever Cloud, Railway...)
- **Apache Tomcat 10.1 o 11** (Jakarta EE; Tomcat 9 o anterior **no** sirve porque usa `jakarta.*`)
- Cuenta de **Gmail con contraseña de aplicación** (para los correos) y, opcionalmente, un **bot de Telegram**

### 1. Crear la base de datos

Basta con crear una base de datos vacía; las tablas las crea la aplicación:

```sql
CREATE DATABASE fernanpop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Configurar la aplicación

Edita `FernanPop/data/config.properties` (ver [la tabla de claves](#configuración-configproperties)) con los datos de tu base de datos y de tu administrador. Si el fichero no existe, la aplicación lo crea con valores por defecto apuntando a un MySQL local.

### 3. Configurar correo y Telegram

Las credenciales de Gmail y el bot de Telegram están en `utils/Communications.java`. Sustitúyelas por las tuyas antes de desplegar (ver [notas de seguridad](#notas-de-seguridad-y-mejoras-pendientes)).

### 4. Compilar

```bash
# Linux / macOS
./mvnw clean package

# Windows
mvnw.cmd clean package
```

Se genera `target/FernanPop-1.0-SNAPSHOT.war`.

### 5. Desplegar en Tomcat

Copia el WAR en la carpeta `webapps/` de Tomcat (o despliégalo desde IntelliJ IDEA con una configuración de Tomcat) y arranca el servidor. La aplicación quedará disponible en:

```
http://localhost:8080/FernanPop-1.0-SNAPSHOT/
```

Al arrancar por primera vez con la base de datos vacía se insertan **usuarios y productos de prueba** (`Testing.mock`).

> **Ubicación de `config.properties`:** `AppConfig` lo busca, por orden, en una ruta absoluta del equipo del autor, en `<directorio de trabajo>/FernanPop/data/` y en `<directorio de trabajo>/data/`. Si lo despliegas en otra máquina, coloca la carpeta `FernanPop/data/` en el directorio de trabajo de Tomcat (o ajusta `resolverRutaConfig()`).

## Configuración (`config.properties`)

| Clave | Descripción | Ejemplo |
|---|---|---|
| `db.url` | URL JDBC de MySQL | `jdbc:mysql://localhost:3306/fernanpop?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true` |
| `db.user` | Usuario de la base de datos | `root` |
| `db.pass` | Contraseña de la base de datos | *(la tuya)* |
| `admin.correoElectronico` | Correo de la cuenta administradora | `admin@tudominio.es` |
| `admin.contrasenia` | Contraseña del administrador (versión consola) | *(la tuya)* |
| `acceso.invitado` | Permite ver productos sin sesión (versión consola) | `true` |
| `pagina.tamano` | Elementos por página (versión consola) | `5` |
| `ruta.datos` | Carpeta de datos | `FernanPop/data/bins` |
| `ruta.log` | Carpeta donde se escribe `log.txt` | `FernanPop/data` |
| `ultimo.login.<correo>` | Última conexión de cada usuario (la escribe la propia app) | — |

## Administración

El administrador es **el usuario cuyo correo coincide con `admin.correoElectronico`**. Para tener acceso al panel web:

1. Regístrate normalmente en `registro.jsp` usando ese correo.
2. Inicia sesión: en el panel aparecerá el banner *«Sesión de administrador · Ir al panel»*.

Desde `admin.jsp` puedes:

- **Ver la configuración** cargada (las contraseñas se muestran ocultas) y las **últimas conexiones** de todos los usuarios.
- **Enviar el CSV de productos** a tu correo.
- **Descargar una copia de seguridad** (`fernanpop-backup.dat`) con todos los usuarios y lo que llevan dentro.
- **Restaurar una copia**: subes el `.dat` y escribes `RESTAURAR`. ⚠️ Borra **todos** los datos actuales y no se puede deshacer.

## Notificaciones y documentos generados

| Evento | Canal | Contenido |
|---|---|---|
| Registro | Correo | Código de verificación |
| Alguien se interesa en un producto | Correo al vendedor | Datos del interesado |
| Producto editado con interesados | Correo a los interesados | Aviso de modificación (y se reinicia la lista de interesados) |
| Venta cerrada | Correo a vendedor y comprador | Confirmación + **recibo PDF** adjunto |
| Eliminación de cuenta | Correo | Código de seguridad (caduca en 10 min) |
| Altas, cambios de datos, productos, ventas | Telegram | Aviso al administrador |
| Exportación | Correo al admin | Fichero **CSV** de productos |
| Sesiones, nuevos productos, ventas | `log.txt` | `"Evento";datos;dd/MM/yyyy HH:mm:ss` |

Los correos usan plantillas HTML propias (`UI.java`) con la identidad visual de la aplicación.

## Flujo de una venta

```
Vendedor publica producto
        │
        ▼
Compradores pulsan «Me interesa»  ──►  correo al vendedor
        │
        ▼
Vendedor elige comprador entre los interesados (Cerrar venta)
        │
        ├─► Se crea UN trato compartido (ventas del vendedor + compras del comprador)
        ├─► El producto sale de «en venta»  (sus datos se conservan en el trato)
        ├─► Se añade una valoración pendiente al comprador
        └─► Recibo PDF + correos de confirmación + aviso por Telegram
        │
        ▼
Comprador valora de 0 a 5 estrellas  ──►  afecta a la nota media del vendedor
```

## Notas de seguridad y mejoras pendientes

Este es un proyecto educativo; antes de exponerlo públicamente conviene abordar lo siguiente:

**Seguridad (prioritario)**
- 🔴 **Credenciales en el código y en el repositorio.** `Communications.java` contiene la contraseña de aplicación de Gmail y el *token* del bot de Telegram escritos a fuego, y `config.properties` guarda credenciales de la base de datos y del administrador. Si el repositorio es o ha sido público, **revoca y regenera esas credenciales** y muévelas a variables de entorno o a un fichero fuera del control de versiones (añade `FernanPop/data/config.properties` al `.gitignore`).
- 🔴 **Contraseñas en texto plano** en la base de datos. Deberían guardarse con un *hash* con sal (BCrypt o Argon2).
- 🟠 El mensaje de Telegram de alta de usuario incluye la contraseña (`UI.msgNuevoUsuario`); conviene eliminarla.
- 🟠 No hay protección CSRF en los formularios.

**Errores conocidos**
- `buscar.jsp`: el modo fragmento (búsqueda en vivo) usa `System.out.print(...)`; debería ser `out.print(...)` para escribir en la respuesta HTTP.
- `admin.jsp` usa `request.getPart(...)` para restaurar copias, pero `web.xml` no declara `<multipart-config>`; en Tomcat esto puede provocar un error al subir el fichero.
- `AppConfig.resolverRutaConfig()` incluye una ruta absoluta de Windows del equipo del autor.

**Limpieza y mantenimiento**
- `views/Main.java` tiene el `main` comentado, así que la versión de consola no se puede arrancar tal cual.
- El `pom.xml` declara Java 8 en las propiedades y 15 en el `maven-compiler-plugin`; unifica las versiones.
- Hay JARs duplicados en `src/main/java/libraries/` (ya gestionados por Maven) y una carpeta `bins/` heredada del almacenamiento antiguo.
- `src/main/webapp/WEB-INF/index.jsp` no es accesible desde el navegador; la portada real es `webapp/index.jsp`.
- La documentación de `Communications` menciona Apache POI/Excel, pero la exportación actual es CSV.
- Faltan tests: JUnit está declarado como dependencia, pero no hay clases de prueba.

## Autor

Hecho por **Francisco Cantero Maestro**.
