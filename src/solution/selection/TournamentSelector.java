package solution.selection;

import msrcpsp.scheduling.BaseIndividual;

import java.util.*;

/**
 * Created by annam on 22.03.2017.
 */
public class TournamentSelector extends BaseSelector
{
    int tournamentSize;

    public TournamentSelector(double selectionProbability, int tournamentSize)
    {
        super(selectionProbability);
        this.tournamentSize = tournamentSize;
    }


    public TournamentSelector(BaseIndividual[] population, double selectionProbability, int tournamentSize)
    {
        super(population, selectionProbability);
        this.tournamentSize = tournamentSize;
    }

    @Override
    public BaseIndividual selectIndividual()
    {
        BaseIndividual[] drawnIndividuals = getRandomIndividuals();
        Arrays.sort(drawnIndividuals);

        return drawnIndividuals[getSelectedIndex()];
    }

    private int getSelectedIndex()
    {
        int selectedIndex = 0;
        double currentProbability = selectionProbability;

        while (selectedIndex < tournamentSize - 1
                && !(random.nextDouble() <= currentProbability))
        {
            currentProbability = currentProbability * (1 - selectionProbability);
            ++selectedIndex;
        }

        return selectedIndex;
    }

    private BaseIndividual[] getRandomIndividuals()
    {
        BaseIndividual[] drawnIndividuals = new BaseIndividual[tournamentSize];
        ArrayList<Integer> drawnIndexes = getRandomIndexes();

        for (int index = 0; index < tournamentSize; ++index)
            drawnIndividuals[index] = population[drawnIndexes.get(index)];

        return drawnIndividuals;
    }

    private ArrayList<Integer> getRandomIndexes()
    {
        Set<Integer> drawnIndexes = new HashSet<>();

        while (drawnIndexes.size() < tournamentSize)
            drawnIndexes.add(random.nextInt(populationSize));

        return new ArrayList<>(drawnIndexes);
    }
}
