#!/usr/bin/env python3

import os
import urllib.request
from subprocess import call

home_directory = os.path.abspath(os.path.join(os.path.dirname(os.path.realpath(__file__)),".."))

# Make sure it's in the same directory as the script so that all the paths are correct
os.chdir(home_directory)
os.mkdir("data")
os.chdir("data")

# Extract all the datasets
call(["unzip","../ukbench.zip"])

for i in 0 until 10:
    call(["unzip","../mirflickr1m.v3/images{}.zip".format(i])

files = ["inria_holidays.zip","ukbench_80.zip"]

