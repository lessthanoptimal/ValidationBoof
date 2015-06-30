#include <iostream>
#include <stdio.h>
#include <signal.h>
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <stdexcept>
#include <fstream>
#include "patterndetector.h"

using namespace std;
using namespace cv;
using namespace ARma;

void loadParameters( string path , Mat &cameraMatrix , Mat &distortions ) {
//    FileStorage fs2(path, FileStorage::READ);
//
//    if( !fs2.isOpened() ) {
//        cout << "Can't open camera parameters at " << path << endl;
//        exit(0);
//    }
//
//    fs2["camera_matrix"] >> cameraMatrix;
//    fs2["distortion_coefficients"] >> distortions;
//
//    fs2.release();
//
//    if( cameraMatrix.cols == 0 ) {
//        cout << "Size zero camera matrix..." << endl;
//        exit(0);
//    }
//    cout << "camera matrix shape " << cameraMatrix.rows << " " << cameraMatrix.cols << endl;
}

void loadConfigFile( std::string directory , std::vector<string> &names ) {
    string config_name = string(directory+"/expected.txt");

    ifstream file(config_name.c_str());

    std::string line;
    while( std::getline(file, line) ) {
        if( line.at(0) == '#')
            continue;

        std::istringstream iss(line);
        double width;
        string name;
        if (!(iss >> width >> name)) {
            throw std::runtime_error("parse error");
        }

        // don't add the same name twice
        if (std::find(names.begin(), names.end(), name) == names.end())
        {
            names.push_back(name);
        }

    }
}

int main(int argc, char** argv)
{
    std::vector<cv::Mat> patternLibrary;
    std::vector<Pattern> detectedPattern;

    if( argc < 2 )
        throw std::runtime_error("Need to specify input directory");


    // parse the the scenario
    string scenario_dir = string(argv[1]);
    vector<string> patternFiles;
    loadConfigFile(scenario_dir,patternFiles);

    // load all the patterns into ARma
    string pattern_dir = string(scenario_dir+"/../");
    for( size_t i = 0; i < patternFiles.size(); ++i ) {

        string p = pattern_dir+patternFiles.at(i);
        if( loadPattern(p.c_str(), patternLibrary,true) >= 0 ) {
            cout << "Loaded pattern " << p << endl;
        } else {
            cout << "Failed to load pattern " << p << endl;
            throw std::runtime_error("Failed to load a pattern");
        }
    }

    // TODO load the intrinsic parameters
    Mat cameraMatrix , distortions;
    loadParameters("camera.yml",cameraMatrix,distortions);


    // TODO process all the images in the directory and save the results

}