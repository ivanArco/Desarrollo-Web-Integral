import { createServer } from 'node:http';

const port = 8201;

function numeroALetras(n) {
  if (!Number.isFinite(n) || !Number.isInteger(n)) {
    throw new Error('n debe ser un entero');
  }

  if (n === 0) return 'cero';
  if (n < 0) return `menos ${numeroALetras(-n)}`;
  if (n > 999999999999) {
    throw new Error('n fuera de rango (maximo 999999999999)');
  }

  const unidades = [
    '',
    'uno',
    'dos',
    'tres',
    'cuatro',
    'cinco',
    'seis',
    'siete',
    'ocho',
    'nueve'
  ];

  const especiales = {
    10: 'diez',
    11: 'once',
    12: 'doce',
    13: 'trece',
    14: 'catorce',
    15: 'quince',
    16: 'dieciseis',
    17: 'diecisiete',
    18: 'dieciocho',
    19: 'diecinueve',
    20: 'veinte',
    21: 'veintiuno',
    22: 'veintidos',
    23: 'veintitres',
    24: 'veinticuatro',
    25: 'veinticinco',
    26: 'veintiseis',
    27: 'veintisiete',
    28: 'veintiocho',
    29: 'veintinueve'
  };

  const decenas = [
    '',
    '',
    'veinte',
    'treinta',
    'cuarenta',
    'cincuenta',
    'sesenta',
    'setenta',
    'ochenta',
    'noventa'
  ];

  const centenas = [
    '',
    'ciento',
    'doscientos',
    'trescientos',
    'cuatrocientos',
    'quinientos',
    'seiscientos',
    'setecientos',
    'ochocientos',
    'novecientos'
  ];

  const convertirDecenas = (x) => {
    if (x < 10) return unidades[x];
    if (x <= 29) return especiales[x];

    const d = Math.floor(x / 10);
    const u = x % 10;
    return u === 0 ? decenas[d] : `${decenas[d]} y ${unidades[u]}`;
  };

  const convertirCentenas = (x) => {
    if (x < 100) return convertirDecenas(x);
    if (x === 100) return 'cien';

    const c = Math.floor(x / 100);
    const resto = x % 100;
    return resto === 0 ? centenas[c] : `${centenas[c]} ${convertirDecenas(resto)}`;
  };

  const convertirMiles = (x) => {
    if (x < 1000) return convertirCentenas(x);

    const miles = Math.floor(x / 1000);
    const resto = x % 1000;

    const prefijoMiles = miles === 1 ? 'mil' : `${convertirCentenas(miles)} mil`;
    return resto === 0 ? prefijoMiles : `${prefijoMiles} ${convertirCentenas(resto)}`;
  };

  const convertirMillones = (x) => {
    if (x < 1000000) return convertirMiles(x);

    const millones = Math.floor(x / 1000000);
    const resto = x % 1000000;

    const prefijoMillones =
      millones === 1 ? 'un millon' : `${convertirMiles(millones)} millones`;

    return resto === 0 ? prefijoMillones : `${prefijoMillones} ${convertirMiles(resto)}`;
  };

  const convertirMilesDeMillones = (x) => {
    if (x < 1000000000) return convertirMillones(x);

    const milesDeMillones = Math.floor(x / 1000000000);
    const resto = x % 1000000000;

    const prefijo =
      milesDeMillones === 1
        ? 'mil millones'
        : `${convertirCentenas(milesDeMillones)} mil millones`;

    return resto === 0 ? prefijo : `${prefijo} ${convertirMillones(resto)}`;
  };

  return convertirMilesDeMillones(n);
}

const server = createServer((req, res) => {
  const url = new URL(req.url || '/', `http://${req.headers.host || `localhost:${port}`}`);

  if (url.pathname !== '/') {
    res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end('Ruta no encontrada');
    return;
  }

  const nParam = url.searchParams.get('n');
  if (nParam === null) {
    res.writeHead(400, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end('Falta parametro n');
    return;
  }

  const n = Number(nParam);
  if (!Number.isInteger(n)) {
    res.writeHead(400, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end('n debe ser un entero');
    return;
  }

  try {
    const texto = numeroALetras(n);
    res.writeHead(200, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end(texto);
  } catch (err) {
    res.writeHead(500, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end(`Error conversion local: ${err.message}`);
  }
});

server.listen(port, () => {
  console.log(`Servidor Node local ES en http://localhost:${port}`);
});
