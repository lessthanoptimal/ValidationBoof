#!/usr/bin/env python3

import argparse
import os
import re
import sys
import csv
from os.path import join

# This script is useful for tests that need to compare results between different dataset folders as well as different detector libraries
# Given unlabeled data, count how many QR codes each library detects and how many unique QR Codes they detect
# For each dataset folder in the Input directory it will generate results for each detector library

# Set up the path and make sure it knows which directory it's in
project_home = os.path.dirname(os.path.realpath(__file__))
os.chdir(project_home)
sys.path.append(os.path.join(project_home,"../../scripts"))

from validationboof import *

# Handle command line options
p = argparse.ArgumentParser(description='Computes how many QR codes are detected and how many unique')
p.add_argument('--Input', '-i', dest="input", default="results",help="Location of results directory")
p.add_argument('--Filter', '-f', dest="filter",default=None,help="Optional regex to filter messages, e.g. '6J\w+'")

def parse_results( file_path , unique_set , filter):
    skip = False
    detected = 0
    with open(file_path) as f:
        for line in f:
            # Skip comments and messages
            if line[0] is '#':
                continue
            if line.startswith("milliseconds"):
                continue
            if not skip:
                if not line.startswith("message = "):
                    raise Exception("BUG!")
                m = line[len("message = "):-1]
                if filter is None or re.match(filter,m):
                    unique_set.add(m)
                    detected += 1
            skip = not skip
    return detected

options = p.parse_args()

dir_results = options.input

# Score information for each library is stored here
library_scores = {}
library_images_with_a_detection = {}
library_unique_detected = {}
library_total_detected = {}

# Number of images in each category is stored here
category_counts = {}

# List number of images with at least one qr code detected by data set
print()
print("=============== {:20s} ================".format("Results by Data Set"))
print("Assumes each image in the data sets has at least one QR code")
boof_dir = join(dir_results,'boofcv')
# data_sets = [int(d[:-2]) for d in os.listdir(boof_dir) if os.path.isdir(join(boof_dir, d))]
# data_sets = (str(d)+'ft' for d in sorted(data_sets))
data_sets = [d for d in os.listdir(boof_dir) if os.path.isdir(join(boof_dir, d))]
for ds in sorted(data_sets):

    print()
    print("=============== {:4s} ================".format(ds))
    result_files = [f for f in os.listdir(join(boof_dir,ds)) if f.endswith("txt")]
    print("  total images      {}".format(len(result_files)))
    # name of directories in the root directory is the same as the project which generated them
    for target_name in os.listdir(dir_results):
        if not os.path.isdir(join(dir_results,target_name)):
            continue

        path_to_target = os.path.join(dir_results,target_name)

        images_with_detections = 0
        total_detections = 0
        unique_set = set()
        file_count = 0

        path_ds_results = os.path.join(path_to_target,ds)
        for f in result_files:
            file_count += 1
            detected = parse_results(os.path.join(path_ds_results,f),unique_set, options.filter)
            if detected > 0:
                images_with_detections += 1
            total_detections += detected

        if target_name in library_images_with_a_detection:
            library_images_with_a_detection[target_name][ds] = images_with_detections
            library_unique_detected[target_name][ds] = len(unique_set)
            library_total_detected[target_name][ds] = total_detections
        else:
            library_images_with_a_detection[target_name] = {ds:images_with_detections}
            library_unique_detected[target_name] = {ds:len(unique_set)}
            library_total_detected[target_name] = {ds:total_detections}

        print("--- {} ".format(target_name))
        print("  images with detections {}".format(images_with_detections))
        print("  total unique           {}".format(len(unique_set)))
        print("  total detected         {}".format(total_detections))

# List of all the datasets
print()
print("=============== {:20s} ================".format("Overall Summary"))

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
            total_detections += parse_results(os.path.join(path_ds_results,f),unique_set, options.filter)

    print()
    print("=============== {:20s} ================".format(target_name))
    print("  data sets         {}".format(len(data_sets)))
    print("  total images      {}".format(file_count))
    print("  total unique      {}".format(len(unique_set)))
    print("  total detected    {}".format(total_detections))

    library_scores[target_name] = 1

