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
#include <opencv2/imgcodecs.hpp>
#include "aruco.h"

using namespace boost::algorithm;
using namespace std;
using namespace cv;

namespace po = boost::program_options;
namespace bf = boost::filesystem;

void run_aruco_grid(const bf::path &image_path, const bf::path &output_path) {
    Mat image = imread(image_path.c_str(), IMREAD_GRAYSCALE | IMREAD_IGNORE_ORIENTATION);
    if (image.channels() != 1) {
        cout << "Color image???" << endl;
        exit(1);
    }

    aruco::MarkerDetector detector;
    detector.setDictionary("ARUCO");
    detector.setDetectionMode(aruco::DetectionMode::DM_NORMAL);

    int rows = 7;
    int cols = 5;

    int cornerCols = cols * 2;

    std::vector<aruco::Marker> markers;

    // output to a stream instead of the file initially just in case it does processing at this stage
    std::ostringstream streamMem;

    auto time0 = chrono::steady_clock::now();
    try {
        markers = detector.detect(image);
    } catch (...) {
        cout << "Exception!!!" << endl;
    }
    auto time1 = chrono::steady_clock::now();

    int valid = 0;
    if (!markers.empty()) {
        valid++;
        for (auto marker: markers) {

            int row = marker.id / cols;
            int col = marker.id % cols;

            int cornerRow = row * 2;
            int cornerCol = col * 2;

            const auto &c0 = marker[0];
            const auto &c1 = marker[1];
            const auto &c2 = marker[2];
            const auto &c3 = marker[3];

            int idx0 = cornerRow * cornerCols + cornerCol;
            int idx1 = cornerRow * cornerCols + cornerCol + 1;
            int idx2 = (cornerRow + 1) * cornerCols + cornerCol + 1;
            int idx3 = (cornerRow + 1) * cornerCols + cornerCol;

            streamMem << idx0 << " " << c0.x << " " << c0.y << std::endl;
            streamMem << idx1 << " " << c1.x << " " << c1.y << std::endl;
            streamMem << idx2 << " " << c2.x << " " << c2.y << std::endl;
            streamMem << idx3 << " " << c3.x << " " << c3.y << std::endl;
        }
    }

    double milliseconds = 1e-6 * chrono::duration_cast<chrono::nanoseconds>(time1 - time0).count();

    ofstream file;
    file.open(output_path.c_str());
    file << "# ArUco 3: " << image_path.filename() << std::endl;
    file << "image.shape=" << image.cols << "," << image.rows << std::endl;
    file << "milliseconds=" << std::setprecision(4) << milliseconds << endl;
    if (valid == 0) {
        file << "markers.size=0" << std::endl;
    } else {
        file << "markers.size=1" << std::endl;
        file << "marker=0" << std::endl;
        file << "landmarks.size=" << markers.size()*4 << std::endl;
        file << streamMem.str();
    }

    if (valid < 10)
        cout << valid;
    else
        cout << "*";
    cout.flush();
}

void detect_markers(const string &input_path, const string &output_path) {
//    cout << "Input Path:  " << input_path << endl;
//    cout << "Output Path: " << output_path << endl;

    bf::create_directory(bf::path(output_path));

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

            run_aruco_grid(itr->path(), output);
        } else {
            bf::path output(output_path);
            output = output / itr->path().filename();
            detect_markers(itr->path().string(), output.string());
        }
    }
    cout << endl;
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