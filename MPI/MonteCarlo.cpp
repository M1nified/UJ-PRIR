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
	this->start_idx = 0;
	this->end_idx = particles->getNumberOfParticles();
	calcInitialDr();
}

void MonteCarlo::setPotential(PotentialEnergy *energy)
{
	this->energy = energy;
}

double MonteCarlo::calcContribution(int idx, double xx, double yy)
{
	double sum = 0;

	// cout << mpi_rank << " range " << start_idx << " " << end_idx << endl;

	if (idx >= start_idx && idx < end_idx)
	{
		for (int i = start_idx; i < idx; i++)
		{
			sum += energy->getPotentialEnergyDistanceSQ(particles->getDistanceBetweenSQ(i, xx, yy));
		}
		for (int i = idx + 1; i < end_idx && i < particles->getNumberOfParticles(); i++)
		{
			sum += energy->getPotentialEnergyDistanceSQ(particles->getDistanceBetweenSQ(i, xx, yy));
		}
	}
	else
	{
		for (int i = start_idx; i < end_idx && i < particles->getNumberOfParticles(); i++)
		{
			sum += energy->getPotentialEnergyDistanceSQ(particles->getDistanceBetweenSQ(i, xx, yy));
		}
	}

	return sum;
}

double MonteCarlo::calcTotalPotentialEnergy()
{
	double tmp = 0;
	for (int i = 0; i < particles->getNumberOfParticles(); i++)
		tmp += calcContribution(i, particles->getX(i), particles->getY(i));

	totalEp = tmp * 0.5;

	return totalEp;
}

double MonteCarlo::deltaEp(int idx, double oldX, double oldY, double newX, double newY)
{
	return calcContribution(idx, newX, newY) - calcContribution(idx, oldX, oldY);
}

// rozesłanie położeń cząstek z procesu o rank=0 do pozostałych
void MonteCarlo::shareParticles()
{
	myMPI->MPI_Barrier(MPI_COMM_WORLD);

	ppp = particles->getNumberOfParticles() / mpi_size;
	if(particles->getNumberOfParticles() % mpi_size != 0){
		ppp++;
	}

	start_idx = mpi_rank * ppp;
	end_idx = (mpi_rank + 1) * ppp;

	myMPI->MPI_Bcast(&ppp, 1, MPI_INT, 0, MPI_COMM_WORLD);

	double *x, *y, *m, *Vx, *Vy, *Fx, *Fy;
	x = new double[particles->getNumberOfParticles()];
	y = new double[particles->getNumberOfParticles()];
	m = new double[particles->getNumberOfParticles()];
	Fx = new double[particles->getNumberOfParticles()];
	Fy = new double[particles->getNumberOfParticles()];
	Vx = new double[particles->getNumberOfParticles()];
	Vy = new double[particles->getNumberOfParticles()];

	for (int i = 0; i < particles->getNumberOfParticles(); i++)
	{
		x[i] = particles->getX(i);
		y[i] = particles->getY(i);
		// Fx[i] = particles->getFx(i);
		// Fy[i] = particles->getFy(i);
		Vx[i] = particles->getVx(i);
		Vy[i] = particles->getVy(i);
	}

	this->myMPI->MPI_Bcast(x, particles->getNumberOfParticles(), MPI_DOUBLE, 0, MPI_COMM_WORLD);
	this->myMPI->MPI_Bcast(y, particles->getNumberOfParticles(), MPI_DOUBLE, 0, MPI_COMM_WORLD);
	this->myMPI->MPI_Bcast(Vx, particles->getNumberOfParticles(), MPI_DOUBLE, 0, MPI_COMM_WORLD);
	this->myMPI->MPI_Bcast(Vy, particles->getNumberOfParticles(), MPI_DOUBLE, 0, MPI_COMM_WORLD);

	for (int i = 0; i < particles->getNumberOfParticles(); i++)
	{
		particles->setXY(i, x[i], y[i]);
		particles->setVx(i, Vx[i]);
		particles->setVy(i, Vy[i]);
	}
}

// proces o rank=0 po zakończeniu tej metody musi zawierać
// zaktualizowane pozycje cząstek
void MonteCarlo::gatherParticles()
{
	start_idx = 0;
	end_idx = particles->getNumberOfParticles();
}

void MonteCarlo::calcMC(int draws)
{
	int accepted = 0;
	int idx;
	double xnew, ynew, xold, yold, localdE, dE, prob;
	bool is_accepted;

	for (int i = 0; i < draws; i++)
	{

		// którą z cząstek będzemy próbowali przestawić
		idx = (int)(particles->getNumberOfParticles() * rnd());
		// stara pozycja dla czastki
		xold = particles->getX(idx);
		yold = particles->getY(idx);
		// nowa pozycja dla czastki
		xnew = xold + dr * (rnd() - 0.5);
		ynew = yold + dr * (rnd() - 0.5);

		this->myMPI->MPI_Bcast(&idx, 1, MPI_INT, 0, MPI_COMM_WORLD);
		this->myMPI->MPI_Bcast(&xnew, 1, MPI_DOUBLE, 0, MPI_COMM_WORLD);
		this->myMPI->MPI_Bcast(&ynew, 1, MPI_DOUBLE, 0, MPI_COMM_WORLD);
		this->myMPI->MPI_Bcast(&xold, 1, MPI_DOUBLE, 0, MPI_COMM_WORLD);
		this->myMPI->MPI_Bcast(&yold, 1, MPI_DOUBLE, 0, MPI_COMM_WORLD);

		// wyliczamy zmianę energii potencjalnej gdy cząstka idx
		// przestawiana jest z pozycji old na new
		localdE = deltaEp(idx, xold, yold, xnew, ynew);

		this->myMPI->MPI_Reduce(&localdE, &dE, 1, MPI_DOUBLE, MPI_SUM, 0, MPI_COMM_WORLD);

		// pradopodobieństwo zależy od temperatury
		prob = exp(dE * kBTinv);
		// czy zaakceptowano zmianę położenia ?
		is_accepted = rnd() < prob;

		this->myMPI->MPI_Bcast(&is_accepted, 1, MPI_CHAR, 0, MPI_COMM_WORLD);

		if (is_accepted)
		{
			// tak zaakceptowano -> zmiana położenia i energii
			particles->setXY(idx, xnew, ynew);
			totalEp += dE;
			accepted++;
		}
	}

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
