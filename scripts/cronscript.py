#!/usr/bin/env python3

import os

# Change directory to make it easy to find the scripts
os.chdir(os.path.realpath(__file__))

if os.system("python3 regression_performance.py"):
    print("Failed to run jmh")

if os.system("python3 regression_jmh.py"):
    print("Failed to run jmh")

print("Master Cronscript Done!")
