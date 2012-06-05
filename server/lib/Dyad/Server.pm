package Dyad::Server;

use 5.014002;
use strict;
use warnings;
use FCGI;
use JSON;

#require Exporter;
#
#our @ISA = qw(Exporter);
#
## Items to export into callers namespace by default. Note: do not export
## names by default without a very good reason. Use EXPORT_OK instead.
## Do not simply export all your public functions/methods/constants.
#
## This allows declaration	use Dyad::Server ':all';
## If you do not need this, moving things directly into @EXPORT or @EXPORT_OK
## will save memory.
#our %EXPORT_TAGS = (
#	'all' => [
#		qw(
#
#		  )
#	]
#);
#
#our @EXPORT_OK = ( @{ $EXPORT_TAGS{'all'} } );

our @EXPORT = qw(

);

# TODO: Use Error.pm to throw exceptions!


our $VERSION = '0.01';

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

1;
__END__
# Below is stub documentation for your module. You'd better edit it!

=head1 NAME

Dyad::Server - Perl extension for blah blah blah

=head1 SYNOPSIS

  use Dyad::Server;
  my $server = Dyad::Server->new("127.0.0.1:8000");
  $server->start();
  
=head1 DESCRIPTION

Stub documentation for Dyad::Server, created by h2xs. It looks like the
author of the extension was negligent enough to leave the stub
unedited.

Blah blah blah.

=head2 EXPORT

None by default.



=head1 SEE ALSO

Mention other useful documentation such as the documentation of
related modules or operating system documentation (such as man pages
in UNIX), or any relevant external documentation such as RFCs or
standards.

If you have a mailing list set up for your module, mention it here.

If you have a web site set up for your module, mention it here.

=head1 AUTHOR

Jolanda Verhoef, E<lt>lojanda@E<gt>

=head1 COPYRIGHT AND LICENSE

Copyright (C) 2012 by Jolanda Verhoef

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself, either Perl version 5.14.2 or,
at your option, any later version of Perl 5 you may have available.


=cut
