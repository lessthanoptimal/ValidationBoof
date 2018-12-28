#!/usr/bin/env python2

import urllib
from subprocess import call

files = ["triple_v1.zip"]

for f in files:
    print("retrieving "+f)
    thefile = urllib.URLopener()
    thefile.retrieve("https://boofcv.org/notwiki/regression/multiview/"+f,f)
    call(["unzip",f])

