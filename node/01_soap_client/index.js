import express from 'express';
import soap from 'soap';

const app = express();
const port = 8201;
const wsdl = 'https://www.dataaccess.com/webservicesserver/NumberConversion.wso?WSDL';

app.get('/', async (req, res) => {
  const n = req.query.n;
  if (n === undefined) return res.status(400).send('Falta parametro n');

  try {
    const client = await soap.createClientAsync(wsdl);
    const [result] = await client.NumberToWordsAsync({ ubiNum: Number(n) });
    res.send((result.NumberToWordsResult || '').trim());
  } catch (err) {
    res.status(500).send(`Error SOAP: ${err.message}`);
  }
});

app.listen(port, () => {
  console.log(`Servidor Node SOAP en http://localhost:${port}`);
});
