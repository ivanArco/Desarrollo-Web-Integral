#include <string>
#include <iostream>

#include <httplib.h>
#include <cpr/cpr.h>
#include <tinyxml2.h>

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

    return (result && result->GetText()) ? result->GetText() : "Sin resultado";
}

int main() {
    httplib::Server app;

    app.Get("/", [](const httplib::Request& req, httplib::Response& res) {
        if (!req.has_param("n")) {
            res.status = 400;
            res.set_content("Falta parametro n", "text/plain");
            return;
        }
        auto n = req.get_param_value("n");
        res.set_content(soap_number_to_words(n), "text/plain");
    });

    std::cout << "Servidor en http://localhost:8601" << std::endl;
    app.listen("0.0.0.0", 8601);
}
