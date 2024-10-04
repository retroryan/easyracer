import com.sun.management.OperatingSystemMXBean;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.concurrent.TimeoutException;

public class ScopedValuesScenarios {
    private final URI url;
    private final HttpClient client;
    private static final Logger LOGGER = Logger.getLogger(ScopedValuesScenarios.class.getName());

    public ScopedValuesScenarios(URI url) {
        this.url = url;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    private static final ScopedValue<String> OPERATION_ID = ScopedValue.newInstance();

    //Add logging using the OPERATION_ID to demonstrate scoped values
    private String sendRequest(HttpRequest req) throws Exception {
        LOGGER.info("Sending request for operation: " + OPERATION_ID.get());
        HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private String sendRequestWithStatusCheck(HttpRequest req) throws Exception {
        LOGGER.info("Sending request with status check for operation: " + OPERATION_ID.get());
        HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new RuntimeException("Unexpected status code: " + response.statusCode());
        }
    }


  /**
     * Race 2 concurrent requests. and the winner is the first request to return a 200 response with a body containing right
     * <p>
     * Use ShutdownOnSuccess -A StructuredTaskScope that captures the result of the first subtask to complete successfully.
     * Once captured, it shuts down the task scope to interrupt unfinished threads and wakeup the task scope owner.
     * The policy implemented by this class is intended for cases where the result of any subtask will do ("invoke any")
     * and where the results of other unfinished subtasks are no longer needed.
     */
    public String scenario1() {
        LOGGER.info("Calling method: scenario1");
        try {
            return ScopedValue.where(OPERATION_ID, "SCENARIO_1").call(this::runScenario1);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unhandled error in scenario1", e);
            return "Unhandled error occurred";
        }
    }

    private String runScenario1() throws InterruptedException, ExecutionException {
        var req = HttpRequest.newBuilder(url.resolve("/1")).build();
        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<String>()) {
            scope.fork(() -> sendRequest(req));
            scope.fork(() -> sendRequest(req));
            scope.join();
            return scope.result();
        }
    }

    public String scenario2() {
        LOGGER.info("Calling method: scenario2");
        try {
            return ScopedValue.where(OPERATION_ID, "SCENARIO_2").call(this::runScenario2);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unhandled error in scenario2", e);
            return "Unhandled error occurred";
        }
    }

