#!/usr/bin/env python3

import urllib.request
from subprocess import call

files = ["disparity_v1.zip"]

for f in files:
    print("retrieving "+f)
    url = "https://boofcv.org/notwiki/regression/disparity/"+f
    urllib.request.urlretrieve(url,f)
    call(["unzip",f])
