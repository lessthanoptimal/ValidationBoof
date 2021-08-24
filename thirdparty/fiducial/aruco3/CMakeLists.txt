cmake_minimum_required(VERSION 3.2.2)
project(aruco3)


find_package(Boost REQUIRED COMPONENTS program_options filesystem)
#set(OpenCV_DIR /home/pja/projects/thirdparty/opencv-3.4.13/build/)
#find_package(OpenCV 3 REQUIRED)
set(OpenCV_DIR /home/pja/projects/thirdparty/opencv-4.5.3/build/)
find_package(OpenCV 4 REQUIRED)

set(aruco_DIR /home/pja/projects/thirdparty/aruco-3.1.12/build)
find_package(aruco REQUIRED)

set(THREADS_PREFER_PTHREAD_FLAG ON)
find_package(Threads REQUIRED)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_FLAGS_DEBUG "-g")
set(CMAKE_CXX_FLAGS_RELEASE "-O3")

# Options. Turn on with 'cmake -Dmyvarname=ON'.
option(test "Build all tests." ON) # Makes boolean 'test' available.
enable_testing()

message("Aruco include ${aruco_INCLUDE_DIRS}")
message("Aruco libs ${aruco_LIBRARIES}")
message("Aruco libs ${aruco_LIBS}")
message("Aruco libs ${aruco_Libs}")

include_directories(${Boost_INCLUDE_DIRS})
include_directories(${OpenCV_INCLUDE_DIRS})
include_directories(${aruco_INCLUDE_DIRS})

add_executable(aruco3_benchmark benchmark.cpp)
target_link_libraries(aruco3_benchmark ${Boost_LIBRARIES} ${OpenCV_LIBS} ${aruco_LIBS} Threads::Threads)
