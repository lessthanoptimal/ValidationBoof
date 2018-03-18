#!/usr/bin/env python

import urllib
from subprocess import call

files = ["background_model_v1.zipp"]

for f in files:
    print("retrieving "+f)
    thefile = urllib.URLopener()
    thefile.retrieve("https://boofcv.org/notwiki/regression/background/"+f,f)
    call(["unzip",f])

