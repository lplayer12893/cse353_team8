# cse353_team8

Compilation: $ ant
Jar Generation: $ ant build-jar

Execution: $ java -jar cse353_team8_p2.jar [number of nodes]


Checklist:
Compiles and builds without warnings or errors - Usually, see Bug Report
Switch class - Complete
Switch has frame buffer, and reads/writes - Complete
Switch allows multiple connections - Complete
Switch floods frame when it doesn't know the destination - Complete
Switch learns destinations - Complete
Node class - Complete
Node instantiates and connects to switch - Complete + Dynamic Reassignment
Nodes open their input files and send data - Complete
Nodes open their output and save data that they receive - Complete
	NOTE: output file variation - we chose to include the acknowledgements and
	termination frames in our output files. They are the lines with empty data
	fields. The termination was sent from the switch (id = 0).

NOTE: Running the program with the maximum number of nodes is a good way to have
	nothing to do for the next 10 minutes. The program may seem to stall, but it
	always continues except in the bugs below.

NOTE: Port 65535 must be available for the program to run. Failure will result in
	the program terminating on a caught exception

Bug Report:
	in testing, we found a few bugs that would occasionally occur. Many of these
	happen as a race condition.

	Sometimes, the program will not terminate. There is a single node that never
	terminates, thereby causing the entire network to remain open. 

	Sometimes, IndexOutOfBounds exceptions occur, despite checks that the index is
	valid immediately before the Array accesses

Extra Credit:
Frame Priority - Complete
	we used a second buffer for priority in order to account for the possibility
	of concurrent modification exceptions to our buffer during the search for a
	priority frame in a single buffer. In short, this implementation is faster
	(O(1) vs. O(n)) at the cost of storage space (2 buffers vs. 1). Each frame
	has a 50% chance of being prioritized on read from the input file.

Standard Output:
	we left marginal standard output only detailing starting and stopping node/switch
	functionality
