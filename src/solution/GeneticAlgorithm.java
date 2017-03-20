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
    private double crossoverProbability;
    private double mutationProbability;

    public static void main(String[] args)
    {
        MSRCPSPIO reader = new MSRCPSPIO();
        Schedule s = reader.readDefinition("src\\resources\\def_small\\15_9_12_9.def");
        GeneticAlgorithm ga = new GeneticAlgorithm(s, 1000, 100, 5, 0.5, 0.01);
        ga.start();
    }

    private GeneticAlgorithm(Schedule schedule, int maxNumberOfGenerations, int populationSize, int contestSize,
                             double crossoverProbability, double mutationProbability)
    {
        this.schedule = schedule;
        this.maxNumberOfGenerations = maxNumberOfGenerations;
        this.greedy = new Greedy(schedule.getSuccesors());
        this.populationSize = populationSize;
        this.contestSize = contestSize;
        this.crossoverProbability = crossoverProbability;
        this.mutationProbability = mutationProbability;
        this.population = new BaseIntIndividual[populationSize];
    }

    private boolean start()
    {
        population = generateRandomPopulation();

        int currentGeneration = 1;
        while (currentGeneration <= maxNumberOfGenerations)
        {
            BaseIntIndividual[] selectedIndividuals = selectIndividuals(population);
            BaseIntIndividual[] crossedIndividuals = performCrossover(selectedIndividuals);
            population = performMutation(crossedIndividuals);
            System.out.println(currentGeneration + ". " + getTheBestIndividual().getDuration());
            ++currentGeneration;
        }

        return true;
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

    private BaseIntIndividual[] selectIndividuals(BaseIntIndividual[] population)
    {
        BaseIntIndividual[] selectedIndividuals = new BaseIntIndividual[populationSize];
        Random r = new Random();
        Set<Integer> drawnIndexes = new HashSet<>();

        int index = 0;
        while (index < populationSize)
        {
            drawnIndexes.clear();

            while (drawnIndexes.size() < contestSize)
                drawnIndexes.add(r.nextInt(populationSize));

            selectedIndividuals[index] = getTheBestIndividual(population, drawnIndexes);
            ++index;
        }

        return selectedIndividuals;
    }

    private BaseIntIndividual getTheBestIndividual(BaseIntIndividual[] population, Set<Integer> drawnIndexes)
    {
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

        return bestIndividual;
    }

    private BaseIntIndividual[] performCrossover(BaseIntIndividual[] selectedIndividuals)
    {
        BaseIntIndividual[] crossedIndividuals = new BaseIntIndividual[populationSize];
        Random r = new Random();

        int index = 0;
        while (index < populationSize)
        {
            BaseIntIndividual individual1 = selectedIndividuals[r.nextInt(populationSize)];
            BaseIntIndividual individual2 = selectedIndividuals[r.nextInt(populationSize)];

            if (r.nextDouble() <= crossoverProbability)
            {
                crossedIndividuals[index] = crossIndividuals(individual1, individual2);
                ++index;
            }
        }

        return crossedIndividuals;
    }

    private BaseIntIndividual crossIndividuals(BaseIntIndividual individual1, BaseIntIndividual individual2)
    {
        int[] genes1 = individual1.getGenes();
        int[] genes2 = individual2.getGenes();

        Random r = new Random();
        int slicePoint = r.nextInt(genes1.length - 1);

        int[] firstSlice;
        int[] secondSlice;

        if (r.nextDouble() < 0.5)
        {
            firstSlice = Arrays.copyOfRange(genes1, 0, slicePoint);
            secondSlice = Arrays.copyOfRange(genes2, slicePoint, genes1.length);
        }
        else
        {
            firstSlice = Arrays.copyOfRange(genes2, slicePoint, genes1.length);
            secondSlice = Arrays.copyOfRange(genes1, 0, slicePoint);
        }
        int[] crossedGenes = new int[genes1.length];
        System.arraycopy(firstSlice, 0, crossedGenes, 0, firstSlice.length);
        System.arraycopy(secondSlice, 0, crossedGenes, firstSlice.length, secondSlice.length);

        Schedule s = new Schedule(schedule);

        Task[] tasks = s.getTasks();
        for(int index = 0; index < tasks.length; ++index)
            s.assign(tasks[index], s.getResource(crossedGenes[index]));

        Greedy tempGreedy = new Greedy(s.getSuccesors());
        tempGreedy.buildTimestamps(s);

        BaseIntIndividual individual = new BaseIntIndividual(s, crossedGenes, new DurationEvaluator(s));
        individual.setDurationAndCost();

        return individual;
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
