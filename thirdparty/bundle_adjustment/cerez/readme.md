# Introduction

http://ceres-solver.org

http://ceres-solver.org/nnls_tutorial.html#bundle-adjustment

# Setup

1) Follow installation instructions at http://ceres-solver.org/installation.html
2) sudo apt-get install libgoogle-glog-dev cmake libatlas-base-dev libeigen3-dev libsuitesparse-dev
3) git clone https://ceres-solver.googlesource.com/ceres-solver
4) git checkout 923fddcd0eabd7902092122b8cdca45800609870
5) mkdir build;cd build; cmake ..;make -j6 