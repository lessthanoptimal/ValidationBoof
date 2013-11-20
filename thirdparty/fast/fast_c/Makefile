SRC = detect.cpp fast.c fast_9.c fast_10.c fast_11.c fast_12.c nonmax.c
DETECT = detect


CC = g++

CPPFLAGS = -O3 -Wall -Wno-unused-function `pkg-config --cflags opencv`

LDFLAGS = `pkg-config --libs opencv`

OBJ = $(SRC:.c=.o) $(SRC:.cpp=.o) 

all: $(DETECT)

$(DETECT): fast_9.o fast_10.o fast_11.o fast_12.o nonmax.o fast.o detect.o
	$(CC) $(LDFLAGS) -o $(DETECT) detect.o fast.o fast_9.o fast_10.o fast_11.o fast_12.o nonmax.o $(CPPFLAGS)

clean: 
	rm -f *~ *.o $(DETECT)
