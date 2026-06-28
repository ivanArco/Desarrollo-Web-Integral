#include <array>
#include <iostream>
#include <string>

#include <httplib.h>

static std::string to_es(unsigned long long n) {
    static const std::array<std::string, 20> units = {
        "cero", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve",
        "diez", "once", "doce", "trece", "catorce", "quince", "dieciséis", "diecisiete", "dieciocho", "diecinueve"
    };

    static const std::array<std::string, 10> tens = {
        "", "", "veinte", "treinta", "cuarenta", "cincuenta", "sesenta", "setenta", "ochenta", "noventa"
    };

    static const std::array<std::string, 10> hundreds = {
        "", "ciento", "doscientos", "trescientos", "cuatrocientos", "quinientos", "seiscientos", "setecientos", "ochocientos", "novecientos"
    };

    if (n == 0) {
        return "cero";
    }

    if (n < 20) {
        return units[static_cast<std::size_t>(n)];
    }

    if (n < 30) {
        switch (n) {
            case 20: return "veinte";
            case 21: return "veintiuno";
            case 22: return "veintidós";
            case 23: return "veintitrés";
            case 24: return "veinticuatro";
            case 25: return "veinticinco";
            case 26: return "veintiséis";
            case 27: return "veintisiete";
            case 28: return "veintiocho";
            case 29: return "veintinueve";
            default: break;
        }
    }

    if (n < 100) {
        const auto dec = n / 10;
        const auto uni = n % 10;
        return uni == 0 ? tens[static_cast<std::size_t>(dec)]
                        : tens[static_cast<std::size_t>(dec)] + std::string(" y ") + units[static_cast<std::size_t>(uni)];
    }

    if (n == 100) {
        return "cien";
    }

    if (n < 1000) {
        const auto cen = n / 100;
        const auto rest = n % 100;
        return rest == 0 ? hundreds[static_cast<std::size_t>(cen)]
                         : hundreds[static_cast<std::size_t>(cen)] + std::string(" ") + to_es(rest);
    }

    if (n < 1000000) {
        const auto mil = n / 1000;
        const auto rest = n % 1000;
        const auto left = mil == 1 ? std::string("mil") : to_es(mil) + " mil";
        return rest == 0 ? left : left + " " + to_es(rest);
    }

    return "fuera de rango";
}

static std::string local_number_to_words(const std::string& n) {
    try {
        return to_es(std::stoull(n));
    } catch (...) {
        return "Parametro n invalido";
    }
}

int main() {
    httplib::Server app;

    app.Get("/", [](const httplib::Request& req, httplib::Response& res) {
        if (!req.has_param("n")) {
            res.status = 400;
            res.set_content("Falta parametro n", "text/plain; charset=utf-8");
            return;
        }

        const auto n = req.get_param_value("n");
        res.set_content(local_number_to_words(n), "text/plain; charset=utf-8");
    });

    std::cout << "Servidor en http://localhost:8601" << std::endl;
    app.listen("0.0.0.0", 8601);
}
