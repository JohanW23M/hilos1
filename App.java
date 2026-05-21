package ec.edu.utpl.carreras.computacion.proava.s6;

import ec.edu.utpl.carreras.computacion.proava.s6.util.URLExpanderTask;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Hello world!
 */
public class App {

    static void main() {
        // Variable para el experimento. Ve cambiando este valor (1, 2, 4, 8, 16, 32, 64, etc.) [cite: 121]
        int poolSize = 256;

        var path = Path.of("C:/Users/SALASC/Desktop/visual/urls.csv"); // [cite: 67]
        List<String> urls;

        // 1. Fase Secuencial: Lectura y filtrado del archivo CSV usando Streams
        try (var lines = Files.lines(path)) {
            urls = lines
                    .filter(line -> !line.isBlank()) // Eliminar urls vacías
                    .map(String::trim)               // Eliminar espacios en blanco
                    .toList();
        } catch (IOException e) {
            System.err.println("Error crítico al leer el archivo de URLs: " + e.getMessage());
            return;
        }

        if (urls.isEmpty()) {
            System.out.println("El archivo no contiene URLs válidas.");
            return;
        }

        // 2. Configuración e Infraestructura de Concurrencia [cite: 46]
        // Declarar el executor en el try con paréntesis elimina el Warning de recursos de raíz
        try (ExecutorService executor = Executors.newFixedThreadPool(poolSize)) { // [cite: 81, 87]

            CountDownLatch startGate = new CountDownLatch(1); // [cite: 83, 88]
            CountDownLatch endGate = new CountDownLatch(urls.size()); // [cite: 83, 89]
            List<Future<String>> futures = new ArrayList<>(); // [cite: 82, 90]

            // Preparar y cargar todas las tareas en el pool
            for (String url : urls) { // [cite: 90]
                futures.add(executor.submit(() -> { // [cite: 82, 91]
                    try {
                        startGate.await(); // Espera la señal de salida simultánea [cite: 83, 92, 96]
                        URLExpanderTask task = new URLExpanderTask(url);
                        return task.call(); // Ejecuta la petición HTTP [cite: 93, 97]
                    } finally {
                        endGate.countDown(); // Anuncia la finalización de esta tarea [cite: 83, 95, 98]
                    }
                }));
            }

            // 3. Fase de Medición Empírica [cite: 62, 85, 102]
            long tInicio = System.nanoTime(); // [cite: 85, 102]

            startGate.countDown(); // ¡ARRANCAN TODOS LOS HILOS SIMULTÁNEAMENTE! [cite: 103, 107]

            try {
                endGate.await(); // El hilo principal espera que termine la última URL [cite: 83, 104, 108]
            } catch (InterruptedException e) {
                System.err.println("El hilo principal fue interrumpido.");
            }

            long tFin = System.nanoTime(); // [cite: 109]
            long elapsedMs = (tFin - tInicio) / 1_000_000; // Conversión a milisegundos [cite: 105, 109]

            // 4. Procesar la Colección 'futures' (Elimina el aviso de "never queried")
            // Al imprimir una pequeña muestra de control, IntelliJ ve que sí consultas la lista
            // 4. Procesar la Colección 'futures' completa
            System.out.println("\n--- IMPRIMIENDO TODAS LAS URLS EXPANDIDAS ---");
            for (int i = 0; i < futures.size(); i++) {
                try {
                    // .get() obtiene el resultado real de cada hilo individual
                    System.out.println("URL [" + i + "] Expandida: " + futures.get(i).get());
                } catch (Exception _) {
                    // Ignora o reporta si una URL individual falló para no detener el bucle
                }
            }
            // Impresión del reporte final para el Anexo A del taller
            System.out.println("\n=============================================");
            System.out.println("Resultados de la Simulación Empírica");
            System.out.println("=============================================");
            System.out.println("Tamaño del Pool probado (poolSize): " + poolSize); // [cite: 81]
            System.out.println("Cantidad de URLs procesadas: " + urls.size());
            System.out.println("Tiempo Total de Procesamiento: " + elapsedMs + " ms"); // [cite: 109]
            System.out.println("=============================================");

        } // El bloque try-with-resources se encarga del executor.shutdown() automáticamente [cite: 110]
    }
}