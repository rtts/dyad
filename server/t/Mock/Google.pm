package Mock::Google;

use JSON;
use base qw(HTTP::Server::Simple::CGI);
 
our $VERSION = '0.01';

sub handle_request {
    my ($self, $cgi) = @_;
    my $token = $ENV{HTTP_AUTHORIZATION};
    
    $token =~ s/OAuth //;
    if ($token eq 'valid_token') {
        print "HTTP/1.1 200 Google says OK\r\n";
        print "Content-Type: application/json\r\n\r\n";
        print encode_json {id => 1};
    } else {
        print "HTTP/1.1 401 Something went wrong\r\n";
        print "Content-Type: application/json\r\n\r\n";
        print encode_json {reason => "Invalid auth token"};
    }
    #... do something, print output to default
    # selected filehandle...
 
}
 
1;