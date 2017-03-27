package solution.selection;

import msrcpsp.scheduling.BaseIndividual;
import msrcpsp.scheduling.BaseIntIndividual;

import java.util.*;

/**
 * Created by annam on 26.03.2017.
 */
public class RouletteWheelSelector extends BaseSelector
{
    double[] rouletteWheel;

    public RouletteWheelSelector(double selectionProbability)
    {
        super(selectionProbability);
    }

    @Override
    public BaseIndividual selectIndividual()
    {
        double selectedValue = random.nextDouble();

        for (int index = 0; index < populationSize; ++index)
        {
            if (selectedValue < rouletteWheel[index])
                return population[index];
        }

        return population[populationSize - 1];
    }

    @Override
    public void setPopulation(BaseIndividual[] population)
    {
        super.setPopulation(population);
        rouletteWheel = new double[populationSize];
        setFitnessForEachIndividual();
    }

    private void setFitnessForEachIndividual()
    {
        int worstFitness = getWorstFitness();
        int totalFitness = getTotalSumOfFitness(worstFitness);

        for (int index = 0; index < populationSize; ++index)
            rouletteWheel[index] = getIndividualsFitness(index, totalFitness, worstFitness);
    }

    private double getIndividualsFitness(int index, int totalFitness, int worstFitness)
    {
        double fitness = ((double) worstFitness - (double) population[index].getDuration() + 1.)
                / ((double) totalFitness );
        return index == 0 ? fitness : (fitness + rouletteWheel[index  - 1]);
    }

    private int getTotalSumOfFitness(int worstFitness)
    {
        return populationSize * worstFitness
                - Arrays.stream(population).mapToInt(BaseIndividual::getDuration).sum()
                + populationSize;
    }

    private Integer getWorstFitness()
    {
        Optional<BaseIndividual> worstDuration = Arrays.stream(population).max(Comparator.naturalOrder());
        return worstDuration.isPresent() ? worstDuration.get().getDuration() : Integer.MAX_VALUE;
    }
}
