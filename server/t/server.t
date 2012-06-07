use strict;
use JSON;
use Data::Compare;

use Dyad::Server qw(run get_body http_response);

use Test::More tests => 16;

#####################
# Tests of get_body #
#####################

sub init {
	my $json = ( shift or "" );
	my $content_length = shift;
	my $fh;

	# yes, you can assign a filehandle to a string ref!
	open $fh, '<', \$json;

	# and you can also reassign STDIN!
	local *STDIN = $fh;

	$ENV{CONTENT_LENGTH} = $content_length;

	return get_body();
}

my ( $body, $message, $json );

$json = encode_json { key1 => "value1", key2 => 42 };

( $body, undef ) = init $json, length $json;
ok Compare( $body, decode_json $json ),
  "normal case: returns correctly parsed hash from json";

( $body, $message ) = init undef, 0;
is $body, undef,
  "returns undef when 0 content length and no body provided on STDIN";
isnt $message, undef,
  "error message is defined when 0 content length and empty body";

( $body, $message ) = init $json, 0;
is $body, undef,
  "returns undef when 0 content length when a body _is_ provided on STDIN";
isnt $message, undef,
  "error message is defined when 0 content length and _no_ empty body";

( $body, $message ) = init $json, ( length $json ) - 1;
is $body, undef, "returns undef when content length is too small";
isnt $message, undef,
  "error message is defined when content length is too small";

( $body, $message ) = init $json, ( length $json ) + 1;
is $body,      undef, "returns undef when content length is too big";
isnt $message, undef, "error message is defined when content length is too big";

$json = 'invalid json ^&$*%^&$*((';
( $body, $message ) = init $json, length $json;
is $body,      undef, "returns undef when body is malformed json";
isnt $message, undef, "error message is defined when body is malformed json";



##########################
# Tests of http_response #
##########################

is http_response(200), "Status: 200\r\n",
  "call without body results in no body";

is http_response(), "Status: 500\r\n",
  "wrong function call results in 500 (internal server error)";

is http_response( 404, "BODY" ),
  "Status: 404\r\nContent-type: text/plain\r\n\r\nBODY",
  "call with body results in body";

is http_response(999), "Status: 500\r\n",
  "invalid status code produces status 500 (internal server error)";

is http_response( undef, "BODY" ), "Status: 500\r\n",
  "body without status code produces status 500 (internal server error)";
