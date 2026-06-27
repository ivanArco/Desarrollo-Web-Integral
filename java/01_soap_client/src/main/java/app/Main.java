package app;

import static spark.Spark.*;

public class Main {
    private static final String[] UNITS = {
        "cero", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve",
        "diez", "once", "doce", "trece", "catorce", "quince", "dieciseis", "diecisiete", "dieciocho", "diecinueve"
    };
    private static final String[] TENS = {
        "", "", "veinte", "treinta", "cuarenta", "cincuenta", "sesenta", "setenta", "ochenta", "noventa"
    };
    private static final String[] HUNDREDS = {
        "", "ciento", "doscientos", "trescientos", "cuatrocientos", "quinientos",
        "seiscientos", "setecientos", "ochocientos", "novecientos"
    };

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

            try {
                long value = Long.parseLong(n.trim());
                return numberToSpanish(value);
            } catch (NumberFormatException e) {
                res.status(400);
                return "El parametro n debe ser un numero entero valido";
            }
        });

        awaitInitialization();
        System.out.println("Servidor iniciado en http://localhost:8502/?n=10");
    }

    private static String numberToSpanish(long value) {
        if (value == 0) {
            return "cero";
        }

        if (value < 0) {
            return "menos " + numberToSpanish(Math.abs(value));
        }

        if (value < 1000) {
            return convertUnderOneThousand((int) value);
        }

        if (value < 1_000_000) {
            return convertThousands(value);
        }

        if (value < 1_000_000_000_000L) {
            return convertMillions(value);
        }

        throw new IllegalArgumentException("El numero es demasiado grande");
    }

    private static String convertMillions(long value) {
        long millions = value / 1_000_000;
        int remainder = (int) (value % 1_000_000);

        String prefix = millions == 1 ? "un millon" : normalizeUno(numberToSpanish(millions)) + " millones";
        if (remainder == 0) {
            return prefix;
        }
        return prefix + " " + convertThousands(remainder);
    }

    private static String convertThousands(long value) {
        long thousands = value / 1000;
        int remainder = (int) (value % 1000);

        String prefix;
        if (thousands == 1) {
            prefix = "mil";
        } else {
            prefix = normalizeUno(convertUnderOneThousand((int) thousands)) + " mil";
        }

        if (remainder == 0) {
            return prefix;
        }
        return prefix + " " + convertUnderOneThousand(remainder);
    }

    private static String convertUnderOneThousand(int value) {
        if (value < 20) {
            return UNITS[value];
        }

        if (value < 100) {
            return convertUnderOneHundred(value);
        }

        if (value == 100) {
            return "cien";
        }

        int hundreds = value / 100;
        int remainder = value % 100;
        if (remainder == 0) {
            return HUNDREDS[hundreds];
        }
        return HUNDREDS[hundreds] + " " + convertUnderOneHundred(remainder);
    }

    private static String convertUnderOneHundred(int value) {
        if (value < 20) {
            return UNITS[value];
        }

        if (value < 30) {
            if (value == 20) {
                return "veinte";
            }
            return "veinti" + UNITS[value - 20];
        }

        int tens = value / 10;
        int units = value % 10;
        if (units == 0) {
            return TENS[tens];
        }
        return TENS[tens] + " y " + UNITS[units];
    }

    private static String normalizeUno(String text) {
        if (text.endsWith(" veintiuno")) {
            return text.substring(0, text.length() - " veintiuno".length()) + " veintiun";
        }
        if (text.equals("veintiuno")) {
            return "veintiun";
        }
        if (text.endsWith(" y uno")) {
            return text.substring(0, text.length() - " y uno".length()) + " y un";
        }
        if (text.endsWith(" uno")) {
            return text.substring(0, text.length() - " uno".length()) + " un";
        }
        if (text.equals("uno")) {
            return "un";
        }
        return text;
    }
}
