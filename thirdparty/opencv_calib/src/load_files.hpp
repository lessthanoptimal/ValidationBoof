#ifndef LOAD_FILES_HPP
#define LOAD_FILES_HPP

#include <vector>
#include "opencv2/core/core.hpp"
#include "opencv2/calib3d/calib3d.hpp"

std::vector<cv::Point3f> loadGrid3D( const char *fileName );
std::vector< std::vector<cv::Point2f> > loadObservations( const char *fileName ) ;

#endif // LOAD_FILES_HPP
