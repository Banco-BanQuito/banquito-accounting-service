package ec.edu.espe.banquito.accountservice.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class AccountingGrpcServer {

    private final int port;
    private final AccountingGrpcService accountingGrpcService;
    private Server server;

    public AccountingGrpcServer(
            @Value("${accounting.grpc.port:9092}") int port,
            AccountingGrpcService accountingGrpcService) {
        this.port = port;
        this.accountingGrpcService = accountingGrpcService;
    }

    @PostConstruct
    public void start() throws IOException {
        server = ServerBuilder
                .forPort(port)
                .addService(accountingGrpcService)
                .build()
                .start();
    }

    @PreDestroy
    public void stop() {
        if (server != null) {
            server.shutdown();
            try {
                server.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                server.shutdownNow();
            }
        }
    }
}
