package solution.mutation;

import msrcpsp.scheduling.BaseIntIndividual;

import java.util.Random;

/**
 * Created by annam on 22.03.2017.
 */
abstract public class BaseMutator
{
    double mutationProbability;
    Random random;

    BaseMutator(double mutationProbability)
    {
        this.mutationProbability = mutationProbability;
        this.random = new Random();
    }

    abstract public BaseIntIndividual mutate(BaseIntIndividual individual);
}
