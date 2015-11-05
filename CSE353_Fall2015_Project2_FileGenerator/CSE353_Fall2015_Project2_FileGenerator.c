#include <stdio.h>
#include <stdlib.h>
#include <time.h>

void printHelp(char *);

int main(int argc, char ** argv)
{
	int numNodes, numLines, src, lineNum, col, length, dest;
	char fname[15];

	// Check arguments
	if (argc != 3)
	{
		printHelp(argv[0]);
		exit(1);
	}
	srand(time(NULL));
	numNodes = atoi(argv[1]);
	numLines = atoi(argv[2]);

	if ((numNodes < 2) || (numNodes > 255))
	{
		printf("Number of nodes is out of range (given %d, should be 2-255)\n", numNodes);
		printHelp(argv[0]);
		exit(1);
	}

	printf("Generating data for %d nodes, %d lines each.\n", numNodes, numLines);
	// Generate files
	for (src = 1; src <= numNodes; src++)
	{
		// Make the file name
		sprintf(fname, "node%d.txt", src);

		// Open the file
		FILE * f = fopen(fname, "w");
		if (f == NULL)
		{
			perror("Cannot open output file");
			exit(2);
		}

		// Generate lines of data
		for (lineNum = 0; lineNum < numLines; lineNum++)
		{
			// Generate Random length between 0 and 100
			length = (rand() % 99) + 1;

			// Generate destination address that isn't source address
			do
				dest = (rand() % numNodes) + 1;
			while (dest == src);

			// Print dest data
			fprintf(f, "%d:", dest);

			// Generate random data
			for (col = 0; col < length; col++)
			{
				// Generates a character that isn't a control character
				fputc((char) (rand() % 93) + 32, f);
			}
			fputc('\n', f);
		}
		fclose(f);
		f = NULL;

	}
	printf("Done.\n");
	return 0;
}

/**
 * Prints the usage of the program.
 * @param progName name of the program
 */
void printHelp(char * progName)
{
	printf("CSE353 Project 2 File Generator\n");
	printf("Usage: %s [Number of Nodes] [Number of Lines]\n", progName);
	printf("\tNumber of Nodes: Number of nodes to generate data for\n");
	printf("\tNumber of Lines: Number of lines to generate for each file.\n");
}
