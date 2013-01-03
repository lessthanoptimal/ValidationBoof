#include <iostream>
#include <stdio.h>
#include <string>
#include <vector>
#include <stdint.h>

#include <viso_stereo.h>
#include <png++/png.hpp>

using namespace std;

VisualOdometryStereo::parameters parseStereoParam( std::string fileName ) {

    FILE *fid = fopen(fileName.c_str(),"r");

    VisualOdometryStereo::parameters param;

    double P0[12],P1[12];
    fscanf(fid,"P0: %lf %lf %lf %lf %lf %lf %lf %lf %lf %lf %lf %lf %lf\n",
          &(P0[0]),&(P0[1]),&(P0[2]),&(P0[3]),&(P0[4]),&(P0[5]),&(P0[6]),&(P0[7]),&(P0[8]),&(P0[9]),&(P0[10]),&(P0[11]));
    fscanf(fid,"P1: %lf %lf %lf %lf %lf %lf %lf %lf %lf %lf %lf %lf %lf\n",
          &P1[0],&P1[1],&P1[2],&P1[3],&P1[4],&P1[5],&P1[6],&P1[7],&P1[8],&P1[9],&P1[10],&P1[11]);

    printf("P0 = %f %f %f\n",P0[0],P0[1],P0[2]);
    printf("P1 = %f %f %f\n",P1[0],P1[1],P1[2]);

    Matrix K(3,3);
    for( int i = 0; i < 3; i++ )
        for( int j = 0; j < 3; j++ )
            K.val[i][j] = P0[i*4 + j];

    K.inv();
    Matrix T(3,1);
    for( int i = 0; i < 3; i++ )
        T.val[i][0] = P1[i*4 + 3];

    printf("Kinv[0,0] = %f\n",K.val[0][0]);

    Matrix foo = K*T;

    param.calib.f = P0[0];
    param.calib.cu = P0[2];
    param.calib.cv = P0[2+4];

    double dx = foo.val[0][0];
    double dy = foo.val[1][0];
    double dz = foo.val[2][0];

    param.base = sqrt(dx*dx + dy*dy + dz*dz);

    printf("base = %f\n",param.base);

    fclose(fid);

    return param;
}

void runScenario( std::string directoryName , std::string outputName ) {

    ofstream fout;
    fout.open(outputName.c_str());

    std::string paramName = directoryName + "/calib.txt";

    VisualOdometryStereo::parameters param = parseStereoParam(paramName);

    // init visual odometry
    VisualOdometryStereo viso(param);

    // current pose (this matrix transforms a point from the current
    // frame's camera coordinates to the first frame's camera coordinates)
    Matrix pose = Matrix::eye(4);

    // loop through all frames
    for (int32_t i=0; ; i++) {

      // input file names
      char base_name[256]; sprintf(base_name,"%06d.png",i);
      string left_img_file_name  = directoryName + "/image_0/" + base_name;
      string right_img_file_name = directoryName + "/image_1/" + base_name;

      // catch image read/write errors here
      try {
        cout << "Processing: Frame: " << i << endl;

        // load left and right input image
        png::image< png::gray_pixel > left_img(left_img_file_name);
        png::image< png::gray_pixel > right_img(right_img_file_name);

        // image dimensions
        int32_t width  = left_img.get_width();
        int32_t height = left_img.get_height();

        // convert input images to uint8_t buffer
        uint8_t* left_img_data  = (uint8_t*)malloc(width*height*sizeof(uint8_t));
        uint8_t* right_img_data = (uint8_t*)malloc(width*height*sizeof(uint8_t));
        int32_t k=0;
        for (int32_t v=0; v<height; v++) {
          for (int32_t u=0; u<width; u++) {
            left_img_data[k]  = left_img.get_pixel(u,v);
            right_img_data[k] = right_img.get_pixel(u,v);
            k++;
          }
        }

        // status


        // compute visual odometry
        int32_t dims[] = {width,height,width};
        if (viso.process(left_img_data,right_img_data,dims)) {

          // on success, update current pose
          pose = pose * Matrix::inv(viso.getMotion());

          // output some statistics
          double num_matches = viso.getNumberOfMatches();
          double num_inliers = viso.getNumberOfInliers();
          cout << ", Matches: " << num_matches;
          cout << ", Inliers: " << 100.0*num_inliers/num_matches << " %" << ", Current pose: " << endl;
          cout << pose << endl << endl;

          for( int j = 0; j < 12; j++ )
              fout << pose.val[j/4][j%4] << " ";
          fout << endl;
          fout.flush();
        } else {
          fout << "1 0 0 0 0 1 0 0 0 0 1 0" << endl;
          cout << " ... failed!" << endl;
        }

        // release uint8_t buffers
        free(left_img_data);
        free(right_img_data);

      // catch image read errors here
      } catch (...) {
        break;
      }
    }
    fout.close();
}

int main (int argc, char** argv) {
    if( argc < 3 ) {
        printf("directory sequenceNumber\n");
        exit(-1);
    }

    std::string sequenceName = argv[2];
    std::string dir = std::string(argv[1])+sequenceName;

    runScenario(dir,sequenceName+".txt");

    return 0;
}
