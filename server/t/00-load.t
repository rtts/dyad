use Test::More tests => 1;

BEGIN {
    use_ok( 'Dyad::Server' ) || print "Bail out!\n";
}

diag( "Testing Dyad::Server $Dyad::Server::VERSION, Perl $], $^X" );
