On this website, we make the first test sequence from our CVPR'07
paper available for non-commercial research use. If you publish
results based on this data, please cite the original paper

- for object recognition/tracking results:
B. Leibe, N. Cornelis, K. Cornelis, L. Van Gool,
Dynamic 3D Scene Analysis from a Moving Vehicle.
In CVPR'07, Minneapolis, USA, June 2007.

- for 3D reconstruction results:
N. Cornelis, B. Leibe, K. Cornelis, L. Van Gool,
3D Urban Scene Modeling Integrating Recognition and Reconstruction.
to appear in IJCV, 2007.

The archive contains the following files
- leuven-left.tgz   : archive with the left camera stream 
- leuven-right.tgz  : archive with the right camera stream
- read_camera.m     : a Matlab file for reading the camera parameters

The sequence contains 1175 stereo camera pairs acquired with setup
mounted on top of a moving vehicle. The stereo setup has a fixed
baseline, and the cameras are calibrated internally and with respect
to each other. Each archive contains the original images of the
respective camera stream at 360*288 pixel resolution (Note that they
were upscaled to twice this original size for object recognition). In
addition, each archive contains a subdirectory "maps" containing for
each frame the external camera calibration and ground plane estimated
by Structure-from-Motion. This data is written in the following format:

K          : (3x3) internal camera parameter matrix
k1 k2 k3   : radial distortion parameters (here: all set to 0)
R          : (3x3) camera rotation matrix
t          : (1x3) camera translation vector
n1 n2 n3 d : ground plane parameters 

For convenience, we have provided the file "read_camera.m", which demonstrates how to read in the camera parameters. We plan to make additional Matlab functions, as well as our baseline object detections available soon.

24.8.2007 Bastian Leibe
leibe@vision.ee.ethz.ch
