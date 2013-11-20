#include "opencv2/calib3d/calib3d.hpp"
#include "opencv2/imgproc/imgproc.hpp"
#include "opencv2/highgui/highgui.hpp"

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
  vector<vector<Point2f> > imagePoints;
  vector<string> imageList;
  Size boardSize(4,6);
  Mat view,viewGray;
  bool found;

  const char *dir = "../../../data/calib/stereo/Bumblebee2_Chess/";
  int numImages = 11;

  for( int i = 0; i < numImages; i++ ) {
    char path[1024];
    sprintf(path,"%sleft%02d.jpg",dir,i+1);
    printf("Processing image %s\n",path);

    vector<Point2f> pointbuf;

    view = imread(path, 1);
    if( view.data == NULL ) {
      fprintf(stderr,"Could not load image file: %s\n",path);
      exit(0);
    }
    cvtColor(view, viewGray, CV_BGR2GRAY);

    found = findChessboardCorners( viewGray, boardSize, pointbuf,
                                   CV_CALIB_CB_ADAPTIVE_THRESH | CV_CALIB_CB_FAST_CHECK );
    if( !found ) {
      fprintf(stderr,"Chessboard not found %d!\n",i);
      exit(0);
    }

    imagePoints.push_back(pointbuf);
  }

  // save results
  FILE *fid = fopen("opencv_calib_pts.txt","w");
  if( fid == NULL ) {
    fprintf(stderr,"Can't open output file\n");
    exit(0);
  }

  for( size_t i = 0; i < imagePoints.size(); i++ ) {
    vector<Point2f> &pts = imagePoints.at(i);
    fprintf(fid,"%d",(int)pts.size());

    for( size_t j = 0; j < pts.size(); j++ ) {
      Point2f &p = pts.at(j);
      fprintf(fid," %f %f",p.x,p.y);
    }
    fprintf(fid,"\n");
  }
  fclose(fid);

}
