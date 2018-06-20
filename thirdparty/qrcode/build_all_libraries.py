#!/usr/bin/env python3

import os
import sys
import time
from os.path import join

# Set up the path and make sure it knows which directory it's in
project_home = os.path.dirname(os.path.realpath(__file__))
os.chdir(project_home)
sys.path.append(os.path.join(project_home,"../../scripts"))

from validationboof import *

timing = {}

# Go through each directory and see if it has a script to
for d in os.listdir():
    if os.path.isdir(join(project_home,d)):
        if d is "results":
            continue

        script_path = join(project_home,join(d,"build.py"))

        if not os.path.isfile(script_path):
            print("skipping "+script_path)
            continue
        print("Running Build Script "+script_path)


        time0 = time.time()
        run_command("python3 "+script_path)
        time1 = time.time()
        timing[d] = time1-time0

print("Total Processing Time")
for s in sorted(timing.keys()):
    print("  {} {}".format(s,timing[s]))

print("Finished! Without errors...")