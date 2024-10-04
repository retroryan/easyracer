import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.*;
import java.util.Scanner;

// Note: Intentionally, code is not shared across scenarios

public class Main {



    void main() throws URISyntaxException, ExecutionException, InterruptedException {

        var scenarios = new ScopedValuesScenarios(new URI("http://localhost:8090"));


        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter 0 to run all scenarios or enter a specific scenario number (1-10) to test a specific scenario:");
        int choice = scanner.nextInt();

        switch (choice) {
            case 0 -> scenarios.results().forEach(System.out::println);
            case 1 -> System.out.println(scenarios.scenario1());
            case 2 -> System.out.println(scenarios.scenario2());
            case 3 -> System.out.println(scenarios.scenario3());
            case 4 -> System.out.println(scenarios.scenario4());
            case 5 -> System.out.println(scenarios.scenario5());
            case 6 -> System.out.println(scenarios.scenario6());
            case 7 -> System.out.println(scenarios.scenario7());
            case 8 -> System.out.println(scenarios.scenario8());
            case 9 -> System.out.println(scenarios.scenario9());
            case 10 -> System.out.println(scenarios.scenario10());
            default -> System.out.println("Invalid choice. Please enter a number between 0 and 10.");
        }
        scanner.close();
    }
}