# Documentación del Proyecto: Microservicio de Entregas (Versión Intermedia)

## 1. Resumen del Proyecto

Este proyecto implementa una solución full-stack para la gestión de entregas. A pesar de que la configuración inicial y el nombre del repositorio (`ms-usuarios`) pudieran sugerir un enfoque en usuarios, la aplicación se centra completamente en la administración del ciclo de vida de los paquetes o "Entregas".

La solución se compone de dos partes interconectadas:
-   **Backend:** Una API RESTful robusta que maneja la lógica de negocio y la persistencia de datos.
-   **Frontend:** Una interfaz de usuario intuitiva basada en el navegador para interactuar con la API del backend.

## 2. Tecnologías Utilizadas

### Backend
-   **Lenguaje:** Java 21
-   **Framework:** Spring Boot 3.2.1
-   **Persistencia:** Spring Data JPA para la interacción con la base de datos.
-   **Base de Datos:** PostgreSQL para el almacenamiento de datos relacionales.
-   **Gestor de Dependencias:** Maven.

### Frontend
-   **Tecnologías Base:** HTML5 (estructura), CSS3 (estilos), JavaScript (lógica interactiva).
-   Se utiliza JavaScript vanilla (sin frameworks como React, Angular o Vue) para la comunicación con el backend y la manipulación del DOM.

## 3. Estructura del Proyecto

El proyecto está organizado de manera modular:

-   `src/main/java/ec/edu/uteq/microservicios/msusuarios/`: Contiene todo el código Java del backend, organizado en controladores, modelos (entidades), repositorios y servicios para la lógica de negocio de las entregas.
-   `src/main/resources/`: Archivos de configuración del backend, como `application.properties`.
-   `frontend/`: Contiene los archivos estáticos del lado del cliente: `index.html` (estructura), `styles.css` (estilos), y `app.js` (lógica JavaScript).

## 4. Configuración y Ejecución

### Backend
1.  **Base de Datos:** Configura una instancia de PostgreSQL. Los detalles de conexión están en `src/main/resources/application.properties` (por defecto: `microservicio_db`, usuario `postgres`, puerto `5432`).
2.  **Ejecutar:** Abre una terminal en la raíz del proyecto y usa `./mvnw spring-boot:run`. El backend estará disponible en `http://localhost:8081`.

### Frontend
1.  Abre el archivo `frontend/index.html` directamente en tu navegador web. La interfaz se cargará y se comunicará automáticamente con el backend en `http://localhost:8081`.

## 5. Descripción Funcional

### Backend (API REST)
El backend provee una API RESTful completa para la gestión de "Entregas". Cada `Entrega` tiene atributos como `id`, `orderId`, `address`, `trackingNumber` y `status` (que puede ser `PENDIENTE`, `ENVIADO`, `ENTREGADO` o `CANCELADO`). Ofrece los endpoints estándar CRUD (Crear, Leer, Actualizar, Borrar) bajo la ruta `/api/entregas`. La lógica de negocio incluye asignar `PENDIENTE` como estado por defecto para nuevas entregas.

### Frontend (Interfaz de Usuario)
La interfaz permite a los usuarios visualizar todas las entregas existentes, así como crear nuevas, editar detalles de entregas específicas o eliminarlas. La interacción se realiza mediante un formulario modal y botones de acción, y toda la comunicación con el backend se gestiona a través de JavaScript.