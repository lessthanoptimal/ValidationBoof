#!/usr/bin/env python3

import urllib.request
from subprocess import call

files = ["boofcv_mvs_planar_v01.zip"]

for f in files:
    print("retrieving "+f)
    url = "https://boofcv.org/notwiki/regression/mvs/"+f
    urllib.request.urlretrieve(url,f)
    call(["unzip",f])