print()


############################################# Plots
import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import numpy as np

# number_of_images_with_a_detection
library_names = sorted(list(library_images_with_a_detection.keys()))
categories = sorted(list(library_images_with_a_detection[library_names[0]].keys()))

indexes = np.arange(len(categories))
fig, ax = plt.subplots()

bar_width = 0.4
bar_space = (len(library_names)*1.4)*bar_width

for idx,lib_name in enumerate(library_names):
    scores_map = library_images_with_a_detection[lib_name]
    scores_list = [scores_map[x] for x in categories]
    locations_x = indexes*bar_space+idx*bar_width
    ax.bar(locations_x,scores_list,bar_width,label=lib_name)
    for score_idx,v in enumerate(scores_list):
        ax.text(locations_x[score_idx]-bar_width*0.4, v + 0.1, "{}".format(v), color='black', fontsize=7,fontweight='bold', rotation=0)

ax.set_ylabel("Number of Images with a Detection")
ax.set_title('Detection Performance by Categories')
ax.set_xticks(indexes*bar_space + (bar_width*int(len(library_names)/2)) )
ax.set_xticklabels(categories, rotation=90)
ax.legend()

plt.gcf().subplots_adjust(bottom=0.25)
fig.set_size_inches(12, 4)
plt.savefig("number_of_images_with_a_detection.pdf", format='pdf')
plt.close()

# unique_detections_by_dataset
library_names = sorted(list(library_unique_detected.keys()))
categories = sorted(list(library_unique_detected[library_names[0]].keys()))

indexes = np.arange(len(categories))
fig, ax = plt.subplots()

bar_width = 0.4
bar_space = (len(library_names)*1.4)*bar_width

for idx,lib_name in enumerate(library_names):
    scores_map = library_unique_detected[lib_name]
    scores_list = [scores_map[x] for x in categories]
    locations_x = indexes*bar_space+idx*bar_width
    ax.bar(locations_x,scores_list,bar_width,label=lib_name)
    for score_idx,v in enumerate(scores_list):
        ax.text(locations_x[score_idx]-bar_width*0.4, v + 0.1, "{}".format(v), color='black', fontsize=7,fontweight='bold', rotation=90)

ax.set_ylabel("Number of Unique Detections")
ax.set_title('Detection Performance by Categories')
ax.set_xticks(indexes*bar_space + (bar_width*int(len(library_names)/2)) )
ax.set_xticklabels(categories, rotation=90)
ax.legend()

plt.gcf().subplots_adjust(bottom=0.25)
fig.set_size_inches(12, 4)
plt.savefig("unique_detections_by_dataset.pdf", format='pdf')
plt.close()

# detections_by_dataset
library_names = sorted(list(library_total_detected.keys()))
categories = sorted(list(library_total_detected[library_names[0]].keys()))

indexes = np.arange(len(categories))
fig, ax = plt.subplots()

bar_width = 0.4
bar_space = (len(library_names)*1.4)*bar_width

for idx,lib_name in enumerate(library_names):
    scores_map = library_total_detected[lib_name]
    scores_list = [scores_map[x] for x in categories]
    locations_x = indexes*bar_space+idx*bar_width
    ax.bar(locations_x,scores_list,bar_width,label=lib_name)
    for score_idx,v in enumerate(scores_list):
        ax.text(locations_x[score_idx]-bar_width*0.4, v + 0.1, "{}".format(v), color='black', fontsize=7,fontweight='bold', rotation=0)

ax.set_ylabel("Number of Detections")
ax.set_title('Detection Performance by Categories')
ax.set_xticks(indexes*bar_space + (bar_width*int(len(library_names)/2)) )
ax.set_xticklabels(categories, rotation=90)
ax.legend()

plt.gcf().subplots_adjust(bottom=0.25)
fig.set_size_inches(12, 4)
plt.savefig("detections_by_dataset.pdf", format='pdf')
plt.close()