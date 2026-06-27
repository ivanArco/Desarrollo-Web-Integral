package main

import (
	"fmt"
	"net/http"
	"strconv"
)

var units = []string{"cero", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve", "diez", "once", "doce", "trece", "catorce", "quince", "dieciseis", "diecisiete", "dieciocho", "diecinueve"}
var tens = []string{"", "", "veinte", "treinta", "cuarenta", "cincuenta", "sesenta", "setenta", "ochenta", "noventa"}
var hundreds = []string{"", "ciento", "doscientos", "trescientos", "cuatrocientos", "quinientos", "seiscientos", "setecientos", "ochocientos", "novecientos"}

func toEs(n int) string {
	if n == 0 {
		return "cero"
	}
	if n < 20 {
		return units[n]
	}
	if n < 30 {
		if n == 20 {
			return "veinte"
		}
		return "veinti" + units[n-20]
	}
	if n < 100 {
		d := n / 10
		u := n % 10
		if u == 0 {
			return tens[d]
		}
		return tens[d] + " y " + units[u]
	}
	if n == 100 {
		return "cien"
	}
	if n < 1000 {
		c := n / 100
		r := n % 100
		if r == 0 {
			return hundreds[c]
		}
		return hundreds[c] + " " + toEs(r)
	}
	if n < 1000000 {
		m := n / 1000
		r := n % 1000
		left := "mil"
		if m > 1 {
			left = toEs(m) + " mil"
		}
		if r == 0 {
			return left
		}
		return left + " " + toEs(r)
	}

	return "fuera de rango"
}

func handler(w http.ResponseWriter, r *http.Request) {
	raw := r.URL.Query().Get("n")
	if raw == "" {
		http.Error(w, "Falta parametro n", http.StatusBadRequest)
		return
	}

	n, err := strconv.Atoi(raw)
	if err != nil || n < 0 {
		http.Error(w, "n debe ser entero positivo", http.StatusBadRequest)
		return
	}

	fmt.Fprint(w, toEs(n))
}

func main() {
	http.HandleFunc("/", handler)
	fmt.Println("Servidor en http://localhost:8403")
	_ = http.ListenAndServe(":8403", nil)
}
