#!/usr/bin/env python3

import urllib.request
from subprocess import call

files = ["inria_holidays.zip","ukbench_80.zip"]

for f in files:
    print("retrieving "+f)
    url = "https://boofcv.org/notwiki/regression/cbir/"+f
    urllib.request.urlretrieve(url,f)
    call(["unzip",f])

