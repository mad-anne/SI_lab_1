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
import solution.selection.RouletteWheelSelector;
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
    private static String path1 = "src\\resources\\dataset_def\\200_20_145_15.def";
//    private static String path1 = "src\\resources\\def_small\\15_9_12_9.def";

    private static int sPopulationSize = 100;
    private static int sGenerations = 100;
    private static double sCrossoverProbability = 0.75;
    private static double selectionProbability = 0.9;
    private static double mutationProbability = 0.01;
    private static int tournamentSize = 5;
    private static int numberOfRuns = 15;

    private String path;
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
    private int currentRun;

    private ArrayList<String> JSONresults = new ArrayList<>();
    private ArrayList<String> JSONdata = new ArrayList<>();

    private ArrayList<Double> bests = new ArrayList<>();
    private ArrayList<Double> average = new ArrayList<>();
    private ArrayList<Double> worsts = new ArrayList<>();

    private int randomEvaluation = 0;
    private int GAEvaluation = 0;

    public static void main(String[] args)
    {
        MSRCPSPIO reader = new MSRCPSPIO();

        BaseSelector selector = new RouletteWheelSelector(selectionProbability);
        BaseSelector selector2 = new TournamentSelector(selectionProbability, tournamentSize);
        BaseCrossover crossover = new SinglePointCrossover();
        BaseMutator mutator = new RandomMutator(mutationProbability);

        long startGA = System.currentTimeMillis();
        Schedule s = reader.readDefinition(path1);
        GeneticAlgorithm ga = new GeneticAlgorithm(s, sGenerations, sPopulationSize, sCrossoverProbability,
                selector, crossover, mutator, path1);
        ga.start();
        long timeGA = System.currentTimeMillis() - startGA;

        System.out.println("BEST DURATION: " + ga.getBestDurationTime() + " FROM GA");
        System.out.println("TOOK " + timeGA);
        System.out.println("EVALUATIONS " + ga.getGAEvaluation());

        Schedule s2 = reader.readDefinition(path1);
        GeneticAlgorithm ga2 = new GeneticAlgorithm(s2, sGenerations, sPopulationSize, sCrossoverProbability,
                selector2, crossover, mutator, path1);
        ga2.start();

        long startRand = System.currentTimeMillis();
        int maxDrawns = 100;
        Schedule best = ga.getBestRandomIndividual(maxDrawns);
        best.setEvaluator(new DurationEvaluator(best));
        best.evaluate();
        long timeRand = System.currentTimeMillis() - startRand;
        System.out.println("BEST DURATION: " + best.getEvaluator().getDuration() + " FROM " + maxDrawns + " DRAWNS");
        System.out.println("TOOK " + timeRand);
        System.out.println("EVALUATIONS " + ga.getRandomEvaluation());
    }

    private GeneticAlgorithm(Schedule schedule, int maxNumberOfGenerations, int populationSize,
                             double crossoverProbability, BaseSelector selector, BaseCrossover crossover,
                             BaseMutator mutator, String path)
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
        this.path = path;
    }

    private boolean start()
    {
        generateRandomPopulation();

        GAEvaluation = 0;
        currentRun = 1;
        while (currentRun <= numberOfRuns)
        {
            System.out.println("RUN NO " + currentRun);
            for (int currentGeneration = 1; currentGeneration <= maxNumberOfGenerations; ++currentGeneration)
            {
                evaluate();
                modifyResults(currentGeneration);
                selector.setPopulation(population);
                population = performCrossover();
                population = performMutation();
            }
            ++currentRun;
        }

        evaluate();
        modifyJSONresults();
        putResultToJSONFile();
        return true;
    }

    private void modifyResults(int currentGeneration)
    {
        if (currentRun == 1)
        {
            bests.add( (double) getBestDurationTime().intValue());
            average.add(getAverageDurationTime());
            worsts.add( (double) getWorstDurationTime().intValue());
        }
        else
        {
            int index = currentGeneration - 1;
            bests.set(index, (bests.get(index) * (currentRun - 1) + getBestDurationTime() ) / currentRun );
            average.set(index, (average.get(index) * (currentRun - 1) + getAverageDurationTime() ) / currentRun );
            worsts.set(index, (worsts.get(index) * (currentRun - 1) + getWorstDurationTime() ) / currentRun );
        }
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
                " useHTML: true, " +
                " text: 'parameters: N = " + populationSize +
                ", G = " + maxNumberOfGenerations +
                ", T = " + tournamentSize +
                ", p<sub>c</sub> = " + crossoverProbability +
                ", p<sub>s</sub> = " + selectionProbability +
                ", p<sub>m</sub> = " + mutationProbability + " '\n" +
                "    },");

        JSONdata.add("yAxis: {\n" +
                "        title: {\n" +
                "            text: 'Schedule Duration of the Individual'\n" +
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
                        "            pointStart: 1\n" +
                        "        }\n" +
                        "    },");
    }

    private void modifyJSONresults()
    {
        JSONresults.add("series: [{");
        JSONresults.add("name: 'Best',");
        JSONresults.add("data: [" + + bests.get(0) + "]");
        JSONresults.add(" }, {");
        JSONresults.add("name: 'Average',");
        JSONresults.add("data: [" + average.get(0) + "]");
        JSONresults.add(" }, {");
        JSONresults.add("name: 'Worst',");
        JSONresults.add("data: [" + worsts.get(0) + "]");
        JSONresults.add(" }]");

        for (int index = 1; index < bests.size(); ++index)
        {
            JSONresults.set(2, JSONresults.get(2).replace("]", ", " + bests.get(index) + "]"));
            JSONresults.set(5, JSONresults.get(5).replace("]", ", " + average.get(index) + "]"));
            JSONresults.set(8, JSONresults.get(8).replace("]", ", " + worsts.get(index) + "]"));
        }
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
            ++GAEvaluation;
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
        Schedule s = generateRandomSchedule();

        int[] genes = new int[s.getTasks().length];
        int index = 0;
        for (Task t : s.getTasks())
            genes[index++] = t.getResourceId();

        BaseIntIndividual individual = new BaseIntIndividual(s, genes, new DurationEvaluator(s));
        individual.setDurationAndCost();
        individual.setNormalDurationAndCost();

        return individual;
    }

    private Schedule generateRandomSchedule()
    {
        Schedule s = new Schedule(schedule);

        Task[] tasks = s.getTasks();
        Random random = new Random();

        for (Task t : tasks)
        {
            List<Resource> capableResources = s.getCapableResources(t);
            int whichResource = random.nextInt(capableResources.size());
            s.assign(t, capableResources.get(whichResource));
        }

        greedy.buildTimestamps(s);

        return s;
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

    public int getRandomEvaluation()
    {
        return randomEvaluation;
    }

    public int getGAEvaluation()
    {
        return GAEvaluation;
    }


    private Schedule getBestRandomIndividual(int maxDrawns)
    {
        Schedule s;
        Schedule best = null;
        Integer minDuration = Integer.MAX_VALUE;
        randomEvaluation = 0;

        while(maxDrawns > 0)
        {
            s = generateRandomSchedule();
            s.setEvaluator(new DurationEvaluator(s));
            s.evaluate();
            ++randomEvaluation;

            if (s.getEvaluator().getDuration() < minDuration)
            {
                minDuration = s.getEvaluator().getDuration();
                best = s;
            }

            --maxDrawns;
        }

        return best;
    }
}
