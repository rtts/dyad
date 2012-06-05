package Dyad::Server;

use strict;
use FCGI;
use JSON;

my $routing_table = { '/register' => \&register };

sub respond;
sub get_body;

my $socket = FCGI::OpenSocket( "127.0.0.1:3454", 100 );
my $request = FCGI::Request( \*STDIN, \*STDOUT, \*STDERR, \%ENV, $socket );
client: while ( $request->Accept() >= 0 ) {

	#print "Content-type: text/plain\r\n\r\n";

	#for ( keys %ENV ) {
	#	print "$_: $ENV{$_}\n";
	#}
	my $func = $routing_table->{ $ENV{REQUEST_URI} };
	respond 404, "unknown API request" if not $func;
	$func->();
}

sub register {
	my $body    = get_body;
	my $token   = $_->{"token"} or respond 422, "token missing";
	my $c2dm_id = $_->{"c2dm_id"} or respond 422, "c2dm_id missing";

}

sub respond {
	my $status = shift;
	my $body   = shift;
	print "Status: $status\r\n";
	print "Content-type: text/plain\r\n\r\n";
	print $body;
	next client;
}

sub get_body {
	respond 405, "only POST request is allowed"
	  if not $ENV{'REQUEST_METHOD'} eq 'POST';
	respond 400, "no body found in POST request"
	  if not $ENV{'CONTENT_LENGTH'};

	my $length = read( STDIN, my $body, $ENV{'CONTENT_LENGTH'} );
	respond 400, "reported content length does not match actual length"
	  if $ENV{'CONTENT_LENGTH'} != $length;

	return (decode_json $body) or respond 400, "json could not be decoded";
}
