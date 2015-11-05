CC=gcc
CFLAGS=-Wall -g

all: generator validator
	@echo "Ready for use."
	
generator: CSE353_Fall2015_Project2_FileGenerator.c
	${CC} ${CFLAGS} CSE353_Fall2015_Project2_FileGenerator.c -o generator
	
validator: CSE353_Fall2015_Project2_FileValidator.c
	${CC} ${CFLAGS} CSE353_Fall2015_Project2_FileValidator.c -o validator
	
clean:
	rm -f validator generator node*.txt
