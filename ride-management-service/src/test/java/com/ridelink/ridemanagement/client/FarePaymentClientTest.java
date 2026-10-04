package com.ridelink.ridemanagement.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class FarePaymentClientTest {
    @Test
    void springUsesConfiguredConstructorWhenClientHasMultipleConstructors() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getBeanFactory().setConversionService(
                    org.springframework.boot.convert.ApplicationConversionService.getSharedInstance());
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test",
                    java.util.Map.of("fare.payment.service.url", "http://fare-payment-service:3004")));
            context.registerBean(RestClient.Builder.class, () -> RestClient.builder());
            context.register(FarePaymentClient.class);
            context.refresh();
            assertNotNull(context.getBean(FarePaymentClient.class));
        }
    }

    @Test
    void retrievesRidePaymentFromFareServiceAndForwardsCallerToken() {
        UUID rideId = UUID.randomUUID();
        String token = "Bearer passenger-token";
        RestClient.Builder builder = RestClient.builder().baseUrl("http://fare-payment-service:3004");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://fare-payment-service:3004/api/v1/payments/ride/" + rideId))
                .andExpect(method(HttpMethod.GET)).andExpect(header("Authorization", token))
                .andRespond(withSuccess("[{\"rideId\":\"" + rideId + "\",\"amount\":50000,"
                        + "\"currency\":\"LKR\",\"status\":\"SUCCESS\",\"paymentMethod\":\"CASH\","
                        + "\"transactionRef\":\"RIDE-event\",\"createdAt\":\"2026-10-03T12:00:00\"}]",
                        MediaType.APPLICATION_JSON));

        FarePaymentClient client = new FarePaymentClient(builder.build());
        var payment = client.getByRideId(rideId, token).getFirst();

        assertEquals(rideId, payment.rideId());
        assertEquals(50000, payment.amount());
        assertEquals("SUCCESS", payment.status());
        server.verify();
    }
}
