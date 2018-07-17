#!/usr/bin/env python

import urllib
from subprocess import call

files = ["bundle_adjustment_in_the_large.tar.bz2"]

for f in files:
    print("retrieving "+f)
    thefile = urllib.URLopener()
    thefile.retrieve("https://boofcv.org/notwiki/regression/sba/"+f,f)
    call(["tar","-xjf",f])

