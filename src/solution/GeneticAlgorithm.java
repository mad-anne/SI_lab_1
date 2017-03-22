package solution;

import msrcpsp.evaluation.DurationEvaluator;
import msrcpsp.io.MSRCPSPIO;
import msrcpsp.scheduling.*;
import msrcpsp.scheduling.greedy.Greedy;
import solution.crossover.BaseCrossover;
import solution.crossover.SinglePointCrossover;
import solution.mutation.BaseMutator;
import solution.mutation.RandomMutator;
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
    BaseSelector selector;
    BaseCrossover crossover;
    BaseMutator mutator;

    public static void main(String[] args)
    {
        MSRCPSPIO reader = new MSRCPSPIO();
        Schedule s = reader.readDefinition("src\\resources\\def_small\\15_9_12_9.def");
        GeneticAlgorithm ga = new GeneticAlgorithm(s, 30, 10, new TournamentSelector(0.5, 5),
                new SinglePointCrossover(), new RandomMutator(0.01));
        ga.start();
    }

    private GeneticAlgorithm(Schedule schedule, int maxNumberOfGenerations, int populationSize,
                             BaseSelector selector, BaseCrossover crossover, BaseMutator mutator)
    {
        this.schedule = schedule;
        this.maxNumberOfGenerations = maxNumberOfGenerations;
        this.populationSize = populationSize;
        this.population = new BaseIntIndividual[populationSize];
        this.selector = selector;
        this.crossover = crossover;
        this.mutator = mutator;
        this.greedy = new Greedy(schedule.getSuccesors());
    }

    private boolean start()
    {
        generateRandomPopulation();

        for (int currentGeneration = 1; currentGeneration <= maxNumberOfGenerations; ++currentGeneration)
        {
            selector.setPopulation(population);
            population = performCrossover();
            performMutation();
            evalute();
            printResult(currentGeneration);
        }

        return true;
    }

    private void printResult(int number)
    {
        System.out.println(number + ". " + getTheBestIndividual().getDuration());
        System.out.print("Individuals: ");
        for (BaseIntIndividual individual : population)
            System.out.print(individual.getDuration() + " ");
        System.out.println("\n");
    }

    private void evalute()
    {
        for (BaseIndividual individual : population)
        {
            greedy.setHasSuccessors(individual.getSchedule().getSuccesors());
            individual.setSchedule(greedy.buildTimestamps(individual.getSchedule()));

            individual.setDurationAndCost();
            individual.setNormalDurationAndCost();
        }
    }

    private BaseIntIndividual getTheBestIndividual()
    {
        return Arrays.asList(population).stream().min(Comparator.naturalOrder()).get();
    }

    private void generateRandomPopulation()
    {
        for (int i = 0; i < populationSize; ++i)
            population[i] = generateRandomIndividual();
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

    private BaseIntIndividual[] performMutation()
    {
        for(BaseIntIndividual individual : population)
            mutator.mutate(individual);

        return population;
    }
}
