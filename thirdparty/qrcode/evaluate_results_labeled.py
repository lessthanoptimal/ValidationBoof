#!/usr/bin/env python3

import optparse
import os
import sys
from os.path import join
from shapely.geometry import Polygon

# Uses labeled QR Code images to determine each detectors ability to detect QR Codes

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
    milliseconds = 0
    with open(file_path) as f:
        for line in f:
            # Skip comments and messages
            if line[0] is '#' or line.startswith("message"):
                continue
            if line.startswith("milliseconds"):
                milliseconds = float(line.split()[2])
            else:
                values = [float(s) for s in line.split()]
                if len(values) != 8:
                    print("Expected 4 corners in results. "+file_path)
                    print(values)
                    exit(1)
                else:
                    locations.append(values)
    return {'locs':locations,'ms':milliseconds}

def reshape_list( l ):
    output = []
    for i in range(0,len(l),2):
        output.append((l[i],l[i+1]))
    return output

def compare_location_results(expected, found):
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
            try:
                x = p1.intersection(p2)
                if x.area/p1.area > 0.1:
                    paired[idx] = True
                    total_matched += 1
                    true_positive += 1
            except:
                pass # not sure what to do here
        if total_matched == 0:
            false_negative += 1
        elif total_matched > 1:
            ambiguous += 1

    for idx in range(len(found)):
        if not paired[idx]:
            false_positive += 1

    return {"tp":true_positive,"fp":false_positive,"fn":false_negative,"ambiguous":ambiguous}


def compute_f( tp , fp , tn , fn ):
    return 2.0*tp/(2.0*tp + fp + fn)

options, arguments = p.parse_args()

dir_images = options.Images
dir_results = options.Results

# Score information for each library is stored here
library_loc_scores = {}
library_run_scores = {}

# Number of images in each category is stored here
category_counts = {}

# List of all the datasets
data_sets = [d for d in os.listdir(dir_images) if os.path.isdir(join(dir_images,d))]

# name of directories in the root directory is the same as the project which generated them
for target_name in sorted(os.listdir(dir_results)):
    if not os.path.isdir(join(dir_results,target_name)):
        continue

    path_to_target = os.path.join(dir_results,target_name)

    # weighted statistical sum. Each category has a weight of 1 independent
    # of the number of QR Codes and images
    total_missing = 0
    total_false_positive = 0
    total_false_negative = 0
    total_true_positive = 0
    total_ambiguous = 0

    ds_results = {}

    for ds in data_sets:
        path_ds_results = os.path.join(path_to_target,ds)
        path_ds_truth = join(dir_images,ds)

        truth_files = [f for f in os.listdir(path_ds_truth) if f.endswith("txt")]

        category_counts[ds] = len(truth_files)

        # DS = dataset
        ds_true_positive = 0
        ds_false_positive = 0
        ds_false_negative = 0

        milliseconds = []

        for truth_file in truth_files:
            if not os.path.isfile(join(path_ds_results,truth_file)):
                total_missing += 1
                print("Missing results for {}".format(join(ds,truth_file)))
                continue

            expected = parse_truth(join(path_ds_truth,truth_file))
            try:
                found = parse_results(join(path_ds_results,truth_file))
            except Exception as e:
                print("Failed parsing {} {}".format(path_ds_results,truth_file))
                print("error = {}".format(e))
                raise e
            metrics_loc = compare_location_results(expected, found['locs'])

            milliseconds.append(found['ms'])

            total_ambiguous += metrics_loc['ambiguous']

            ds_false_negative += metrics_loc['fn']
            ds_true_positive += metrics_loc['tp']
            ds_false_positive += metrics_loc['fp']

        ds_total_qr = ds_true_positive + ds_false_negative
        total_false_positive += ds_false_negative/ds_total_qr
        total_true_positive  += ds_true_positive/ds_total_qr
        total_false_negative += ds_false_positive/ds_total_qr

        milliseconds.sort()
        ms50 = milliseconds[int(len(milliseconds)/2)]
        ms95 = milliseconds[int(len(milliseconds)*0.95)]
        msMean = sum( milliseconds) / len(milliseconds)

        ds_results[ds] = {"tp":ds_true_positive,"fp":ds_false_positive,"fn":ds_false_negative,
                          "ms50":ms50, "ms95":ms95, "msMean":msMean }


    print()
    print("=============== {:20s} ================".format(target_name))
    print("  total input      {}".format(total_true_positive+total_false_negative))
    print("  missing results  {}".format(total_missing))
    print()
    print("  false positive   {}".format(total_false_positive))
    print("  false negative   {}".format(total_false_negative))
    print("  true positive    {}".format(total_true_positive))
    print("  ambiguous        {}".format(total_ambiguous))

    scoresF = {}
    scoresRun = {}

    scoresF["summary"] = compute_f(total_true_positive,total_false_positive,0,total_false_negative)
    scoresRun["summary"] = sum( [ds_results[n]["ms50"] for n in ds_results]) / len(ds_results)
    for n in sorted(list(ds_results.keys())):
        r = ds_results[n]
        F = compute_f(r['tp'],r['fp'],0,r['fn'])
        scoresF[n] = F
        scoresRun[n] = {"ms50":r["ms50"], "ms95":r["ms95"], "msMean":r["msMean"]}
        print("{:15s} F={:.2f} ms50={:7.2f} ms95={:7.2f}".format(n,F,r["ms50"],r["ms95"]))

    library_loc_scores[target_name] = scoresF
    library_run_scores[target_name] = scoresRun

