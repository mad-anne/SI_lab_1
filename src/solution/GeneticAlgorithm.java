package solution;

import msrcpsp.evaluation.DurationEvaluator;
import msrcpsp.io.MSRCPSPIO;
import msrcpsp.scheduling.*;
import msrcpsp.scheduling.greedy.Greedy;
import solution.crossover.BaseCrossover;
import solution.crossover.SinglePointCrossover;
import solution.selection.BaseSelector;
import solution.selection.TournamentSelector;

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
    private double crossoverProbability;
    private double mutationProbability;
    BaseSelector selector;
    BaseCrossover crossover;

    public static void main(String[] args)
    {
        MSRCPSPIO reader = new MSRCPSPIO();
        Schedule s = reader.readDefinition("src\\resources\\def_small\\15_9_12_9.def");
        GeneticAlgorithm ga = new GeneticAlgorithm(s, 100, 100, 5, 0.5, 0.01, new TournamentSelector(0.5, 5),
                new SinglePointCrossover());
        ga.start();
    }

    private GeneticAlgorithm(Schedule schedule, int maxNumberOfGenerations, int populationSize, int contestSize,
                             double crossoverProbability, double mutationProbability, BaseSelector selector,
                             BaseCrossover crossover)
    {
        this.schedule = schedule;
        this.maxNumberOfGenerations = maxNumberOfGenerations;
        this.greedy = new Greedy(schedule.getSuccesors());
        this.populationSize = populationSize;
        this.contestSize = contestSize;
        this.crossoverProbability = crossoverProbability;
        this.mutationProbability = mutationProbability;
        this.population = new BaseIntIndividual[populationSize];
        this.selector = selector;
        this.crossover = crossover;
    }

    private boolean start()
    {
        population = generateRandomPopulation();

        int currentGeneration = 1;
        while (currentGeneration <= maxNumberOfGenerations)
        {
            evalute();
            selector.setPopulation(population);
            BaseIntIndividual[] crossedIndividuals = performCrossover();
            population = performMutation(crossedIndividuals);
            System.out.println(currentGeneration + ". " + getTheBestIndividual().getDuration());
            ++currentGeneration;
        }

        return true;
    }

    private void evalute()
    {
        for (BaseIndividual individual : population)
        {
            individual.setDurationAndCost();
            individual.setNormalDurationAndCost();
        }
    }

    private BaseIntIndividual getTheBestIndividual()
    {
        return Arrays.asList(population).stream().min(Comparator.naturalOrder()).get();
    }

    private BaseIntIndividual[] generateRandomPopulation()
    {
        BaseIntIndividual[] randomIndividuals = new BaseIntIndividual[populationSize];

        for (int i = 0; i < populationSize; ++i)
            randomIndividuals[i] = generateRandomIndividual();

        return randomIndividuals;
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

    private BaseIntIndividual[] performCrossover()
    {
        BaseIntIndividual[] crossedIndividuals = new BaseIntIndividual[populationSize];

        for (int index = 0; index < populationSize; ++index)
            crossedIndividuals[index] = getCrossedIndividual();

        return crossedIndividuals;
    }

    private BaseIntIndividual getCrossedIndividual()
    {
        BaseIntIndividual crossedIndividual = null;

        while (crossedIndividual == null)
        {
            BaseIntIndividual individual1 = (BaseIntIndividual) selector.selectIndividual();
            BaseIntIndividual individual2 = (BaseIntIndividual) selector.selectIndividual();

            crossedIndividual = crossover.crossIndividuals(individual1, individual2);
        }

        return crossedIndividual;
    }

    private BaseIntIndividual[] performMutation(BaseIntIndividual[] crossedIndividuals)
    {
        Random r = new Random();

        for(BaseIntIndividual individual : crossedIndividuals)
        {
            int[] genes = individual.getGenes();

            for (int index = 0; index < genes.length; ++index)
            {
                if (r.nextDouble() <= mutationProbability)
                {
                    genes[index] = mutate(individual, index);
                    individual.setGenes(genes);
                }
            }

            Greedy g = new Greedy(individual.getSchedule().getSuccesors());
            individual.setSchedule(g.buildTimestamps(individual.getSchedule()));
            individual.setDurationAndCost();
        }

        return crossedIndividuals;
    }

    private int mutate(BaseIntIndividual individual, int index)
    {
        Schedule s = individual.getSchedule();
        Task t = s.getTasks()[index];
        List<Resource> resources = s.getCapableResources(t);
        int afterMutationValue = (new Random()).nextInt(resources.size());
        s.assign(t, resources.get(afterMutationValue));
        return resources.get(afterMutationValue).getId();
    }
}
