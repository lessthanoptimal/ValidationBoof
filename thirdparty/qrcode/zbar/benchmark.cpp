#include <cstddef>
#include <zbar.h>
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
using namespace zbar;
namespace po = boost::program_options;
namespace bf = boost::filesystem;

std::string filter_string( const std::string& message ) {
    std::string c = message;
    for( std::string::iterator iter = c.begin() ; iter != c.end() ; ) {
        if( !std::isprint(*iter) )
            iter = c.erase(iter);
        else
            ++iter ; // not erased, increment iterator
    }
    return c;
}

void run_zbar( const bf::path& image_path , const bf::path& output_path, ImageScanner *scanner ) {
    Mat image = imread(image_path.c_str(), CV_LOAD_IMAGE_GRAYSCALE | CV_LOAD_IMAGE_IGNORE_ORIENTATION);

//    cout << "output_pat "<<output_path<<endl;

//    cout << "loaded image. w="<<image.rows<<" x "<<image.cols<<" channels "<<image.channels()<<endl;

    if( image.channels() != 1 ) {
        cout << "Color image???" << endl;
        exit(1);
    }

    ofstream file;
    file.open(output_path.c_str());
    file << "# ZBar "<<image_path.filename() << endl;

    int width=image.cols,height=image.rows;
    Image zimage(width, height, "Y800", image.data, width * height);
    int n = scanner->scan(zimage);
    int valid = 0;

    // extract results
    for(Image::SymbolIterator symbol = zimage.symbol_begin();
        symbol != zimage.symbol_end();
        ++symbol) {
//        cout << symbol->get_type_name() << endl;
        // it can detect things other than a QR Code
        if( !boost::algorithm::ends_with(symbol->get_type_name(), "QR-Code"))
            continue;

        file << "message = " << filter_string(symbol->get_data()) << endl;
        file << symbol->get_location_x(0) << " " << symbol->get_location_y(0);
        for( int j = 1; j < symbol->get_location_size(); j ++ ) {
            file << " " << symbol->get_location_x(j) << " " << symbol->get_location_y(j);
        }
        file << endl;
        valid++;
    }

    zimage.set_data(NULL, 0);
    file.close();

    if( valid < 10)
        cout << valid;
    else
        cout << "*";
    cout.flush();
//    cout << "detected " << total << " valid " << valid << endl;
}

void detect_markers( const string& input_path , const string& output_path , ImageScanner *scanner=nullptr) {
//    cout << "Input Path:  " << input_path << endl;
//    cout << "Output Path: " << output_path << endl;

    bf::create_directory(bf::path(output_path));

    bool first = scanner == nullptr;

    if( first ) {
        scanner = new ImageScanner();
        //scanner->set_config(ZBAR_NONE, ZBAR_CFG_ENABLE, 1);
        scanner->set_config(ZBAR_NONE, ZBAR_CFG_ENABLE, 0);
        scanner->set_config(ZBAR_QRCODE,ZBAR_CFG_ENABLE,1);
    }

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

            run_zbar(itr->path(),output,scanner);
//            cout << current_file << endl;
        } else {
            bf::path output(output_path);
            output = output / itr->path().filename();
            detect_markers(itr->path().string(),output.string(),scanner);
        }
    }
    cout << endl;


    if( first ) {
        delete scanner;
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