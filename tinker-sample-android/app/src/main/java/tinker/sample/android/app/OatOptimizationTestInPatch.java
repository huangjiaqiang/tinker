package tinker.sample.android.app;

import android.content.Context;
import android.widget.Toast;

public class OatOptimizationTestInPatch
{

    public static void test(Context context) {
        int iterations = 1_000_000;

        // Warm up the function to allow for JIT/OAT optimizations
        warmUp(iterations);

        // Measure the time taken for the function
        long startTime = System.nanoTime();
        performComputation(iterations);
        long endTime = System.nanoTime();

        String msg = "4Execution time in patch: " + (endTime - startTime) + " ns";
        System.out.println(msg);
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * Perform warm-up to allow optimizations to take effect.
     */
    private static void warmUp(int iterations) {
        for (int i = 0; i < 10; i++) {
            performComputation(iterations);
        }
    }

    /**
     * A simple computational function for testing.
     */
    private static void performComputation(int iterations) {
        long result = 0;
        for (int i = 0; i < iterations; i++) {
            result += i * i; // Example of computational work
        }
    }
}