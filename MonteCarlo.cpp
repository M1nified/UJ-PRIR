/*
 * MonteCarlo.cpp
 *
 *  Created on: 13 cze 2018
 *      Author: oramus
 */

#include "MonteCarlo.h"
#include "Consts.h"
#include <math.h>
#include <iostream>
#include <stdlib.h>
#include <omp.h>

using namespace std;

MonteCarlo::MonteCarlo() : MAX_RANDOM(1.0 / (1.0 + RAND_MAX))
{
}

MonteCarlo::~MonteCarlo()
{
	// TODO Auto-generated destructor stub
}

void MonteCarlo::calcInitialDr()
{
	double drMin = calcAvrMinDistance();
	dr = DR_INITIAL_RATIO * drMin;
}

double MonteCarlo::calcAvrMinDistance()
{
	double drMinSQ = 100000.0;
	double tmp;
	for (int i = 0; i < particles->getNumberOfParticles(); i++)
	{
		tmp = particles->getDistanceSQToClosest(i);
		if (tmp < drMinSQ)
			drMinSQ = tmp;
	}
	return sqrt(drMinSQ);
}

void MonteCarlo::setParticles(Particles *particles)
{
	this->particles = particles;
	calcInitialDr();
}

void MonteCarlo::setPotential(PotentialEnergy *energy)
{
	this->energy = energy;
}

double MonteCarlo::calcContribution( int idx, double xx, double yy ) {
	double sum = 0;
	for ( int i = 0; i < idx; i++ ) {
		sum += energy->getPotentialEnergyDistanceSQ( particles->getDistanceBetweenSQ( i, xx, yy ));
	}
	for ( int i = idx+1; i < particles->getNumberOfParticles(); i++ ) {
		sum += energy->getPotentialEnergyDistanceSQ( particles->getDistanceBetweenSQ( i, xx, yy ));
	}
	return sum;
}

double MonteCarlo::calcContributionScatt(int idx, double xx, double yy)
{
	double sum = 0;
	if (idx < ids[0] || idx > ids[ppp - 1])
	{
		for (int i = 0; i < ppp; i++)
		{
			double dx = xx - x[i];
			double dy = yy - y[i];
			// sum += energy->getPotentialEnergyDistanceSQ(particles->getDistanceBetweenSQ(i, xx, yy));
			sum += energy->getPotentialEnergyDistanceSQ(dx * dx + dy * dy);
		}
	}
	else
	{
		for (int i = 0; i < ppp; i++)
		{
			if (ids[i] == i)
				continue;
			double dx = xx - x[i];
			double dy = yy - y[i];
			// sum += energy->getPotentialEnergyDistanceSQ(particles->getDistanceBetweenSQ(i, xx, yy));
			sum += energy->getPotentialEnergyDistanceSQ(dx * dx + dy * dy);
		}
	}
	return sum;
}

double MonteCarlo::calcTotalPotentialEnergy()
{
	double tmp = 0;
	for (int i = 0; i < ppp; i++)
		tmp += calcContribution(i, x[i], y[i]);

	totalEp = tmp * 0.5;

	return totalEp;
}

double MonteCarlo::deltaEp(int idx, double oldX, double oldY, double newX, double newY)
{
	return calcContributionScatt(idx, newX, newY) - calcContributionScatt(idx, oldX, oldY);
}

// rozesłanie położeń cząstek z procesu o rank=0 do pozostałych
void MonteCarlo::shareParticles()
{
	cout << "(1) MonteCarlo::shareParticles " << mpi_rank << endl;

	double *send_x, *send_y;
	int *send_ids;

	this->ppp = particles->getNumberOfParticles() / mpi_size;

	send_x = new double[particles->getNumberOfParticles()];
	send_y = new double[particles->getNumberOfParticles()];
	send_ids = new int[particles->getNumberOfParticles()];

	this->x = new double[ppp];
	this->y = new double[ppp];
	this->ids = new int[ppp];

	if (!mpi_rank)
	{
		for (int i = 0; i < particles->getNumberOfParticles(); i++)
		{
			send_x[i] = particles->getX(i);
			send_y[i] = particles->getY(i);
			send_ids[i] = i;
		}
		cout << "(3) MonteCarlo::shareParticles " << mpi_rank << "\t" << send_ids[0] << "\t" << send_x[0] << "\t" << send_y[0] << endl;
		cout << "(4) MonteCarlo::shareParticles " << mpi_rank << "\t" << send_ids[ppp] << "\t" << send_x[ppp] << "\t" << send_y[ppp] << endl;
	}

	this->myMPI->MPI_Scatter(send_x, ppp, MPI_DOUBLE, this->x, ppp, MPI_DOUBLE, 0, MPI_COMM_WORLD);
	this->myMPI->MPI_Scatter(send_y, ppp, MPI_DOUBLE, this->y, ppp, MPI_DOUBLE, 0, MPI_COMM_WORLD);
	this->myMPI->MPI_Scatter(send_ids, ppp, MPI_INT, this->ids, ppp, MPI_INT, 0, MPI_COMM_WORLD);

	cout << "(2) MonteCarlo::shareParticles " << mpi_rank << "\t" << ids[0] << "\t" << x[0] << "\t" << y[0] << endl;
}

