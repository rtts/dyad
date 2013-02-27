#!/usr/bin/perl -w
use strict;

use Dyad::Server;
use FCGI;
use MongoDB::Connection;
use Log::Any::Adapter ('File', '/home/lojanda/tmp/dyad.log');

my $QUEUE_SIZE = 100;
my $DB_NAME    = 'dyad';

my $request =
  FCGI::Request( \*STDIN, \*STDOUT, \*STDERR, \%ENV,
    FCGI::OpenSocket( '0.0.0.0:3454', $QUEUE_SIZE ) );
my $conn        = MongoDB::Connection->new;
my $db          = $conn->$DB_NAME;
my $dyad_server = Dyad::Server->new(mongodb => $db);

while ( $request->Accept() >= 0 ) {
    print $dyad_server->process_request();
}
