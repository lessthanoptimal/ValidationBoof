cmake_minimum_required(VERSION 2.8)
project( FindContours )

set (CMAKE_CXX_STANDARD 11)

find_package(Boost REQUIRED COMPONENTS program_options filesystem)
find_package(OpenCV 3.3.1 REQUIRED)

include_directories( ${OpenCV_INCLUDE_DIRS} )
include_directories( ${Boost_INCLUDE_DIRS} )

add_executable( FindContours FindContours.cpp )
target_link_libraries( FindContours ${Boost_LIBRARIES} ${OpenCV_LIBS} )