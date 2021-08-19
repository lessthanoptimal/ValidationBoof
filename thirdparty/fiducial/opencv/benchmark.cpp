#include <cstddef>
#include <string>
#include <iostream>
#include <iterator>
#include <boost/program_options.hpp>
#include <boost/filesystem.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include <fstream>
#include <chrono>
#include <iomanip>
#include <opencv2/objdetect.hpp>
#include <opencv2/imgcodecs.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/aruco/charuco.hpp>

using namespace boost::algorithm;
using namespace std;
using namespace cv;

namespace po = boost::program_options;
namespace bf = boost::filesystem;

std::string filter_string(const std::string &message) {
    std::string c = message;
    for (std::string::iterator iter = c.begin(); iter != c.end();) {
        if (!std::isprint(*iter))
            iter = c.erase(iter);
        else
            ++iter; // not erased, increment iterator
    }
    return c;
}

void run_opencv(const bf::path &image_path, const bf::path &output_path) {
    Mat image = imread(image_path.c_str(), IMREAD_GRAYSCALE | IMREAD_IGNORE_ORIENTATION);

    //    cout << "output_pat "<<output_path<<endl;

    //    cout << "loaded image. w="<<image.rows<<" x "<<image.cols<<" channels "<<image.channels()<<endl;

    if (image.channels() != 1) {
        cout << "Color image???" << endl;
        exit(1);
    }

    cv::Ptr<cv::aruco::Dictionary> dictionary = cv::aruco::getPredefinedDictionary(cv::aruco::DICT_6X6_100);
    cv::Ptr<cv::aruco::CharucoBoard> board = cv::aruco::CharucoBoard::create(6, 8, 0.03f, 0.02f, dictionary);
    cv::Ptr<cv::aruco::DetectorParameters> params = cv::aruco::DetectorParameters::create();
    params->cornerRefinementMethod = cv::aruco::CORNER_REFINE_NONE;

    std::vector<int> markerIds;
    std::vector<std::vector<cv::Point2f> > markerCorners;
    std::vector<cv::Point2f> charucoCorners;
    std::vector<int> charucoIds;

    // output to a stream instead of the file initially just in case it does processing at this stage
    std::ostringstream streamMem;

    auto time0 = chrono::steady_clock::now();
    try {
        cv::aruco::detectMarkers(image, board->dictionary, markerCorners, markerIds, params);
        if (!markerIds.empty()) {
            cv::aruco::drawDetectedMarkers(image, markerCorners, markerIds);
            cv::aruco::interpolateCornersCharuco(markerCorners, markerIds, image, board, charucoCorners, charucoIds);
        }
    } catch (...) {
        cout << "Exception!!!" << endl;
    }
    auto time1 = chrono::steady_clock::now();

    int valid = 0;
    if (!charucoCorners.empty()) {
        valid++;
        for (size_t i = 0; i < charucoIds.size(); ++i) {
            const auto &c = charucoCorners[i];
            const auto id = charucoIds[i];
            streamMem << id << " " << c.x << " " << c.y << std::endl;
        }
    }

    double milliseconds = 1e-6 * chrono::duration_cast<chrono::nanoseconds>(time1 - time0).count();

    ofstream file;
    file.open(output_path.c_str());
    file << "# Charuco OpenCV: " << image_path.filename() << std::endl;
    file << "image.shape=" << image.cols << "," << image.rows << std::endl;
    file << "milliseconds=" << std::setprecision(4) << milliseconds << endl;
    if (valid == 0) {
        file << "markers.size=0" << std::endl;
    } else {
        file << "markers.size=1" << std::endl;
        file << "marker=0" << std::endl;
        file << "landmarks.size=" << charucoIds.size() << std::endl;
        file << streamMem.str();
    }

    if (valid < 10)
        cout << valid;
    else
        cout << "*";
    cout.flush();
//    cout << "detected " << total << " valid " << valid << endl;
}

void detect_markers(const string &input_path, const string &output_path, cv::QRCodeDetector *scanner = nullptr) {
//    cout << "Input Path:  " << input_path << endl;
//    cout << "Output Path: " << output_path << endl;

    bf::create_directory(bf::path(output_path));

    bool first = scanner == nullptr;

    if (first) {
        scanner = new cv::QRCodeDetector();
    }

    bf::path p(input_path);

    bf::directory_iterator end_itr;

    // cycle through the directory
    for (bf::directory_iterator itr(p); itr != end_itr; ++itr) {
        if (bf::is_regular_file(itr->path())) {
            string current_file = itr->path().string();

            if (!(ends_with(current_file, "jpg") || ends_with(current_file, "png"))) {
                continue;
            }
            bf::path output(output_path);
            output = output / ("found_" + itr->path().filename().string());
            output = bf::change_extension(output, "txt");

            run_opencv(itr->path(), output);
//            cout << current_file << endl;
        } else {
            bf::path output(output_path);
            output = output / itr->path().filename();
            detect_markers(itr->path().string(), output.string(), scanner);
        }
    }
    cout << endl;


    if (first) {
        delete scanner;
    }
}

int main(int argc, char *argv[]) {
    try {
        po::options_description desc("Allowed options");
        desc.add_options()
                ("help", "produce help message")
                ("Input,I", po::value<std::string>(), "input path")
                ("Output,O", po::value<std::string>(), "output path");

        po::variables_map vm;
        po::store(po::parse_command_line(argc, argv, desc), vm);
        po::notify(vm);

        if (vm.count("help")) {
            cout << desc << "\n";
            return 0;
        }

        if (!vm.count("Input")) {
            cout << desc << "\n";
            cout << "Input path was not set.\n";
            return 0;
        }
        if (!vm.count("Output")) {
            cout << desc << "\n";
            cout << "Output path was not set.\n";
            return 0;
        }
        detect_markers(vm["Input"].as<string>(), vm["Output"].as<string>());
    } catch (exception &e) {
        cerr << "error: " << e.what() << "\n";
        return 1;
    } catch (...) {
        cerr << "Exception of unknown type!\n";
    }

    printf("done!\n");
    return 0;
}