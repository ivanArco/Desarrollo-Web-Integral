package app;

import okhttp3.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static spark.Spark.*;

public class Main {
    private static final OkHttpClient HTTP = new OkHttpClient();
    private static final Pattern SOAP_RESULT_PATTERN = Pattern.compile(
            "<(?:\\w+:)?NumberToWordsResult>(.*?)</(?:\\w+:)?NumberToWordsResult>",
            Pattern.DOTALL
    );
    private static final Pattern TRANSLATED_TEXT_PATTERN = Pattern.compile("\\\"translatedText\\\"\\s*:\\s*\\\"(.*?)\\\"");

    public static void main(String[] args) {
        port(8502);
        exception(Exception.class, (e, req, res) -> {
            res.status(500);
            res.type("text/plain; charset=utf-8");
            res.body("Error interno: " + e.getMessage());
        });

        get("/", (req, res) -> {
            String n = req.queryParams("n");
            if (n == null || n.isBlank()) {
                res.status(400);
                return "Falta parametro n";
            }

        String soap = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
                + "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Body><NumberToWords xmlns=\"http://www.dataaccess.com/webservicesserver/\">"
                + "<ubiNum>" + n + "</ubiNum></NumberToWords></soap:Body></soap:Envelope>";

            RequestBody body = RequestBody.create(soap, MediaType.parse("text/xml; charset=utf-8"));
            Request request = new Request.Builder()
                    .url("https://www.dataaccess.com/webservicesserver/NumberConversion.wso")
                    .addHeader("SOAPAction", "http://www.dataaccess.com/webservicesserver/NumberToWords")
                    .post(body)
                    .build();

            String english = "";
            try (Response response = HTTP.newCall(request).execute()) {
                String xml = response.body() != null ? response.body().string() : "";
                Matcher m = SOAP_RESULT_PATTERN.matcher(xml);
                english = m.find() ? m.group(1).trim() : "";
            }

            if (english.isBlank()) {
                res.status(502);
                return "No se pudo obtener resultado del SOAP";
            }

            try {
                String translated = translateWithMyMemory(english);
                return translated.isBlank() ? english : translated;
            } catch (Exception e) {
                res.status(500);
                return "Error de traduccion externa: " + e.getMessage();
            }
        });

        awaitInitialization();
        System.out.println("Servidor iniciado en http://localhost:8502/?n=10");
    }

    private static String translateWithMyMemory(String text) throws Exception {
        String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
        String url = "https://api.mymemory.translated.net/get?q=" + encoded + "&langpair=en|es";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = HTTP.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("HTTP " + response.code());
            }
            String json = response.body() != null ? response.body().string() : "";
            Matcher m = TRANSLATED_TEXT_PATTERN.matcher(json);
            if (!m.find()) {
                return "";
            }
            return decodeJsonString(m.group(1));
        }
    }

    private static String decodeJsonString(String value) {
        return value
                .replace("\\u003c", "<")
                .replace("\\u003e", ">")
                .replace("\\u0027", "'")
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\/", "/")
                .trim();
    }
}
