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

        System.out.println("\n" + "=".repeat(85));
        System.out.printf("%-25s | %-10s | %-15s | %-10s%n", "Problem Instance", "Seed", "Best Solution", "Runtime(s)");
        System.out.println("-".repeat(85));

        for (File file : listOfFiles) {
            if (file.isDirectory() || file.getName().startsWith(".")) continue;

            try {
                KnapsackData data = readInstance(file.getPath());

                long startTime = System.currentTimeMillis();
                GeneticAlgorithm ga = new GeneticAlgorithm(data.items, data.capacity, seed);
                int result = ga.runAndReturnBest();
                long endTime = System.currentTimeMillis();

                double duration = (endTime - startTime) / 1000.0;
                System.out.printf("%-25s | %-10d | %-15d | %-10.3f%n",
                        file.getName(), seed, result, duration);

            } catch (Exception e) {
                System.out.printf("%-25s | Error: %s%n", file.getName(), e.getMessage());
            }
        }
        System.out.println("=".repeat(85));
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

        // read everything as a double to handle potential decimals
        while (fileScanner.hasNext()) {
            if (fileScanner.hasNextDouble()) {
                allNumbers.add(fileScanner.nextDouble());
            } else {
                fileScanner.next(); // Skip non-numeric junk
            }
        }
        fileScanner.close();

        if (allNumbers.size() < 2) throw new Exception("Insufficient data in file");

        // cast to int for logic
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

