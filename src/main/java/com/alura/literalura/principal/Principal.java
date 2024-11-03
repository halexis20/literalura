package com.alura.literalura.principal;

import com.alura.literalura.model.*;
import com.alura.literalura.repository.AutorRepository;
import com.alura.literalura.service.ConsumoAPI;
import com.alura.literalura.service.ConvierteDatos;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {
    private final Scanner teclado = new Scanner(System.in);
    private final ConsumoAPI consumoAPI = new ConsumoAPI();
    private final ConvierteDatos conversor = new ConvierteDatos();
    private final String URL_BASE = "https://gutendex.com/books/";
    private final AutorRepository repository;

    public Principal(AutorRepository repository) {
        this.repository = repository;
    }


    public void mostrarMenu() {
        final String MENU_PRINCIPAL = """
                ----- 📚 Bienvenido(a) a Literalura 📚 -----
                --------------------------------------------
                             📑 MENU PRINCIPAL 📑
                --------------------------------------------
                1 - Buscar Libros por TÍtulo
                2 - Buscar Autor por Nombre
                3 - Listar Libros Registrados
                4 - Listar Autores Registrados
                5 - Listar Autores Vivos
                6 - Listar Libros por Idioma
                7 - Listar Autores por Año
                8 - Top 10 Libros más Buscados
                9 - Generar Estadísticas
                ----------------------------------------------
                0 - 🆗 SALIR DEL PROGRAMA 🆗
                ----------------------------------------------
                Elija una opción:
                """;

        int opcion = -1;
        while (opcion != 0) {
            System.out.println(MENU_PRINCIPAL);
            opcion = leerOpcion();
            ejecutarOpcion(opcion);
        }
    }

    private int leerOpcion() {
        try {
            return Integer.parseInt(teclado.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Opción no válida: " + e.getMessage());
            return -1; // Valor por defecto en caso de excepción
        }
    }

    private void ejecutarOpcion(int opcion) {
        switch (opcion) {
            case 1 -> buscarLibroPorTitulo();
            case 2 -> buscarAutorPorNombre();
            case 3 -> listarLibrosRegistrados();
            case 4 -> listarAutoresRegistrados();
            case 5 -> listarAutoresVivos();
            case 6 -> listarLibrosPorIdioma();
            case 7 -> listarAutoresPorAnio();
            case 8 -> top10Libros();
            case 9 -> generarEstadisticas();
            case 0 -> cerrarPrograma();
            default -> System.out.println("Opción no válida!");
        }
    }

    private void cerrarPrograma() {
        System.out.println("Gracias por utilizar Literalura ✔\uFE0F");
        System.out.println("Cerrando la aplicacion Literalura \uD83D\uDCD3 ...");
    }

    public void buscarLibroPorTitulo() {
        mostrarEncabezado("📔 BUSCAR LIBROS POR TÍTULO 📔");
        System.out.println("Introduzca el nombre del libro que desea buscar:");
        String nombre = teclado.nextLine();
        String json = consumoAPI.obtenerDatos(URL_BASE + "?search=" + nombre.replace(" ", "+").toLowerCase());

        if (jsonValido(json)) {
            var datos = conversor.obtenerDatos(json, Datos.class);
            datos.libros().stream().findFirst().ifPresentOrElse(
                    libro -> {
                        mostrarDetallesLibro(libro);
                        guardarLibroEnBD(libro, nombre);
                    },
                    () -> System.out.println("Libro no encontrado!")
            );
        } else {
            System.out.println("No se encontraron resultados para el título proporcionado.");
        }
    }

    // Verifica si el JSON contiene resultados válidos
    private boolean jsonValido(String json) {
        return !json.isEmpty() && !json.contains("\"count\":0,\"next\":null,\"previous\":null,\"results\":[]");
    }

    // Muestra los detalles de un libro
    private void mostrarDetallesLibro(DatosLibro libro) {
        System.out.println(
                "\n------------- LIBRO 📕 --------------" +
                        "\nTítulo: " + libro.titulo() +
                        "\nAutor: " + libro.autores().stream().map(DatosAutor::nombre).collect(Collectors.joining(", ")) +
                        "\nIdioma: " + String.join(", ", libro.idiomas()) +
                        "\nNúmero de descargas: " + libro.descargas() +
                        "\n--------------------------------------\n"
        );
    }

    // Guarda el libro y el autor en la base de datos si no existen
    private void guardarLibroEnBD(DatosLibro libro, String nombre) {
        try {
            Optional<Autor> autorBD = repository.buscarAutorPorNombre(libro.autores().get(0).nombre());
            if (repository.buscarLibroPorNombre(nombre).isPresent()) {
                System.out.println("El libro ya está guardado en la BD.");
            } else {
                Autor autor = autorBD.orElseGet(() -> guardarNuevoAutor(libro.autores().get(0)));
                List<Libro> libroEncontrado = List.of(new Libro(libro));
                autor.setLibros(libroEncontrado);
                repository.save(autor);
                System.out.println("Autor y libro guardados en la BD.");
            }
        } catch (Exception e) {
            System.out.println("Warning! " + e.getMessage());
        }
    }

    // Guarda un nuevo autor en la base de datos
    private Autor guardarNuevoAutor(DatosAutor datosAutor) {
        Autor nuevoAutor = new Autor(datosAutor);
        repository.save(nuevoAutor);
        System.out.println("Nuevo autor guardado en la BD.");
        return nuevoAutor;
    }

    // Muestra encabezado general para cada sección
    private void mostrarEncabezado(String titulo) {
        System.out.println("\n--------------------------------");
        System.out.println(titulo);
        System.out.println("--------------------------------");
    }


    public void buscarAutorPorNombre() {
        mostrarEncabezado("📙 BUSCAR AUTOR POR NOMBRE 📙");
        System.out.println("Ingrese el nombre del autor que deseas buscar:");
        String nombre = teclado.nextLine();

        repository.findByNombreContaining(nombre).ifPresentOrElse(
                this::imprimirDetallesAutor,
                () -> System.out.println("El autor no existe en la BD")
        );
    }


    // Método auxiliar para imprimir los detalles del autor
    private void imprimirDetallesAutor(Autor autor) {
        System.out.println(
                "\nAutor: " + autor.getNombre() +
                        "\nFecha de Nacimiento: " + autor.getNacimiento() +
                        "\nFecha de Fallecimiento: " + autor.getFallecimiento() +
                        "\nLibros: " + autor.getLibros().stream()
                        .map(Libro::getTitulo)
                        .collect(Collectors.joining(", ")) + "\n"
        );
    }


    public void listarLibrosRegistrados() {
        mostrarEncabezado("📕 LISTAR LIBROS REGISTRADOS 📕");
        List<Libro> libros = repository.buscarTodosLosLibros();
        libros.forEach(this::imprimirDetallesLibro);
    }

    // Método auxiliar para imprimir los detalles de un libro
    private void imprimirDetallesLibro(Libro libro) {
        System.out.println(
                "-------------- LIBRO 📕 -----------------" +
                        "\nTítulo: " + libro.getTitulo() +
                        "\nAutor: " + libro.getAutor().getNombre() +
                        "\nIdioma: " + libro.getIdioma().getIdioma() +
                        "\nNúmero de descargas: " + libro.getDescargas() +
                        "\n----------------------------------------\n"
        );
    }


    public void listarAutoresRegistrados() {
        mostrarEncabezado("📗 LISTAR AUTORES REGISTRADOS 📗");
        List<Autor> autores = repository.findAll();
        autores.forEach(this::imprimirDetallesAutor);
    }


    public void listarAutoresVivos() {
        mostrarEncabezado("📒 LISTAR AUTORES VIVOS 📒");
        Integer fecha = leerAnio("Introduzca un año para verificar los autores vivos en ese año:");

        if (fecha != null) {
            List<Autor> autores = repository.buscarAutoresVivos(fecha);
            if (autores.isEmpty()) {
                System.out.println("No hay autores vivos en el año registrado.");
            } else {
                autores.forEach(this::imprimirDetallesAutor);
            }
        }
    }


    // Método auxiliar para leer y validar el año ingresado por el usuario
    private Integer leerAnio(String mensaje) {
        System.out.println(mensaje);
        try {
            return Integer.parseInt(teclado.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Ingresa un año válido. Error: " + e.getMessage());
            return null;
        }
    }


    public void listarLibrosPorIdioma() {
        mostrarEncabezado("📘 LISTAR LIBROS POR IDIOMA 📘");
        mostrarMenuIdiomas();

        Integer opcion = leerOpcionIdioma();
        if (opcion != null) {
            String idioma = obtenerCodigoIdioma(opcion);
            if (idioma != null) {
                buscarLibrosPorIdioma(idioma);
            } else {
                System.out.println("Opción inválida!");
            }
        }
    }

    // Método auxiliar para mostrar el menú de idiomas
    private void mostrarMenuIdiomas() {
        System.out.println("""
                ---------------------------------------------------
                Seleccione el idioma del libro que desea encontrar:
                ---------------------------------------------------
                1 - Español
                2 - Francés
                3 - Inglés
                4 - Portugués
                ----------------------------------------------------
                """);
    }

    // Método auxiliar para leer la opción del usuario y validar la entrada
    private Integer leerOpcionIdioma() {
        try {
            return Integer.parseInt(teclado.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Opción no válida: " + e.getMessage());
            return null;
        }
    }

    // Método auxiliar para obtener el código de idioma según la opción seleccionada
    private String obtenerCodigoIdioma(int opcion) {
        Map<Integer, String> idiomas = Map.of(
                1, "es",
                2, "fr",
                3, "en",
                4, "pt"
        );
        return idiomas.get(opcion);
    }

    private void buscarLibrosPorIdioma(String idioma) {
        try {
            Idioma idiomaEnum = Idioma.valueOf(idioma.toUpperCase());
            List<Libro> libros = repository.buscarLibrosPorIdioma(idiomaEnum);
            if (libros.isEmpty()) {
                System.out.println("No hay libros registrados en ese idioma");
            } else {
                System.out.println();
                libros.forEach(l -> System.out.println(
                        "----------- LIBRO \uD83D\uDCD9  --------------" +
                                "\nTítulo: " + l.getTitulo() +
                                "\nAutor: " + l.getAutor().getNombre() +
                                "\nIdioma: " + l.getIdioma().getIdioma() +
                                "\nNúmero de descargas: " + l.getDescargas() +
                                "\n----------------------------------------\n"
                ));
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Introduce un idioma válido en el formato especificado.");
        }
    }

    public void listarAutoresPorAnio() {
        mostrarEncabezado("📓 LISTAR AUTORES POR AÑO 📓");
        mostrarMenuOpcionesAnio();

        Integer opcion = leerOpcion("Selecciona una opción: ");
        if (opcion != null) {
            switch (opcion) {
                case 1 -> listarAutoresPorNacimiento();
                case 2 -> listarAutoresPorFallecimiento();
                default -> System.out.println("Opción inválida!");
            }
        }
    }


    // Método auxiliar para mostrar el menú de opciones
    private void mostrarMenuOpcionesAnio() {
        System.out.println("""
                ------------------------------------------
                Ingresa una opción para listar los autores
                -------------------------------------------
                1 - Listar autor por Año de Nacimiento
                2 - Listar autor por Año de Fallecimiento
                -------------------------------------------
                """);
    }

    // Método auxiliar para leer y validar la opción seleccionada
    private Integer leerOpcion(String mensaje) {
        System.out.println(mensaje);
        try {
            return Integer.parseInt(teclado.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Opción no válida: " + e.getMessage());
            return null;
        }
    }

    // Métodos para listar autores por año de nacimiento y fallecimiento
    private void listarAutoresPorNacimiento() {
        System.out.println("Introduzca el año de nacimiento del autor que desea buscar:");
        Integer anio = leerAnio();
        if (anio != null) {
            List<Autor> autores = repository.listarAutoresPorNacimiento(anio);
            mostrarAutores(autores, "No existen autores con el año de nacimiento especificado.");
        }
    }

    private void listarAutoresPorFallecimiento() {
        System.out.println("Introduzca el año de fallecimiento del autor que desea buscar:");
        Integer anio = leerAnio();
        if (anio != null) {
            List<Autor> autores = repository.listarAutoresPorFallecimiento(anio);
            mostrarAutores(autores, "No existen autores con el año de fallecimiento especificado.");
        }
    }

    // Método auxiliar para leer y validar el año ingresado por el usuario
    private Integer leerAnio() {
        try {
            return Integer.parseInt(teclado.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Año no válido: " + e.getMessage());
            return null;
        }
    }

    // Método auxiliar para mostrar la lista de autores o un mensaje si la lista está vacía
    private void mostrarAutores(List<Autor> autores, String mensajeVacio) {
        if (autores.isEmpty()) {
            System.out.println(mensajeVacio);
        } else {
            autores.forEach(this::imprimirDetallesAutor);
        }
    }


    public void ListarAutoresPorNacimiento() {
        System.out.println("""
                ---------------------------------------------
                 📖 BUSCAR AUTOR POR SU AÑO DE NACIMIENTO 📖
                ---------------------------------------------
                """);
        System.out.println("Introduzca el año de nacimiento del autor que desea buscar:");
        try {
            var nacimiento = Integer.valueOf(teclado.nextLine());
            List<Autor> autores = repository.listarAutoresPorNacimiento(nacimiento);
            if (autores.isEmpty()) {
                System.out.println("No existen autores con año de nacimiento igual a " + nacimiento);
            } else {
                System.out.println();
                autores.forEach(a -> System.out.println(
                        "Autor: " + a.getNombre() +
                                "\nFecha de Nacimiento: " + a.getNacimiento() +
                                "\nFecha de Fallecimiento: " + a.getFallecimiento() +
                                "\nLibros: " + a.getLibros().stream().map(l -> l.getTitulo()).collect(Collectors.toList()) + "\n"
                ));
            }
        } catch (NumberFormatException e) {
            System.out.println("Año no válido: " + e.getMessage());
        }
    }

    public void ListarAutoresPorFallecimiento() {
        System.out.println("""
                ---------------------------------------------------------
                 📖  BUSCAR LIBROS POR AÑO DE FALLECIMIENTO DEL AUTOR 📖
                ----------------------------------------------------------
                """);
        System.out.println("Introduzca el año de fallecimiento del autor que desea buscar:");
        try {
            var fallecimiento = Integer.valueOf(teclado.nextLine());
            List<Autor> autores = repository.listarAutoresPorFallecimiento(fallecimiento);
            if (autores.isEmpty()) {
                System.out.println("No existen autores con año de fallecimiento igual a " + fallecimiento);
            } else {
                System.out.println();
                autores.forEach(a -> System.out.println(
                        "Autor: " + a.getNombre() +
                                "\nFecha de Nacimiento: " + a.getNacimiento() +
                                "\nFecha de Fallecimeinto: " + a.getFallecimiento() +
                                "\nLibros: " + a.getLibros().stream().map(l -> l.getTitulo()).collect(Collectors.toList()) + "\n"
                ));
            }
        } catch (NumberFormatException e) {
            System.out.println("Opción no válida: " + e.getMessage());
        }
    }

    public void top10Libros() {
        mostrarEncabezado("📚 TOP 10 LIBROS MÁS BUSCADOS 📚");
        List<Libro> libros = repository.top10Libros();
        libros.forEach(this::imprimirDetallesLibro);
    }


    public void generarEstadisticas() {
        System.out.println("""
                ----------------------------
                 📊 GENERAR ESTADÍSTICAS 📊
                ----------------------------
                """);
        var json = consumoAPI.obtenerDatos(URL_BASE);
        var datos = conversor.obtenerDatos(json, Datos.class);
        IntSummaryStatistics est = datos.libros().stream()
                .filter(l -> l.descargas() > 0)
                .collect(Collectors.summarizingInt(DatosLibro::descargas));
        Integer media = (int) est.getAverage();
        System.out.println("\n--------- ESTADÍSTICAS \uD83D\uDCCA ------------");
        System.out.println("Media de descargas: " + media);
        System.out.println("Máxima de descargas: " + est.getMax());
        System.out.println("Mínima de descargas: " + est.getMin());
        System.out.println("Total registros para calcular las estadísticas: " + est.getCount());
        System.out.println("---------------------------------------------------\n");
    }
}
