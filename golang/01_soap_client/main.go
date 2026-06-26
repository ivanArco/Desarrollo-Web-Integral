package main

import (
    "bytes"
    "encoding/xml"
    "fmt"
    "io"
    "net/http"
)

type soapResp struct {
    XMLName xml.Name `xml:"Envelope"`
    Body    struct {
        NumberToWordsResponse struct {
            NumberToWordsResult string `xml:"NumberToWordsResult"`
        } `xml:"NumberToWordsResponse"`
    } `xml:"Body"`
}

func handler(w http.ResponseWriter, r *http.Request) {
    n := r.URL.Query().Get("n")
    if n == "" {
        http.Error(w, "Falta parametro n", http.StatusBadRequest)
        return
    }

    envelope := fmt.Sprintf(`<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xmlns:xsd="http://www.w3.org/2001/XMLSchema"
               xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <NumberToWords xmlns="http://www.dataaccess.com/webservicesserver/">
      <ubiNum>%s</ubiNum>
    </NumberToWords>
  </soap:Body>
</soap:Envelope>`, n)

    req, _ := http.NewRequest(http.MethodPost, "https://www.dataaccess.com/webservicesserver/NumberConversion.wso", bytes.NewBufferString(envelope))
    req.Header.Set("Content-Type", "text/xml; charset=utf-8")
    req.Header.Set("SOAPAction", "http://www.dataaccess.com/webservicesserver/NumberToWords")

    resp, err := http.DefaultClient.Do(req)
    if err != nil {
        http.Error(w, err.Error(), http.StatusInternalServerError)
        return
    }
    defer resp.Body.Close()

    body, _ := io.ReadAll(resp.Body)
    var parsed soapResp
    if err := xml.Unmarshal(body, &parsed); err != nil {
        http.Error(w, err.Error(), http.StatusInternalServerError)
        return
    }

    fmt.Fprint(w, parsed.Body.NumberToWordsResponse.NumberToWordsResult)
}

func main() {
    http.HandleFunc("/", handler)
    fmt.Println("Servidor en http://localhost:8401")
    _ = http.ListenAndServe(":8401", nil)
}
