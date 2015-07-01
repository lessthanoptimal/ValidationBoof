#!/usr/bin/env python

import os, sys, subprocess
from os.path import isdir, join

if len(sys.argv) < 3:
    print "arguments: <binary> <output directory>"
    exit(0)

path_bin = sys.argv[1]
path_data = "../../data/fiducials/image/"
path_output = sys.argv[2]

scenarios = [ f for f in os.listdir(path_data) if isdir(join(path_data,f))]

if not os.path.exists(path_output):
    os.makedirs(path_output)

for scenario in scenarios:
    input = join(path_data,scenario)
    output = join(path_output,scenario)
    os.makedirs(output)

    subprocess.call([path_bin,input,output])
