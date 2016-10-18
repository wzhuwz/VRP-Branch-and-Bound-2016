package Main.AutomatedTests.Experiment1.ATC;

import Main.Algorithms.Heuristics.GA.GA1.GeneticAlgorithm;
import Main.GlobalVars;
import Main.Graph.Graph;
import Main.IOLoader.LoadRandomGraph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

/**
 * for running the algorithm
 */
public class ATCAutoTest {
    static final int testBatch = 5;

    public static void main(String[] args) throws FileNotFoundException {
        FileOutputStream fileOutputStream = new FileOutputStream(
                new File("resources/Experiments/Ex2/ex1-automated-test-results-ga1-tmp.csv"));
        PrintWriter out = new PrintWriter(fileOutputStream);

        double sumOfCosts = 0;
        double sumOfIterations = 0;
        double sumOfChromosomes = 0;
        String tableHeader = "ID,TestID,Customers,Vehicles,Cost,Iterations,ChromosomesQty";
        out.println(tableHeader);
        System.out.println(tableHeader);
        for (int id = 0; id < 100; id++) {
            int testId = id / testBatch;

            Graph originalGraph = LoadRandomGraph.loadWithDoubleParams(testId);

            // fill the global variables
            originalGraph.setIds();
            GlobalVars.setTheGlobalVariables(originalGraph);
            originalGraph.printVertices();
//            preprocessedGraph.printGraph();

            System.out.println("Test # " + id);
            System.out.println("Number of Customers, Vehicles: " +
                    GlobalVars.numberOfCustomers + " " + GlobalVars.numberOfVehicles);

            int geneticTime = 10000;

            // run the genetic algorithm
            GeneticAlgorithm geneticAlgorithm = new GeneticAlgorithm(
                    originalGraph, GlobalVars.numberOfCustomers, GlobalVars.numberOfVehicles, 40);
            geneticAlgorithm.run(geneticTime);
//            geneticAlgorithm.printBestChromosome();


            String iterations = "" + geneticAlgorithm.iterations;
            String chromosomeQty = "" + geneticAlgorithm.chromosomesQty;
            String cost = String.format("%.2f", geneticAlgorithm.getMinimumCost());

            String tableRow = String.format("%d,%d,%d,%d,%s,%s,%s", id, testId,
                    GlobalVars.numberOfCustomers, GlobalVars.numberOfVehicles, cost, iterations, chromosomeQty);

//            System.out.println(testInfo);
            System.out.println(tableHeader);
            System.out.println(tableRow);
            System.out.println("Optimal Value: " + cost);
            System.out.println("Total Calculation time: " + iterations + "s");
            System.out.println("-------------------------------------");

            out.println(tableRow);
            out.flush();


            sumOfCosts += geneticAlgorithm.getMinimumCost();
            sumOfChromosomes += geneticAlgorithm.chromosomesQty;
            sumOfIterations  += geneticAlgorithm.iterations;
            if ((id + 1) % testBatch == 0) {
                String averageRow = String.format("avg,%d,%d,%d,%s,%s,%s", testId,
                        GlobalVars.numberOfCustomers, GlobalVars.numberOfVehicles, sumOfCosts / testBatch,
                        sumOfIterations / testBatch, sumOfChromosomes / testBatch);
                System.out.println(averageRow);

                out.println(averageRow);
                out.flush();

                sumOfCosts = 0;
                sumOfChromosomes = 0;
                sumOfIterations = 0;
            }
        }
        out.close();
    }
}