package Dyad::Server;
our $VERSION = '0.01';

=head1 NAME

Dyad::Server - The signalling server accompanying Dyad (see http://r2src.github.com/dyad/).

=head1 SYNOPSIS

The Dyad Server is used to set up direct peer-to-peer streams between two Android devices.
It keeps track of users, sessions, bonds, and streams.

Perhaps a little code snippet.
It can for example be approached with an FCGI script:

    use Dyad::Server;
    use FCGI;
    use MongoDB::Connection;
    use Log::Any::Adapter ('File', '/path/to/log/file.log');

    my $request =
      FCGI::Request( \*STDIN, \*STDOUT, \*STDERR, \%ENV,
        FCGI::OpenSocket( '0.0.0.0:3454', 100 ) );
    my $conn        = MongoDB::Connection->new;
    my $db          = $conn->'YOUR_DB_NAME';
    my $dyad_server = Dyad::Server->new(mongodb => $db);

    while ( $request->Accept() >= 0 ) {
        print $dyad_server->process_request();
    }

=cut

use v5.14;
use strict;
use Log::Any qw($log);
use JSON;
use MongoDB;
use HTTP::Status qw(:constants status_message);
use LWP::UserAgent;
require HTTP::Request;
require Exporter;

# change this from outside when testing
our $GOOGLE_URL = 'https://www.googleapis.com/oauth2/v2/userinfo';

my $http_client = LWP::UserAgent->new;

our $api = [
    [
        0,
        POST => qr(^/v1/register$),
        \&register => [ 'token' ]
    ],

	[
        1,    # authorization required
		POST => qr(^/v1/register_gcm$),
		\&register_gcm => ['gcm_id']
	],

    [
        1,    # authorization required
        POST   => qr(^/v1/bond$), #r
        \&bond => ['secret']
    ],

    [
        1,  # authorization required
        POST => qr(^/v1/sdp_message$), #r
        \&sdp_message => ['message']
    ],

    [
        1,  #authorization required
        GET => qr(^/v1/sdp_message$), #r
        \&get_sdp_message => []
    ]
];

=head1 EXPLANATION

=head2 new

Classic Perl half-assed OO.

=cut

sub new {
    my $class  = shift;
    my %kwargs = @_;

    my $self = {};

    $self->{db} = $kwargs{mongodb} and $self->{db}->isa("MongoDB::Database")
      or die "You have to pass a MongoDB instance to Dyad::Server::new\n";
	
	$log->info("Dyad Server ready.");
	
    bless $self, $class;
}

=head2 process_request

Processes an FCGI request.

=cut

sub process_request {
    my $self = shift;

	$log->info("Received $ENV{REQUEST_METHOD} request for $ENV{REQUEST_URI}");

    for my $call ( @{$api} ) {
        if (    $ENV{REQUEST_METHOD} eq $call->[1]
            and $ENV{REQUEST_URI} =~ $call->[2] )
        {
        	$log->debug("Request matches entry '$call->[2]' in the dispatch table.");

            my $google_id;
            if ( $call->[0] ) {
            	$log->debug("Request requires authorization.");
            
                my $session_token = $ENV{HTTP_X_DYAD_AUTHORIZATION};
				unless ($session_token) {
					$log->warning("Authorization failed: session token not present.");
	                return http_response( 401, "Missing X-Dyad-Authorization header." );
				}

				$log->debug("Validating session token...");
                # note: Devel::Cover fails after the following statement
                # (please tell me why!)
                my $user =
                  $self->{db}->users->find_one(
                    { session_token => $session_token } );
                unless ($user) {
                	$log->warning("Authorization failed: session token invalid.");
                	return http_response( 401, "Invalid session token. Please re-register." );
                }
                
                $google_id = $user->{google_id};
                unless ($google_id) {
                	$log->error("User with id $user->{id} has no attribute google_id! Have you been messing with the database?");
                	return http_response(500, "We're really sorry. This shouldn't have happened.");
                }

                $log->debug("Authorization succeeded.");
            }

			$log->debug("Validating request parameters...");
			
            given ( $call->[1] ) {
                when ( 'GET' ) {
                	if ($ENV{CONTENT_LENGTH}) {
						$log->warning("GET request has a body, aborting.");                    	
                    	return http_response( 400,
                        	"Request contains a body, but shouldn't" )
                	}

					$log->debug("GET request is valid, dispatching...");
                    return http_response(
                        $call->[3]->( $self->{db}, $google_id ) );
                }
                when ( 'POST' ) {
                    my $body = &stdin;
                    
                    unless (defined $body) {
                    	$log->warning("POST request has incorrect (or no) content length, aborting.");
	                    return http_response( 400,
    	                    "Body is empty or wrong content length." )
                    }

                    # parse request body into json
                    $body = json_to_hashref($body);
                    unless ($body) {
                    	$log->warning("Request body contains invalid JSON, aborting.");
                    	return http_response( 400, "Invalid JSON." );
                	}
                	
					$log->debug("Looking for the following parameters: " . join(", ", @{$call->[4]}));
                    my @params;
                    for my $req ( @{$call->[4]} ) {
                        if ( defined $body->{$req} ) {
                            push @params, $body->{$req};
                        }
                        else {
                        	$log->warning("Missing required parameter $req, aborting.");
                            return http_response( 400,
                                "Missing required parameter '$req'." );
                        }
                    }

                    $log->debug("POST request is valid, dispatching...");
                    return http_response(
                        $call->[3]->( $self->{db}, @params, $google_id ) );
                }
            }
        }
    }
    $log->warning("Unknown request, returning 404.");
    return http_response( 404, "Unknown request." );
}

=head1 INTERNAL SUBROUTINES

These subroutines are only used internally, but if you really want you can
import and use them directly.

=cut

sub register;
sub register_gcm;
sub bond;
sub sdp_message;
sub get_sdp_message;
sub stdin;
sub http_response;
sub json_to_hashref;

our @ISA       = qw(Exporter);
our @EXPORT_OK = qw(register register_gcm bond sdp_message get_sdp_message stdin http_response json_to_hashref);

=head2 register

Should be called with a valid Google OAuth2 token 

Returns the tuple ($status_code, $body) where $body might be a hashref or an
error string.

=cut

sub register {
    my $db      = shift;
    my $token   = shift;

    my $request = HTTP::Request->new(
        GET => $GOOGLE_URL,
        [ Authorization => "OAuth $token" ]
    );
    my $response = $http_client->request($request);
    if ( $response->is_success ) {

        my $json = json_to_hashref $response->decoded_content;
        return 500, "Google has a virus." unless $json;
        my $str = $response->as_string;
        $log->info("GOOGLE RETURNED: $str");
        my $google_id     = $json->{id};
        my $session_token = '';
        $session_token .= chr( ( rand 93 ) + 33 ) for ( 1 .. 32 );

        $db->users->update(
            { google_id => $google_id },
            {
                '$set' => {
                    session_token => $session_token
                }
            },
            { upsert => 1 }
        );

        return 200, { session_token => $session_token };
    }
    else {
        return 401, "Auth token rejected by Google.";
    }
}

=head2 register_gcm

Adds the supplied gcm id to the database.

=cut

sub register_gcm {
	my $db = shift;
	my $gcm_id = shift;
	my $google_id = shift;
	
    if ( $gcm_id ) {
	    $db->users->update(
            { google_id => $google_id },
            {
                '$set' => {
                    gcm_id => $gcm_id
                }
            }
        );
        return 200, "GCM id has been set.";
    }
    else {
    	return 400, "GCM id should not be empty.";
    }

	
}

=head2 bond

Should be called with a couple's shared secret and a user id.

Returns the tuple ($status_code, $body) where $body is a status string.

=cut

sub bond {
    my $db        = shift;
    my $secret    = shift;
    my $google_id = shift;

    # TODO: use timestamps
    # in this case for selecting the most recent secret

    # Retrieve an _other_ user with the same secret and unset it
    # and set the other field (all in the same atomic operation!)
    my $other = $db->run_command(
        {
            findAndModify => 'users',
            query         => {
                secret    => $secret,
                google_id => { '$ne' => $google_id }
            },
            update => {
                '$unset' => { secret => 1 },
                '$set'   => { other  => $google_id }
            }
        }
    )->{value};

    if ($other) {
        $db->users->update(
            { google_id => $google_id },
            {
                '$set'   => { other  => $other->{google_id} },
                '$unset' => { secret => 1 }
            }
        );

        # TODO: send c2dm push to other.

        return 200, "Successfully bonded.";
    }
    else {
        $db->users->update(
            { google_id => $google_id },
            { '$set'    => { secret => $secret } }
        );
        return 202, "Please wait for other to send secret";
    }
}

sub sdp_message {
    my $db = shift;
    my $message = shift;
    my $google_id = shift;
    $db->users->update(
        { google_id => $google_id },
        { '$set'  => { message => $message } }
    );
    # TODO: send gcm push to other, containing sdp message
    return 202, "Please wait for sdp answer";
}

sub get_sdp_message {
    my $db = shift;
    my $google_id = shift;
    my $other = $db->users->find_one({other => $google_id});
    if ($other && $other->{message}) {
        return 200, $other->{message};
    }
    else {
        return 404, "Please wait for other to send offer";
    }
}


=head2 stdin

For use in a CGI context. Reads the correct amount of bytes from standard
input and returns it. On error, returns an undefined value.

=cut

sub stdin {
    return if not $ENV{'CONTENT_LENGTH'};
    my $length = read( STDIN, my $body, $ENV{'CONTENT_LENGTH'} );
    return if $ENV{'CONTENT_LENGTH'} != $length;
    return $body;
}

=head2 http_response

Returns a proper CGI response given an http status code and an optional
second argument. If this argument is a hash reference, it will encode it
to JSON and return an application/json response. Otherwise, returns a text/plain
response with the unaltered second argument as the body.

Returns a 500 Internal Server Error response if the status
code is omitted or invalid.

=cut

sub http_response {
    my $status = shift;
    my $body = shift;
	my $status_message = status_message $status if defined $status;
	
    unless ($status_message) {
    	$status = 500;
    	$body = $status_message = "Internal Server Error";
    }

    my $type = "text/plain";
    if ( ref $body eq 'HASH' ) {
        $body = encode_json $body;
        $type = "application/json";
    }

    my $result = "Status: $status\r\n";
    $result .= "Content-type: $type\r\n\r\n$body" if $body;
    
    $log->info("Returned $status $status_message");
    return $result;
}

=head2 json_to_hashref

A small wrapper function for decode_json that traps its croaks.

=cut

sub json_to_hashref {
    my $json = eval { decode_json shift };
    $json = 0 if ref $json ne "HASH";
    return $json;
}

=head1 AUTHOR

Return to the Source, C<< <info at r2src.com> >>

=head1 BUGS

Please report any bugs or feature requests to C<bug-dyad-server at rt.cpan.org>, or through
the web interface at L<http://rt.cpan.org/NoAuth/ReportBug.html?Queue=Dyad-Server>.  I will be notified, and then you'll
automatically be notified of progress on your bug as I make changes.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc Dyad::Server


You can also look for information at:

=over 4

=item * RT: CPAN's request tracker (report bugs here)

L<http://rt.cpan.org/NoAuth/Bugs.html?Dist=Dyad-Server>

=item * AnnoCPAN: Annotated CPAN documentation

L<http://annocpan.org/dist/Dyad-Server>

=item * CPAN Ratings

L<http://cpanratings.perl.org/d/Dyad-Server>

=item * Search CPAN

L<http://search.cpan.org/dist/Dyad-Server/>

=back


=head1 ACKNOWLEDGEMENTS


=head1 LICENSE AND COPYRIGHT

Copyright 2012 Return to the Source.

This program is free software; you can redistribute it and/or modify it
under the terms of either: the GNU General Public License as published
by the Free Software Foundation; or the Artistic License.

See http://dev.perl.org/licenses/ for more information.


=cut

1;    # End of Dyad::Server

