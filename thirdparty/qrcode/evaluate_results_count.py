#!/usr/bin/env python3

import optparse
import os
import sys
from os.path import join

# Given unlabeled data, count how many QR codes each library detects and how many unique QR Codes they detect

# Set up the path and make sure it knows which directory it's in
project_home = os.path.dirname(os.path.realpath(__file__))
os.chdir(project_home)
sys.path.append(os.path.join(project_home,"../../scripts"))

from validationboof import *

# Handle command line options
p = optparse.OptionParser()
p.add_option('--Results', '-r', default="results",help="Location of root results directory")

def parse_results( file_path , unique_set):
    skip = False
    detected = 0
    with open(file_path) as f:
        for line in f:
            # Skip comments and messages
            if line[0] is '#':
                continue
            if not skip:
                if not line.startswith("message = "):
                    raise Exception("BUG!")
                unique_set.add(line[len("message = "):-1])
                detected += 1
            skip = not skip
    return detected

options, arguments = p.parse_args()

dir_results = options.Results

# Score information for each library is stored here
library_scores = {}

# Number of images in each category is stored here
category_counts = {}

# List of all the datasets

# name of directories in the root directory is the same as the project which generated them
for target_name in os.listdir(dir_results):
    if not os.path.isdir(join(dir_results,target_name)):
        continue

    path_to_target = os.path.join(dir_results,target_name)

    total_detections = 0
    unique_set = set()

    data_sets = [d for d in os.listdir(path_to_target) if os.path.isdir(join(path_to_target, d))]
    file_count = 0
    for ds in data_sets:
        path_ds_results = os.path.join(path_to_target,ds)
        result_files = [f for f in os.listdir(path_ds_results) if f.endswith("txt")]
        for f in result_files:
            file_count += 1
            total_detections += parse_results(os.path.join(path_ds_results,f),unique_set)

    print()
    print("=============== {:20s} ================".format(target_name))
    print("  data sets         {} {}".format(len(data_sets),file_count))
    print("  total unique      {}".format(len(unique_set)))
    print("  total detected    {}".format(total_detections))

    library_scores[target_name] = 1

print()