print()

print("Summary Runtime")
for n in sorted(list(library_run_scores.keys())):
    print("{:10s} {:7.3f} (ms)".format(n,library_run_scores[n]["summary"]))
print()

# Create the plot showing a summary by category
import matplotlib.pyplot as plt
import numpy as np

library_names = sorted(list(library_loc_scores.keys()))
categories = sorted(list(library_loc_scores[library_names[0]].keys()))
categories = [x for x in categories if x is not "summary"]
count_list = [category_counts[x] for x in categories]

indexes = np.arange(len(categories))
fig, ax = plt.subplots()

bar_width = 0.4
bar_space = (len(library_names)*1.4)*bar_width

for idx,lib_name in enumerate(library_names):
    scores_map = library_loc_scores[lib_name]
    scores_list = [scores_map[x] for x in categories]
    locations_x = indexes*bar_space+idx*bar_width
    ax.bar(locations_x,scores_list,bar_width,label=lib_name)
    for score_idx,v in enumerate(scores_list):
        ax.text(locations_x[score_idx]-bar_width*0.4, v + 0.1, "{:.2f}".format(v), color='black', fontsize=7,fontweight='bold', rotation=90)

ax.set_ylim([0.0,1.15])
ax.set_ylabel("F-Score (1 = best)")
ax.set_title('Detection Performance by Categories')
ax.set_xticks(indexes*bar_space + (bar_width*int(len(library_names)/2)) )
ax.set_xticklabels(categories, rotation=90)
ax.legend()

plt.gcf().subplots_adjust(bottom=0.25)
fig.set_size_inches(12, 4)
plt.savefig("detection_categories.pdf", format='pdf')
plt.close()

# Create plots showing runtime speed by category
stats = ["ms50","ms95","msMean"]
for stat in stats:
    fig, ax = plt.subplots()

    bar_width = 0.4
    bar_space = (len(library_names)*1.4)*bar_width

    for idx,lib_name in enumerate(library_names):
        scores_map = library_run_scores[lib_name]
        scores_list = [scores_map[x][stat] for x in categories]
        locations_x = indexes*bar_space+idx*bar_width
        ax.bar(locations_x,scores_list,bar_width,label=lib_name)
        for score_idx,v in enumerate(scores_list):
            ax.text(locations_x[score_idx]-bar_width*0.4, 700, "{:.2f}".format(v), color='black', fontsize=7,fontweight='bold', rotation=90)

    ax.set_ylim([0.0,800])
    ax.set_ylabel("Milliseconds (smaller better)")
    ax.set_title('Runtime Performance by Categories: '+stat)
    ax.set_xticks(indexes*bar_space + (bar_width*int(len(library_names)/2)) )
    ax.set_xticklabels(categories, rotation=90)
    ax.legend()

    plt.gcf().subplots_adjust(bottom=0.25)
    fig.set_size_inches(12, 4)
    plt.savefig("runtime_categories_{}.pdf".format(stat), format='pdf')
    plt.close()

# Ratios of 95% and 50% ratios
fig, ax = plt.subplots()

bar_width = 0.4
bar_space = (len(library_names)*1.4)*bar_width

for idx,lib_name in enumerate(library_names):
    scores_map = library_run_scores[lib_name]
    scores_list = [scores_map[x]["ms95"]/scores_map[x]["ms50"] for x in categories]
    locations_x = indexes*bar_space+idx*bar_width
    ax.bar(locations_x,scores_list,bar_width,label=lib_name)
    for score_idx,v in enumerate(scores_list):
        ax.text(locations_x[score_idx]-bar_width*0.4, 8.0, "{:.2f}".format(v), color='black', fontsize=7,fontweight='bold', rotation=90)

