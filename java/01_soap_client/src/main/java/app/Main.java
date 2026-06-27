package app;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class Main {
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final Pattern RESULT_PATTERN = Pattern.compile(
            "<(?:\\w+:)?NumberToWordsResult>(.*?)</(?:\\w+:)?NumberToWordsResult>",
            Pattern.DOTALL
    );
    private static final int PORT = 8501;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new NumberHandler());
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        System.out.println("Servidor iniciado en http://localhost:" + PORT + "/?n=10");
    }

    private static class NumberHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Solo se permite GET");
                return;
            }

            String query = Optional.ofNullable(exchange.getRequestURI().getRawQuery()).orElse("");
            String n = getQueryParam(query, "n");
            if (n == null || n.isBlank()) {
                sendText(exchange, 400, "Falta parametro n");
                return;
            }

            try {
                String result = numberToWords(n.trim());
                sendText(exchange, 200, result);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                sendText(exchange, 500, "Error interno");
            } catch (Exception e) {
                sendText(exchange, 500, "Error SOAP: " + e.getMessage());
            }
        }
    }

    private static String getQueryParam(String query, String key) {
        return Arrays.stream(query.split("&"))
                .map(part -> part.split("=", 2))
                .filter(parts -> parts.length == 2)
                .filter(parts -> key.equals(URLDecoder.decode(parts[0], StandardCharsets.UTF_8)))
                .map(parts -> URLDecoder.decode(parts[1], StandardCharsets.UTF_8))
                .findFirst()
                .orElse(null);
    }

    private static String numberToWords(String n) throws IOException, InterruptedException {
        String soap = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
                + "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Body><NumberToWords xmlns=\"http://www.dataaccess.com/webservicesserver/\">"
                + "<ubiNum>" + n + "</ubiNum></NumberToWords></soap:Body></soap:Envelope>";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.dataaccess.com/webservicesserver/NumberConversion.wso"))
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "http://www.dataaccess.com/webservicesserver/NumberToWords")
                .POST(HttpRequest.BodyPublishers.ofString(soap))
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("HTTP " + response.statusCode());
        }

        String xml = response.body() != null ? response.body() : "";
        Matcher m = RESULT_PATTERN.matcher(xml);
        return m.find() ? m.group(1).trim() : "Sin resultado";
    }

    private static void sendText(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
