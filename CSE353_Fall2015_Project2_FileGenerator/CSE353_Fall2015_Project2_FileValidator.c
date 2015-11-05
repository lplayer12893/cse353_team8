#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <string.h>

void printHelp(char *);
char * getLine(FILE *);

typedef struct _data
{
	unsigned int lineNum;
	unsigned short src;
	unsigned short dst;
	char seen;
	char * data;
	struct _data * next;
} data;

int main(int argc, char ** argv)
{
	int numNodes, src, dst;
	char fname[25];
	data * list = NULL;

	// Check arguments
	if (argc != 2)
	{
		printHelp(argv[0]);
		exit(1);
	}
	numNodes = atoi(argv[1]);

	if ((numNodes < 2) || (numNodes > 255))
	{
		printf("Number of nodes is out of range (given %d, should be 2-255)\n", numNodes);
		printHelp(argv[0]);
		exit(1);
	}

	printf("Validating data for %d nodes.\n", numNodes);

	// Read input files
	printf("Reading input files. ");
	for (src = 1; src <= numNodes; src++)
	{
		// Generate file name
		sprintf(fname, "node%d.txt", src);

		// Open file
		FILE * f = fopen(fname, "r");
		if (f == NULL)
		{
			perror("Cannot open input file");
			exit(2);
		}
		char * line = NULL;
		int lineNum = 1;
		while (1)
		{
			// Read line from file
			line = getLine(f);
			// Keep pointer for later freeing
			char * read = line;
			if (line == NULL)
				break;

			// Make a new structure for it
			data * entry = malloc(sizeof(data));
			if (entry == NULL)
			{
				perror("Cannot allocate space for entry");
				exit(2);
			}
			// Make space for the data.
			char * data = malloc(sizeof(char) * 100);

			// Parse file
			line = strtok(line, ":");
			dst = atoi(line);
			line = strtok(NULL, "\n");
			strcpy(data, line);

			// Done with what we read, free it.
			free(read);
			read = NULL;

			// Check destination field
			if ((dst <= 0) || (dst > numNodes))
			{
				printf("node%d.txt, line %d: Destination is out of range! Given %d, should be between 1 and %d.\n", src, lineNum, dst, numNodes);
			}

			// Fill out the struct fields
			entry->lineNum = lineNum;
			entry->dst = (unsigned short) dst;
			entry->src = (unsigned short) src;
			entry->data = data;
			entry->seen = 0x0;

			entry->next = list;
			list = entry;

			lineNum++;
		}
		fclose(f);
		f = NULL;
	}
	printf("Done.\n");

	// Read and Validate Output files.
	printf("Reading and validating output files. \n");
	int numErrors = 0;
	for (dst = 1; dst <= numNodes; dst++)
	{
		// Construct file name
		sprintf(fname, "node%doutput.txt", dst);

		// Open file
		FILE * f = fopen(fname, "r");
		if (f == NULL)
		{
			fprintf(stderr, "Cannot open generated output file node%doutput.txt: ", dst);
			perror(NULL);
			continue;
		}
		char * line = NULL;
		int lineNum = 1;
		while (1)
		{
			// Read line from file
			line = getLine(f);
			char * read = line;

			// Check for EOF
			if (line == NULL)
			{
				// EOF
				break;
			}

			// Read file line
			char * dataSent = malloc(sizeof(char) * 100);
			// Parse
			line = strtok(line, ":");
			src = atoi(line);
			line = strtok(NULL, "\n");
			strcpy(dataSent, line);

			// Done with read line, free it.
			free(read);
			read = NULL;

			// Check what we read
			if ((src <= 0) || (src > numNodes))
			{
				printf("node%doutput.txt, line %d: Source is out of range! Given %d, should be between 1 and %d.\n", dst, lineNum, src, numNodes);
				numErrors++;
			}

			// Search our structures looking for what we found.
			data *s = list;
			char found = 0;
			while (s != NULL)
			{
				if (strcmp(s->data, dataSent) == 0)
				{
					// Found it
					found = 1;

					// Check to make sure we haven't seen it already
					if (s->seen == 1)
					{
						printf("node%doutput.txt, line %d: Duplicated Traffic!\n",dst,lineNum);
						numErrors++;
					}
					// Check the source field
					if (src != s->src)
					{
						printf("node%doutput.txt, line %d: Source address doesn't match (given %d, should be %d)\n",dst,lineNum,src,s->src);
						numErrors++;
					}
					// Mark it as seen
					s->seen = 1;
					break;
				}
				else
				{
					// Not the right traffic, keep looking
					s = s->next;
				}
			}
			if (found == 0)
			{
				// Couldn't find in the list of acceptable traffic
				printf("node%doutput.txt, line %d: Renegade Traffic (traffic not in input files)\n",dst,lineNum);
				printf("\tSRC: %d\n",src);
				printf("\tDST: %d\n",dst);
				printf("\tData (%u bytes): \"%s\"\n",(unsigned int)(strlen(dataSent)),dataSent);
				numErrors++;
			}
			free(dataSent);
			lineNum++;
		}
		fclose(f);
		f = NULL;

	}

	// Check to make sure we saw all traffic
	data * c = list;
	while (c != NULL)
	{
		if (c->seen == 0)
		{
			printf("Didn't see the following traffic:\n");
			printf("\tnode%d.txt, line %d\n",c->src, c->lineNum);
			printf("\tSRC: %d\n",c->src);
			printf("\tDST: %d\n",c->dst);
			printf("\tData (%u bytes): \"%s\"\n",(unsigned int)(strlen(c->data)),c->data);
			numErrors++;
		}
		c = c->next;
	}

	// Free all heap memory
	c = list;
	while (c != NULL)
	{
		data * n = c->next;
		free(c->data);
		free(c);
		c = n;
	}
	printf("Number of Errors: %d\n",numErrors);
	return 0;
}

/**
 * Prints the help menu.
 * @param progName Name of the program
 */
void printHelp(char * progName)
{
	printf("CSE353 Project 2 File Validator\n");
	printf("Usage: %s [Number of Nodes]\n", progName);
	printf("\tNumber of Nodes: Number of nodes to validate data for\n");
}

/**
 * Gets a line of text from a file.
 * @param f file to use
 * @return string up to the newline character, NULL if reached EOF
 */
char * getLine(FILE *f)
{
	char c;
	unsigned int len = 0;
	char * line = malloc(sizeof(char));
	if (line == NULL)
	{
		perror("Cannot allocate space for string");
		exit(1);
	}
	while ((c = fgetc(f)) != '\n')
	{
		if (c == EOF)
		{
			free(line);
			return NULL;
		}
		line = realloc(line, len + 1);
		line[len] = c;
		len++;
	}
	line = realloc(line,len+1);
	line[len] = '\0';
	// printf("getLine (len %d): >%s<\n", len, line);
	return line;
}
