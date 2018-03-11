#include <quirc.h>
#include <string>
#include <iostream>
#include <iterator>
#include <boost/program_options.hpp>
#include <boost/filesystem.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>

using namespace boost::algorithm;
using namespace std;
using namespace cv;
namespace po = boost::program_options;
namespace bf = boost::filesystem;

void detect_markers( const bf::path& image_path , struct quirc *detector=nullptr ) {
    Mat image = imread(image_path.c_str(), CV_LOAD_IMAGE_GRAYSCALE);

    cout << "loaded image. w="<<image.rows<<" x "<<image.cols<<" channels "<<image.channels()<<endl;

//    byte[] return_buff = new byte[(int) (image.total() *  image.channels())];
//    result_mat.get(0, 0, return_buff);
}

void detect_markers( const string& input_path , const string& output_path , struct quirc *detector=nullptr) {
    cout << "Input Path:  " << input_path << endl;
    cout << "Output Path: " << output_path << endl;

    bool first = detector == nullptr;

    if( first )
        detector = quirc_new();

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
            detect_markers(itr->path(),detector);
//            cout << current_file << endl;
        } else {
            bf::path output(output_path);
            output = output / itr->path().filename();
            detect_markers(itr->path().string(),output.string(),detector);
        }
    }


    if( first ) {
        quirc_destroy(detector);
    }

    // load image

    // set up quirc
//    quirc_resize(detector,width,height);
//    uint8_t *quirc_begin(struct quirc *q, int *w, int *h);
//    void quirc_end(struct quirc *q);

    // run detector

    // save results

    // clean up
}

int main( int argc, char *argv[] ) {
    try {

        po::options_description desc("Allowed options");
        desc.add_options()
            ("help", "produce help message")
            ("Input,I", po::value<std::string >(), "input path")
            ("Output,O", po::value<std::string >(), "output path");

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
        detect_markers(vm["Input"].as<string>(),vm["Output"].as<string>());
    } catch(exception& e) {
        cerr << "error: " << e.what() << "\n";
        return 1;
    } catch(...) {
        cerr << "Exception of unknown type!\n";
    }

    printf("done!\n");
    return 0;
}