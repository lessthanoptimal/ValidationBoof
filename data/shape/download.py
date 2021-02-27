#!/usr/bin/env python3

import urllib.request
from subprocess import call

files = ["ellipse_v1.zip","polygon_v1.zip","lines_v1.zip"]

for f in files:
    print("retrieving "+f)
    url = "https://boofcv.org/notwiki/regression/shape/"+f
    urllib.request.urlretrieve(url,f)
    call(["unzip",f])
