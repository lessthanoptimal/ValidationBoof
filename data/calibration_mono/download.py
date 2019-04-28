#!/usr/bin/env python

import urllib
from subprocess import call

files = ["chessboard_v2.zip","circle_hexagonal_v1.zip","circle_regular_v1.zip","square_grid_v1.zip"]

for f in files:
    print("retrieving "+f)
    thefile = urllib.URLopener()
    thefile.retrieve("https://boofcv.org/notwiki/regression/calibration_mono/"+f,f)
    call(["unzip",f])

