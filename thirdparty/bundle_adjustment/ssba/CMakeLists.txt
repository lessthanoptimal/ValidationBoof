cmake_minimum_required(VERSION 3.2.2)
project(ssba_benchmark)


find_package(Boost REQUIRED COMPONENTS program_options filesystem)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_FLAGS_DEBUG "-g")
set(CMAKE_CXX_FLAGS_RELEASE "-O3")

include (v3d_macros.cmake)

set(SSBA_PATH ${CMAKE_CURRENT_SOURCE_DIR}/SSBA/SSBA-4.0)

enable_feature (V3DLIB_ENABLE_SUITESPARSE)
enable_feature_inc_path (V3DLIB_ENABLE_SUITESPARSE /usr/include/suitesparse)
enable_feature_libraries (V3DLIB_ENABLE_SUITESPARSE colamd)

#set (CMAKE_BUILD_TYPE Release)
set (CMAKE_BUILD_TYPE Debug)

include_directories(/usr/include/suitesparse ${SSBA_PATH} ${CMAKE_CURRENT_SOURCE_DIR})

add_executable(ssba_benchmark bundle_large.cpp)
target_link_libraries(ssba_benchmark ${SSBA_PATH}/build/libV3D.a ${EXTRA_LIBRARIES} ${Boost_LIBRARIES})