    /**
     * Race 2 concurrent requests, where one produces a connection error
     * The winner returns a 200 response with a body containing right
     *
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private String runScenario2() throws InterruptedException, ExecutionException {
        var req = HttpRequest.newBuilder(url.resolve("/2")).build();
        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<String>()) {
            scope.fork(() -> sendRequest(req));
            scope.fork(() -> sendRequest(req));
            scope.join();
            return scope.result();
        }
    }

    public String scenario3() {
        LOGGER.info("Calling method: scenario3");
        try {
            return ScopedValue.where(OPERATION_ID, "SCENARIO_3").call(this::runScenario3);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unhandled error in scenario3", e);
            return "Unhandled error occurred";
        }
    }

    /**
     * Race 10,000 concurrent requests
     * The winner returns a 200 response with a body containing right
     *
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private String runScenario3() throws InterruptedException, ExecutionException {
        var req = HttpRequest.newBuilder(url.resolve("/3")).build();
        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<String>()) {
            IntStream.range(0, 10_000)
                    .forEach(i -> scope.fork(() -> sendRequest(req)));
            scope.join();
            return scope.result();
        }
    }


    public String scenario4() {
        LOGGER.info("Calling method: scenario4");
        try {
            return ScopedValue.where(OPERATION_ID, "SCENARIO_4").call(this::runScenario4);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unhandled error in scenario4", e);
            return "Unhandled error occurred";
        }
    }

    /**
     * Race 2 concurrent requests but 1 of them should have a 1 second timeout
     * The winner returns a 200 response with a body containing right
     *
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private String runScenario4() throws InterruptedException, ExecutionException {
        var req = HttpRequest.newBuilder(url.resolve("/4")).build();
        try (var outer = new StructuredTaskScope.ShutdownOnSuccess<String>()) {
            outer.fork(() -> {
                try (var inner = new StructuredTaskScope.ShutdownOnSuccess<String>()) {
                    inner.fork(() -> sendRequest(req));
                    inner.joinUntil(Instant.now().plusSeconds(1));
                    return inner.result();
                }
            });

            outer.fork(() -> sendRequest(req));

            outer.join();

            return outer.result();
        }
    }

    public String scenario5() {
        LOGGER.info("Calling method: scenario5");
        try {
            return ScopedValue.where(OPERATION_ID, "SCENARIO_5").call(this::runScenario5);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unhandled error in scenario5", e);
            return "Unhandled error occurred";
        }
    }

    /**
     * Race 2 concurrent requests where a non-200 response is a loser
     * The winner returns a 200 response with a body containing right
     *
     * @return
     */
    private String runScenario5() throws InterruptedException, ExecutionException {
        var req = HttpRequest.newBuilder(url.resolve("/5")).build();
        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<String>()) {
            scope.fork(() -> sendRequestWithStatusCheck(req));
            scope.fork(() -> sendRequestWithStatusCheck(req));
            scope.join();
            return scope.result();
        }
    }

    public String scenario6() {
        LOGGER.info("Calling method: scenario6");
        try {
            return ScopedValue.where(OPERATION_ID, "SCENARIO_6").call(this::runScenario6);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unhandled error in scenario6", e);
            return "Unhandled error occurred";
        }
    }

    /**
     * Race 3 concurrent requests where a non-200 response is a loser
     * The winner returns a 200 response with a body containing right
     *
     * @return
     */
    private String runScenario6() throws InterruptedException, ExecutionException {
        var req = HttpRequest.newBuilder(url.resolve("/6")).build();
        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<String>()) {
            IntStream.range(0, 3)
                    .forEach(i -> scope.fork(() -> sendRequestWithStatusCheck(req)));
            scope.join();
            return scope.result();
        }
    }


    public String scenario7() {
        LOGGER.info("Calling method: scenario7");
        try {
            return ScopedValue.where(OPERATION_ID, "SCENARIO_7").call(this::runScenario7);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unhandled error in scenario7", e);
            return "Unhandled error occurred";
        }
    }

    /**
     * Start a request, wait at least 3 seconds then start a second request (hedging)
     * The winner returns a 200 response with a body containing right
     */
    private String runScenario7() throws InterruptedException, ExecutionException {
        var req = HttpRequest.newBuilder(url.resolve("/7")).build();
        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<String>()) {
            // Start the first request
            scope.fork(() -> {
                LOGGER.info("Starting first request");
                return sendRequest(req);
            });

            // Start the second request (hedge)
            scope.fork(() -> {

                // Wait for 3 seconds before starting the second request
                Thread.sleep(3000);

                LOGGER.info("Starting second request (hedge)");
                return sendRequest(req);
            });

            // Wait for either request to complete
            scope.join();
            return scope.result();
        }
    }

    //Scenario 8 - resource management
    private static final ScopedValue<String> RESOURCE_NAME = ScopedValue.newInstance();
    private static final ScopedValue<String> RESOURCE_ID = ScopedValue.newInstance();

    public String scenario8() {
        LOGGER.info("Calling method: scenario8");
        try {
            String scenario8 = ScopedValue.where(OPERATION_ID, "SCENARIO_8").call(this::runScenario8);
            return scenario8;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unhandled error in scenario8", e);
            return "Unhandled error occurred";
        }
    }

    /**
     * Race 2 concurrent requests that "use" a resource which is obtained and released through other requests.
     * The "use" request can return a non-20x request, in which case it is not a winner.
     * <p>
     * GET /8?open
     * GET /8?use=<id obtained from open request>
     * GET /8?close=<id obtained from open request>
     * The winner returns a 200 response with a body containing right
     *
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private String runScenario8() throws InterruptedException, ExecutionException {
        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<String>()) {
            scope.fork(() -> ScopedValue.where(RESOURCE_NAME, "Resource 1").call(() -> {
                return ScopedValue.callWhere(RESOURCE_ID, openResource(), () -> {
                    try (var req = new Req()) {
                        return req.make();
                    }
                });
            }));
            scope.fork(() -> ScopedValue.where(RESOURCE_NAME, "Resource 2").call(() -> {
                return ScopedValue.callWhere(RESOURCE_ID, openResource(), () -> {
                    try (var req = new Req()) {
                        return req.make();
                    }
                });
            }));
            scope.join();
            return scope.result();
        }
    }

    private String openResource() throws Exception {
        LOGGER.info("Opening resource for operation id " + OPERATION_ID.get() + " resource name: " + RESOURCE_NAME.get());
        HttpRequest openReq = HttpRequest.newBuilder(url.resolve("/8?open")).build();
        return sendRequest(openReq);
    }

    class Req implements AutoCloseable {
        final Function<String, HttpRequest> useReq = (id) ->
                HttpRequest.newBuilder(url.resolve("/8?use=" + id)).build();
        final Function<String, HttpRequest> closeReq = (id) ->
                HttpRequest.newBuilder(url.resolve("/8?close=" + id)).build();

        public Req() {
            // The resourceID is now a scoped value, so we don't need to set it here
            LOGGER.info("Req instance created for resource ID: " + RESOURCE_ID.get());
        }

        String make() throws Exception {
            LOGGER.info("Using resource " + RESOURCE_ID.get() + " for " + RESOURCE_NAME.get());
            return sendRequestWithStatusCheck(useReq.apply(RESOURCE_ID.get()));
        }

        @Override
        public void close() throws Exception {
            LOGGER.info("Closing resource " + RESOURCE_ID.get() + " for " + RESOURCE_NAME.get());
            sendRequest(closeReq.apply(RESOURCE_ID.get()));
        }
    }


    //Scenario 9
    private static final ScopedValue<Integer> REQUEST_NUMBER = ScopedValue.newInstance();

    private record TimedResponse(Instant time, String response, int requestNumber) {
    }

    public String scenario9() {
        LOGGER.info("Calling method: scenario9");
        try {
            String scenario9 = ScopedValue.where(OPERATION_ID, "SCENARIO_9").call(this::runScenario9);
            return scenario9;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unhandled error in scenario9", e);
            return "Unhandled error occurred";
        }
    }

    /**
     * Make 10 concurrent requests where 5 return a 200 response with a letter
     * When assembled in order of when they responded, form the "right" answer
     *
     * @return
     * @throws InterruptedException
     */
    private String runScenario9() throws InterruptedException {
        List<TimedResponse> responses = new ArrayList<>();

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            List<StructuredTaskScope.Subtask<TimedResponse>> tasks = IntStream.range(0, 10)
                    .mapToObj(i -> scope.fork(() ->
                            executeRequest(i + 1))
                    ).toList();
            scope.join();
            scope.throwIfFailed();
            processTaskResults(tasks, responses);
        } catch (ExecutionException e) {
            LOGGER.warning("One or more requests failed: " + e.getMessage());
        }

        return assembleResult(responses);
    }

    private TimedResponse executeRequest(int requestNumber) throws Exception {
        return ScopedValue.where(REQUEST_NUMBER, requestNumber).call(() -> {
            HttpRequest request = HttpRequest.newBuilder(url.resolve("/9")).build();
            try {
                String response = sendRequestWithStatusCheck(request);
                return new TimedResponse(Instant.now(), response, REQUEST_NUMBER.get());
            } catch (RuntimeException e) {
                LOGGER.info(() -> String.format("Non-200 status received for request %d: %s", REQUEST_NUMBER.get(), e.getMessage()));
                return new TimedResponse(Instant.now(), null, REQUEST_NUMBER.get());
            }
        });
    }

    private void processTaskResults(List<StructuredTaskScope.Subtask<TimedResponse>> tasks, List<TimedResponse> responses) {
        for (var task : tasks) {
            try {
                TimedResponse result = task.get();
                if (result.response() != null) {
                    responses.add(result);
                }
            } catch (Exception e) {
                LOGGER.warning("Failed to get task result: " + e.getMessage());
            }
        }
    }

    private String assembleResult(List<TimedResponse> responses) {
        String result = responses.stream()
                .sorted(Comparator.comparing(TimedResponse::time))
                .map(tr -> {
                    LOGGER.info(() -> String.format("Request %d responded with: %s", tr.requestNumber(), tr.response()));
                    return tr.response();
                })
                .reduce("", String::concat);

        LOGGER.info("Final assembled result: " + result);
        return result;
    }


    public String scenario10() throws InterruptedException {
        var id = UUID.randomUUID().toString();

        Supplier<String> blocker = () -> {
            try (var scope = new StructuredTaskScope.ShutdownOnSuccess<HttpResponse<String>>()) {
                var req = HttpRequest.newBuilder(url.resolve(STR."/10?\{id}")).build();
                var messageDigest = MessageDigest.getInstance("SHA-512");

                scope.fork(() -> client.send(req, HttpResponse.BodyHandlers.ofString()));
                scope.fork(() -> {
                    var result = new byte[512];
                    new Random().nextBytes(result);
                    while (!Thread.interrupted())
                        result = messageDigest.digest(result);
                    return null;
                });
                scope.join();
                return scope.result().body();
            } catch (ExecutionException | InterruptedException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        };

        class Recursive<I> {
            public I func;
        }

        Recursive<Supplier<String>> recursive = new Recursive<>();
        recursive.func = () -> {
            var osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
            var load = osBean.getProcessCpuLoad() * osBean.getAvailableProcessors();
            var req = HttpRequest.newBuilder(url.resolve(STR."/10?\{id}=\{load}")).build();
            try {
                var resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                if ((resp.statusCode() >= 200) && (resp.statusCode() < 300)) {
                    return resp.body();
                } else if ((resp.statusCode() >= 300) && (resp.statusCode() < 400)) {
                    Thread.sleep(1000);
                    return recursive.func.get();
                } else {
                    throw new RuntimeException(resp.body());
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        };

        try (var scope = new StructuredTaskScope<String>()) {
            scope.fork(blocker::get);
            var task = scope.fork(recursive.func::get);
            scope.join();
            return task.get();
        }
    }

    List<String> results() throws ExecutionException, InterruptedException {
        //return List.of(scenario1(), scenario2(), scenario3(), scenario4(), scenario5(), scenario6(), scenario7(), scenario8(), scenario9());
        return List.of(scenario1(), scenario2(), scenario4(), scenario5(), scenario6(), scenario7(), scenario8(), scenario9());
        //return List.of(scenario10());
    }
}