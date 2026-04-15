import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Iterated Local Search (ILS) for the 0/1 Knapsack Problem
 * COS314 - Assignment 2
 *
 * HOW TO COMPILE:  javac ILS.java
 * HOW TO RUN:      java ILS
 *
 * At runtime the program will ask for:
 *   1. A seed value
 *   2. The path to the folder containing the problem instance files
 *      e.g.  C:\Users\you\Desktop\knapsack_data
 *        or  ./data
 *        or  .   (if the files are in the same folder as ILS.class)
 */
public class ILS {

    // ---------------------------------------------------------------
    // Problem data (populated when a file is loaded)
    // double is used throughout so decimal weights/values are handled
    // ---------------------------------------------------------------
    static int      numItems;    // n  – number of items
    static double   capacity;    // W  – knapsack weight limit (may be decimal)
    static double[] weights;     // w[i] – weight of item i   (may be decimal)
    static double[] values;      // v[i] – value  of item i   (may be decimal)

    // ---------------------------------------------------------------
    // ILS hyper-parameters  (tune these for your report)
    // ---------------------------------------------------------------
    static final int MAX_ITERATIONS    = 1000;  // outer ILS loop limit
    static final int PERTURBATION_BITS = 3;     // bits flipped during perturbation

    static Random rng;  // seeded random number generator

    // ===============================================================
    //  MAIN
    // ===============================================================
    public static void main(String[] args) throws IOException {

        Scanner sc = new Scanner(System.in);

        // --- 1. Ask for seed (required by assignment spec) ----------
        System.out.print("Enter seed value: ");
        long seed = sc.nextLong();
        rng = new Random(seed);

        // --- 2. Ask for the folder that holds the data files --------
        System.out.print("Enter path to problem instances folder: ");
        sc.nextLine(); // clear buffer after nextLong()
        String folder = sc.nextLine().trim();

        // Remove any trailing slash so File.separator works cleanly
        if (folder.endsWith("/") || folder.endsWith("\\")) {
            folder = folder.substring(0, folder.length() - 1);
        }

        // --- 3. Map display name -> filename on disk ----------------
        //  Column 0 : short name shown in the results table
        //  Column 1 : exact filename inside the folder you provided
        //  Update the filenames in column 1 if yours differ.
        String[][] instances = {
                { "f1_l-d_kp_10_269",   "f1_l-d_kp_10_269"   },
                { "f2_l-d_kp_20_878",   "f2_l-d_kp_20_878"   },
                { "f3_l-d_kp_4_20",     "f3_l-d_kp_4_20"     },
                { "f4_l-d_kp_4_11",     "f4_l-d_kp_4_11"     },
                { "f5_l-d_kp_15_375",   "f5_l-d_kp_15_375"   },
                { "f6_l-d_kp_10_60",    "f6_l-d_kp_10_60"    },
                { "f7_l-d_kp_7_50",     "f7_l-d_kp_7_50"     },
                { "f8_l-d_kp_23_10000", "f8_l-d_kp_23_10000" },
                { "f9_l-d_kp_5_80",     "f9_l-d_kp_5_80"     },
                { "f10_l-d_kp_20_879",  "f10_l-d_kp_20_879"  }
        };

        // --- 4. Print results table header -------------------------
        System.out.printf("%n%-25s %-10s %-15s %-12s%n",
                "Problem Instance", "Algorithm", "Best Solution", "Runtime(s)");
        System.out.println("-".repeat(65));

        // --- 5. Solve each instance --------------------------------
        for (String[] entry : instances) {
            String displayName = entry[0];
            String fullPath    = folder + File.separator + entry[1];

            // Check the file exists before trying to load it
            File f = new File(fullPath);
            if (!f.exists()) {
                System.out.printf("%-25s  [FILE NOT FOUND: %s]%n", displayName, fullPath);
                continue;
            }

            loadInstance(fullPath);
            solveAndPrint(displayName, seed);
        }
    }

