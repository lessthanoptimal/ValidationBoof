#!/usr/bin/env python3

import urllib.request
from subprocess import call

files = ["bundle_adjustment_in_the_large.tar.bz2"]

for f in files:
    print("retrieving "+f)
    url = "https://boofcv.org/notwiki/regression/sba/"+f
    urllib.request.urlretrieve(url,f)
    call(["tar","-xjf",f])