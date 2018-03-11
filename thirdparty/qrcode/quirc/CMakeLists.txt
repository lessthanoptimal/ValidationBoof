cmake_minimum_required(VERSION 3.2.2)
project(quirc_benchmark)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_FLAGS_DEBUG "-g")
set(CMAKE_CXX_FLAGS_RELEASE "-O3")

# Options. Turn on with 'cmake -Dmyvarname=ON'.
option(test "Build all tests." ON) # Makes boolean 'test' available.
enable_testing()


include_directories(${CMAKE_SOURCE_DIR}/quirc/lib/)

file(GLOB QUIRC_HDR "quirc/lib/*.h")
file(GLOB QUIRC_SRC "quirc/lib/*.c")


add_executable(quirc_benchmark benchmark.cpp ${QUIRC_HDR} ${QUIRC_SRC})