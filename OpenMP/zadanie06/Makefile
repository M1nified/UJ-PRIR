
PC = 2
PROC_COUNT = $(PC)

.DEFAULT_GOAL := default

run:
	env OMP_NUM_THREADS=$(PROC_COUNT) ./a.out

default:
	c++ -03 -fopenmp *.cpp

