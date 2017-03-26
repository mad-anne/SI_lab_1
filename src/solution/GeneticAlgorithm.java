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

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * Created by annam on 13.03.2017.
 */
public class GeneticAlgorithm
{
    private static String path = "src\\resources\\dataset_def\\200_20_55_9.def";

    private static int sPopulationSize = 100;
    private static int sGenerations = 100;
    private static double sCrossoverProbability = 0.5;
    private static double mutationProbability = 0.01;
    private static double selectionProbability = 1;
    private static int tournamentSize = 10;

    private int maxNumberOfGenerations = 0;
    private Schedule schedule;
    private Greedy greedy;
    private BaseIntIndividual[] population;
    private int populationSize;
    private double crossoverProbability;
    private BaseSelector selector;
    private BaseCrossover crossover;
    private BaseMutator mutator;
    private Random random;

    private ArrayList<String> results = new ArrayList<>();
    private ArrayList<String> JSONresults = new ArrayList<>();
    private ArrayList<String> JSONdata = new ArrayList<>();

    public static void main(String[] args)
    {
        MSRCPSPIO reader = new MSRCPSPIO();
        Schedule s = reader.readDefinition(path);

        BaseSelector selector = new TournamentSelector(selectionProbability, tournamentSize);
        BaseCrossover crossover = new SinglePointCrossover();
        BaseMutator mutator = new RandomMutator(mutationProbability);

        GeneticAlgorithm ga = new GeneticAlgorithm(s, sGenerations, sPopulationSize, sCrossoverProbability,
                selector, crossover, mutator);
        ga.start();
    }

    private GeneticAlgorithm(Schedule schedule, int maxNumberOfGenerations, int populationSize,
                             double crossoverProbability, BaseSelector selector, BaseCrossover crossover,
                             BaseMutator mutator)
    {
        this.schedule = schedule;
        this.maxNumberOfGenerations = maxNumberOfGenerations;
        this.populationSize = populationSize;
        this.population = new BaseIntIndividual[populationSize];
        this.crossoverProbability = crossoverProbability;
        this.selector = selector;
        this.crossover = crossover;
        this.mutator = mutator;
        this.greedy = new Greedy(schedule.getSuccesors());
        this.random = new Random();
    }

    private boolean start()
    {
        generateRandomPopulation();

        for (int currentGeneration = 1; currentGeneration <= maxNumberOfGenerations; ++currentGeneration)
        {
            evaluate();
            results.add(getCurrentState(currentGeneration));
            modifyJSONresults();
            System.out.println("GENERATION " + currentGeneration + " OF " + maxNumberOfGenerations);
            selector.setPopulation(population);
            population = performCrossover();
            population = performMutation();
        }

        putResultToJSONFile();
        putResultToFile();
        return true;
    }

    private void prepareJSONdata()
    {
        JSONdata.add("Highcharts.chart('container', {\n" +
                "\n" +
                "    title: {\n" +
                "        text: 'Genetic Algorithm on " +
                path.replace("src\\resources\\def_small\\", "")
                    .replace("src\\resources\\dataset_def\\", "")
                + " dataset'\n" +
                "    },");
        JSONdata.add("    subtitle: {\n" +
                "        text: 'parameters: N = " + populationSize +
                ", G = " + maxNumberOfGenerations +
                ", T = " + tournamentSize +
                ", pm =" + mutationProbability +
                ", ps = " + selectionProbability +
                ", pc = " + crossoverProbability +" '\n" +
                "    },");

        JSONdata.add("yAxis: {\n" +
                "        title: {\n" +
                "            text: 'Duration Time'\n" +
                "        }\n" +
                "    },");

        JSONdata.add("xAxis: {\n" +
                "        title: {\n" +
                "            text: 'Generation'\n" +
                "        }\n" +
                "    },");

        JSONdata.add("legend: {\n" +
                "        layout: 'vertical',\n" +
                "        align: 'right',\n" +
                "        verticalAlign: 'middle'\n" +
                "    }," +
                "    plotOptions: {\n" +
                        "        series: {\n" +
                        "            pointStart: 0\n" +
                        "        }\n" +
                        "    },");
    }