// proces o rank=0 po zakończeniu tej metody musi zawierać
// zaktualizowane pozycje cząstek
void MonteCarlo::gatherParticles() {}

void MonteCarlo::calcMC(int draws)
{
	int accepted = 0;
	int idx;
	double xnew, ynew, xold, yold, local_dE, global_dE, prob, rnd_val;

	double msg[5];
	double setxy[3];

	for (int i = 0; i < draws; i++)
	{
		if (!mpi_rank)
		{
			// którą z cząstek będzemy próbowali przestawić
			idx = (int)(particles->getNumberOfParticles() * rnd());
			// stara pozycja dla czastki
			xold = particles->getX(idx);
			yold = particles->getY(idx);
			// nowa pozycja dla czastki
			xnew = xold + dr * (rnd() - 0.5);
			ynew = yold + dr * (rnd() - 0.5);

			msg[0] = (double)idx;
			msg[1] = xold;
			msg[2] = yold;
			msg[3] = xnew;
			msg[4] = ynew;
		}
		this->myMPI->MPI_Barrier(MPI_COMM_WORLD);
		// cout << "(1) MonteCarlo::calcMC " << mpi_rank << "\tbefore bcast" << endl;
		this->myMPI->MPI_Bcast(msg, 5, MPI_DOUBLE, 0, MPI_COMM_WORLD);

		// cout << "(2) MonteCarlo::calcMC " << mpi_rank << "\t" << msg[0] << "\t" << msg[1] << "\t" << msg[2] << "\t" << msg[3] << "\t" << msg[4] << endl;

		idx = (int)msg[0];
		xold = msg[1];
		yold = msg[2];
		xnew = msg[3];
		ynew = msg[4];

		// wyliczamy zmianę energii potencjalnej gdy cząstka idx
		// przestawiana jest z pozycji old na new
		local_dE = deltaEp(idx, xold, yold, xnew, ynew);

		MPI_Reduce(&local_dE, &global_dE, 1, MPI_DOUBLE, MPI_SUM, 0, MPI_COMM_WORLD);

		// pradopodobieństwo zależy od temperatury
		prob = exp(global_dE * kBTinv);
		// czy zaakceptowano zmianę położenia ?
		rnd_val = rnd();

		MPI_Bcast(&prob, 1, MPI_DOUBLE, 0, MPI_COMM_WORLD);
		MPI_Bcast(&rnd_val, 1, MPI_DOUBLE, 0, MPI_COMM_WORLD);

		if (rnd_val < prob)
		{
			// tak zaakceptowano -> zmiana położenia i energii
			setxy[0] = (double)idx;
			setxy[1] = xnew;
			setxy[2] = ynew;
			this->myMPI->MPI_Bcast(setxy, 3, MPI_DOUBLE, 0, MPI_COMM_WORLD);
			if (!mpi_rank)
			{
				particles->setXY(idx, xnew, ynew);
			}
			for (int j = 0; j < ppp; j++)
			{
				if (ids[j] == (int)setxy[0])
				{
					x[j] = setxy[1];
					y[j] = setxy[2];
				}
			}
			totalEp += global_dE;
			accepted++;
		}
	}

	if (!mpi_rank)
	{

		// zmiana dr jeśli zmian było ponado 50%, to
		// dr rośnie, jeśli było mniej, to dr maleje.
		if (accepted * 2 > draws)
		{
			dr *= (1.0 + DR_CORRECTION);
		}
		else
		{
			dr *= (1.0 - DR_CORRECTION);
		}
		if (dr > DR_MAX)
			dr = DR_MAX;

		if (dr < DR_MIN)
			dr = DR_MIN;
	}
}
