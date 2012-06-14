package Dyad::Server;

use v5.14;
use strict;
use warnings;
use FCGI;
use JSON;
use Try::Tiny;
use HTTP::Status qw(:constants status_message);
use LWP::UserAgent;
require HTTP::Request;
require Exporter;

sub register;
sub process_request;
sub stdin;
sub json;
sub http_response;

our @ISA       = qw(Exporter);
our @EXPORT_OK = qw(register process_request stdin json http_response);

=head1 NAME

Dyad::Server - The great new Dyad::Server!

=head1 VERSION

Version 0.01

=cut

our $VERSION = '0.01';

=head1 SYNOPSIS

Quick summary of what the module does.

Perhaps a little code snippet.

    use Dyad::Server;

    my $foo = Dyad::Server->new();
    ...

=head1 SUBROUTINES/METHODS

=head2 new

=cut

my $http_client = LWP::UserAgent->new;
my $dbh;
my $google_url = 'https://www.googleapis.com/oauth2/v2/userinfo';

=head2 run

Main function of this module. Starts the Dyad FCGI server on the specified host and port.
This call should never return, but make sure to run a process manager anyway!

=cut

my $api = [
    [
        0,
        POST => '^/register$',
        \&register => [ 'token', 'c2dm_id' ]
    ],

    [
        1,    # authorization required
        POST => '^/bond$',
        \&bond => [ 'secret' ]
    ],

    [
        GET       => '^/session$',
        \&session => [],
        'authorization_required'
    ]
];

# classic Perl half-assed OO
sub new {
    my $class = shift;
    $dbh = shift;
    my $url = shift;
    $google_url = $url if $url;
    my $self = {};
    bless $self, $class;
    return $self;
}

sub register {
    my $token   = shift;
    my $c2dm_id = shift;

    my $request = HTTP::Request->new(
        GET => $google_url,
        { Authorization => "OAuth $token" }
    );
    my $response = $http_client->request($request);
    if ( $response->is_success ) {
        my $google_id;
        try {
            my $json = decode_json $response->decoded_content;
            $google_id = $json->{id};
        }
        catch {
            return 500, "Google has a virus.";
        }

        my $session_token .= chr( ( rand 93 ) + 33 ) for ( 1 .. 32 );
        
        $dbh->do(
            "INSERT OR REPLACE INTO Users (google_id, c2dm_id, session, other_id) 
             VALUES (?,?,?, (SELECT other_id FROM Users WHERE google_id = ?));",
            $google_id, $c2dm_id, $session_token, $google_id
        );

        return 200, { session_token => $session_token };
    }
    else {
        return 401, "Auth token rejected by Google.";
    }
}

sub bond {
    my $secret = shift;
    
    my $sth = $dbh->do("SELECT * FROM Users WHERE secret = ?", $secret);
    
}

=head1 process_request


=cut

sub process_request {

    $dbh = shift;

    for my $call ( @{$api} ) {
        if (    $ENV{REQUEST_METHOD} eq $call->[1]
            and $ENV{REQUEST_URI} =~ $call->[2] )
        {

            if ( $call->[0] ) {    # if authorization required

                # TODO: authorize
            }

            given ( $call->[1] ) {
                when ( 'GET' or 'DELETE' ) {
                    return http_response 400,
                      "Request contains a body, but shouldn't"
                      if $ENV{CONTENT_LENGTH};

                    return http_response $call->[3]->();
                }
                when ( 'PUT' or 'POST' ) {
                    my $body = stdin;
                    return http_response 400,
                      "Body is empty or wrong content length."
                      if not defined $body;

                    # parse request body into json
                    $body = json($body) or return 400, "Invalid JSON.";
                    return 400, "Expected JSON object, got array."
                      unless ref $body eq 'HASH';

                    # check if the required parameters are present,
                    # and store them in @params
                    my @params;
                    for my $req ( $call->[4] ) {
                        if ( defined $body->{$req} ) {
                            push @params, $body->{$req};
                        }
                        else {
                            return 400, "Missing required parameter '$req'.";
                        }
                    }

                    # run api function with the neccesary parameters
                    return http_response $call->[3]->(@params);
                }
            }
        }
    }
}

=head2 get_body

Reads the body from the standard input in a cgi context, decodes and returns it as json.
On error, returns a list of which the first value is undefined, and the second is a string
that describes the error.

=cut

sub stdin {
    return if not $ENV{'CONTENT_LENGTH'};
    my $length = read( STDIN, my $body, $ENV{'CONTENT_LENGTH'} );
    return if $ENV{'CONTENT_LENGTH'} != $length;
    return $body;
}

=head2 http_response

Returns an http response string given an http status code and an optional
response body. Will return 500 Internal Server Error response if the status
code is omitted or invalid.

=cut

sub http_response {
    my $status = ( shift or 500 );
    $status = 500 unless status_message $status;
    my $body = shift unless $status == 500;
    if ( ref $body eq 'HASH' ) {
        try {
            $body = encode_json $body;
        }
        catch {
            $status = 500;
            $body   = "Error encoding JSON.";
        }
    }
    my $result = "Status: $status\r\n";
    $result = "${result}Content-type: text/plain\r\n\r\n$body" if $body;
    return $result;
}

sub json {
    my $body = shift;

    try {
        $body = decode_json $body;
    }
    catch {
        return;
    }
    return $body;
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

