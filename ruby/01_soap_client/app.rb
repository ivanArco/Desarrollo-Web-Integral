require 'sinatra'

set :port, 8103

def to_es(n)
  return 'cero' if n == 0

  units = %w[cero uno dos tres cuatro cinco seis siete ocho nueve diez once doce trece catorce quince dieciseis diecisiete dieciocho diecinueve]
  tens = ['', '', 'veinte', 'treinta', 'cuarenta', 'cincuenta', 'sesenta', 'setenta', 'ochenta', 'noventa']
  hundreds = ['', 'ciento', 'doscientos', 'trescientos', 'cuatrocientos', 'quinientos', 'seiscientos', 'setecientos', 'ochocientos', 'novecientos']

  return units[n] if n < 20
  return n == 20 ? 'veinte' : "veinti#{units[n - 20]}" if n < 30

  if n < 100
    dec = n / 10
    uni = n % 10
    return uni.zero? ? tens[dec] : "#{tens[dec]} y #{units[uni]}"
  end

  return 'cien' if n == 100

  if n < 1000
    cen = n / 100
    rest = n % 100
    return rest.zero? ? hundreds[cen] : "#{hundreds[cen]} #{to_es(rest)}"
  end

  if n < 1_000_000
    mil = n / 1000
    rest = n % 1000
    left = mil == 1 ? 'mil' : "#{to_es(mil)} mil"
    return rest.zero? ? left : "#{left} #{to_es(rest)}"
  end

  'fuera de rango'
end

get '/' do
  n = params['n']
  halt 400, 'Falta parametro n' unless n
  halt 400, 'Parametro n debe ser numerico' unless n.match?(/^\d+$/)

  to_es(n.to_i)
rescue StandardError => e
  status 500
  "Error conversion local: #{e.message}"
end