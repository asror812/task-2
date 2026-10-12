import java.math.BigDecimal;
import java.util.Scanner;

public class Main1 {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        long value = sc.nextLong();
        long value2 = sc.nextLong();


        long startTime = System.currentTimeMillis();
        try {
            BigDecimal bigDecimal = new BigDecimal(value);
            BigDecimal bigDecimal2 = new BigDecimal(value2);

            BigDecimal value3 = bigDecimal.add(bigDecimal2);
            System.out.println(value3.longValue());
        } catch (ArithmeticException e) {
            System.err.println("Arithmetic exception occurred: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Execution time: " + (endTime - startTime) + " ms");
        System.out.println("Execution time in seconds: " + (endTime - startTime) / 1000.0);
    }
}
