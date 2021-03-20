#!/usr/bin/env python3

import argparse
import os
import sys
from os import listdir
from os.path import join

# Set up the path and make sure it knows which directory it's in
project_home = os.path.dirname(os.path.realpath(__file__))
os.chdir(project_home)
sys.path.append(os.path.join(project_home, "../../../scripts"))

from validationboof import *

parser = argparse.ArgumentParser(description='Evalutes scores from batch tuning')
parser.add_argument('--Path', dest="path", default=None,
                    help="Directory containing results")

options = parser.parse_args()

if options.path:
    # List of all the directories
    directory_list = [f for f in listdir(options.path) if os.path.isdir(join(options.path, f))]
    scores = []

    # Read results from each directory
    for d in directory_list:
        path_dir = join(options.path, d)

        # Skip directory if there is no score
        if not os.path.exists(join(path_dir, "score.txt")):
            print("missing file: {}".format(join(d, "score.txt")))
            continue

        with open(join(path_dir, "score.txt"), 'r') as file:
            score = float(file.read().replace('\n', ''))

        results = {}
        results['score'] = score
        results['directory'] = d
        scores.append(results)

    print("scores.size={}".format(len(scores)))
    scores.sort(key=lambda x: x['score'])
    for s in scores:
        print(s)

    best_parameters = max(scores, key=lambda x: x['score'])
    print("Best result")
    print(best_parameters)
