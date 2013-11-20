#include "opencv2/calib3d/calib3d.hpp"
#include "opencv2/imgproc/imgproc.hpp"

#include <iostream>
#include <vector>
#include <algorithm>
#include <iterator>
#include <stdio.h>

#include "load_files.hpp"

using namespace cv;
using namespace std;

int main( int argc , char **argv )
{
  const Size imgSize(640, 480);

  vector<Point3f> target = loadGrid3D("../../../results/calib/chess.txt");
//  vector< vector<Point2f> > imagePoints = loadObservations("../../../results/calib/points_boofcv_chess_bumblebee2_left.txt");
  vector< vector<Point2f> > imagePoints = loadObservations("../../../results/calib/points_opencv_chess_bumblebee2_left.txt");

  vector< vector<Point3f> > objectPoints;

  // the full target is visible in every image
  for( size_t i = 0; i < imagePoints.size(); i++ ) {
    objectPoints.push_back(target);
  }

  // debugging
  if( false ) {
    printf("Calibration Points\n");
    for( size_t i = 0; i < target.size(); i++ ) {
      Point3f &p = target.at(i);
      printf("%8f %8f %8f\n",p.x,p.y,p.z);
    }
  }

  Mat K;
  Mat distCoeffs;
  vector<Mat> rvecs, tvecs;

  double rep_err = calibrateCamera(objectPoints, imagePoints, imgSize, K, distCoeffs, rvecs, tvecs,
                                   CV_CALIB_ZERO_TANGENT_DIST | CV_CALIB_FIX_K3 );

  FILE *fid = fopen("opencv_calib.txt","w");

  fprintf(fid,"%f %f %f %f %f\n",
          K.at<double>(0,0),K.at<double>(1,1),K.at<double>(0,1),K.at<double>(0,2),K.at<double>(1,2));
  fprintf(fid,"%d",distCoeffs.cols);
  for( int i = 0; i < distCoeffs.cols; i++ ) {
    fprintf(fid," %f",distCoeffs.at<double>(i));
  }
  fprintf(fid,"\n");

  fprintf(fid,"%d\n",(int)rvecs.size());

  for( size_t i = 0; i < rvecs.size(); i++ ) {
      Mat &r = rvecs.at(i);
      Mat &t = tvecs.at(i);

      fprintf(fid,"%1.15f %1.15f %1.15f %1.15f %1.15f %1.15f\n",
              r.at<double>(0),r.at<double>(1),r.at<double>(2),
              t.at<double>(0),t.at<double>(1),t.at<double>(2));
  }


  fclose(fid);



  printf("Done %f\n",rep_err);
}
