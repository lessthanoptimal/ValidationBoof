#!/usr/bin/env python3

import urllib.request
from subprocess import call

files = ["triple_v1.zip"]

for f in files:
    print("retrieving "+f)
    g = urllib.request.urlopen("https://boofcv.org/notwiki/regression/multiview/"+f)
    with open(f, 'b+w') as o:
        o.write(g.read())
        call(["unzip",f])



