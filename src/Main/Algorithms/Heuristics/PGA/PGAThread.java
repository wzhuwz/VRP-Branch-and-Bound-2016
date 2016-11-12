package Main.Algorithms.Heuristics.PGA;

import Main.Algorithms.Heuristics.GA.GA2.GeneticAlgorithm;
import Main.GlobalVars;
import Main.Graph.Graph;


public class PGAThread extends Thread {
    String threadName;
    Integer computeDuration;
    GeneticAlgorithm geneticAlgorithm;

    public PGAThread(Graph graph, int populationSize, int computeDuration, String threadName) {
        this.threadName = threadName;
        this.computeDuration = computeDuration;
        this.geneticAlgorithm = new GeneticAlgorithm(graph,
                GlobalVars.numberOfCustomers, 2, populationSize);
    }

    @Override
    public void run() {
//        geneticAlgorithm.run(computeDuration);
    }

    public double getResult(){
        return geneticAlgorithm.getMinimumCost();
    }
}