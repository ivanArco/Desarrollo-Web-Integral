use strict;
use warnings;
use Mojolicious::Lite -signatures;

sub to_es {
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
        return $rest ? $c[$cen] . ' ' . to_es($rest) : $c[$cen];
    }
    if ($n < 1000000) {
        my $mil = int($n / 1000);
        my $rest = $n % 1000;
        my $left = $mil == 1 ? 'mil' : to_es($mil) . ' mil';
        return $rest ? $left . ' ' . to_es($rest) : $left;
    }

    return 'fuera de rango';
}

get '/' => sub ($c) {
    my $n = $c->param('n');
    return $c->render(text => 'Falta parametro n', status => 400) unless defined $n && $n =~ /^\d+$/;
    $c->render(text => to_es($n + 0));
};

app->start;
