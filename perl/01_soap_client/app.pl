use strict;
use warnings;
use Mojolicious::Lite -signatures;
use Mojo::UserAgent;
use SOAP::Lite;

my $wsdl = 'https://www.dataaccess.com/webservicesserver/NumberConversion.wso?WSDL';
my $ua   = Mojo::UserAgent->new;

sub to_es_local {
    my ($n) = @_;
    return 'cero' if $n == 0;

    my @u = qw(cero uno dos tres cuatro cinco seis siete ocho nueve diez once doce trece catorce quince dieciseis diecisiete dieciocho diecinueve);
    my @d = qw('' '' veinte treinta cuarenta cincuenta sesenta setenta ochenta noventa);
    my @c = qw('' ciento doscientos trescientos cuatrocientos quinientos seiscientos setecientos ochocientos novecientos);

    if ($n < 20) { return $u[$n]; }
    if ($n < 30) { return $n == 20 ? 'veinte' : 'veinti' . $u[$n - 20]; }
    if ($n < 100) {
        my $dec = int($n / 10);
        my $uni = $n % 10;
        return $uni ? $d[$dec] . ' y ' . $u[$uni] : $d[$dec];
    }
    if ($n == 100) { return 'cien'; }
    if ($n < 1000) {
        my $cen = int($n / 100);
        my $rest = $n % 100;
        return $rest ? $c[$cen] . ' ' . to_es_local($rest) : $c[$cen];
    }
    if ($n < 1000000) {
        my $mil = int($n / 1000);
        my $rest = $n % 1000;
        my $left = $mil == 1 ? 'mil' : to_es_local($mil) . ' mil';
        return $rest ? $left . ' ' . to_es_local($rest) : $left;
    }

    return undef;
}

sub translate_en_to_es {
    my ($text) = @_;
    my $tx = $ua->post(
        'https://libretranslate.de/translate' =>
        { Accept => 'application/json' } =>
        json => {
            q      => $text,
            source => 'en',
            target => 'es',
            format => 'text'
        }
    );

    return undef unless $tx->result->is_success;
    my $json = $tx->result->json;
    return undef unless ref $json eq 'HASH';
    return $json->{translatedText};
}

get '/' => sub ($c) {
    my $n = $c->param('n');
    return $c->render(text => 'Falta parametro n', status => 400) unless defined $n && $n =~ /^\d+$/;

    my $soap = SOAP::Lite->service($wsdl);
    my $english = $soap->NumberToWords($n + 0) // '';
    my $spanish = translate_en_to_es($english);

    # Fallback local si la API de traduccion no responde.
    $spanish //= to_es_local($n + 0);

    $c->render(text => $spanish // 'Sin traduccion');
};

app->start;
