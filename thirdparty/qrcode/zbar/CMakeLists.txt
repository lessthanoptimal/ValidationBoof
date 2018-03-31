cmake_minimum_required(VERSION 3.2.2)
project(zbar_benchmark)


find_package(Boost REQUIRED COMPONENTS program_options filesystem)
find_package(OpenCV REQUIRED)

set(THREADS_PREFER_PTHREAD_FLAG ON)
find_package(Threads REQUIRED)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_FLAGS_DEBUG "-g")
set(CMAKE_CXX_FLAGS_RELEASE "-O3")

# Options. Turn on with 'cmake -Dmyvarname=ON'.
option(test "Build all tests." ON) # Makes boolean 'test' available.
enable_testing()

set(ZBAR_PATH "${CMAKE_SOURCE_DIR}/zbar-0.10")

link_directories(${ZBAR_PATH}/zbar/.libs)
include_directories(${Boost_INCLUDE_DIRS})
include_directories(${OpenCV_INCLUDE_DIRS})
include_directories(${ZBAR_PATH}/include/)

add_executable(zbar_benchmark benchmark.cpp ${ZBAR_HEADERS})
target_link_libraries(zbar_benchmark -lzbar ${Boost_LIBRARIES} ${OpenCV_LIBS} Threads::Threads)