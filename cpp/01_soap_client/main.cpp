#include <string>
#include <iostream>

#include <httplib.h>
#include <cpr/cpr.h>
#include <tinyxml2.h>

static std::string html_unescape(std::string text) {
    struct Entity { const char* from; const char* to; };
    static const Entity entities[] = {
        {"&amp;", "&"},
        {"&lt;", "<"},
        {"&gt;", ">"},
        {"&quot;", "\""},
        {"&#39;", "'"},
    };

    for (const auto& entity : entities) {
        std::string::size_type pos = 0;
        while ((pos = text.find(entity.from, pos)) != std::string::npos) {
            text.replace(pos, std::strlen(entity.from), entity.to);
            pos += std::strlen(entity.to);
        }
    }

    return text;
}

static std::string translate_en_to_es(const std::string& english_text) {
    auto r = cpr::Get(
        cpr::Url{"https://translate.googleapis.com/translate_a/single"},
        cpr::Parameters{{"client", "gtx"}, {"sl", "en"}, {"tl", "es"}, {"dt", "t"}, {"q", english_text}}
    );

    if (r.status_code != 200 || r.text.empty()) {
        return english_text;
    }

    const std::string prefix = R"([[[")";
    const auto start = r.text.find(prefix);
    if (start == std::string::npos) {
        return english_text;
    }

    const auto first_quote = start + prefix.size();
    const auto second_quote = r.text.find("\"", first_quote);
    if (second_quote == std::string::npos) {
        return english_text;
    }

    return html_unescape(r.text.substr(first_quote, second_quote - first_quote));
}

static std::string soap_number_to_words(const std::string& n) {
    const std::string envelope =
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
        "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
        "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
        "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        "<soap:Body><NumberToWords xmlns=\"http://www.dataaccess.com/webservicesserver/\">"
        "<ubiNum>" + n + "</ubiNum></NumberToWords></soap:Body></soap:Envelope>";

    auto r = cpr::Post(
        cpr::Url{"https://www.dataaccess.com/webservicesserver/NumberConversion.wso"},
        cpr::Header{{"Content-Type", "text/xml; charset=utf-8"}, {"SOAPAction", "http://www.dataaccess.com/webservicesserver/NumberToWords"}},
        cpr::Body{envelope}
    );

    tinyxml2::XMLDocument doc;
    doc.Parse(r.text.c_str());

    auto* root = doc.RootElement();
    if (!root) return "Sin resultado";

    tinyxml2::XMLElement* result = nullptr;
    for (auto* e = root->FirstChildElement(); e && !result; e = e->NextSiblingElement()) {
        for (auto* b = e->FirstChildElement(); b && !result; b = b->NextSiblingElement()) {
            for (auto* c = b->FirstChildElement(); c && !result; c = c->NextSiblingElement()) {
                if (std::string(c->Name()).find("NumberToWordsResult") != std::string::npos) {
                    result = c;
                }
            }
        }
    }

    const auto english = (result && result->GetText()) ? result->GetText() : "Sin resultado";
    return translate_en_to_es(english);
}

int main() {
    httplib::Server app;

    app.Get("/", [](const httplib::Request& req, httplib::Response& res) {
        if (!req.has_param("n")) {
            res.status = 400;
            res.set_content("Falta parametro n", "text/plain; charset=utf-8");
            return;
        }
        auto n = req.get_param_value("n");
        res.set_content(soap_number_to_words(n), "text/plain; charset=utf-8");
    });

    std::cout << "Servidor en http://localhost:8601" << std::endl;
    app.listen("0.0.0.0", 8601);
}
