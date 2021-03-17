#!/usr/bin/env python3

import argparse
import os
import subprocess
import sys
import time
from os import listdir
from os.path import join

# Set up the path and make sure it knows which directory it's in
project_home = os.path.dirname(os.path.realpath(__file__))
os.chdir(project_home)
sys.path.append(os.path.join(project_home, "../../../scripts"))

from validationboof import *


def main():
    parser = argparse.ArgumentParser(description='Plots results from tuning')
    parser.add_argument('--Command', dest="command", default=None, help="Where the exe is for querying")
    parser.add_argument('--DataBase', dest="database", default=None, help="Path to the database")
    parser.add_argument('--Query', dest="query_path", default=None, help="Path to images that need to be queried")

    options = parser.parse_args()

    file_out = open('ipol_results.csv', 'w')
    file_out.write("# Results from the 2017 IPOL paper 'Efficient Large-scale Image Search with a Vocabulary Tree'\n")
    file_out.write("# query={}\n".format(options.query_path))

    image_list = [f for f in listdir(options.query_path) if
                  os.path.isfile(join(options.query_path, f)) and f.endswith("jpg")]
    image_list.sort()
    print("Found query images: {}".format(len(image_list)))

    average_time = 0.0
    for idx, f in enumerate(image_list):
        time0 = time.perf_counter()
        found_stdout = subprocess.check_output(
            [options.command, '-query', options.database, join(options.query_path, f)])
        time1 = time.perf_counter()
        lines = found_stdout.splitlines()
        print(
            "{:.2f}% {} results.size={} time={:.4f} (s)".format((idx + 1) / float(len(image_list)), f, len(lines) - 2,
                                                                 time1 - time0))
        average_time += time1 - time0
        file_out.write("{},{},{}".format(idx, join(options.query_path, f), len(lines) - 2))
        for line in lines[1:-1]:
            words = str(line).split(',')
            file_out.write(',{}'.format(int(words[1])))
        file_out.write('\n')

    file_out.close()
    average_time /= len(image_list)
    print(f"average call time {average_time:0.4f} (s)")


if __name__ == '__main__':
    main()
    print("done!")
