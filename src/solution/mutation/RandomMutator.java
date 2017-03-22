package solution.mutation;

import msrcpsp.scheduling.*;

import java.util.List;

/**
 * Created by annam on 22.03.2017.
 */
public class RandomMutator extends BaseMutator
{
    public RandomMutator(double mutationProbability)
    {
        super(mutationProbability);
    }

    @Override
    public BaseIntIndividual mutate(BaseIntIndividual individual)
    {
        int[] genes = individual.getGenes();

        for (int index = 0; index < genes.length; ++index)
        {
            if (random.nextDouble() <= mutationProbability)
                genes[index] = assignRandomResource(individual, index);
        }

        individual.setGenes(genes);

        return individual;
    }

    private int assignRandomResource(BaseIntIndividual individual, int taskIndex)
    {
        Schedule schedule = individual.getSchedule();
        Task task = schedule.getTasks()[taskIndex];

        List<Resource> resources = schedule.getCapableResources(task);
        Resource selectedResource = resources.get(random.nextInt(resources.size()));

        schedule.assign(task, selectedResource);

        return selectedResource.getId();
    }
}
