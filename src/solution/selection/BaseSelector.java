package solution.selection;

import msrcpsp.scheduling.BaseIndividual;

import java.util.Random;

/**
 * Created by annam on 22.03.2017.
 */
abstract public class BaseSelector
{
    BaseIndividual[] population;
    int populationSize;
    double selectionProbability;
    Random random;

    BaseSelector(double selectionProbability)
    {
        this.population = null;
        this.populationSize = 0;
        this.selectionProbability = selectionProbability;
        this.random = new Random();
    }

    BaseSelector(BaseIndividual[] population, double selectionProbability)
    {
        this.population = population;
        this.populationSize = population.length;
        this.selectionProbability = selectionProbability;
        this.random = new Random();
    }

    abstract public BaseIndividual selectIndividual();

    public void setPopulation(BaseIndividual[] population)
    {
        this.population = population;
        this.populationSize = population.length;
    }
}
