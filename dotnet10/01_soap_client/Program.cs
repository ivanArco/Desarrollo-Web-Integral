using System.Xml.Linq;
using System.Text.Json;

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

  var result = await GetSoapNumberToWordsAsync(n);
    return Results.Text(result ?? "Sin resultado");
});

app.MapGet("/parte2", async (HttpContext ctx) =>
{
  var nValue = ctx.Request.Query["n"].ToString();
  if (string.IsNullOrWhiteSpace(nValue) || !int.TryParse(nValue, out var n))
  {
    return Results.BadRequest("Falta parametro n o no es valido");
  }

  var english = await GetSoapNumberToWordsAsync(n);
  if (string.IsNullOrWhiteSpace(english))
  {
    return Results.Problem("No se pudo obtener respuesta del servicio SOAP.");
  }

  var spanish = await TranslateEnToEsAsync(english);
  return Results.Text(spanish ?? "Sin traduccion");
});

app.MapGet("/parte3", (HttpContext ctx) =>
{
  var nValue = ctx.Request.Query["n"].ToString();
  if (string.IsNullOrWhiteSpace(nValue) || !int.TryParse(nValue, out var n) || n < 0)
  {
    return Results.BadRequest("Falta parametro n o no es valido");
  }

  var text = NumberToSpanish(n);
  return Results.Text(text);
});

async Task<string?> GetSoapNumberToWordsAsync(int n)
{
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
  return doc.Descendants(ns + "NumberToWordsResult").FirstOrDefault()?.Value?.Trim();
}

async Task<string?> TranslateEnToEsAsync(string text)
{
  try
  {
    using var http = new HttpClient();
    var encoded = Uri.EscapeDataString(text);
    var url = $"https://api.mymemory.translated.net/get?q={encoded}&langpair=en|es";
    var json = await http.GetStringAsync(url);

    using var doc = JsonDocument.Parse(json);
    if (doc.RootElement.TryGetProperty("responseData", out var responseData)
        && responseData.TryGetProperty("translatedText", out var translated))
    {
      return translated.GetString();
    }

    return null;
  }
  catch
  {
    return null;
  }
}

string NumberToSpanish(int n)
{
  var units = new[]
  {
    "cero", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve",
    "diez", "once", "doce", "trece", "catorce", "quince", "dieciseis", "diecisiete", "dieciocho", "diecinueve"
  };

  var tens = new[] { "", "", "veinte", "treinta", "cuarenta", "cincuenta", "sesenta", "setenta", "ochenta", "noventa" };
  var hundreds = new[] { "", "ciento", "doscientos", "trescientos", "cuatrocientos", "quinientos", "seiscientos", "setecientos", "ochocientos", "novecientos" };

  if (n < 20) return units[n];
  if (n < 30) return n == 20 ? "veinte" : "veinti" + units[n - 20];

  if (n < 100)
  {
    var d = n / 10;
    var u = n % 10;
    return u == 0 ? tens[d] : tens[d] + " y " + units[u];
  }

  if (n == 100) return "cien";

  if (n < 1000)
  {
    var h = n / 100;
    var r = n % 100;
    return r == 0 ? hundreds[h] : hundreds[h] + " " + NumberToSpanish(r);
  }

  if (n < 1000000)
  {
    var thousands = n / 1000;
    var rest = n % 1000;
    var left = thousands == 1 ? "mil" : NumberToSpanish(thousands) + " mil";
    return rest == 0 ? left : left + " " + NumberToSpanish(rest);
  }

  return "fuera de rango";
}

app.Run("http://localhost:8301");
