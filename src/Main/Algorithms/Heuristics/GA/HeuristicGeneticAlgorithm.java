package Main.Algorithms.Heuristics.GA;

import Main.Algorithms.Heuristics.DispatchingRules.RankingIndex;
import Main.Algorithms.Other.Random;
import Main.Algorithms.Other.RollingWheel;
import Main.Algorithms.TSP.SimpleTSP.SimpleTSP;
import Main.GlobalVars;
import Main.Graph.Graph;
import Main.Graph.Vertex;

import java.util.*;


/**
 * An Implementation of GA Algorithm used for
 * calculating an upper bound for our problem (VRPD)
 * Created by iman on 7/27/16.
 */
public class HeuristicGeneticAlgorithm {

    private Graph graph;
    private int vehicleQty;
    private int customerQty;
    private int populationSize;

    private double averageOfMaxGains;
    private double totalProcessTimes;

    private List<Chromosome> population;
    public double maximumCost;

    public Chromosome bestChromosome;
    private final double MUTATION_PROBABILITY = GeneticConfigs.MUTATION_PROBABILITY;
    private final double CROSSOVER_PROBABILITY = GeneticConfigs.CROSSOVER_PROBABILITY;

    private final int TOURNAMENT_SIZE = GeneticConfigs.TOURNAMENT_SIZE;
    private final int NUMBER_OF_DISPATCHING_RULES = RankingIndex.NUMBER_OF_RULES;
    private final boolean IS_VERBOSE = true;

    private final boolean IS_DEBUG_MODE = false;
    private int depotId;

    private long printTimeStepSize;
    public long chromosomesQty = 0;

    public long iterations = 0;
    private long startTime = 0;
    private long finishTime = 0;


    /**
     * Constructor Creates a population with given qty
     */
    public HeuristicGeneticAlgorithm(Graph graph, int customerQty, int vehicleQty) {
        this.graph = graph;
        this.vehicleQty = vehicleQty;
        this.customerQty = customerQty;
        this.maximumCost = -GlobalVars.INF;
        this.population = new ArrayList<>();

        this.depotId = graph.getDepotId();
        this.printTimeStepSize = GlobalVars.printTimeStepSize;

        for (Vertex v : graph.getCustomerVertices()) {
            this.totalProcessTimes += v.processTime;
            this.averageOfMaxGains += v.maximumGain;
        }
        this.averageOfMaxGains /= customerQty;
    }

    /**
     * run The algorithm to the given time
     *
     * @param computeDurationMilliSecond is how much time can be consumed
     */
    public void run(int computeDurationMilliSecond, int maxIterationsNoUpdate, int iterationsLimit, int populationSize) {
        if (IS_VERBOSE) {
            GlobalVars.log.println(GlobalVars.equalsLine);
            GlobalVars.log.println("\t\t\t\t\t\t\t\t\tGenetic algorithm");
            GlobalVars.log.println(GlobalVars.equalsLine);
        }

        this.populationSize = populationSize;
        this.startTime = System.currentTimeMillis();

        long printTime = startTime + printTimeStepSize;
        long iterationsNoUpdate = 0;

        initializePopulation();
        while (System.currentTimeMillis() < startTime + computeDurationMilliSecond) {
            List<Chromosome> newPopulation = new ArrayList<>();
            Set<Chromosome> newPopulationSet = new HashSet<>();

            // cross over
            while (newPopulation.size() < 2 * populationSize) {
                Collections.shuffle(population);
                for (int i = 0; i < populationSize; i += 2 * TOURNAMENT_SIZE) {
                    Chromosome c1 = tournament(population, i, i + TOURNAMENT_SIZE);
                    Chromosome c2 = tournament(population, i + TOURNAMENT_SIZE, i + TOURNAMENT_SIZE * 2);

                    if (getRandom0to1() < CROSSOVER_PROBABILITY) {
                        Chromosome nc1 = crossOver(c1, c2);
                        Chromosome nc2 = crossOver(c2, c1);

                        if (!newPopulationSet.contains(nc1)) {
                            newPopulation.add(nc1);
                            newPopulationSet.add(nc1);
                        }

                        if (!newPopulationSet.contains(nc2)) {
                            newPopulation.add(nc2);
                            newPopulationSet.add(nc2);
                        }
                        chromosomesQty += 2;
                    } else {
                        if (!newPopulationSet.contains(c1)) {
                            newPopulation.add(c1);
                            newPopulationSet.add(c1);
                        }

                        if (!newPopulationSet.contains(c2)) {
                            newPopulation.add(c2);
                            newPopulationSet.add(c2);
                        }
                    }
                }
            }

            // mutate
            for (Chromosome c1 : population) mutate(c1);

            // selection
            population = selection(newPopulation);

//            for (Chromosome c : population) {
//                System.out.println(c);
//            }

            // update best answer
            if (population.get(0).getCost() > maximumCost) {
                maximumCost = population.get(0).getCost();
                bestChromosome = population.get(0);
                iterationsNoUpdate = 0;
            }

            // print the progress
            if (IS_VERBOSE && System.currentTimeMillis() > printTime) {
                printTime += printTimeStepSize;
                GlobalVars.log.printf("Iteration #%d,\tTime elapsed: %.2fs,\tChromosomesQty: %d,\tMinimum Cost: %.2f\n",
                        iterations, (System.currentTimeMillis() - startTime) / 1000., chromosomesQty, maximumCost);
            }

            if (iterationsNoUpdate > maxIterationsNoUpdate) break;
            if (iterations > iterationsLimit) break;

            iterations++;
            iterationsNoUpdate++;
        }

        finishTime = System.currentTimeMillis();
    }

