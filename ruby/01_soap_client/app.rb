require 'sinatra'
require 'savon'
require 'json'
require 'net/http'
require 'uri'

set :port, 8101

SOAP_HOST = 'www.dataaccess.com'
SOAP_IP = '52.7.155.169'
WSDL_URL = "https://#{SOAP_IP}/webservicesserver/NumberConversion.wso?WSDL"
SOAP_ENDPOINT = "https://#{SOAP_IP}/webservicesserver/NumberConversion.wso"

class GoogleTranslate
  def initialize(from:, to:)
    @from = from
    @to = to
  end

  def translate(text)
    return '' if text.to_s.strip.empty?

    params = {
      client: 'gtx',
      sl: @from,
      tl: @to,
      dt: 't',
      q: text
    }

    uri = URI('https://translate.googleapis.com/translate_a/single')
    uri.query = URI.encode_www_form(params)

    response = Net::HTTP.get_response(uri)
    raise "Error de traduccion HTTP #{response.code}" unless response.is_a?(Net::HTTPSuccess)

    payload = JSON.parse(response.body)
    chunks = payload[0] || []
    chunks.map { |item| item[0] }.join
  end
end

get '/' do
  n = params['n']
  halt 400, 'Falta parametro n' unless n
  halt 400, 'Parametro n debe ser numerico' unless n.match?(/^\d+$/)

  # Workaround for environments where IPv6 to dataaccess.com times out.   
  client = Savon.client(
    wsdl: WSDL_URL,
    endpoint: SOAP_ENDPOINT,
    namespace: 'http://www.dataaccess.com/webservicesserver/',
    headers: { 'Host' => SOAP_HOST },
    ssl_verify_mode: :none,
    open_timeout: 10,
    read_timeout: 10,
    log: false
  )
  response = client.call(:number_to_words, message: { 'ubiNum' => n.to_i })
  english_text = response.body.dig(:number_to_words_response, :number_to_words_result).to_s.strip

  translator = GoogleTranslate.new(from: 'en', to: 'es')
  spanish_text = translator.translate(english_text)

  spanish_text.to_s.strip
rescue StandardError => e
  status 500
  "Error SOAP: #{e.message}"
end
