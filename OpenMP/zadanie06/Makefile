
PC = 2
PROC_COUNT = $(PC)

.DEFAULT_GOAL := default

run:
	env OMP_NUM_THREADS=$(PROC_COUNT) > /dev/null
	./a.out

default:
	c++ -O3 -fopenmp LennardJonesPotential.cpp main.cpp MonteCarlo.cpp Particles.cpp PotentialEnergy.cpp
