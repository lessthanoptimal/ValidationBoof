#!/usr/bin/env python2

import urllib
from subprocess import call

files = ["disparity_v1.zip"]

for f in files:
    print("retrieving "+f)
    thefile = urllib.URLopener()
    thefile.retrieve("https://boofcv.org/notwiki/regression/disparity/"+f,f)
    call(["unzip",f])

