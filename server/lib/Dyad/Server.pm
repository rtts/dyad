package Dyad::Server;

use 5.006;
use strict;
use warnings;
use FCGI;
use JSON;



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

sub new {
	my $class = shift;
	my $self  = {};
	$self->{SOCKET} = FCGI::OpenSocket( shift, 100 );
	$self->{REQUEST} =
	  FCGI::Request( \*STDIN, \*STDOUT, \*STDERR, \%ENV, $self->{SOCKET} );
	bless $self, $class;
	return $self;
}

sub DESTROY {
	my $self = shift;
	FCGI::CloseSocket $self->{SOCKET};
}

sub run {
	my $self = shift;
  client: while ( $self->{REQUEST}->Accept() >= 0 ) {

		#print "Content-type: text/plain\r\n\r\n";

		#for ( keys %ENV ) {
		#	print "$_: $ENV{$_}\n";
		#}
		my $func = $Routing_table->{ $ENV{REQUEST_URI} };
		respond 404, "unknown API request" if not $func;
		print $func->();
	}
}

sub register {
	return respond 405, "expecting POST request"
	  if not $ENV{'REQUEST_METHOD'} eq 'POST';
	my $result = get_body;

	my $token   = $_->{"token"}   or respond 422, "token missing";
	my $c2dm_id = $_->{"c2dm_id"} or respond 422, "c2dm_id missing";

}

sub get_body {
	return respond 400, "no body found in request"
	  if not $ENV{'CONTENT_LENGTH'};

	my $length = read( STDIN, my $body, $ENV{'CONTENT_LENGTH'} );
	return respond 400, "reported content length does not match actual length"
	  if $ENV{'CONTENT_LENGTH'} != $length;

	return ( ( decode_json $body)
		  or ( respond 400, "json could not be decoded" ) );
}

sub respond {
	my $status = shift;
	my $body   = shift;
	return "Status: $status\r\nContent-type: text/plain\r\n\r\n$body";
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

1; # End of Dyad::Server
