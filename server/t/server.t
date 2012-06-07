#!perl -T

use strict;

use Test::More tests => 3;

my $server = Dyad::Server->("localhost:3454");
isa_ok $server, 'Dyad::Server';
ok defined $server->{SOCKET};
ok defined $server->{REQUEST};
