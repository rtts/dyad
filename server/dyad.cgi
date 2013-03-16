#!/usr/bin/perl -w
use strict;

print "Content-type: text/html\n\n";
print "hoi!\n";
exit;

my $sdp_offer;
my $max_length = 10000;
my $supposed_length = $ENV{"CONTENT_LENGTH"};

yourfault("There's no body in your request, dummy!") if not $supposed_length;
yourfault("The body of your request is too freaking large!") if $supposed_length > $max_length;
my $actual_length = read(STDIN, $sdp_offer, $supposed_length) or yourfault("Where's your body, again?");
yourfault("Your body isn't as long as you say it is...") unless $actual_length == $supposed_length;

# TODO: send GCM message to the other person

# TODO: spawn a server waiting for the other person to contact back

print "Status: 200\n\n";

# TODO: print the sdp_response to stdout

sub yourfault {
    print "Status: 400\n\n", @_, "\n";
    exit;
}

sub ourfault {
    print "Status: 500\n\n", @_, "\n";
    exit;
}
