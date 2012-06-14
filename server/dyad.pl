use strict;

use Dyad::Server;
use FCGI;
use DBI;

my $request =
  FCGI::Request( \*STDIN, \*STDOUT, \*STDERR, \%ENV, '127.0.0.1:3454' );

my $dbh = DBI->connect( "dbi:SQLite:dbname=dyad.db", "", "" );

my $dyad_server = Dyad::Server->new($dbh);

while ( $request->Accept() >= 0 ) {
    $dyad_server->process_request();
}
