#!/usr/bin/env python

import urllib
from subprocess import call

files = ["ellipse_v1.zip","polygon_v1.zip"]

for f in files:
    print("retrieving "+f)
    thefile = urllib.URLopener()
    thefile.retrieve("https://boofcv.org/notwiki/regression/shape/"+f,f)
    call(["unzip",f])

