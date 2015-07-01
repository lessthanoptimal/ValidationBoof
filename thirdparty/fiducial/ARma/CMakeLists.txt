cmake_minimum_required(VERSION 2.8)
project(ARma)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")

FIND_PACKAGE(OpenCV 2.4 REQUIRED core imgproc highgui calib3d )
INCLUDE_DIRECTORIES(${OpenCV_INCLUDE_DIRS})

# See if Boost is around
FIND_PACKAGE( Boost 1.54 COMPONENTS filesystem REQUIRED )
INCLUDE_DIRECTORIES( ${Boost_INCLUDE_DIR} )

SET(ARMA_FILES pattern.h patterndetector.cpp patterndetector.h pattern.cpp)

add_library(ARma STATIC ${ARMA_FILES} )
add_library(ARmaShared SHARED ${ARMA_FILES})

add_executable(DetectFiducials DetectFiducials.cpp)
target_link_libraries(DetectFiducials ARma ${OpenCV_LIBS}  pthread -lboost_system ${Boost_LIBRARIES})

#------------- Installation
install(TARGETS ARmaShared DESTINATION /usr/local/lib/romotive)