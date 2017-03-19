package solution;

import msrcpsp.evaluation.DurationEvaluator;
import msrcpsp.io.MSRCPSPIO;
import msrcpsp.scheduling.*;
import msrcpsp.scheduling.greedy.Greedy;
import msrcpsp.validation.CompleteValidator;

import java.util.*;

/**
 * Created by annam on 13.03.2017.
 */
public class GeneticAlgorithm
{
    private int maxNumberOfGenerations = 0;
    private Schedule schedule;
    private Greedy greedy;
    private ArrayList<BaseIntIndividual> population;
    private int populationSize;
    private int contestSize;
    private double crossoverProbability;
    private double mutationProbability;

    public static void main(String[] args)
    {
        MSRCPSPIO reader = new MSRCPSPIO();
        Schedule s = reader.readDefinition("src\\resources\\def_small\\10_3_5_3.def");
        GeneticAlgorithm ga = new GeneticAlgorithm(s, 100, 100, 5, 0.75, 0.05);
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
    }

    private boolean start()
    {
        population = generateRandomPopulation();

        System.out.println(getTheBestIndividual(population).getDuration());
        int currentGeneration = 1;
        while (currentGeneration <= maxNumberOfGenerations)
        {
            ArrayList<BaseIntIndividual> selectedIndividuals = selectIndividuals(population);
            ArrayList<BaseIntIndividual> crossedIndividuals = performCrossover(selectedIndividuals);
            population = performMutation(crossedIndividuals);
            System.out.println(getTheBestIndividual(population).getDuration());
            ++currentGeneration;
        }

        return true;
    }

    private BaseIntIndividual getTheBestIndividual(ArrayList<BaseIntIndividual> individuals)
    {
        return individuals.stream().max(Comparator.naturalOrder()).get();
    }

    private ArrayList<BaseIntIndividual> generateRandomPopulation()
    {
        ArrayList<BaseIntIndividual> population = new ArrayList<>();

        for (int i = 0; i < populationSize; ++i)
            population.add(generateRandomIndividual());

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

    private ArrayList<BaseIntIndividual> selectIndividuals(ArrayList<BaseIntIndividual> population)
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
            BaseIntIndividual bestIndividual = population.get(drawnIndexesList.get(0));
            int bestDurationTime = population.get(drawnIndexesList.get(0)).getDuration();

            for (int index = 1; index < drawnIndexes.size(); ++index)
            {
                if (population.get(drawnIndexesList.get(index)).getDuration() < bestDurationTime)
                {
                    bestIndividual = population.get(drawnIndexesList.get(index));
                    bestDurationTime = population.get(drawnIndexesList.get(index)).getDuration();
                }
            }

            individuals.add(bestIndividual);
        }

        return individuals;
    }

    private ArrayList<BaseIntIndividual> performCrossover(ArrayList<BaseIntIndividual> selectedIndividuals)
    {
        ArrayList<BaseIntIndividual> crossedIndividuals = new ArrayList<>();
        Random r = new Random();

        while (crossedIndividuals.size() < populationSize)
        {
            BaseIntIndividual individual1 = selectedIndividuals.get(r.nextInt(populationSize));
            BaseIntIndividual individual2 = selectedIndividuals.get(r.nextInt(populationSize));

            if (r.nextDouble() <= crossoverProbability)
                crossedIndividuals.add(crossIndividuals(individual1, individual2));
        }

        return crossedIndividuals;
    }

    private BaseIntIndividual crossIndividuals(BaseIntIndividual individual1, BaseIntIndividual individual2)
    {
        int[] genes1 = individual1.getGenes();
        int[] genes2 = individual2.getGenes();

        Random r = new Random();
        int slicePoint = r.nextInt(genes1.length - 1);

        int[] firstSlice = Arrays.copyOfRange(genes1, 0, slicePoint);
        int[] secondSlice = Arrays.copyOfRange(genes2, slicePoint, genes1.length);
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

    private ArrayList<BaseIntIndividual> performMutation(ArrayList<BaseIntIndividual> crossedIndividuals)
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
