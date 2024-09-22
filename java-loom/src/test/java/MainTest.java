import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class MainTest {

    @Test
    public void testScenarios() throws URISyntaxException, ExecutionException, InterruptedException {
        try (GenericContainer<?> scenarioServer = new GenericContainer<>(DockerImageName.parse("ghcr.io/jamesward/easyracer"))) {
            scenarioServer.withExposedPorts(8080).waitingFor(new HttpWaitStrategy()).start();

            var url = new URI("http://" + scenarioServer.getHost() + ":" + scenarioServer.getFirstMappedPort());
            var scenarios = new OriginalScenarios(url);
            assertThat(scenarios.results(), CoreMatchers.everyItem(equalTo("right")));
        }
    }
}
