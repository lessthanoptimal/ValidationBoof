#!/usr/bin/env python3

import urllib.request
from subprocess import call

files = ["other-gray-twoframes.zip","other-gt-flow.zip"]

for f in files:
    print("retrieving "+f)
    url = "http://vision.middlebury.edu/flow/data/comp/zip/"+f
    urllib.request.urlretrieve(url,f)
    call(["unzip",f])