    private void modifyJSONresults()
    {
        if (JSONresults.size() == 0)
        {
            JSONresults.add("series: [{");
            JSONresults.add("name: 'Best',");
            JSONresults.add("data: [" + getBestDurationTime() + "]");
            JSONresults.add(" }, {");
            JSONresults.add("name: 'Average',");
            JSONresults.add("data: [" + getAverageDurationTime() + "]");
            JSONresults.add(" }, {");
            JSONresults.add("name: 'Worst',");
            JSONresults.add("data: [" + getWorstDurationTime() + "]");
            JSONresults.add(" }]");
        }

        JSONresults.set(2, JSONresults.get(2).replace("]", ", " + getBestDurationTime() + "]"));
        JSONresults.set(5, JSONresults.get(5).replace("]", ", " + getAverageDurationTime() + "]"));
        JSONresults.set(8, JSONresults.get(8).replace("]", ", " + getWorstDurationTime() + "]"));
    }

    private void putResultToJSONFile()
    {
        try
        {
            PrintWriter writer = new PrintWriter("JSON" + generateFileName(), "UTF-8");

            prepareJSONdata();
            JSONdata.forEach(writer::println);
            JSONresults.forEach(writer::println);
            writer.println("});");
            writer.close();
        }
        catch (FileNotFoundException | UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
    }

    private String getCurrentState(int generation)
    {
        Integer best = getBestDurationTime();
        Double average = getAverageDurationTime();
        Integer worst = getWorstDurationTime();

        return generation + ";" + best + ";" + average + ";" + worst + ";";
    }

    private void putResultToFile()
    {
        try
        {
            PrintWriter writer = new PrintWriter(generateFileName(), "UTF-8");

            results.forEach(writer::println);
            writer.close();
        }
        catch (FileNotFoundException | UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
    }

    private String generateFileName()
    {
        return "result"
                + "_path_" + path
                .replace("src\\resources\\def_small\\", "").replace(".def", "")
                .replace("src\\resources\\dataset_def\\", "").replace(".def", "")
                + "_gen" + sGenerations
                + "_popSize" + sPopulationSize
                + "_crossProb" + String.valueOf(sCrossoverProbability).replace(".", "-")
                + "_mutProb" + String.valueOf(mutationProbability).replace(".", "-")
                + "_selProb" + String.valueOf(selectionProbability).replace(".", "-")
                + "_tSize" + tournamentSize + ".txt";
    }

    private void evaluate()
    {
        for (BaseIndividual individual : population)
        {
            greedy.setHasSuccessors(individual.getSchedule().getSuccesors());
            individual.setSchedule(greedy.buildTimestamps(individual.getSchedule()));

            individual.setDurationAndCost();
            individual.setNormalDurationAndCost();
        }
    }

    private Integer getBestDurationTime()
    {
        Optional<BaseIntIndividual> bestDurationTime = Arrays.stream(population).min(Comparator.naturalOrder());
        return bestDurationTime.isPresent() ? bestDurationTime.get().getDuration() : null;
    }

    private Integer getWorstDurationTime()
    {
        Optional<BaseIntIndividual> worstDurationTime = Arrays.stream(population).max(Comparator.naturalOrder());
        return worstDurationTime.isPresent() ? worstDurationTime.get().getDuration() : null;
    }

    private Double getAverageDurationTime()
    {
        OptionalDouble averageDurationTime = Arrays.stream(population).mapToInt(BaseIntIndividual::getDuration).average();
        return averageDurationTime.isPresent() ? averageDurationTime.getAsDouble() : null;
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
        individual.setNormalDurationAndCost();

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

            if (random.nextDouble() < crossoverProbability)
            {
                BaseIntIndividual individual2 = (BaseIntIndividual) selector.selectIndividual();
                crossedIndividual = crossover.crossIndividuals(individual1, individual2);
            }
            else
                crossedIndividual = individual1;
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
