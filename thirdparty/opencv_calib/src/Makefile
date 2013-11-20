SRC = load_files.cpp
CALIB = calib_pts detect_pts

CC = g++

CPPFLAGS = -g -Wall -Wno-unused-function `pkg-config --cflags opencv` -Isrc/

LDFLAGS = `pkg-config --libs opencv`

OBJS = $(SRC:.cpp=.o)

all: $(SRC) $(CALIB)

%.o: %.c 
	$(CC) -c -o $@ $< $(CFLAGS)

calib_pts: load_files.o calib_from_pts.o
	$(CC) -o calib_pts calib_from_pts.o $(OBJS) $(LDFLAGS) $(CPPFLAGS)

detect_pts: load_files.o detect_calib_points.o
	$(CC) -o detect_pts detect_calib_points.o $(OBJS) $(LDFLAGS) $(CPPFLAGS)

clean:
	rm -f *~ *.o $(CALIB)