ax.set_ylim([0.0,10.0])
ax.set_ylabel("Ratio (smaller better)")
ax.set_title('Outlier vs Median Speed Ratio')
ax.set_xticks(indexes*bar_space + (bar_width*int(len(library_names)/2)) )
ax.set_xticklabels(categories, rotation=90)
ax.legend()

plt.gcf().subplots_adjust(bottom=0.25)
fig.set_size_inches(12, 4)
plt.savefig("runtime_categories_outlier_ratios.pdf", format='pdf')
plt.close()

# Relative Mean Speed of each Library
fig, ax = plt.subplots()

bar_width = 0.4
bar_space = (len(library_names)*1.4)*bar_width

fastest_category = {}

for idx,lib_name in enumerate(library_names):
    scores_map = library_run_scores[lib_name]

    for x in categories:
        v = scores_map[x]["msMean"]
        fastest_category[x] = min(v,fastest_category.get(x,v))

for idx,lib_name in enumerate(library_names):
    scores_map = library_run_scores[lib_name]
    scores_list = [scores_map[x]["msMean"]/fastest_category[x] for x in categories]
    locations_x = indexes*bar_space+idx*bar_width
    ax.bar(locations_x,scores_list,bar_width,label=lib_name)
    for score_idx,v in enumerate(scores_list):
        ax.text(locations_x[score_idx]-bar_width*0.4, 9.0, "{:.2f}".format(v), color='black', fontsize=7,fontweight='bold', rotation=90)

ax.set_ylim([0.0,10.0])
ax.set_ylabel("Ratio (smaller better)")
ax.set_title('Relative Mean Speed')
ax.set_xticks(indexes*bar_space + (bar_width*int(len(library_names)/2)) )
ax.set_xticklabels(categories, rotation=90)
ax.legend()

plt.gcf().subplots_adjust(bottom=0.25)
fig.set_size_inches(12, 4)
plt.savefig("runtime_categories_relative_mean.pdf", format='pdf')
plt.close()

# Image Count by Category
fig, ax = plt.subplots()
indexes = np.arange(len(categories))
ax.bar(indexes,count_list)
max_count = int(np.max(count_list)*1.1)
for score_idx,v in enumerate(count_list):
    ax.text(indexes[score_idx]-0.4, v + 1, "{:3d}".format(v), color='black', fontsize=12,fontweight='bold')
ax.set_ylim([0,max_count])
ax.set_ylabel("Number of Images")
ax.set_title('Total Images by Categories')
ax.set_xticks(indexes)
ax.set_xticklabels(categories, rotation=90)
plt.gcf().subplots_adjust(bottom=0.25)
plt.savefig("category_counts.pdf", format='pdf')
plt.close()

# Create plot Showing overall summary
fig, ax = plt.subplots()
indexes = np.arange(len(library_names))
library_summary = [library_loc_scores[x]['summary'] for x in library_names]
ax.bar(indexes,library_summary)
for score_idx,v in enumerate(library_summary):
    ax.text(indexes[score_idx]-0.14, v, "{:.2f}".format(v), color='black', fontsize=12,fontweight='bold')
ax.set_ylim([0.0,1.1])
ax.set_ylabel("F-Score (1=best)")
ax.set_title('Overall Detection Performance\nAverage by Category')
ax.set_xticks(indexes)
ax.set_xticklabels(library_names, rotation=90)
plt.gcf().subplots_adjust(bottom=0.15)
plt.savefig("detection_summary.pdf", format='pdf')

# Create plot Showing overall summary
fig, ax = plt.subplots()
indexes = np.arange(len(library_names))
library_summary = [library_run_scores[x]['summary'] for x in library_names]
ax.bar(indexes,library_summary)
for score_idx,v in enumerate(library_summary):
    ax.text(indexes[score_idx]-0.14, 350, "{:.2f}".format(v), color='black', fontsize=12,fontweight='bold')
ax.set_ylim([0.0,400])
ax.set_ylabel("Mean 50% Time (ms)")
ax.set_title('Overall Runtime')
ax.set_xticks(indexes)
ax.set_xticklabels(library_names, rotation=90)
plt.gcf().subplots_adjust(bottom=0.15)
plt.savefig("runtime_summary.pdf", format='pdf')
