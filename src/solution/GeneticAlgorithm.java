package solution;

import msrcpsp.io.MSRCPSPIO;
import msrcpsp.scheduling.Schedule;

/**
 * Created by annam on 13.03.2017.
 */
public class GeneticAlgorithm
{
    private int maxNumberOfGenerations = 0;
    private Schedule schedule;

    public static void main(String[] args)
    {
        MSRCPSPIO reader = new MSRCPSPIO();
        Schedule s = reader.readDefinition("../resources/def_small/10_3_5_3.def");
        GeneticAlgorithm ga = new GeneticAlgorithm(s, 100);
        ga.start();
    }

    public GeneticAlgorithm(Schedule schedule, int maxNumberOfGenerations)
    {
        this.schedule = schedule;
        this.maxNumberOfGenerations = maxNumberOfGenerations;
    }

    public boolean start()
    {
        int currentGeneration = 1;

        while (currentGeneration <= maxNumberOfGenerations)
        {
            selectIndividuals();
            performCrossover();
            performMutation();
            System.out.println(currentGeneration);
            ++currentGeneration;
        }

        return true;
    }

    private void selectIndividuals()
    {

    }

    private void performCrossover()
    {

    }

    private void performMutation()
    {

    }
}
