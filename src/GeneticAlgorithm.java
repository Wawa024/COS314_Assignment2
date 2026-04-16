import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GeneticAlgorithm {
    private final int populationSize = 200;
    private final double mutationRate = 0.02;
    private final double crossoverRate = 0.85;
    private final int generations = 5000;

    private final List<Item> items;
    private final int maxCapacity;
    private final Random random;

    public GeneticAlgorithm(List<Item> items, int maxCapacity, long seed) {
        this.items = items;
        this.maxCapacity = maxCapacity;
        this.random = new Random(seed);
    }

    private List<boolean[]> initPopulation() {
        List<boolean[]> population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            boolean[] individual = new boolean[items.size()];

            if (items.size() > 0) {
                individual[random.nextInt(items.size())] = true;
            }
            population.add(individual);
        }
        return population;
    }

    public int calculateFitness(boolean[] individual) {
        int totalWeight = 0;
        int totalValue = 0;
        for (int i = 0; i < individual.length; i++) {
            if (individual[i]) {
                totalWeight += items.get(i).weight;
                totalValue += items.get(i).value;
            }
        }
        return (totalWeight > maxCapacity) ? 0 : totalValue;
    }

    private boolean[] selectParent(List<boolean[]> population) {
        boolean[] best = null;
        for (int i = 0; i < 3; i++) {
            boolean[] contender = population.get(random.nextInt(populationSize));
            if (best == null || calculateFitness(contender) > calculateFitness(best)) {
                best = contender;
            }
        }
        return best;
    }

    private boolean[][] crossover(boolean[] p1, boolean[] p2) {
        if (random.nextDouble() > crossoverRate || items.size() < 2) {
            return new boolean[][]{p1.clone(), p2.clone()};
        }
        int split = random.nextInt(items.size());
        boolean[] c1 = new boolean[items.size()];
        boolean[] c2 = new boolean[items.size()];
        for (int i = 0; i < items.size(); i++) {
            if (i < split) {
                c1[i] = p1[i]; c2[i] = p2[i];
            } else {
                c1[i] = p2[i]; c2[i] = p1[i];
            }
        }
        return new boolean[][]{c1, c2};
    }

    private void mutate(boolean[] individual) {
        for (int i = 0; i < individual.length; i++) {
            if (random.nextDouble() < mutationRate) {
                individual[i] = !individual[i];
            }
        }
    }

    public int runAndReturnBest() {
        if (items.isEmpty()) return 0;
        List<boolean[]> population = initPopulation();
        int absoluteBest = 0;

        for (int g = 0; g < generations; g++) {
            List<boolean[]> nextGen = new ArrayList<>();
            while (nextGen.size() < populationSize) {
                boolean[] p1 = selectParent(population);
                boolean[] p2 = selectParent(population);
                boolean[][] children = crossover(p1, p2);
                mutate(children[0]);
                mutate(children[1]);
                nextGen.add(children[0]);
                nextGen.add(children[1]);
            }
            population = nextGen;


            for (boolean[] ind : population) {
                absoluteBest = Math.max(absoluteBest, calculateFitness(ind));
            }
        }
        return absoluteBest;
    }
}