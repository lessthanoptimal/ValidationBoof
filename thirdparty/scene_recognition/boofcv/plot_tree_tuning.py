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


def plot_results(scores):
    import matplotlib.pyplot as plt
    import numpy as np

    # set of unique keys
    key_set = list(set([item for sublist in scores for item in sublist]))
    # Just want the two axises
    key_set.remove('score')
    # Make sure the order is constant each time this is called
    key_set.sort()

    # Find unique values for each set
    x_values = list(set([x[key_set[0]] for x in scores]))
    y_values = list(set([x[key_set[1]] for x in scores]))

    print("len(x)={}".format(len(x_values)))

    min_x = min(x_values)
    min_y = min(y_values)

    print(key_set)
    Y, X = np.meshgrid(x_values + [x_values[-1] + 1], y_values + [y_values[-1] + 1])
    Z = np.zeros((len(y_values), len(x_values)))
    for s in scores:
        Z[s[key_set[1]] - min_y, s[key_set[0]] - min_x] = s['score']

    print(X.shape)
    print(Z.shape)

    plt.pcolor(X, Y, Z, cmap='YlOrRd')

    # Print the values of the best scores
    for i in range(-20, 0, 1):
        plt.text(scores[i][key_set[1]] + 0.5, scores[i][key_set[0]], "{:.3f}".format(scores[i]['score']),
                 horizontalalignment='center')

    plt.title("Tree Structure vs mAP Score")
    plt.xlabel(key_set[1])
    plt.ylabel(key_set[0])
    plt.xticks(np.arange(len(y_values)) + min_y + 0.5, y_values) # Center the labels
    plt.colorbar()
    plt.savefig('tree_tuning.pdf')


parser = argparse.ArgumentParser(description='Plots results from tuning')
parser.add_argument('--TreeGrid', dest="tree_grid_path", default=None,
                    help="2D grid of tree tuning parameters location")

options = parser.parse_args()

if options.tree_grid_path:
    # List of all the directories
    directory_list = [f for f in listdir(options.tree_grid_path) if os.path.isdir(join(options.tree_grid_path, f))]
    scores = []

    # Read in what parameters were searched in this grid search
    grid_settings = {}
    with open(join(options.tree_grid_path, "grid_settings.txt"), 'r') as file:
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
        if not os.path.exists(join(d, "score.txt")):
            print("missing file: {}".format(join(d, "score.txt")))
            continue

        with open(join(d, "score.txt"), 'r') as file:
            score = float(file.read().replace('\n', ''))

        parameters = {}
        with open(join(d, "grid_state.txt"), 'r') as file:
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
    scores.sort(key=lambda x: x['score'])
    for s in scores:
        print(s)

    best_parameters = max(scores, key=lambda x: x['score'])
    print("Best Parameter")
    print(best_parameters)
    print("Grid Search Parameters")
    print(grid_settings)

    plot_results(scores)
