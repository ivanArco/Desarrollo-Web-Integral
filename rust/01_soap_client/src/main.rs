use axum::{extract::Query, response::IntoResponse, routing::get, Router};
use regex::Regex;
use serde_json::Value;
use std::{collections::HashMap, net::SocketAddr};

#[tokio::main]
async fn main() {
    let app = Router::new().route("/", get(handler));
    let addr = SocketAddr::from(([127, 0, 0, 1], 8701));
    println!("Servidor en http://{}", addr);
    let listener = tokio::net::TcpListener::bind(addr).await.unwrap();
    axum::serve(listener, app).await.unwrap();
}

async fn handler(Query(params): Query<HashMap<String, String>>) -> impl IntoResponse {
    let Some(n) = params.get("n") else {
        return (axum::http::StatusCode::BAD_REQUEST, "Falta parametro n".to_string());
    };

    if !n.chars().all(|ch| ch.is_ascii_digit()) {
        return (
            axum::http::StatusCode::BAD_REQUEST,
            "Parametro n debe ser numerico".to_string(),
        );
    }

    let envelope = format!(
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\
        <soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \
        xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" \
        xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\
        <soap:Body><NumberToWords xmlns=\"http://www.dataaccess.com/webservicesserver/\">\
        <ubiNum>{}</ubiNum></NumberToWords></soap:Body></soap:Envelope>",
        n
    );

    let client = reqwest::Client::new();
    let xml = match client
        .post("https://www.dataaccess.com/webservicesserver/NumberConversion.wso")
        .header("Content-Type", "text/xml; charset=utf-8")
        .header("SOAPAction", "http://www.dataaccess.com/webservicesserver/NumberToWords")
        .body(envelope)
        .send()
        .await
    {
        Ok(resp) => match resp.text().await {
            Ok(body) => body,
            Err(err) => {
                return (
                    axum::http::StatusCode::BAD_GATEWAY,
                    format!("Error leyendo respuesta SOAP: {}", err),
                )
            }
        },
        Err(err) => {
            return (
                axum::http::StatusCode::BAD_GATEWAY,
                format!("Error llamando servicio SOAP: {}", err),
            )
        }
    };

    let re = Regex::new(r"<(?:\w+:)?NumberToWordsResult>(.*?)</(?:\w+:)?NumberToWordsResult>").unwrap();
    let english = re
        .captures(&xml)
        .and_then(|c| c.get(1))
        .map(|m| m.as_str().trim().to_string())
        .unwrap_or_else(|| "Sin resultado".to_string());

    let spanish = match translate_en_to_es(&client, &english).await {
        Ok(text) if !text.trim().is_empty() => text,
        _ => english,
    };

    (axum::http::StatusCode::OK, spanish)
}

async fn translate_en_to_es(client: &reqwest::Client, text: &str) -> Result<String, String> {
    let url = reqwest::Url::parse_with_params(
        "https://translate.googleapis.com/translate_a/single",
        &[
            ("client", "gtx"),
            ("sl", "en"),
            ("tl", "es"),
            ("dt", "t"),
            ("q", text),
        ],
    )
    .map_err(|err| err.to_string())?;

    let body = client
        .get(url)
        .send()
        .await
        .map_err(|err| err.to_string())?
        .text()
        .await
        .map_err(|err| err.to_string())?;

    let parsed: Value = serde_json::from_str(&body).map_err(|err| err.to_string())?;
    let chunks = parsed
        .get(0)
        .and_then(|v| v.as_array())
        .ok_or("Respuesta de traduccion invalida")?;

    let mut out = String::new();
    for chunk in chunks {
        if let Some(piece) = chunk.get(0).and_then(|v| v.as_str()) {
            out.push_str(piece);
        }
    }

    Ok(out.trim().to_string())
}
