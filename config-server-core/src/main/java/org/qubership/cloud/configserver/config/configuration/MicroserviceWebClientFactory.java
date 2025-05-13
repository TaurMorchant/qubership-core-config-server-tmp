package org.qubership.cloud.configserver.config.configuration;

import org.qubership.cloud.restclient.MicroserviceRestClient;
import org.qubership.cloud.restclient.MicroserviceRestClientFactory;
import org.qubership.cloud.restclient.webclient.MicroserviceWebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.util.retry.Retry;

import java.time.Duration;

public class MicroserviceWebClientFactory implements MicroserviceRestClientFactory {
    private final int maxIdleTime;
    private final int pendingAcquireTimeout;
    private final int evictInBackground;

    public MicroserviceWebClientFactory(int maxIdleTime, int pendingAcquireTimeout, int evictInBackground) {
        this.maxIdleTime = maxIdleTime;
        this.pendingAcquireTimeout = pendingAcquireTimeout;
        this.evictInBackground = evictInBackground;
    }

    public MicroserviceRestClient create() {
        return new MicroserviceWebClient(HttpClient.create(connectionProviderWithTimeouts()))
                .withRetry(retryPolicy());
    }

    // retry on ServiceUnavailable AND GatewayTimeout
    private Retry retryPolicy() {
        return Retry.backoff(2, Duration.ofSeconds(7))
                .filter(this::isRetryableException)
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure());
    }

    private boolean isRetryableException(Throwable throwable) {
        return throwable instanceof WebClientResponseException.ServiceUnavailable
                || throwable instanceof WebClientResponseException.GatewayTimeout;
    }

    private ConnectionProvider connectionProviderWithTimeouts() {
        return ConnectionProvider.builder("cs-connection-provider")
                .maxIdleTime(Duration.ofSeconds(this.maxIdleTime))
                .pendingAcquireTimeout(Duration.ofSeconds(this.pendingAcquireTimeout))
                .evictInBackground(Duration.ofSeconds(this.evictInBackground)).build();
    }
}