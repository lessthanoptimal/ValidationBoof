#!/usr/bin/env python3

import argparse
import os
import sys
from os import listdir
from os.path import join

# Set up the path and make sure it knows which directory it's in
project_home = os.path.dirname(os.path.realpath(__file__))
os.chdir(project_home)
sys.path.append(os.path.join(project_home,"../../../scripts"))

from validationboof import *

parser = argparse.ArgumentParser(description='Plots results from tuning')
parser.add_argument('--TreeGrid', dest="tree_grid_path", default=None,help="2D grid of tree tuning parameters location")

options = parser.parse_args()

if options.tree_grid_path:
    # List of all the directories
    directory_list = [f for f in listdir(options.tree_grid_path) if os.path.isdir(join(options.tree_grid_path, f))]
    scores = []

    # Read in what parameters were searched in this grid search
    grid_settings = {}
    with open(join(options.tree_grid_path,"../grid_settings.txt"), 'r') as file:
        lines = file.read().splitlines()
        for line in lines:
            if 'range-integers' not in line:
                continue
            words = line.split(',')
            grid_settings[words[0]] = (int(words[2]), int(words[3]))

    # Read results from each directory
    for d in directory_list:
        d = join(options.tree_grid_path, d)

        # Skip directory if there is no score
        if not os.path.exists(join(d,"score.txt")):
            print("missing file: {}".format(join(d,"score.txt")))
            continue

        with open(join(d,"score.txt"), 'r') as file:
            score = float(file.read().replace('\n', ''))

        parameters = {}
        with open(join(d,"grid_state.txt"), 'r') as file:
            lines = file.read().splitlines()
            for line in lines:
                words = line.split(',')
                parameters[words[0]] = words[1]

        results = {}
        results['score'] = score
        results['tree.branchFactor'] = int(parameters['tree.branchFactor'])
        results['tree.maximumLevel'] = int(parameters['tree.maximumLevel'])
        scores.append(results)

    print("scores.size={}".format(len(scores)))
    scores.sort(key=lambda x:x['score'])
    for s in scores:
        print(s)

    best_parameters = max(scores, key=lambda x:x['score'])
    print("Best Parameter")
    print(best_parameters)
    print("Grid Search Parameters")
    print(grid_settings)


