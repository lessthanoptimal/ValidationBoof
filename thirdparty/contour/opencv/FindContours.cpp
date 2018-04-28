#include <cstddef>
#include <string>
#include <iostream>
#include <iterator>
#include <boost/program_options.hpp>
#include <boost/filesystem.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <fstream>
#include <opencv2/imgproc.hpp>
#include <chrono>

using namespace boost::algorithm;
using namespace std;
using namespace cv;
using namespace std::chrono;
namespace po = boost::program_options;
namespace bf = boost::filesystem;

void perform_thresholding( int threshold_value, const bf::path& image_path ,
                           const bf::path& contour_path, const bf::path& runtime_path )
{
//    cout << "image_path = " << image_path << endl;
//    cout << "contour_path = " << contour_path << endl;
//    cout << "runtime_path = " << runtime_path << endl;

    Mat image = imread(image_path.c_str(), CV_LOAD_IMAGE_GRAYSCALE | CV_LOAD_IMAGE_IGNORE_ORIENTATION);
    Mat binary;

    cv::threshold( image, binary, threshold_value, 255,THRESH_BINARY_INV );

    vector<vector<Point> > contours;
    vector<Vec4i> hierarchy;

    // Copy the binary image into a larger one so that the image border isn't ignored
    cv::Mat enlarged = cv::Mat::zeros(binary.rows+2, binary.cols+2, CV_8UC1);
    int64_t before = duration_cast< milliseconds >(system_clock::now().time_since_epoch()).count();
    binary.copyTo(enlarged.rowRange(1, binary.rows+1).colRange(1, binary.cols+1));
    cv::findContours(enlarged,contours, hierarchy, RETR_CCOMP , CHAIN_APPROX_NONE,cv::Point(-1,-1));
    int64_t after = duration_cast< milliseconds >(system_clock::now().time_since_epoch()).count();

    cout << "Image "<<binary.cols<<"x"<<binary.rows<<" Contour " << (after-before) << " (ms)  found=" << contours.size() << "  " << image_path << endl;

    ofstream file_contour;
    file_contour.open(contour_path.c_str());
    file_contour << "# OpenCV contour "<<contour_path.filename() << endl;

    for (size_t cpt = 0; cpt < contours.size(); cpt++) {
        // this should be external contours only
        if( hierarchy[cpt][3] != -1 )
            continue;
        vector<Point> &contour = contours.at(cpt);
        for( size_t i = 0; i < contour.size(); i++ ) {
            Point &p = contour.at(i);
            file_contour << p.x << " " << p.y << " ";
        }
        file_contour << endl;
    }
    file_contour.close();

    ofstream file_runtime;
    file_runtime.open(runtime_path.c_str());
    file_runtime << after-before << endl;
    file_runtime.close();

}

void find_contours_directory( const string& input_path , const string& output_path , int threshold ) {
    cout << "Input Path:  " << input_path << endl;
    cout << "Output Path: " << output_path << endl;

    bf::create_directory(bf::path(output_path));

    bf::path p(input_path);

    bf::directory_iterator end_itr;

    // cycle through the directory
    for (bf::directory_iterator itr(p); itr != end_itr; ++itr)
    {
        if (bf::is_regular_file(itr->path())) {
            string current_file = itr->path().string();

            if( !(ends_with(current_file, "jpg")|| ends_with(current_file, "png"))) {
                continue;
            }
            bf::path base_file_path(output_path);
            base_file_path = base_file_path / itr->path().filename();
            bf::path output_contour = bf::change_extension(base_file_path, "txt");
            bf::path output_runtime = bf::path(bf::change_extension(base_file_path, "").string()+"_runtime.txt");

            perform_thresholding(threshold,itr->path(),output_contour,output_runtime);
//            cout << current_file << endl;
        } else {
            bf::path output(output_path);
            output = output / itr->path().filename();
            find_contours_directory(itr->path().string(),output.string(),threshold);
        }
    }
//    cout << endl;
}

int main( int argc, char *argv[] ) {
    try {

        po::options_description desc("Allowed options");
        desc.add_options()
            ("help", "produce help message")
            ("Input,i", po::value<std::string >(), "Path to input directory")
            ("Output,o", po::value<std::string >(), "Path to output directory")
            ("Threshold,t", po::value<int>(), "Binarization threshold");

        po::variables_map vm;
        po::store(po::parse_command_line(argc, argv, desc), vm);
        po::notify(vm);

        if (vm.count("help")) {
            cout << desc << "\n";
            return 0;
        }

        if ( !vm.count("Input")) {
            cout << desc << "\n";
            cout << "Input path was not set.\n";
            return 0;
        }
        if ( !vm.count("Output")) {
             cout << desc << "\n";
             cout << "Output path was not set.\n";
             return 0;
        }
        if ( !vm.count("Threshold")) {
            cout << desc << "\n";
            cout << "Threshold was not set.\n";
            return 0;
        }
        find_contours_directory(vm["Input"].as<string>(),vm["Output"].as<string>(),vm["Threshold"].as<int>());
    } catch(exception& e) {
        cerr << "error: " << e.what() << "\n";
        return 1;
    } catch(...) {
        cerr << "Exception of unknown type!\n";
    }

    printf("done!\n");
    return 0;
}