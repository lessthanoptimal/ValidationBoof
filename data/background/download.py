#!/usr/bin/env python3

import urllib.request
from subprocess import call

files = ["background_model_v1.zip"]

for f in files:
    print("retrieving "+f)
    url = "https://boofcv.org/notwiki/regression/background/"+f
    urllib.request.urlretrieve(url,f)
    call(["unzip",f])