#!/usr/bin/perl

# step 1:
$sdp_offer = <STDIN>;

# step 2:
unless (&c2dm_request()) {
	print "Status: 502\n\n";
	exit;
}

$ENV{'QUERY_STRING'} == "/dyad/api/v2/dyad.cgi";
$ENV{'REQUEST_METHOD'} == "POST";

my $sdp_answer = &spawn_server(8484);

# step 7:
print "Status: 200\n\n";
print $sdp_answer;


sub c2dm_request() {
	#TODO
	1;
}

sub retrieve_answer socket {
	bind socket Server
	listen Server, 1
	accept Client Server

	print Client "You can start ICEing now!"
	return $sdp_answer;
}
