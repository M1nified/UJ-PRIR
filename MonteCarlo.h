/*
 * MonteCarlo.h
 *
 *  Created on: 13 cze 2018
 *      Author: oramus
 */

#ifndef MONTECARLO_H_
#define MONTECARLO_H_

#include"Particles.h"
#include "PotentialEnergy.h"
#include "MyMPI.h"

class MonteCarlo {
private:
	double dx, dy, dr;
	double kBTinv;
	double MAX_RANDOM;
	Particles *particles;
	PotentialEnergy *energy;
	MyMPI *myMPI;
	double totalEp;
	void calcInitialDr();
	double calcContribution( int idx, double xx, double yy );
	double calcContributionScatt( int idx, double xx, double yy );
	double deltaEp( int idx, double oldX, double oldY, double newX, double newY );
	double rnd() {
		return random() * MAX_RANDOM;
	}

	int mpi_rank, mpi_size, ppp;
	double * x, *y;
	int * ids;

public:
	MonteCarlo();
	virtual ~MonteCarlo();
	void setParticles( Particles *particles );
	void setPotential( PotentialEnergy *energy );
	void calcMC( int draws );
	double calcAvrMinDistance();
	double calcTotalPotentialEnergy();

	double getTotalPotentialEnergy() {
		return totalEp;
	}
	void setMyMPI( MyMPI *myMPI ) {
		this->myMPI = myMPI;
		this->myMPI->MPI_Comm_rank(MPI_COMM_WORLD, &(this->mpi_rank));
		this->myMPI->MPI_Comm_size(MPI_COMM_WORLD, &(this->mpi_size));
	}
	void setKBTinv( double kBTinv ) {
		this->kBTinv = -kBTinv;
	}

	void shareParticles();
	void gatherParticles();
};

#endif /* MONTECARLO_H_ */
