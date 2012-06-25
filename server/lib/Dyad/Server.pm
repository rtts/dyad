package Dyad::Server;
our $VERSION = '0.01';

=head1 NAME

Dyad::Server - The great new Dyad::Server!

=head1 SYNOPSIS

Quick summary of what the module does.

Perhaps a little code snippet.

    use Dyad::Server;

    my $foo = Dyad::Server->new();
    ...

=cut

use v5.14;
use strict;
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
        POST => '^/register$',
        \&register => [ 'token', 'c2dm_id' ]
    ],

    [
        1,    # authorization required
        POST   => '^/bond$',
        \&bond => ['secret']
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

    bless $self, $class;
}

=head2 process_request

Processes an FCGI request.

=cut

sub process_request {
    my $self = shift;

    for my $call ( @{$api} ) {
        if (    $ENV{REQUEST_METHOD} eq $call->[1]
            and $ENV{REQUEST_URI} =~ $call->[2] )
        {

            my $google_id;
            if ( $call->[0] ) {    # if authorization required
                my $session_token = $ENV{HTTP_X_DYAD_AUTHORIZATION}
                  or return http_response( 401,
                    "Missing X-Dyad-Authorization header." );

                # note: Devel::Cover fails after the following statement
                # (please tell me why!)
                my $user =
                  $self->{db}->users->find_one(
                    { session_token => $session_token } )
                  or return http_response( 401,
                    "Invalid session token. Please re-register." );

                $google_id = $user->{google_id};
            }

            given ( $call->[1] ) {
                when ( 'GET' ) {
                    return http_response( 400,
                        "Request contains a body, but shouldn't" )
                      if $ENV{CONTENT_LENGTH};

                    return http_response(
                        $call->[3]->( $self->{db}, $google_id ) );
                }
                when ( 'POST' ) {
                    my $body = &stdin;
                    return http_response( 400,
                        "Body is empty or wrong content length." )
                      if not defined $body;

                    # parse request body into json
                    $body = json_to_hashref($body)
                      or return http_response( 400, "Invalid JSON." );

                    # check if the required parameters are present,
                    # and store them in @params
                    my @params;
                    for my $req ( @{$call->[4]} ) {
                        if ( defined $body->{$req} ) {
                            push @params, $body->{$req};
                        }
                        else {
                            return http_response( 400,
                                "Missing required parameter '$req'." );
                        }
                    }

                    # run api function with the neccesary parameters
                    return http_response(
                        $call->[3]->( $self->{db}, @params, $google_id ) );
                }
            }
        }
    }
    return http_response( 404, "API function not found." );
}

=head1 INTERNAL SUBROUTINES

These subroutines are only used internally, but if you really want you can
import and use them directly.

=cut

sub register;
sub bond;
sub stdin;
sub http_response;
sub json_to_hashref;

our @ISA       = qw(Exporter);
our @EXPORT_OK = qw(register bond stdin http_response json_to_hashref);

=head2 register

Should be called with a valid Google OAuth2 token and C2DM registration id. 

Returns the tuple ($status_code, $body) where $body might be a hashref or an
error string.

=cut

sub register {
    my $db      = shift;
    my $token   = shift;
    my $c2dm_id = shift;

    my $request = HTTP::Request->new(
        GET => $GOOGLE_URL,
        [ Authorization => "OAuth $token" ]
    );
    my $response = $http_client->request($request);
    if ( $response->is_success ) {
        my $json = json_to_hashref $response->decoded_content;
        return 500, "Google has a virus." unless $json;

        my $google_id     = $json->{id};
        my $session_token = '';
        $session_token .= chr( ( rand 93 ) + 33 ) for ( 1 .. 32 );

        $db->users->update(
            { google_id => $google_id },
            {
                '$set' => {
                    c2dm_id       => $c2dm_id,
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
    my $status = ( shift or 500 );
    $status = 500 unless status_message $status;
    my $body = $status == 500 ? "Internal Server Error" : shift;
    my $type = "text/plain";

    if ( ref $body eq 'HASH' ) {
        $body = encode_json $body;
        $type = "application/json";
    }

    my $result = "Status: $status\r\n";
    $result .= "Content-type: $type\r\n\r\n$body" if $body;
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

