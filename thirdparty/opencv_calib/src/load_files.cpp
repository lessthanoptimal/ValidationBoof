#include "load_files.hpp"
#include <stdio.h>

using namespace std;
using namespace cv;

vector<Point3f> loadGrid3D( const char *fileName ) {
  FILE *fid = fopen(fileName,"r");

  if( fid == NULL ) {
    fprintf(stderr,"3D grid file not found\n");
    exit(0);
  }

  vector<Point3f> ret;

  int N;
  fscanf(fid,"%d\n",&N);

  float x,y;

  while( fscanf(fid,"%f %f\n",&x,&y) == 2 ) {
    ret.push_back(Point3f(x,y,0));
  }

  fclose(fid);

  return ret;
}

vector< vector<Point2f> > loadObservations( const char *fileName ) {
  FILE *fid = fopen(fileName,"r");

  if( fid == NULL ) {
    fprintf(stderr,"Target observaiton file not found\n");
    exit(0);
  }

  vector< vector<Point2f> > ret;

  int N;

// number of points observed
  while( fscanf(fid,"%d ",&N) == 1 ) {
    vector<Point2f> target;
    for( int i = 0; i < N; i++ ) {
      float x,y;

      if( fscanf(fid,"%f %f",&x,&y) != 2 )
        printf("Unexpected number read found\n");

      target.push_back(Point2f(x,y));
    }
    ret.push_back(target);
  }

  fclose(fid);

  return ret;
}

