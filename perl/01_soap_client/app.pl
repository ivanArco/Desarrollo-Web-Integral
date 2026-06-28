use strict;
use warnings;
use Mojolicious::Lite -signatures;
use SOAP::Lite;

my $wsdl = 'https://www.dataaccess.com/webservicesserver/NumberConversion.wso?WSDL';

get '/' => sub ($c) {
    my $n = $c->param('n');
    return $c->render(text => 'Falta parametro n', status => 400) unless defined $n;

    my $soap = SOAP::Lite->service($wsdl);
    my $result = $soap->NumberToWords($n + 0);

    $c->render(text => $result // 'Sin resultado');
};

app->start;
