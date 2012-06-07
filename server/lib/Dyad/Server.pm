package Dyad::Server;

use 5.006;
use strict;
use warnings;
use FCGI;
use JSON;
use Try::Tiny;
use HTTP::Status qw(:constants status_message);

require Exporter;
our @ISA       = qw(Exporter);
our @EXPORT_OK = qw(run register get_body http_response);

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

my $Routing_table = { '/register' => \&register };

sub respond;
sub get_body;

=head2 run

Main function of this module. Starts the Dyad FCGI server on the specified host and port.
This call should never return, but make sure to run a process manager anyway!

=cut

sub run {
	my $socket = FCGI::OpenSocket( shift, 100 );
	my $request = FCGI::Request( \*STDIN, \*STDOUT, \*STDERR, \%ENV, $socket );

	while ( $request->Accept() >= 0 ) {

		#print "Content-type: text/plain\r\n\r\n";
		#for ( keys %ENV ) {
		#	print "$_: $ENV{$_}\n";
		#}

		my $func = $Routing_table->{ $ENV{REQUEST_URI} };
		print http_response HTTP_NOT_FOUND, "unknown API request" if not $func;
		print http_response $func->();
	}
}

sub register {
	return 405, "expecting POST request"
	  if not $ENV{'REQUEST_METHOD'} eq 'POST';
	( my $body, my $message ) = get_body;
	return 400, $message if not defined $body;

	my $token   = $body->{"token"}   or return 422, "token missing";
	my $c2dm_id = $body->{"c2dm_id"} or return 422, "c2dm_id missing";

}

=head2 get_body

Reads the body from the standard input in a cgi context, decodes and returns it as json.
On error, returns a list of which the first value is undefined, and the second is a string
that describes the error.

=cut

sub get_body {
	return undef, "no body found in request"
	  if not $ENV{'CONTENT_LENGTH'};

	my $length = read( STDIN, my $body, $ENV{'CONTENT_LENGTH'} );
	return undef, "reported content length does not match actual length"
	  if $ENV{'CONTENT_LENGTH'} != $length;

	try {
		return decode_json $body;
	}
	catch {
		return undef, "json could not be decoded";
	}
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
	my $result = "Status: $status\r\n";
	$result = "${result}Content-type: text/plain\r\n\r\n$body" if $body;
	return $result;
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
