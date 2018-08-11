# Introduction

http://ceres-solver.org

http://ceres-solver.org/nnls_tutorial.html#bundle-adjustment

# Setup

1) Orignal instructions at http://ceres-solver.org/installation.html
2) Enter the commands below:
```
sudo apt-get install libgoogle-glog-dev cmake libatlas-base-dev libeigen3-dev libsuitesparse-dev
git clone https://ceres-solver.googlesource.com/ceres-solver
cd ceres-solver
git checkout 1.14.0
mkdir build;cd build; cmake ..;make -j6 
```
