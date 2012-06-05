# Before `make install' is performed this script should be runnable with
# `make test'. After `make install' it should work as `perl Dyad-Server.t'

#########################

# change 'tests => 1' to 'tests => last_test_to_print';

use strict;
use warnings;

use Test::More tests => 4;
BEGIN { use_ok('Dyad::Server') };

#########################

# Insert your test code below, the Test::More module is use()ed here so read
# its man page ( perldoc Test::More ) for help writing this test script.

my $server = Dyad::Server->new("localhost:3454");
isa_ok $server, 'Dyad::Server';
ok defined $server->{SOCKET};
ok defined $server->{REQUEST};


#$ENV{CONTENT_LENGTH} = 10;
#$server->get_body();