    // ===============================================================
    //  FILE LOADER
    //  Expected format (typical Pisinger-style knapsack file):
    //      <numItems> <capacity>
    //      <value1> <weight1>
    //      <value2> <weight2>
    //      ...
    //  Adjust the parsing below if your files use a different layout.
    // ===============================================================
    static void loadInstance(String filepath) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filepath));

        // First line: number of items and capacity
        StringTokenizer st = new StringTokenizer(br.readLine());
        numItems = Integer.parseInt(st.nextToken());
        capacity = Double.parseDouble(st.nextToken());

        weights = new double[numItems];
        values  = new double[numItems];

        // Remaining lines: one item per line (value then weight)
        for (int i = 0; i < numItems; i++) {
            st = new StringTokenizer(br.readLine());
            values[i]  = Double.parseDouble(st.nextToken());
            weights[i] = Double.parseDouble(st.nextToken());
        }
        br.close();
    }

    // ===============================================================
    //  SOLVE AND PRINT  - runs ILS and prints one result row
    // ===============================================================
    static void solveAndPrint(String instanceName, long seed) {
        long startTime = System.currentTimeMillis();

        int[] bestSolution = iteratedLocalSearch();

        long   endTime    = System.currentTimeMillis();
        double runtimeSec = (endTime - startTime) / 1000.0;
        double bestValue  = fitness(bestSolution);

        System.out.printf("%-25s %-10s %-15.2f %-12.3f%n",
                instanceName, "ILS", bestValue, runtimeSec);
    }

    // ===============================================================
    //  ITERATED LOCAL SEARCH  - top-level ILS loop
    // ===============================================================
    static int[] iteratedLocalSearch() {

        // Step 1: Generate a random valid initial solution
        int[] currentSolution = generateRandomSolution();

        // Step 2: Improve it with local search
        currentSolution = localSearch(currentSolution);

        int[] bestSolution = currentSolution.clone();

        // Step 3: Main ILS loop
        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {

            // 3a. Perturb to escape the current local optimum
            int[] perturbedSolution = perturb(currentSolution);

            // 3b. Apply local search from the perturbed point
            int[] newSolution = localSearch(perturbedSolution);

            // 3c. Acceptance criterion - accept if equal or better
            if (fitness(newSolution) >= fitness(currentSolution)) {
                currentSolution = newSolution;
            }

            // 3d. Update the global best
            if (fitness(currentSolution) > fitness(bestSolution)) {
                bestSolution = currentSolution.clone();
            }
        }

        return bestSolution;
    }

    // ===============================================================
    //  LOCAL SEARCH  - bit-flip hill climber
    //  Tries flipping each bit one at a time.
    //  Keeps the flip only if it strictly improves fitness.
    //  Repeats until a full pass produces no improvement (local optimum).
    // ===============================================================
    static int[] localSearch(int[] solution) {
        int[]   current  = solution.clone();
        boolean improved = true;

        while (improved) {
            improved = false;

            for (int i = 0; i < numItems; i++) {
                current[i] = 1 - current[i];          // flip bit i

                if (fitness(current) > fitness(solution)) {
                    solution = current.clone();        // accept
                    improved = true;
                } else {
                    current[i] = 1 - current[i];      // reject - flip back
                }
            }
        }

        return current;
    }

    // ===============================================================
    //  PERTURBATION  - randomly flip PERTURBATION_BITS distinct bits
    //  This "kick" moves ILS away from a local optimum.
    // ===============================================================
    static int[] perturb(int[] solution) {
        int[]        perturbed = solution.clone();
        Set<Integer> flipped   = new HashSet<>();

        while (flipped.size() < PERTURBATION_BITS) {
            int idx = rng.nextInt(numItems);
            if (!flipped.contains(idx)) {
                perturbed[idx] = 1 - perturbed[idx];
                flipped.add(idx);
            }
        }

        return perturbed;
    }

    // ===============================================================
    //  RANDOM SOLUTION GENERATOR
    //  Shuffles item indices and greedily adds items until full.
    //  Guarantees the returned solution never exceeds capacity.
    // ===============================================================
    static int[] generateRandomSolution() {
        int[]   solution    = new int[numItems];
        double  totalWeight = 0.0;

        Integer[] indices = new Integer[numItems];
        for (int i = 0; i < numItems; i++) indices[i] = i;
        List<Integer> indexList = Arrays.asList(indices);
        Collections.shuffle(indexList, rng);

        for (int idx : indexList) {
            if (totalWeight + weights[idx] <= capacity) {
                solution[idx] = 1;
                totalWeight  += weights[idx];
            }
        }

        return solution;
    }

    //  FITNESS FUNCTION
    //  Returns total value of the solution.
    //  Returns 0.0 if total weight exceeds capacity (invalid solution).
    static double fitness(int[] solution) {
        double totalWeight = 0.0;
        double totalValue  = 0.0;

        for (int i = 0; i < numItems; i++) {
            if (solution[i] == 1) {
                totalWeight += weights[i];
                totalValue  += values[i];
            }
        }

        return (totalWeight <= capacity) ? totalValue : 0.0;
    }
}