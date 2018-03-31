cmake_minimum_required(VERSION 3.2.2)
project(quirc_benchmark)


find_package(Boost REQUIRED COMPONENTS program_options filesystem)
find_package(OpenCV 3.3.1 REQUIRED)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_FLAGS_DEBUG "-g")
set(CMAKE_CXX_FLAGS_RELEASE "-O3")

# Options. Turn on with 'cmake -Dmyvarname=ON'.
option(test "Build all tests." ON) # Makes boolean 'test' available.
enable_testing()


include_directories(${CMAKE_SOURCE_DIR}/quirc/lib/)
include_directories(${Boost_INCLUDE_DIRS})
include_directories(${OpenCV_INCLUDE_DIRS})

file(GLOB QUIRC_HDR "quirc/lib/*.h")
file(GLOB QUIRC_SRC "quirc/lib/*.c")

add_executable(quirc_benchmark benchmark.cpp ${QUIRC_HDR} ${QUIRC_SRC})
target_link_libraries(quirc_benchmark ${Boost_LIBRARIES} ${OpenCV_LIBS})