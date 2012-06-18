use strict;

use Dyad::Server;
use FCGI;
use DBI;

my $QUEUE_SIZE = 100;

my $request =
  FCGI::Request( \*STDIN, \*STDOUT, \*STDERR, \%ENV, FCGI::OpenSocket('127.0.0.1:3454', $QUEUE_SIZE) );

my $dyad_server = Dyad::Server->new;

while ( $request->Accept() >= 0 ) {
    $dyad_server->process_request();
}
