#!/usr/bin/env python2

import urllib
from subprocess import call

files = ["chessboard_v2.zip","circle_hexagonal_v1_1.zip","circle_regular_v1.zip",
         "square_border_binary_v1.zip","square_border_image_v1.zip","square_grid_v1.zip","qrcodes_v3.zip"]

for f in files:
    print("retrieving "+f)
    thefile = urllib.URLopener()
    thefile.retrieve("https://boofcv.org/notwiki/regression/fiducial/"+f,f)
    call(["unzip",f])

