#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
This is a python reimplementation of the open source tracker in
http://www2.isr.uc.pt/~henriques/circulant/index.html

Found http://wiki.scipy.org/NumPy_for_Matlab_Users very useful

Based on the work of João F. Henriques, 2012
http://www.isr.uc.pt/~henriques

Rodrigo Benenson, MPI-Inf 2013
http://rodrigob.github.io
"""

from __future__ import print_function

import os
import glob
import time
from optparse import OptionParser

import pylab

from circulant_matrix_tracker import *

debug = False
gui = True
save = False

def load_video_info(video_path):
    """
    Loads all the relevant information for the video in the given path:
    the list of image files (cell array of strings), initial position
    (1x2), target size (1x2), whether to resize the video to half
    (boolean), and the ground truth information for precision calculations
    (Nx2, for N frames). The ordering of coordinates is always [y, x].

    The path to the video is returned, since it may change if the images
    are located in a sub-folder (as is the default for MILTrack's videos).
    """

    # load ground truth from text file (MILTrack's format)
    text_files = glob.glob(os.path.join(video_path, "*_gt.txt"))
    assert text_files, \
        "No initial position and ground truth (*_gt.txt) to load."

    first_file_path = text_files[0]
    #f = open(first_file_path, "r")
    #ground_truth = textscan(f, '%f,%f,%f,%f') # [x, y, width, height]
    #ground_truth = cat(2, ground_truth{:})
    ground_truth = pylab.loadtxt(first_file_path, delimiter=",")
    #f.close()

    # set initial position and size
    first_ground_truth = ground_truth[0, :]
    # target_sz contains height, width
    target_sz = pylab.array([first_ground_truth[3], first_ground_truth[2]])
    # pos contains y, x center
    pos = [first_ground_truth[1], first_ground_truth[0]] \
        + pylab.floor(target_sz / 2)

    #try:
    if True:
        # interpolate missing annotations
        # 4 out of each 5 frames is filled with zeros
        for i in range(4):  # x, y, width, height
            xp = range(0, ground_truth.shape[0], 5)
            fp = ground_truth[xp, i]
            x = range(ground_truth.shape[0])
            ground_truth[:, i] = pylab.interp(x, xp, fp)
        # store positions instead of boxes
        ground_truth = ground_truth[:, [1, 0]] + ground_truth[:, [3, 2]] / 2
    #except Exception as e:
    else:
        print("Failed to gather ground truth data")
        #print("Error", e)
        # ok, wrong format or we just don't have ground truth data.
        ground_truth = []

    # list all frames. first, try MILTrack's format, where the initial and
    # final frame numbers are stored in a text file. if it doesn't work,
    # try to load all png/jpg files in the folder.

    text_files = glob.glob(os.path.join(video_path, "*_frames.txt"))
    if text_files:
        first_file_path = text_files[0]
        #f = open(first_file_path, "r")
        #frames = textscan(f, '%f,%f')
        frames = pylab.loadtxt(first_file_path, delimiter=",", dtype=int)
        #f.close()

        # see if they are in the 'imgs' subfolder or not
        test1_path_to_img = os.path.join(video_path,
                                         "imgs/img%05i.png" % frames[0])
        test2_path_to_img = os.path.join(video_path,
                                         "img%05i.png" % frames[0])
        if os.path.exists(test1_path_to_img):
            video_path = os.path.join(video_path, "imgs/")
        elif os.path.exists(test2_path_to_img):
            video_path = video_path  # no need for change
        else:
            raise Exception("Failed to find the png images")

        # list the files
        img_files = ["img%05i.png" % i
                     for i in range(frames[0], frames[1] + 1)]
        #img_files = num2str((frames{1} : frames{2})', 'img%05i.png')
        #img_files = cellstr(img_files);
    else:
        # no text file, just list all images
        img_files = glob.glob(os.path.join(video_path, "*.png"))
        if len(img_files) == 0:
            img_files = glob.glob(os.path.join(video_path, "*.jpg"))

        assert len(img_files), "Failed to find png or jpg images"

        img_files.sort()

    # if the target is too large, use a lower resolution
    # no need for so much detail
    if pylab.sqrt(pylab.prod(target_sz)) >= 100:
        pos = pylab.floor(pos / 2)
        target_sz = pylab.floor(target_sz / 2)
        resize_image = True
    else:
        resize_image = False

    ret = [img_files, pos, target_sz, resize_image, ground_truth, video_path]
    return ret

def show_precision(positions, ground_truth, video_path, title):
    """
    Calculates precision for a series of distance thresholds (percentage of
    frames where the distance to the ground truth is within the threshold).
    The results are shown in a new figure.

    Accepts positions and ground truth as Nx2 matrices (for N frames), and
    a title string.
    """

    print("Evaluating tracking results.")

    pylab.ioff()  # interactive mode off

    max_threshold = 50  # used for graphs in the paper

    if positions.shape[0] != ground_truth.shape[0]:
        raise Exception(
            "Could not plot precisions, because the number of ground"
            "truth frames does not match the number of tracked frames.")

    # calculate distances to ground truth over all frames
    delta = positions - ground_truth
    distances = pylab.sqrt((delta[:, 0]**2) + (delta[:, 1]**2))
    #distances[pylab.isnan(distances)] = []

    # compute precisions
    precisions = pylab.zeros((max_threshold, 1), dtype=float)
    for p in range(max_threshold):
        precisions[p] = pylab.sum(distances <= p, dtype=float) / len(distances)

    if False:
        pylab.figure()
        pylab.plot(distances)
        pylab.title("Distances")
        pylab.xlabel("Frame number")
        pylab.ylabel("Distance")

    # plot the precisions
    pylab.figure()  # 'Number', 'off', 'Name',
    pylab.title("Precisions - " + title)
    pylab.plot(precisions, "k-", linewidth=2)
    pylab.xlabel("Threshold")
    pylab.ylabel("Precision")

    pylab.show()
    return


def plot_tracking(frame, pos, target_sz, should_resize_image, im, ground_truth):

    global \
        tracking_figure, tracking_figure_title, tracking_figure_axes, \
        tracking_rectangle, gt_point, \
        z_figure_axes, response_figure_axes

    timeout = 1e-6
    #timeout = 0.05  # uncomment to run slower
    if frame == 0:
        #pylab.ion()  # interactive mode on
        tracking_figure = pylab.figure()
        gs = pylab.GridSpec(1, 3, width_ratios=[3, 1, 1])

        tracking_figure_axes = tracking_figure.add_subplot(gs[0])
        tracking_figure_axes.set_title("Tracked object (and ground truth)")

        z_figure_axes = tracking_figure.add_subplot(gs[1])
        z_figure_axes.set_title("Template")

        response_figure_axes = tracking_figure.add_subplot(gs[2])
        response_figure_axes.set_title("Response")

        tracking_rectangle = pylab.Rectangle((0, 0), 0, 0)
        tracking_rectangle.set_color((1, 0, 0, 0.5))
        tracking_figure_axes.add_patch(tracking_rectangle)

        gt_point = pylab.Circle((0, 0), radius=5)
        gt_point.set_color((0, 0, 1, 0.5))
        tracking_figure_axes.add_patch(gt_point)

        tracking_figure_title = tracking_figure.suptitle("")

        pylab.show(block=False)

    elif tracking_figure is None:
        return  # we simply go faster by skipping the drawing
    elif not pylab.fignum_exists(tracking_figure.number):
        #print("Drawing window closed, end of game. "
        #      "Have a nice day !")
        #sys.exit()
        print("From now on drawing will be omitted, "
              "so that computation goes faster")
        tracking_figure = None
        return

    global z, response
    tracking_figure_axes.imshow(im, cmap=pylab.cm.gray)

    rect_y, rect_x = tuple(pos - target_sz/2.0)
    rect_height, rect_width = target_sz

    if should_resize_image:
        rect_y = rect_y * 2
        rect_x = rect_x * 2
        rect_height = rect_height * 2
        rect_width = rect_width * 2

    tracking_rectangle.set_xy((rect_x, rect_y))
    tracking_rectangle.set_width(rect_width)
    tracking_rectangle.set_height(rect_height)

    if len(ground_truth) > 0:
        gt = ground_truth[frame]
        gt_y, gt_x = gt
        gt_point.center = (gt_x, gt_y)

    if tracker.z is not None:
        z_figure_axes.imshow(tracker.z, cmap=pylab.cm.hot)

    if tracker.response is not None:
        response_figure_axes.imshow(tracker.response, cmap=pylab.cm.hot)

    tracking_figure_title.set_text("Frame %i (out of %i)"
                                   % (frame + 1, len(ground_truth)))

    if debug and False and (frame % 1) == 0:
        print("Tracked pos ==", pos)

    #tracking_figure.canvas.draw()  # update
    pylab.draw()
    pylab.waitforbuttonpress(timeout=timeout)

    return

def printTrack( fid , resized , frame , pos , sz ):
    p0 = [pos[0]-pylab.floor(sz[0]/2),pos[1]-pylab.ceil(sz[1]/2)]
    p1 = [pos[0]+pylab.floor(sz[0]/2),pos[1]+pylab.ceil(sz[1]/2)]

    if resized:
        p0 = [x*2 for x in p0]
        p1 = [x*2 for x in p1]

    fid.write(str(frame)+","+str(p0[1])+","+str(p0[0])+","+str(p1[1])+","+str(p1[0])+"\n")

def track(input_video_path):
    """
    notation: variables ending with f are in the frequency domain.
    """
    global tracker

    info = load_video_info(input_video_path)
    img_files, pos, target_sz, \
        should_resize_image, ground_truth, video_path = info

    total_time = 0  # to calculate FPS
    positions = pylab.zeros((len(img_files), 2))  # to calculate precision

    tracker = CirculantMatrixTracker(should_resize_image)

    if save:
        file_name = "PCirculant_"+os.path.split(input_video_path)[1]+".txt"
        file_out = open(file_name, 'w')

    for frame, image_filename in enumerate(img_files):

        if True and ((frame % 10) == 0):
            print("Processing frame", frame)

        # load image
        image_path = os.path.join(video_path, image_filename)
        im = pylab.imread(image_path)
        if len(im.shape) == 3 and im.shape[2] > 1:
            im = rgb2gray(im)

        #print("Image max/min value==", im.max(), "/", im.min())

        start_time = time.time()

        if frame == 0:
            tracker.initialize(im,pos,target_sz)
            if save:
                printTrack(file_out,should_resize_image,frame,pos,target_sz)
        else:
            pos = tracker.find(im)
            tracker.update_template()

            if save:
                printTrack(file_out,should_resize_image,frame,pos,target_sz)

            if debug:
                print("Frame ==", frame)
                print("Max response", tracker.r.max(), "at", [tracker.row, tracker.col])
                pylab.figure()
                pylab.imshow(tracker.cos_window)
                pylab.title("cos_window")

                pylab.figure()
                pylab.imshow(tracker.x)
                pylab.title("x")

                pylab.figure()
                pylab.imshow(response)
                pylab.title("response")
                pylab.show(block=True)


        tl = pos - pylab.floor(target_sz / 2)
        print(str(frame)+' loc '+str(tl[1])+" "+str(tl[0]))

        # save position and calculate FPS
        positions[frame, :] = pos
        total_time += time.time() - start_time

        # visualization
        if gui:
            plot_tracking(frame, pos, target_sz, should_resize_image, im, ground_truth)
        # end of "for each image in video"

    if should_resize_image:
        positions = positions * 2

    if save:
        file_out.close()

    print("Frames-per-second:",  len(img_files) / total_time)

    title = os.path.basename(os.path.normpath(input_video_path))

    if gui and len(ground_truth) > 0:
        # show the precisions plot
        show_precision(positions, ground_truth, video_path, title)

    return


def parse_arguments():

    parser = OptionParser()
    parser.description = \
        "This program will track objects " \
        "on videos in the MILTrack paper format. " \
        "See http://goo.gl/pSTo9r"

    parser.add_option("-i", "--input", dest="video_path",
                      metavar="PATH", type="string", default=None,
                      help="path to a folder o a MILTrack video")

    parser.add_option("--gui", dest="store_gui",
                      default=gui,
                      help="turn gui and and off")

    parser.add_option("--save", dest="store_save",
                      default=save,
                      help="turn on saving tracks")


    (options, args) = parser.parse_args()
    #print (options, args)

    if not options.video_path:
        parser.error("'input' option is required to run this program")
    if not os.path.exists(options.video_path):
            parser.error("Could not find the input file %s"
                         % options.video_path)

    return options


def main():
    global gui,save

    options = parse_arguments()

    gui = options.store_gui.lower() == 'true'
    save = options.store_save.lower() == 'true'

    track(options.video_path)

    print("End of game, have a nice day!")
    return


if __name__ == "__main__":

    main()

# end of file
