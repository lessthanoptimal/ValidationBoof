#!/usr/bin/env python2

import urllib
from subprocess import call

files = ["boofcv_mvs_planar_v01.zip"]

for f in files:
    print("retrieving "+f)
    thefile = urllib.URLopener()
    thefile.retrieve("https://boofcv.org/notwiki/regression/mvs/"+f,f)
    call(["unzip",f])