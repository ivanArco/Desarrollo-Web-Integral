use axum::{extract::Query, response::IntoResponse, routing::get, Router};
use regex::Regex;
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
    let xml = client
        .post("https://www.dataaccess.com/webservicesserver/NumberConversion.wso")
        .header("Content-Type", "text/xml; charset=utf-8")
        .header("SOAPAction", "http://www.dataaccess.com/webservicesserver/NumberToWords")
        .body(envelope)
        .send()
        .await
        .unwrap()
        .text()
        .await
        .unwrap();

    let re = Regex::new(r"<(?:\w+:)?NumberToWordsResult>(.*?)</(?:\w+:)?NumberToWordsResult>").unwrap();
    let result = re
        .captures(&xml)
        .and_then(|c| c.get(1))
        .map(|m| m.as_str().trim().to_string())
        .unwrap_or_else(|| "Sin resultado".to_string());

    (axum::http::StatusCode::OK, result)
}
