package solution;

import msrcpsp.evaluation.DurationEvaluator;
import msrcpsp.io.MSRCPSPIO;
import msrcpsp.scheduling.*;
import msrcpsp.scheduling.greedy.Greedy;

import java.util.*;

/**
 * Created by annam on 13.03.2017.
 */
public class GeneticAlgorithm
{
    private int maxNumberOfGenerations = 0;
    private Schedule schedule;
    private Greedy greedy;
    private BaseIntIndividual[] population;
    private int populationSize;
    private int contestSize;

    public static void main(String[] args)
    {
        MSRCPSPIO reader = new MSRCPSPIO();
        Schedule s = reader.readDefinition("src\\resources\\def_small\\10_3_5_3.def");
        GeneticAlgorithm ga = new GeneticAlgorithm(s, 1, 100, 5);
        ga.start();
    }

    private GeneticAlgorithm(Schedule schedule, int maxNumberOfGenerations, int populationSize, int contestSize)
    {
        this.schedule = schedule;
        this.maxNumberOfGenerations = maxNumberOfGenerations;
        this.greedy = new Greedy(schedule.getSuccesors());
        this.populationSize = populationSize;
        this.contestSize = contestSize;
    }

    private boolean start()
    {
        population = generateRandomPopulation();

        int currentGeneration = 1;
        while (currentGeneration <= maxNumberOfGenerations)
        {
            ArrayList<BaseIntIndividual> selectedIndividuals = selectIndividuals();
            performCrossover();
            performMutation();
            ++currentGeneration;
        }

        return true;
    }

    private BaseIntIndividual[] generateRandomPopulation()
    {
        BaseIntIndividual[] population = new BaseIntIndividual[populationSize];

        for (int i = 0; i < population.length; ++i)
            population[i] = generateRandomIndividual();

        return population;
    }

    private BaseIntIndividual generateRandomIndividual()
    {
        Schedule s = new Schedule(schedule);

        Task[] tasks = s.getTasks();
        Random random = new Random();
        int[] genes = new int[tasks.length];

        int index = 0;
        for (Task t : tasks)
        {
            List<Resource> capableResources = s.getCapableResources(t);
            int whichResource = random.nextInt(capableResources.size());
            s.assign(t, capableResources.get(whichResource));
            genes[index++] = capableResources.get(whichResource).getId();
        }

        greedy.buildTimestamps(s);

        BaseIntIndividual individual = new BaseIntIndividual(s, genes, new DurationEvaluator(s));
        individual.setDurationAndCost();

        return individual;
    }

    private ArrayList<BaseIntIndividual> selectIndividuals()
    {
        ArrayList<BaseIntIndividual> individuals = new ArrayList<>();
        Random r = new Random();
        Set<Integer> drawnIndexes = new HashSet<>();

        while (individuals.size() < populationSize)
        {
            drawnIndexes.clear();

            while (drawnIndexes.size() < contestSize)
                drawnIndexes.add(r.nextInt(populationSize));

            ArrayList<Integer> drawnIndexesList = new ArrayList<>(drawnIndexes);
            BaseIntIndividual bestIndividual = population[drawnIndexesList.get(0)];
            int bestDurationTime = population[drawnIndexesList.get(0)].getDuration();

            for (int index = 1; index < drawnIndexes.size(); ++index)
            {
                if (population[drawnIndexesList.get(index)].getDuration() < bestDurationTime)
                {
                    bestIndividual = population[drawnIndexesList.get(index)];
                    bestDurationTime = population[drawnIndexesList.get(index)].getDuration();
                }
            }

            individuals.add(bestIndividual);
        }

        return individuals;
    }

    private void performCrossover()
    {

    }

    private void performMutation()
    {

    }
}
