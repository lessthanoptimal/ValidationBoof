#!/usr/bin/env python3

import optparse
import os
import sys
from os.path import join
from shapely.geometry import Polygon

# Set up the path and make sure it knows which directory it's in
project_home = os.path.dirname(os.path.realpath(__file__))
os.chdir(project_home)
sys.path.append(os.path.join(project_home,"../../scripts"))

from validationboof import *

# Handle command line options
p = optparse.OptionParser()
p.add_option('--Images', '-i', default="../../data/fiducials/qrcodes/detection",help="Location of directory with input images")
p.add_option('--Results', '-r', default="results",help="Location of root results directory")


def parse_truth( file_path ):
    locations = []
    with open(file_path) as f:
        sets = False
        corners = []
        for line in f:
            if line[0] is '#':
                continue
            if line.startswith("SETS"):
                sets = True
                continue
            values = [float(s) for s in line.split()]
            if sets:
                if len(values) != 8:
                    print("Expected 4 corners in truth. "+file_path)
                    print(values)
                    exit(1)
                else:
                    locations.append(values)
            else:
                corners.extend(values)

        if not sets:
            if len(corners) != 8:
                print("Expected 4 corners in truth. "+file_path)
                print(corners)
                exit(1)
            else:
                locations.append(corners)
    return locations

def parse_results( file_path ):
    locations = []
    with open(file_path) as f:
        for line in f:
            # Skip comments and messages
            if line[0] is '#' or line[0] is 'm':
                continue
            values = [float(s) for s in line.split()]
            if len(values) != 8:
                print("Expected 4 corners in results. "+file_path)
                print(values)
                exit(1)
            else:
                locations.append(values)
    return locations

def reshape_list( l ):
    output = []
    for i in range(0,len(l),2):
        output.append((l[i],l[i+1]))
    return output

def compare_results( expected , found ):
    true_positive = 0
    false_positive = 0
    false_negative = 0

    ambiguous = 0

    paired = [False]*len(found)

    for e in expected:
        p1=Polygon(reshape_list(e))
        total_matched = 0
        for idx,f in enumerate(found):
            p2=Polygon(reshape_list(f))
            x = p1.intersection(p2)
            if x.area/p1.area > 0.1:
                paired[idx] = True
                total_matched += 1
                true_positive += 1
        if total_matched == 0:
            false_negative += 1
        elif total_matched > 1:
            ambiguous += 1

    for idx in range(len(found)):
        if not paired[idx]:
            false_positive += 1

    return {"tp":true_positive,"fp":false_positive,"fn":false_negative,"ambiguous":ambiguous}

options, arguments = p.parse_args()

dir_images = options.Images
dir_results = options.Results

# List of all the datasets
data_sets = [d for d in os.listdir(dir_images) if os.path.isdir(join(dir_images,d))]

# name of directories in the root directory is the same as the project which generated them
for target_name in os.listdir(dir_results):
    if not os.path.isdir(join(dir_results,target_name)):
        continue

    path_to_target = os.path.join(dir_results,target_name)

    total_missing = 0
    total_false_positive = 0
    total_false_negative = 0
    total_true_positive = 0
    total_ambiguous = 0

    for ds in data_sets:
        path_ds_results = os.path.join(path_to_target,ds)
        path_ds_truth = join(dir_images,ds)

        truth_files = [f for f in os.listdir(path_ds_truth) if f.endswith("txt")]

        for truth_file in truth_files:
            if not os.path.isfile(join(path_ds_results,truth_file)):
                total_missing += 1
                print("Missing results for {}".format(join(ds,truth_file)))
                continue

            expected = parse_truth(join(path_ds_truth,truth_file))
            found = parse_results(join(path_ds_results,truth_file))

            metrics = compare_results(expected,found)
            total_ambiguous += metrics['ambiguous']
            total_false_positive += metrics['fp']
            total_true_positive += metrics['tp']
            total_false_negative += metrics['fn']

    print("Statistics")
    print("  missile results  {}".format(total_missing))
    print("  false positive   {}".format(total_false_positive))
    print("  false negative   {}".format(total_false_negative))
    print("  true positive    {}".format(total_true_positive))
    print("  ambiguous        {}".format(total_ambiguous))