    /**
     * Runs the genetic algorithm with Genetic Configs Class attributes
     */
    public void runUsingConfigFile() {
        this.run(
                GeneticConfigs.COMPUTE_DURATION_MILLISECONDS,
                GeneticConfigs.MAX_ITERATIONS_NO_UPDATE,
                GeneticConfigs.ITERATIONS_LIMIT,
                GeneticConfigs.POPULATION_SIZE
        );
    }

    /**
     * initializes the population by shuffling the ids of nodes
     */
    public void initializePopulation() {
        for (int i = 0; i < populationSize; i++) {
            population.add(getRandomChromosome());
            chromosomesQty++;
        }
    }

    /**
     * returns a chromosome that satisfies the capacity constraint.
     */
    public Chromosome getRandomChromosome() {
        Chromosome newChromosome = new Chromosome();

        for (int i = 0; i < vehicleQty; i++)
            newChromosome.vehiclesRules.add(Random.getRandomIntInRange(new Random.IRange(1, RankingIndex.NUMBER_OF_RULES)));

        heuristicCustomerVehicleFill(newChromosome);
        heuristicOrderAcceptanceFill(newChromosome);

        return newChromosome;
    }

    public void heuristicCustomerVehicleFill(Chromosome newChromosome) {
        List<Integer> vehicles = new ArrayList<>();
        for (int j = 0; j < vehicleQty; j++) vehicles.add(j);

        int[] remainedCapacity = new int[vehicleQty];
        Arrays.fill(remainedCapacity, GlobalVars.depot.capacity);

        for (int i = 0; i < customerQty; i++) {
            double[] vehicleProbabilities = new double[vehicleQty];
            for (int vId = 0; vId < vehicleQty; vId++) {
                if (remainedCapacity[vId] <= 0) {
                    vehicleProbabilities[vId] = 0;
                } else {
                    double sumOfDistance = 0, count = 0;
                    for (int j = 0; j < newChromosome.customersVehicle.size(); j++) {
                        if (newChromosome.customersVehicle.get(j) != vId) continue;

                        count += 1;
                        sumOfDistance += graph.getDistance(i, j);
                    }

                    if (count != 0) {
                        vehicleProbabilities[vId] = sumOfDistance / count; // average distance
                    }

                    if (count == 0) {
                        int sumOfDistancesToRemainingCustomers = 0;
                        int countOfRemainingCustomers = 0;
                        for (int j = i + 1; j < customerQty; j++) {
                            countOfRemainingCustomers++;
                            sumOfDistance += graph.getDistance(i, j);
                        }

                        if (count != 0)
                            vehicleProbabilities[vId] = sumOfDistancesToRemainingCustomers / countOfRemainingCustomers;
                        if (count == 0) vehicleProbabilities[vId] = graph.getOverallAverageDistance();
                    }
                }
            }
            int selectedVId = RollingWheel.run(vehicleProbabilities);

            remainedCapacity[selectedVId]--;
            newChromosome.customersVehicle.add(selectedVId);
        }
    }

    public void heuristicOrderAcceptanceFill(Chromosome newChromosome) {
        for (int i = 0; i < customerQty; i++) {
            Vertex v = graph.getVertexById(i);
            double[] acceptanceProbabilities = new double[]{averageOfMaxGains, v.maximumGain};
            newChromosome.orderAcceptance.add(RollingWheel.run(acceptanceProbabilities));
        }
    }

    public Chromosome tournament(List<Chromosome> population, int begin, int end) {
        Chromosome bestChromosome = null;
        double bestValue = -GlobalVars.INF - 1e-9;
        for (int i = begin; i < end; i++)
            if (population.get(i).getCost() > bestValue) {
                bestChromosome = population.get(i);
                bestValue = population.get(i).getCost();
            }

        if (bestValue <= -GlobalVars.INF - 1e-9) bestChromosome = population.get(begin);
        return bestChromosome;
    }

