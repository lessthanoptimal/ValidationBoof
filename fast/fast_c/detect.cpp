#include "fast.h"
#include <stdio.h>
#include <cv.h>
#include <highgui.h>

int main( int argc , char **argv ) {
  if( argc <= 1 ) {
    printf("Specify the input image\n");
    return 0;
  }

  printf("Loading image %s\n",argv[1]);
  IplImage* img = cvLoadImage(argv[1]);

  if( img == NULL ) {
    printf("Can't find image\n");
    return 0;
  }

  IplImage* gray = NULL;
  if( img->depth != IPL_DEPTH_8U ) {
    printf("Must be an 8-bit image\n");
    return 0;
  } else if( img->nChannels != 1 ) {
    gray = cvCreateImage(cvSize(img->width,img->height),IPL_DEPTH_8U,1);
    // Convert the iamge to gray scale the same way as BoofCV
    for( int y = 0; y < img->height; y++ ) {
      unsigned char *src = (unsigned char *)(img->imageData + y*img->widthStep);
      unsigned char *dst = (unsigned char *)(gray->imageData + y*gray->widthStep);
      for( int x = 0; x < img->width; x++ ) {
	int total = 0;
	total += *src++;
	total += *src++;
	total += *src++;

	*dst++ = (unsigned char)(total/3);
      }
    }

  } else {
    gray = img;
  }


  int numFound = 0;
  xy* pts = fast9_detect_nonmax((byte*)gray->imageData,gray->width,gray->height,gray->widthStep,20,&numFound);

  printf("Found %d corners\n",numFound);
  FILE *fid = fopen("detected.txt","w");


  fprintf(fid,"# Detected points for original FAST-9\n");
  fprintf(fid,"%d\n",numFound);
  for( int i = 0; i < numFound; i++ ) {
    fprintf(fid,"%d %d\n",pts[i].x,pts[i].y);
  }
  fclose(fid);
}
