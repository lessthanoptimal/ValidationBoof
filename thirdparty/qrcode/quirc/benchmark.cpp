#include <quirc.h>
#include <string>
#include <iostream>
#include <iterator>
#include <boost/program_options.hpp>
#include <boost/filesystem.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <fstream>

using namespace boost::algorithm;
using namespace std;
using namespace cv;
namespace po = boost::program_options;
namespace bf = boost::filesystem;

void run_quirc( const bf::path& image_path , const bf::path& output_path, struct quirc *detector ) {
    Mat image = imread(image_path.c_str(), CV_LOAD_IMAGE_GRAYSCALE);

//    cout << "output_pat "<<output_path<<endl;

//    cout << "loaded image. w="<<image.rows<<" x "<<image.cols<<" channels "<<image.channels()<<endl;

    if( image.channels() != 1 ) {
        cout << "Color image???" << endl;
        exit(1);
    }

    int width=image.cols,height=image.rows;
    if( quirc_resize(detector,width,height) == -1 ) {
        cout << "Failed to resize image for quirc" << endl;
        exit(1);
    }

    uint8_t *raw_data = quirc_begin(detector, &width, &height);
    std::memcpy(raw_data,image.data,width*height*sizeof(uint8_t));
    quirc_end(detector);

    ofstream file;
    file.open(output_path.c_str());
    file << "# Quirc " << quirc_version() << " "<<image_path.filename() << endl;

    int total = quirc_count(detector);
    int valid = 0;

    for( int i = 0; i < total; i++ ) {
        struct quirc_code code;
        struct quirc_data data;
        quirc_extract(detector, i,&code);
        if( quirc_decode(&code,&data) == QUIRC_SUCCESS )  {
            file << "message = " << data.payload << endl;
            file << code.corners[0].x << " " << code.corners[1].y;
            for( int j = 1; j < 4; j ++ ) {
                file << " " << code.corners[j].x << " " << code.corners[j].y;
            }
            file << endl;
            valid++;
        }
    }

    file.close();

    cout << valid;
    cout.flush();
//    cout << "detected " << total << " valid " << valid << endl;

}

void detect_markers( const string& input_path , const string& output_path , struct quirc *detector=nullptr) {
//    cout << "Input Path:  " << input_path << endl;
//    cout << "Output Path: " << output_path << endl;

    bf::create_directory(bf::path(output_path));

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
            bf::path output(output_path);
            output = output / itr->path().filename();
            output = bf::change_extension(output, "txt");

            run_quirc(itr->path(),output,detector);
//            cout << current_file << endl;
        } else {
            bf::path output(output_path);
            output = output / itr->path().filename();
            detect_markers(itr->path().string(),output.string(),detector);
        }
    }
    cout << endl;


    if( first ) {
        quirc_destroy(detector);
    }
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