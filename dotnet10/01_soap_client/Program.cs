using System.Xml.Linq;

var builder = WebApplication.CreateBuilder(args);
var app = builder.Build();

const string endpoint = "https://www.dataaccess.com/webservicesserver/NumberConversion.wso";

app.MapGet("/", async (HttpContext ctx) =>
{
    var nValue = ctx.Request.Query["n"].ToString();
    if (string.IsNullOrWhiteSpace(nValue) || !int.TryParse(nValue, out var n))
    {
        return Results.BadRequest("Falta parametro n o no es valido");
    }

    var envelope = $"""
    <?xml version="1.0" encoding="utf-8"?>
    <soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                   xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
      <soap:Body>
        <NumberToWords xmlns="http://www.dataaccess.com/webservicesserver/">
          <ubiNum>{n}</ubiNum>
        </NumberToWords>
      </soap:Body>
    </soap:Envelope>
    """;

    using var http = new HttpClient();
    using var content = new StringContent(envelope, System.Text.Encoding.UTF8, "text/xml");
    content.Headers.Add("SOAPAction", "http://www.dataaccess.com/webservicesserver/NumberToWords");

    var response = await http.PostAsync(endpoint, content);
    var xml = await response.Content.ReadAsStringAsync();

    var doc = XDocument.Parse(xml);
    XNamespace ns = "http://www.dataaccess.com/webservicesserver/";
    var result = doc.Descendants(ns + "NumberToWordsResult").FirstOrDefault()?.Value?.Trim();

    return Results.Text(result ?? "Sin resultado");
});

app.Run("http://localhost:8301");
