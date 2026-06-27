import express from 'express';
import soap from 'soap';
import { translate } from '@vitalets/google-translate-api';

const app = express();
const port = 8201;
const wsdl = 'https://www.dataaccess.com/webservicesserver/NumberConversion.wso?WSDL';

app.get('/', async (req, res) => {
  const n = req.query.n;
  if (n === undefined) return res.status(400).send('Falta parametro n');

  try {
    const client = await soap.createClientAsync(wsdl);
    const [result] = await client.NumberToWordsAsync({ ubiNum: Number(n) });

    const english = (result.NumberToWordsResult || '').trim();
    const translated = await translate(english, { from: 'en', to: 'es' });

    res.send(translated.text);
  } catch (err) {
    res.status(500).send(`Error traduccion: ${err.message}`);
  }
});

app.listen(port, () => {  
  console.log(`Servidor Node SOAP en http://localhost:${port}`);
});
