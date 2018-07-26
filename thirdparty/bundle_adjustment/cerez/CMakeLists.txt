cmake_minimum_required(VERSION 3.2.2)
project(ceres_benchmark)


find_package(Boost REQUIRED COMPONENTS program_options filesystem)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_FLAGS_DEBUG "-g")
set(CMAKE_CXX_FLAGS_RELEASE "-O3")


#set (CMAKE_BUILD_TYPE Release)
set (CMAKE_BUILD_TYPE Debug)

set (CERES_DIR ${CMAKE_CURRENT_SOURCE_DIR}/ceres-solver/)
set (Ceres_INCLUDE_DIRS ${CERES_DIR}/include)
set (Ceres_LIBRARIES ${CERES_DIR}/build/lib/libceres.a)

include_directories(${Boost_INCLUDE_DIRS})
include_directories(${Ceres_INCLUDE_DIRS})

add_executable(ceres_benchmark benchmark.cpp)
target_link_libraries(ceres_benchmark ${Ceres_LIBRARIES} ${Boost_LIBRARIES})