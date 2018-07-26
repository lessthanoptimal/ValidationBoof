#include <cstddef>
#include <string>
#include <iostream>
#include <iterator>
#include <boost/program_options.hpp>
#include <boost/filesystem.hpp>
#include <boost/algorithm/string/predicate.hpp>

using namespace boost::algorithm;
using namespace std;
namespace po = boost::program_options;
namespace bf = boost::filesystem;


void process_in_the_large( const string& input_path , const string& output_path ) {
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
        process_in_the_large(vm["Input"].as<string>(),vm["Output"].as<string>());
    } catch(exception& e) {
        cerr << "error: " << e.what() << "\n";
        return 1;
    } catch(...) {
        cerr << "Exception of unknown type!\n";
    }

    printf("done!\n");
    return 0;
}