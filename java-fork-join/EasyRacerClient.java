import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class EasyRacerScenario1 {
    private static final String BASE_URL = "http://example.com";

    public static void main(String[] args) {
        ForkJoinPool pool = new ForkJoinPool();
        String result = pool.invoke(new RaceTask("/1", "/1"));
        System.out.println("Winner: " + result);
    }

    static class RaceTask extends RecursiveTask<String> {
        private final String path1;
        private final String path2;

        RaceTask(String path1, String path2) {
            this.path1 = path1;
            this.path2 = path2;
        }

        @Override
        protected String compute() {
            RaceTask task1 = new RaceTask(path1, null);
            RaceTask task2 = new RaceTask(path2, null);

            task1.fork();
            String result2 = task2.compute();
            String result1 = task1.join();

            return result1 != null ? result1 : result2;
        }

        private String makeRequest(String path) {
            try {
                URL url = new URL(BASE_URL + path);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == 200) {
                    // In a real scenario, we'd read the response body here
                    return "right";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
