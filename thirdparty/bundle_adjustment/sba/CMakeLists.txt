cmake_minimum_required(VERSION 3.2.2)
project(sba_benchmark)


find_package(Boost REQUIRED COMPONENTS program_options filesystem)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_FLAGS_DEBUG "-g")
set(CMAKE_CXX_FLAGS_RELEASE "-O3")

set(SBA_PATH "${CMAKE_SOURCE_DIR}/sba-1.6")

include_directories(${SBA_PATH})

add_executable(sba_benchmark benchmark.cpp ${SBA_PATH})
target_link_libraries(sba_benchmark -lsba -llapack -lblas -lf2c  -lm)