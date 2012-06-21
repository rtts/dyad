use strict;
use JSON;
use lib 't';    # the Mock::Google module lives in the test directory
use Mock::Google;
use Test::More tests => 46;
use Dyad::Server qw(register bond http_response json_to_hashref);

my $PORT = 8899;
$Dyad::Server::GOOGLE_URL = "http://localhost:$PORT";
my $DB_NAME = 'testdb';

my $conn  = MongoDB::Connection->new;
my $db    = $conn->$DB_NAME;
my $users = $db->users;

################
# Tests of new #
################

ok defined Dyad::Server->new( mongodb => $db )->{db},
  "Instantiating server object creates MongoDB reference.";

ok not( defined eval { Dyad::Server->new; 1 } ),
  "You have to pass the key mongodb to the constructor.";

ok not( defined eval { Dyad::Server->new( mongodb => 'invalid_db' ); 1 } )
  ,
  "You have to pass a _MongoDB_ instance as the value for the key mongodb";

############################
# Tests of process_request #
############################

my $server = Dyad::Server->new( mongodb => $db );

$Dyad::Server::api = [
    [
        0,
        GET => '^/url$',
        sub {
            my $db = shift;
            ok $db->isa("MongoDB::Database"), "First argument is a MongoDB Database.";
            return 200, "Sub executed.";
        } => [  ]
    ],
    [
        1,    # authorization required
        GET   => '^/bond$',
        \&bond => ['secret']
    ]
];

$ENV{REQUEST_METHOD} = 'GET';
$ENV{REQUEST_URI} = '/url';
my $response = $server->process_request;
is $response, http_response(200, "Sub executed."), "Right response.";

#####################
# Tests of register #
#####################

$users->remove;

my $pid = Mock::Google->new($PORT)->background();

# first registration
my ( $status, $body ) = register $db, 'valid_token', 'c2dm_id';
is $status, 200, "Registering with a valid token succeeds.";
ok defined $body, "There is a body in the response";
is ref $body, 'HASH', "The body is a hashref.";
my $stoken = $body->{session_token};
ok defined $stoken, "The response contains a key 'session_token'";
like $stoken, qr/^.{32}$/,
  "There is a valid session_token in the response body";
my $cursor = $users->find;
my $user   = $cursor->next;
ok defined $user, "After registering the user is present in the database.";
is undef, $cursor->next, "There is only one user inserted in the database.";
is $user->{google_id}, 1, "The id returned by the google server is stored.";
is $user->{session_token}, $stoken, "A session token is generated and stored.";
is $user->{c2dm_id}, 'c2dm_id', "The right c2dm_id is stored.";

# re-registration
( $status, $body ) = register $db, 'valid_token', 'new_c2dm_id';
is $status, 200, "Re-registering with a valid token succeeds.";
ok defined $body, "There is a body in the response";
like $body->{session_token}, qr/^.{32}$/,
  "There is a valid session_token in the response body";
isnt $body->{session_token}, $stoken,
  "Re-registering results in a new session token.";
$cursor = $users->find;
$user   = $cursor->next;
ok defined $user,
  "After re-registering the user is still present in the database.";
is undef, $cursor->next, "There is still only one user in the database.";
is $user->{google_id}, 1, "The id returned by the google server is stored.";
is $user->{session_token}, $body->{session_token},
  "A new session token is generated and stored.";
is $user->{c2dm_id}, 'new_c2dm_id', "The new c2dm_id is stored.";

# invalid registration
$users->remove;
( $status, $body ) = register $db, 'invalid_token', 'c2dm_id';
is $status, 401, "Registering with a invalid token fails.";
ok defined $body, "There is a body in the response";
is undef, $users->find_one, "Failed registrations do not touch the database.";

kill 15, $pid;

#################
# Tests of bond #
#################

my ( $token1, $token2 );
$token1 .= 'a' for ( 1 .. 32 );
$token2 .= 'b' for ( 1 .. 32 );

# user submitting existing secret
$users->remove;
$users->batch_insert(
    [
        {
            google_id     => 1,
            session_token => $token1,
            c2dm_id       => 'a'
        },
        {
            google_id     => 2,
            secret        => 'secret',
            session_token => $token2,
            c2dm_id       => 'a'
        }
    ]
);

( $status, $body ) = bond $db, 'secret', 1;
my $user1 = $users->find_one( { google_id => 1 } );
my $user2 = $users->find_one( { google_id => 2 } );

