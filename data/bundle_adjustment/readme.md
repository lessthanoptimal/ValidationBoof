# Data Source

bundle_adjustment_in_the_large.zip is a subset of the datasets from [1]. Currently none
of the largest data sets are tested to keep this regression running fast. Plus current
SBA algorithm in BoofCV isn't designed to handle such large problems yet.

[1] http://grail.cs.washington.edu/projects/bal/

## Description

The skeletal sets and the Ladybug datasets were reconstructed incrementally using a modified version of Bundler, which was instrumented to dump intermediate unoptimized reconstructions to disk. This gave rise to the Ladybug, Trafalgar Square, Dubrovnik and Venice datasets. We refer to the bundle adjustment problems obtained after adding the leaf images to the skeletal set and triangulating the remaing points as the Final problems.

## Camera Model
We use a pinhole camera model; the parameters we estimate for each camera area rotation R, a translation t, a focal length f and two radial distortion parameters k1 and k2. The formula for projecting a 3D point X into a camera R,t,f,k1,k2 is:
P  =  R * X + t       (conversion from world to camera coordinates)
p  = -P / P.z         (perspective division)
p' =  f * r(p) * p    (conversion to pixel coordinates)
where P.z is the third (z) coordinate of P. In the last equation, r(p) is a function that computes a scaling factor to undo the radial distortion:
r(p) = 1.0 + k1 * ||p||^2 + k2 * ||p||^4.
This gives a projection in pixels, where the origin of the image is the center of the image, the positive x-axis points right, and the positive y-axis points up (in addition, in the camera coordinate system, the positive z-axis points backwards, so the camera is looking down the negative z-axis, as in OpenGL).

## Data Format

Each problem is provided as a bzip2 compressed text file in the following format.

<num_cameras> <num_points> <num_observations>
<camera_index_1> <point_index_1> <x_1> <y_1>
...
<camera_index_num_observations> <point_index_num_observations> <x_num_observations> <y_num_observations>
<camera_1>
...
<camera_num_cameras>
<point_1>
...
<point_num_points>

Where, there camera and point indices start from 0. Each camera is a set of 9 parameters - R,t,f,k1 and k2. The rotation R is specified as a Rodrigues' vector.