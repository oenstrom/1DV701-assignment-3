# 1DV701-assignment-3
Assignment 3 for the course 1DV701

## Compilation:
Make sure you are in the directory with the `App.java`, `packet` directory and `server` directory.
Then run the following command to quickly compile everything: 
 
`find . -name "*.java" > sources.txt && javac @sources.txt`

Just compiling the `App.java` file should work too:  
`javac App.java`


## Run:
Run the TFTP server with the following command:  
`java App`

## Tests:
Go into the directory tests/ and run the command:  
`python -m pytest`  
This will run the tests for the TFTP server.
Make sure to remove all `.ul` files created by the test file before every run of the test.

### Work distribution
Christoffer Eid - ce223af : 50%  
Olof Enström - oe222fh : 50%