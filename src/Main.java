import java.io.File;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        Scanner inputScanner = new Scanner(System.in);
        System.out.print("Enter seed value: ");
        if (!inputScanner.hasNextLong()) return;
        long seed = inputScanner.nextLong();

        File folder = new File("data");
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null || listOfFiles.length == 0) {
            System.out.println("Error: No files found in 'data' folder.");
            return;
        }

        Arrays.sort(listOfFiles);

        int w = 100;
        System.out.println("\n" + "=".repeat(w));
        System.out.printf("%-26s | %-5s | %-10s | %-15s | %-10s%n",
                "Problem Instance", "Algo", "Seed", "Best Solution", "Runtime(s)");
        System.out.println("-".repeat(w));

        for (File file : listOfFiles) {
            if (file.isDirectory() || file.getName().startsWith(".")) continue;

            String name = file.getName();

            try {
                KnapsackData data = readInstance(file.getPath());

                // ---- ILS ----
                ILS.numItems = data.items.size();
                ILS.capacity = data.capacity;
                ILS.weights = new double[data.items.size()];
                ILS.values = new double[data.items.size()];
                for (int i = 0; i < data.items.size(); i++) {
                    ILS.weights[i] = data.items.get(i).weight;
                    ILS.values[i] = data.items.get(i).value;
                }
                ILS.rng = new Random(seed);

                long ilsStart = System.currentTimeMillis();
                int[] ilsBest = ILS.iteratedLocalSearch();
                long ilsEnd = System.currentTimeMillis();
                double ilsValue = ILS.fitness(ilsBest);
                double ilsRuntime = (ilsEnd - ilsStart) / 1000.0;

                // ---- GA ----
                long gaStart = System.currentTimeMillis();
                GeneticAlgorithm ga = new GeneticAlgorithm(data.items, data.capacity, seed);
                int gaResult = ga.runAndReturnBest();
                long gaEnd = System.currentTimeMillis();
                double gaRuntime = (gaEnd - gaStart) / 1000.0;

                // ---- Print side by side ----
                System.out.printf("%-26s | %-5s | %-10d | %-15.2f | %-10.3f%n",
                        name, "ILS", seed, ilsValue, ilsRuntime);
                System.out.printf("%-26s | %-5s | %-10d | %-15.2f | %-10.3f%n",
                        name, "GA", seed, (double) gaResult, gaRuntime);
                System.out.println("-".repeat(w));

            } catch (Exception e) {
                System.out.printf("%-26s | Error: %s%n", name, e.getMessage());
            }
        }

        System.out.println("=".repeat(w));
    }

    static class KnapsackData {
        int capacity;
        List<Item> items;

        KnapsackData(int c, List<Item> i) {
            this.capacity = c;
            this.items = i;
        }
    }

    public static KnapsackData readInstance(String filePath) throws Exception {
        Scanner fileScanner = new Scanner(new File(filePath));
        fileScanner.useLocale(Locale.US);
        List<Double> allNumbers = new ArrayList<>();

        while (fileScanner.hasNext()) {
            if (fileScanner.hasNextDouble()) {
                allNumbers.add(fileScanner.nextDouble());
            } else {
                fileScanner.next();
            }
        }
        fileScanner.close();

        if (allNumbers.size() < 2) throw new Exception("Insufficient data in file");

        int numItems = allNumbers.get(0).intValue();
        int capacity = allNumbers.get(1).intValue();
        List<Item> items = new ArrayList<>();
        int numbersRemaining = allNumbers.size() - 2;

        if (numbersRemaining >= numItems * 3) {
            for (int i = 0; i < numItems; i++) {
                int base = 2 + (i * 3);
                int val = allNumbers.get(base + 1).intValue();
                int weight = allNumbers.get(base + 2).intValue();
                items.add(new Item(weight, val));
            }
        } else {
            for (int i = 0; i < numItems; i++) {
                int base = 2 + (i * 2);
                if (base + 1 < allNumbers.size()) {
                    int val = allNumbers.get(base).intValue();
                    int weight = allNumbers.get(base + 1).intValue();
                    items.add(new Item(weight, val));
                }
            }
        }
        return new KnapsackData(capacity, items);
    }
}