    /**
     * perform cross over on two chromosomes
     *
     * @param chromosome1 array of id of the nodes
     * @param chromosome2 array of id of the nodes
     * @return a new chromosome by crossover of two given chromosomes
     */
    public Chromosome crossOver(Chromosome chromosome1, Chromosome chromosome2) {
        Chromosome newChromosome = new Chromosome();
        Chromosome randomChromosome = getRandomChromosome();

        // for customer vehicles

        int p1 = getRandInt(customerQty);
        for (int i = 0; i < p1; i++) {
            int cvi = randomChromosome.customersVehicle.get(i);
            newChromosome.customersVehicle.add(cvi);
        }

        for (int i = p1; i < customerQty; i++) {
            int cvi = chromosome2.customersVehicle.get(i);
            newChromosome.customersVehicle.add(cvi);
        }

        // for order acceptance

        for (int i = 0; i < p1; i++) {
            int oai = randomChromosome.orderAcceptance.get(i);
            newChromosome.orderAcceptance.add(oai);
        }

        for (int i = p1; i < customerQty; i++) {
            int oai = chromosome2.orderAcceptance.get(i);
            newChromosome.orderAcceptance.add(oai);
        }

        // for vehicle rules

        int p2 = getRandInt(vehicleQty);
        for (int i = 0; i < p2; i++) {
            int vri = randomChromosome.vehiclesRules.get(i);
            newChromosome.vehiclesRules.add(vri);
        }

        for (int i = p2; i < vehicleQty; i++) {
            int vri = chromosome2.vehiclesRules.get(i);
            newChromosome.vehiclesRules.add(vri);
        }

        return newChromosome;
    }

    /**
     * perform mutation on a given chromosome
     *
     * @param chromosome array of id of the nodes
     */
    public void mutate(Chromosome chromosome) {
        if (getRandom0to1() > MUTATION_PROBABILITY)
            return;

        int p1 = getRandInt(customerQty);
        int p2 = getRandInt(customerQty);

        if (p1 > p2) {
            int t = p1;
            p1 = p2;
            p2 = t;
        }

        // for the customer vehicles

        int tmp = chromosome.customersVehicle.get(p1);
        chromosome.customersVehicle.set(p1, chromosome.customersVehicle.get(p2));
        chromosome.customersVehicle.set(p2, tmp);

        // for the order acceptance
        chromosome.orderAcceptance.set(p1, 1 - chromosome.orderAcceptance.get(p1));

        // for the vehicle rules

        p1 = getRandInt(vehicleQty);
        p2 = getRandInt(vehicleQty);

        if (p1 > p2) {
            int t = p1;
            p1 = p2;
            p2 = t;
        }

        tmp = chromosome.vehiclesRules.get(p1);
        chromosome.vehiclesRules.set(p1, chromosome.vehiclesRules.get(p2));
        chromosome.vehiclesRules.set(p2, tmp);

    }


    /**
     * Selects top chromosomes from old population and their children
     */
    public List<Chromosome> selection(List<Chromosome> chromosomes) {
        List<Chromosome> newPopulation = new ArrayList<>();

        // select top nodes
        Collections.sort(chromosomes);

        for (int i = 0; i < 7 * (populationSize / 10); i++) {
            newPopulation.add(chromosomes.get(i));
        }

        for (int i = chromosomes.size() - (3 * (populationSize / 10)); i < chromosomes.size(); i++) {
            newPopulation.add(chromosomes.get(i));
        }

        return newPopulation;
    }

    /**
     * @return minimum cost so far
     */
    public double getMaximumCost() {
        return maximumCost;
    }

    public double getElapsedTimeInSeconds() {
        return (finishTime - startTime) / 1000.0;
    }

    public String bestChromosomeString() {
        return ("Best Chromosome: " + bestChromosome
                + ", " + String.format("Cost: %.2f", maximumCost)
                + ", " + String.format("iterations: %d", iterations));
    }


    /**
     * @return a random number less than given bound
     */
    public int getRandInt(int bound) {
        return Random.getRandomIntInRange(new Random.IRange(0, bound - 1));
    }

    /**
     * @return a random number between 0, 1 for probability
     */
    public double getRandom0to1() {
        return getRandInt(1001) / 1000.;
    }

    /**
     * Chromosome class for a setCustomer of ids (for VRPD) problem
     */
    public class Chromosome implements Comparable<Chromosome> {
        public List<Integer> customersVehicle;
        public List<Integer> vehiclesRules;
        public List<Integer> orderAcceptance;

        private double cost;
        private double travelCost;
        private double penaltyCost;
        private double vehicleUsageCost;
        private double maxGainCost;

        private boolean isCostCalculated = false;

        /**
         * default constructor
         */
        public Chromosome() {
            vehiclesRules = new ArrayList<>();
            customersVehicle = new ArrayList<>();
            orderAcceptance = new ArrayList<>();
        }