is $status, 200, "Bonding results in a 200 response.";
ok defined $body, "Response has a body.";
is $user1->{other}, $user2->{google_id}, "User 1 is now bonded to user 2.";
is $user2->{other}, $user1->{google_id}, "User 2 is now bonded to user 1.";
ok not( exists $user2->{secret} ), "The secret has disappeared.";

# user submitting new secret
$users->remove;
$users->insert(
    {
        google_id     => 1,
        session_token => $token1,
        c2dm_id       => 'a'
    }
);
( $status, $body ) = bond $db, 'secret', 1;
$user1 = $users->find_one( { google_id => 1 } );
$user2 = $users->find_one( { google_id => 2 } );

is $status, 202, "New secret results in 202 response.";
ok defined $body, "Response has a body.";
is $user1->{secret}, 'secret', "Secret is inserted in the database";

# user re-submits secret
$users->remove;
$users->insert(
    {
        google_id     => 1,
        secret        => 'secret',
        session_token => $token1,
        c2dm_id       => 'a'
    }
);
( $status, $body ) = bond $db, 'secret', 1;
$user1 = $users->find_one( { google_id => 1 } );

is $status, 202, "Re-submitting same secret results in 202 response.";

######################
## Tests of get_body #
######################
#
#sub init {
#	my $json = ( shift or "" );
#	my $content_length = shift;
#	my $fh;
#
#	# yes, you can assign a filehandle to a string ref!
#	open $fh, '<', \$json;
#
#	# and you can also reassign STDIN!
#	local *STDIN = $fh;
#
#	$ENV{CONTENT_LENGTH} = $content_length;
#
#	return get_body();
#}
#
#my ( $body, $message, $json );
#
#$json = encode_json { key1 => "value1", key2 => 42 };
#
#( $body, undef ) = init $json, length $json;
#ok Compare( $body, decode_json $json ),
#  "normal case: returns correctly parsed hash from json";
#
#( $body, $message ) = init undef, 0;
#is $body, undef,
#  "returns undef when 0 content length and no body provided on STDIN";
#isnt $message, undef,
#  "error message is defined when 0 content length and empty body";
#
#( $body, $message ) = init $json, 0;
#is $body, undef,
#  "returns undef when 0 content length when a body _is_ provided on STDIN";
#isnt $message, undef,
#  "error message is defined when 0 content length and _no_ empty body";
#
#( $body, $message ) = init $json, ( length $json ) - 1;
#is $body, undef, "returns undef when content length is too small";
#isnt $message, undef,
#  "error message is defined when content length is too small";
#
#( $body, $message ) = init $json, ( length $json ) + 1;
#is $body,      undef, "returns undef when content length is too big";
#isnt $message, undef, "error message is defined when content length is too big";
#
#$json = 'invalid json ^&$*%^&$*((';
#( $body, $message ) = init $json, length $json;
#is $body,      undef, "returns undef when body is malformed json";
#isnt $message, undef, "error message is defined when body is malformed json";

##########################
# Tests of http_response #
##########################

my $hashref = { key1 => "value1", key2 => 42 };
my $json = encode_json $hashref;

is http_response( 200, $hashref ),
  "Status: 200\r\nContent-type: application/json\r\n\r\n$json",
  "passing a hashref encodes it to json";

is http_response( 404, "BODY" ),
  "Status: 404\r\nContent-type: text/plain\r\n\r\nBODY",
  "call with body results in body";

is http_response(200), "Status: 200\r\n",
  "call without body results in no body";

like http_response(), qr(Status: 500\r\nContent-type: text/plain\r\n\r\n.+),
  "wrong function call results in 500 (internal server error)";

like http_response(999), qr(Status: 500\r\nContent-type: text/plain\r\n\r\n.+),
  "invalid status code produces status 500 (internal server error)";

like http_response( undef, "BODY" ),
  qr(Status: 500\r\nContent-type: text/plain\r\n\r\n.+),
  "body without status code produces status 500 (internal server error)";

############################
# Tests of json_to_hashref #
############################

my $good_string = '{ "valid": "json_string" }';
$hashref = json_to_hashref $good_string;
is ref $hashref, 'HASH', "Returns a hashref.";
is $hashref->{valid}, "json_string", "The returned hash contains the JSON key.";
is json_to_hashref("invalid"), 0, "Returns 0 on invalid JSON strings.";
is json_to_hashref('["json", "array"]'), 0, "Returns 0 on JSON arrays.";
