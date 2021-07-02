#!/usr/bin/env python3

import urllib.request
from subprocess import call

files = ["tracking_sequences_2012_11_09.zip"]

for f in files:
    print("retrieving "+f)
    url = "https://boofcv.org/notwiki/regression/tracking/"+f
    urllib.request.urlretrieve(url,f)
    call(["unzip",f])
