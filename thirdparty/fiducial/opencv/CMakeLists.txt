cmake_minimum_required(VERSION 3.2.2)
project(opencv_aruco)


find_package(Boost REQUIRED COMPONENTS program_options filesystem)
set(OpenCV_DIR /home/pja/projects/thirdparty/opencv-4.5.3/build/)
find_package(OpenCV 4 REQUIRED)

set(THREADS_PREFER_PTHREAD_FLAG ON)
find_package(Threads REQUIRED)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_FLAGS_DEBUG "-g")
set(CMAKE_CXX_FLAGS_RELEASE "-O3")

# Options. Turn on with 'cmake -Dmyvarname=ON'.
option(test "Build all tests." ON) # Makes boolean 'test' available.
enable_testing()

include_directories(${Boost_INCLUDE_DIRS})
include_directories(${OpenCV_INCLUDE_DIRS})

add_executable(opencv_benchmark benchmark.cpp)
target_link_libraries(opencv_benchmark ${Boost_LIBRARIES} ${OpenCV_LIBS} Threads::Threads)