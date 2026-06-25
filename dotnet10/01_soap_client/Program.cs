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

app.Run("http://localhost:8301");
