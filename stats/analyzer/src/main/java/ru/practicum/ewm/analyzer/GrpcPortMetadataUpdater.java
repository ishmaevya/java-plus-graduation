package ru.practicum.ewm.analyzer;

import java.util.Map;
import net.devh.boot.grpc.server.event.GrpcServerStartedEvent;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaAutoServiceRegistration;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class GrpcPortMetadataUpdater {

    private final ObjectProvider<EurekaRegistration> registrationProvider;
    private final ObjectProvider<EurekaAutoServiceRegistration> autoRegistrationProvider;

    public GrpcPortMetadataUpdater(ObjectProvider<EurekaRegistration> registrationProvider,
                                   ObjectProvider<EurekaAutoServiceRegistration> autoRegistrationProvider) {
        this.registrationProvider = registrationProvider;
        this.autoRegistrationProvider = autoRegistrationProvider;
    }

    @EventListener
    public void update(GrpcServerStartedEvent event) {
        EurekaRegistration registration = registrationProvider.getIfAvailable();
        if (registration == null) {
            return;
        }
        Map<String, String> metadata = Map.of("gRPC_port", String.valueOf(event.getPort()));
        registration.getApplicationInfoManager().registerAppMetadata(metadata);
        EurekaAutoServiceRegistration autoRegistration = autoRegistrationProvider.getIfAvailable();
        if (autoRegistration != null) {
            autoRegistration.stop();
            autoRegistration.start();
        }
    }
}
