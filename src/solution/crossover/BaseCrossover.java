package solution.crossover;

import msrcpsp.scheduling.BaseIntIndividual;

import java.util.Random;

/**
 * Created by annam on 22.03.2017.
 */
abstract public class BaseCrossover
{
    Random random;

    public BaseCrossover()
    {
        random = new Random();
    }

    abstract public BaseIntIndividual crossIndividuals(BaseIntIndividual individual1, BaseIntIndividual individual2);
}