use axum::{extract::Query, response::IntoResponse, routing::get, Router};
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

    let value = match n.parse::<u64>() {
        Ok(v) => v,
        Err(_) => {
            return (
                axum::http::StatusCode::BAD_REQUEST,
                "Parametro n fuera de rango".to_string(),
            )
        }
    };

    (axum::http::StatusCode::OK, to_es(value))
}

fn to_es(n: u64) -> String {
    if n == 0 {
        return "cero".to_string();
    }

    if n < 20 {
        return [
            "cero",
            "uno",
            "dos",
            "tres",
            "cuatro",
            "cinco",
            "seis",
            "siete",
            "ocho",
            "nueve",
            "diez",
            "once",
            "doce",
            "trece",
            "catorce",
            "quince",
            "dieciseis",
            "diecisiete",
            "dieciocho",
            "diecinueve",
        ][n as usize]
            .to_string();
    }

    if n < 30 {
        return if n == 20 {
            "veinte".to_string()
        } else {
            format!("veinti{}", to_es(n - 20))
        };
    }

    if n < 100 {
        let tens = [
            "",
            "",
            "veinte",
            "treinta",
            "cuarenta",
            "cincuenta",
            "sesenta",
            "setenta",
            "ochenta",
            "noventa",
        ];
        let dec = n / 10;
        let uni = n % 10;
        return if uni == 0 {
            tens[dec as usize].to_string()
        } else {
            format!("{} y {}", tens[dec as usize], to_es(uni))
        };
    }

    if n == 100 {
        return "cien".to_string();
    }

    if n < 1000 {
        let hundreds = [
            "",
            "ciento",
            "doscientos",
            "trescientos",
            "cuatrocientos",
            "quinientos",
            "seiscientos",
            "setecientos",
            "ochocientos",
            "novecientos",
        ];
        let cen = n / 100;
        let rest = n % 100;
        return if rest == 0 {
            hundreds[cen as usize].to_string()
        } else {
            format!("{} {}", hundreds[cen as usize], to_es(rest))
        };
    }

    if n < 1_000_000 {
        let mil = n / 1000;
        let rest = n % 1000;
        let left = if mil == 1 {
            "mil".to_string()
        } else {
            format!("{} mil", to_es(mil))
        };
        return if rest == 0 {
            left
        } else {
            format!("{} {}", left, to_es(rest))
        };
    }

    "fuera de rango".to_string()
}
