use TAP::Harness;
my $harness = TAP::Harness->new(
	{
		verbosity  => 1,
		timer      => 1,
		show_count => 1
	}
  );

# Run all tests that end in .t
$harness->runtests(<./*.t>);
