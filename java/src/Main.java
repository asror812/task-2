import java.math.BigDecimal;
import java.util.Scanner;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        long value = input.nextLong();
        long value2 = input.nextLong();

        BigDecimal bigDecimal = new BigDecimal(value);
        BigDecimal bigDecimal2 = new BigDecimal(value2);

        long startTime = System.currentTimeMillis();
        BigDecimal result = bigDecimal.add(bigDecimal2);
        System.out.println(result.longValue());
        long endTime = System.currentTimeMillis();


        System.out.println("Execution time: " + (endTime - startTime) + " ms");
        System.out.println("Execution time in seconds: " + (endTime - startTime) / 1000.0);
    }
}