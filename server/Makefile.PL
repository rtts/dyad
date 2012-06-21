use 5.006;
use strict;
use warnings;
use ExtUtils::MakeMaker;

WriteMakefile(
    NAME                => 'Dyad::Server',
    AUTHOR              => q{Return to the Source <info@r2src.com>},
    VERSION_FROM        => 'lib/Dyad/Server.pm',
    ABSTRACT_FROM       => 'lib/Dyad/Server.pm',
    ($ExtUtils::MakeMaker::VERSION >= 6.3002
      ? ('LICENSE'=> 'perl')
      : ()),
    PL_FILES            => {},
    PREREQ_PM => {
        'Test::More' => 0,
    },
    dist                => { COMPRESS => 'gzip -9f', SUFFIX => 'gz', },
    clean               => { FILES => 'Dyad-Server-* cover_db' },
);
