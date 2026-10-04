package com.ridelink.farepayment.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import com.ridelink.farepayment.error.RideServiceUnavailableError;

class RideServiceRestClientTest {
    private static final UUID RIDE_ID = UUID.fromString("dddddddd-dddd-4ddd-8ddd-dddddddddddd");
    private static final String RIDE_URL = "http://ride-service/api/rides/" + RIDE_ID;
    private static final String TOKEN = "Bearer integration-test-token";

    @Test
    void loadsTypedRideFromTheRideManagementContractAndForwardsBearerToken() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ride-service");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(RIDE_URL)).andExpect(method(HttpMethod.GET))
            .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.header("Authorization", TOKEN))
            .andRespond(withSuccess("{\"id\":\"" + RIDE_ID + "\",\"status\":\"COMPLETED\","
                    + "\"passengerId\":\"1001\",\"driverId\":\"2001\"}",
                MediaType.APPLICATION_JSON));
        var client = new RideServiceRestClient(builder.build(), 2, Duration.ZERO);

        var ride = client.getRide(RIDE_ID, TOKEN).orElseThrow();

        assertEquals(RIDE_ID, ride.id());
        assertTrue(ride.isCompleted());
        assertEquals("1001", ride.passengerId());
        assertEquals("1001", ride.passengerId());
        server.verify();
    }

    @Test
    void maps404ToNoRideWithoutRetrying() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ride-service");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(RIDE_URL)).andRespond(withStatus(HttpStatus.NOT_FOUND));
        var client = new RideServiceRestClient(builder.build(), 2, Duration.ZERO);

        assertTrue(client.getRide(RIDE_ID, TOKEN).isEmpty());
        server.verify();
    }

    @Test
    void retriesTransientServiceErrorsTwiceThenRaisesUnavailable() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ride-service");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(times(3), requestTo(RIDE_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        var client = new RideServiceRestClient(builder.build(), 2, Duration.ZERO);

        assertThrows(RideServiceUnavailableError.class, () -> client.getRide(RIDE_ID, TOKEN));
        server.verify();
    }
}
