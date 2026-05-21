package ec.edu.utpl.carreras.computacion.proava.s6.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Callable;

public class URLExpanderTask implements Callable<String> {
    private final String urlShortened;

    // Cliente HTTP configurado una sola vez estáticamente para reutilizar conexiones
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)) // Timeout de conexión de la rúbrica [cite: 84]
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    public URLExpanderTask(String urlShortened) {
        this.urlShortened = urlShortened;
    }

    @Override
    public String call() {
        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(urlShortened))
                    .timeout(Duration.ofSeconds(5)) // Timeout por petición [cite: 84, 211]
                    .method("HEAD", HttpRequest.BodyPublishers.noBody()) // Optimización usando HEAD
                    .build();

            HttpResponse<Void> response = CLIENT.send(
                    request,
                    HttpResponse.BodyHandlers.discarding()
            );

            // Estructura limpia que reemplaza al switch redundante
            if (response.statusCode() == 200) {
                return response.uri().toString();
            }
            return "ERROR_HTTP_" + response.statusCode();

        } catch (Exception e) {
            return "FAILED_TO_EXPAND: " + e.getMessage();
        }
    }
}