package solution.crossover;

import msrcpsp.evaluation.DurationEvaluator;
import msrcpsp.scheduling.*;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Created by annam on 22.03.2017.
 */
public class SinglePointCrossover extends BaseCrossover
{
    public SinglePointCrossover()
    {}

    @Override
    public BaseIntIndividual crossIndividuals(BaseIntIndividual individual1, BaseIntIndividual individual2)
    {
        int crossoverPoint = random.nextInt(individual1.getGenes().length);

        int[] genes;
        if (random.nextDouble() < 0.5)
            genes = getCrossedGenes(individual1.getGenes(), individual2.getGenes(), crossoverPoint);
        else
            genes = getCrossedGenes(individual2.getGenes(), individual1.getGenes(), crossoverPoint);

        return generateIndividual(individual1.getSchedule(), genes);
    }

    private BaseIntIndividual generateIndividual(Schedule s, int[] genes)
    {
        Schedule schedule = new Schedule(s);

        int index = 0;
        for (Task t : schedule.getTasks())
            schedule.assign(t.getId(), genes[index++], -1);

        return new BaseIntIndividual(schedule, genes, new DurationEvaluator(schedule));
    }

    private int[] getCrossedGenes(int[] genes1, int[] genes2, int crossoverPoint)
    {
        int[] firstSlice = Arrays.copyOfRange(genes1, 0, crossoverPoint);
        int[] secondSlice = Arrays.copyOfRange(genes2, crossoverPoint, genes2.length);

        return IntStream.concat(Arrays.stream(firstSlice), Arrays.stream(secondSlice)).toArray();
    }
}
