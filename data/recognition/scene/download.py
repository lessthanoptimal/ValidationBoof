#!/usr/bin/env python3

import urllib.request
from subprocess import call

files = ["scene_recognition_v1.zip"]

for f in files:
    print("retrieving "+f)
    url = "https://boofcv.org/notwiki/regression/recognition/"+f
    urllib.request.urlretrieve(url,f)
    call(["unzip",f])

