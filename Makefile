
PROC_COUNT = 2
MACHINES_FILE = machines

.DEFAULT_GOAL := default

run:
	mpirun -np $(PROC_COUNT) ./exec

run_tcp:
	mpirun --mca btl tcp,self -hostfile $(MACHINES_FILE) -n $(PROC_COUNT) ./exec

default:
	mpiCC -o exec -O3 MyMPI.cpp mpiusage.cpp LennardJonesPotential.cpp MonteCarlo.cpp Particles.cpp main.cpp PotentialEnergy.cpp

