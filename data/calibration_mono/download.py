#!/usr/bin/env python3

import urllib.request
from subprocess import call

files = ["chessboard_v4.zip","circle_hexagonal_v1.zip","circle_regular_v1.zip","square_grid_v1.zip"]

for f in files:
    print("retrieving "+f)
    url = "https://boofcv.org/notwiki/regression/calibration_mono/"+f
    urllib.request.urlretrieve(url,f)
    call(["unzip",f])