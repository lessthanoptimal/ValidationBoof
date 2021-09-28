#!/usr/bin/env python3

import os

if os.system("python3 regression_performance.py"):
    print("Failed to run jmh")

if os.system("python3 regression_jmh.py"):
    print("Failed to run jmh")

print("Master Cronscript Done!")
