#!/usr/bin/env python3

import urllib.request
from subprocess import call

files = ["chessboard_v2.zip","circle_hexagonal_v1_1.zip","circle_regular_v1.zip",
         "square_border_binary_v1.zip","square_border_image_v1.zip","square_grid_v1.zip","qrcodes_v3.zip",
         "random_dots_v1.zip", "square_border_hamming_v1.zip", "microqr_v1.zip"]

for f in files:
    print("retrieving "+f)
    url = "https://boofcv.org/notwiki/regression/fiducial/"+f
    urllib.request.urlretrieve(url,f)
    call(["unzip",f])