        /**
         * copy constructor
         */
        public Chromosome(Chromosome chromosome) {
            this.customersVehicle = new ArrayList<>(chromosome.customersVehicle);
            this.vehiclesRules = new ArrayList<>(chromosome.vehiclesRules);
            this.orderAcceptance = new ArrayList<>(chromosome.orderAcceptance);
        }

        /**
         * order them by dispatching rules
         */
        List<Integer> orderThem1(List<Integer> customers, double previousProcessTimes, int indexType) {
            int servedNodesQty = 0;
            boolean[] isServed = new boolean[customers.size()];

            double sumOfProcessTimes = totalProcessTimes;

            double thisBatchProcessTimes = 0;
            for (Integer vId : customers)
                thisBatchProcessTimes += graph.getVertexById(vId).processTime;

            double arrivalTime = previousProcessTimes + thisBatchProcessTimes;

            Vertex u = graph.getDepot();
            List<Integer> orderedCustomers = new ArrayList<>();
            while (servedNodesQty < customers.size()) {

                int nextId = -1;
                Vertex next = null;
                double bestValue = -GlobalVars.INF;
                for (int i = 0; i < customers.size(); i++) {
                    if (isServed[i]) continue;
                    Vertex v = graph.getVertexById(customers.get(i));

                    double indexValue = -GlobalVars.INF;

                    switch (indexType) {
                        case 1:
                            indexValue = RankingIndex.getIndexValue1(v);
                            break;
                        case 2:
                            indexValue = RankingIndex.getIndexValue2(v);
                            break;
                        case 3:
                            indexValue = RankingIndex.getIndexValue3(v);
                            break;
                        case 4:
                            indexValue = RankingIndex.getIndexValue4(v);
                            break;
                        case 5:
                            indexValue = RankingIndex.getIndexValue5(v);
                            break;
                    }

                    if (indexValue > bestValue) {
                        bestValue = indexValue;
                        next = v;
                        nextId = i;
                    }
                }

                orderedCustomers.add(next.id);
                servedNodesQty++;
                isServed[nextId] = true;
                u = next;
            }

            return orderedCustomers;
        }

        /**
         * fitness function for this chromosome
         */
        public double getCost() {
            if (isCostCalculated == true)
                return cost;

            travelCost = 0;
            penaltyCost = 0;
            maxGainCost = 0;
            vehicleUsageCost = 0;

            if (this.orderAcceptance.toString().equals("[0, 1, 1, 0]")) {
                customersVehicle = customersVehicle;
            }

            List<Integer>[] batch = new ArrayList[vehicleQty];
            for (int i = 0; i < customersVehicle.size(); i++) {
                if (orderAcceptance.get(i) == 1) {
                    Vertex v = graph.getVertexById(i);
                    maxGainCost += v.maximumGain;

                    if (batch[customersVehicle.get(v.getId())] == null) {
                        batch[customersVehicle.get(v.getId())] = new ArrayList<>();
                    }
                    batch[customersVehicle.get(v.getId())].add(v.getId());
                }
            }


            double cumulativeProcessTime = 0;
            Vertex depot = graph.getVertexById(depotId);

            for (int i = 0; i < batch.length; i++) {
                if (batch[i] == null) continue;
                if (batch[i].size() == 0) continue;

                if (batch[i].size() > depot.capacity) {
                    this.cost = -GlobalVars.INF;
                    this.isCostCalculated = true;
                    return cost;
                }
                // batch[i] = orderThem(batch[i]);
                batch[i] = orderThem1(batch[i], cumulativeProcessTime, vehiclesRules.get(i));

                batch[i].add(depotId);
                for (int j = 0; j < batch[i].size(); j++) {
                    int vId = batch[i].get(j);
                    Vertex v = graph.getVertexById(vId);
                    cumulativeProcessTime += v.processTime;
                }

                SimpleTSP tsp = new SimpleTSP(graph, batch[i], cumulativeProcessTime);
                tsp.run();

                vehicleUsageCost -= depot.fixedCost;
                travelCost -= tsp.travelTime;
                penaltyCost -= tsp.penaltyTaken;
            }

            this.isCostCalculated = true;
            this.cost = maxGainCost + penaltyCost + vehicleUsageCost + travelCost;
            return cost;
        }

        public String detailedCostString() {
            return String.format("travelCost = %.1f; penaltyCost = %.1f; maxGainCost = %.1f; vehicleUsageCost = %.1f;",
                    travelCost, penaltyCost, maxGainCost, vehicleUsageCost);
        }

        @Override
        public int compareTo(Chromosome o) {
            return Double.compare(o.getCost(), this.getCost());
        }

        @Override
        public String toString() {
            return vehiclesRules.toString() + " " +
                    customersVehicle.toString() + " " +
                    orderAcceptance.toString();
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }
    }
}