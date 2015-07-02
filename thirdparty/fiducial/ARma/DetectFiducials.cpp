#include <iostream>
#include <stdio.h>
#include <signal.h>
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <stdexcept>
#include <fstream>
#include "patterndetector.h"
#include "boost/filesystem.hpp"

using namespace std;
using namespace cv;
using namespace ARma;
using namespace boost::filesystem;

void loadParameters( string path , Mat &cameraMatrix , Mat &distortions ) {
//    cameraMatrix.reshape(3,3);
//    distortions.reshape(1,4);
    for( int i = 0; i < 4; i++ )
        distortions.at<float>(0,i) = 0;

    ifstream file(path.c_str());

    int row = 0;
    std::string line;
    while( std::getline(file, line) ) {
        if( line.at(0) == '#')
            continue;

//        cout << "Read in line " << line << endl;
        if( row < 3 ) {
            std::istringstream iss(line);
            double x, y, z;
            if (!(iss >> x >> y >> z)) {
                throw std::runtime_error("parse error in loadParameters");
            }
            cameraMatrix.at<float>(row, 0) = x;
            cameraMatrix.at<float>(row, 1) = y;
            cameraMatrix.at<float>(row, 2) = z;
        } else {
            // ignore the image size
//            cout << "ignoring line..." << endl;
        }
        row++;
    }
}

bool has_ending(std::string const &fullString, std::string const &ending) {
    if (fullString.length() >= ending.length()) {
        return (0 == fullString.compare (fullString.length() - ending.length(), ending.length(), ending));
    } else {
        return false;
    }
}

void loadImageList( std::string dir_path , std::vector<string> &files ) {
    if ( !exists( dir_path ) ) return;
    directory_iterator end_itr; // default construction yields past-the-end
    for ( directory_iterator itr( dir_path );
          itr != end_itr;
          ++itr )
    {
        string p = itr->path().string();
        if( has_ending(p,"jpg") || has_ending(p,"png"))
            files.push_back(p);
    }
}

void loadConfigFile( std::string directory , std::vector<string> &names , std::vector<double>& sizes ) {
    string config_name = string(directory+"/library.txt");

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
            sizes.push_back(width);
        }

    }
}

int main(int argc, char** argv)
{
    std::vector<cv::Mat> patternLibrary;
    std::vector<Pattern> detectedPattern;

    if( argc < 3 )
        throw std::runtime_error("Need to specify input and output directory");


    // parse the the scenario
    string scenario_dir = string(argv[1]);
    string output_dir = string(argv[2]);
    vector<string> patternFiles;
    std::vector<double> fiducialWidths;
    loadConfigFile(scenario_dir,patternFiles,fiducialWidths);

    if ( !boost::filesystem::exists( output_dir ) )
    {
        std::cout << "output directory doesn't exist!" << std::endl;
        return -1;

    }

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

    // load the intrinsic parameters
    Mat cameraMatrix(3, 3,CV_32F);
    Mat distortions(1, 4,CV_32F);

    loadParameters(scenario_dir+"/intrinsic.txt",cameraMatrix,distortions);

    cout << cameraMatrix << endl;

    // Configure the actual detector
    int norm_pattern_size = PAT_SIZE;
    double fixed_thresh = 40;
    double adapt_thresh = 5;//non-used with FIXED_THRESHOLD mode
    int adapt_block_size = 45;//non-used with FIXED_THRESHOLD mode
    double confidenceThreshold = 0.35;
    int mode = 2;//1:FIXED_THRESHOLD, 2: ADAPTIVE_THRESHOLD

    PatternDetector myDetector( fixed_thresh, adapt_thresh, adapt_block_size, confidenceThreshold, norm_pattern_size, mode);

    vector<string> file_list;
    loadImageList(scenario_dir,file_list);


    for( size_t i = 0; i < file_list.size(); ++i ) {

        boost::filesystem::path p(file_list.at(i));

        string file_name =  p.filename().string();

        boost::filesystem::path output_path = boost::filesystem::path(output_dir);
        output_path /= file_name.substr(0,file_name.find(".")+1)+"csv";
        ofstream file_out(output_path.c_str());
        cout << "Saving to " << output_path.string() << "  ";
        file_out << "# Detected fiducials inside of " <<file_name << " using ARma" << endl;
        file_out << "# 4 lines for each detection. line 1 = detected fiducial.  lines 2-4 = rigid body transform, row major" << endl;

        Mat imgMat =imread(file_list.at(i).c_str(),0);

        //run the detector
        myDetector.detect(imgMat, cameraMatrix, distortions, patternLibrary, detectedPattern);

        cout << "Detected: " << detectedPattern.size() << endl;

        for( size_t j = 0; j < detectedPattern.size(); j++ ) {
            Pattern &pattern = detectedPattern.at(j);

            if( pattern.id > 0 ) {
                Mat &R = pattern.getRotationMatrix();
                Mat &T = pattern.transVec;

                float patternWidth = (float)fiducialWidths.at((int)(pattern.id-1));

                float X = (T.at<float>(0, 0))*patternWidth;
                float Y = (T.at<float>(1, 0))*patternWidth;
                float Z = T.at<float>(2, 0)*patternWidth;


                file_out << (pattern.id-1) << endl;
                file_out << R.at<float>(0, 0) << " " << R.at<float>(0, 1) << " " << R.at<float>(0, 2) << " " <<
                        X << endl;
                file_out << R.at<float>(1, 0) << " " << R.at<float>(1, 1) << " " << R.at<float>(1, 2) << " " <<
                        Y << endl;
                file_out << R.at<float>(2, 0) << " " << R.at<float>(2, 1) << " " << R.at<float>(2, 2) << " " <<
                        Z << endl;
            }
        }


        detectedPattern.clear();
    }

    return 0;
}