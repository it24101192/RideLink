package com.ridelink.farepayment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.ridelink.farepayment.config.OpenApiConfig;
import com.ridelink.farepayment.config.SecurityConfig;
import com.ridelink.farepayment.dto.FareEstimateDto;
import com.ridelink.farepayment.dto.PaymentDto;
import com.ridelink.farepayment.dto.ReceiptDto;
import com.ridelink.farepayment.error.ApiExceptionHandler;
import com.ridelink.farepayment.error.DuplicatePaymentError;
import com.ridelink.farepayment.error.NotFoundError;
import com.ridelink.farepayment.error.RideNotCompletedError;
import com.ridelink.farepayment.error.RideServiceUnavailableError;
import com.ridelink.farepayment.model.PaymentMethod;
import com.ridelink.farepayment.model.PaymentStatus;
import com.ridelink.farepayment.security.RequestLoggingFilter;
import com.ridelink.farepayment.service.FareService;
import com.ridelink.farepayment.service.PaymentService;

@WebMvcTest(controllers = {FareController.class, PaymentController.class})
@Import({
    SecurityConfig.class,
    OpenApiConfig.class,
    ApiExceptionHandler.class,
    RequestLoggingFilter.class,
})
class FarePaymentApiWebTest {

    private static final UUID USER_ID =
        UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");

    private static final UUID OTHER_ID =
        UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");

    private static final UUID ENTITY_ID =
        UUID.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccccc");

    private static final UUID RIDE_ID =
        UUID.fromString("dddddddd-dddd-4ddd-8ddd-dddddddddddd");

    private static final String ESTIMATE_JSON = """
        {"pickupLocation":"A","destinationLocation":"B","distanceKm":5.0,"durationMinutes":10}
        """;

    private static final String PAYMENT_JSON = """
        {"rideId":"dddddddd-dddd-4ddd-8ddd-dddddddddddd","passengerId":"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
         "driverId":"eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee","actualDistanceKm":5.0,"actualDurationMinutes":10,
         "paymentMethod":"SIMULATED_CARD","transactionRef":"txn-123"}
        """;

    @Autowired
    MockMvc mvc;

    @MockitoBean
    FareService fareService;

    @MockitoBean
    PaymentService paymentService;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @TestConfiguration
    static class TestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Test
    void createEstimateReturns201AndJson() throws Exception {
        when(fareService.estimateFare(any())).thenReturn(estimate(USER_ID.toString()));

        mvc.perform(request(HttpMethod.POST, "/api/v1/fares/estimate")
                .with(asUser("PASSENGER", USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(ESTIMATE_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estimatedFare").value(30000));
    }

    @Test
    void estimateValidationReturns400() throws Exception {
        mvc.perform(request(HttpMethod.POST, "/api/v1/fares/estimate")
                .with(asUser("PASSENGER", USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"pickupLocation\":\"\",\"destinationLocation\":\"B\",\"distanceKm\":1,\"durationMinutes\":0}"
                ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    void getEstimateReturns200AndMissingEstimateReturns404() throws Exception {
        when(fareService.getEstimateById(ENTITY_ID))
            .thenReturn(estimate(USER_ID.toString()));

        mvc.perform(request(HttpMethod.GET, "/api/v1/fares/" + ENTITY_ID)
                .with(asUser("PASSENGER", USER_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.passengerId").value(USER_ID.toString()));

        when(fareService.getEstimateById(ENTITY_ID))
            .thenThrow(new NotFoundError("Fare estimate not found"));

        mvc.perform(request(HttpMethod.GET, "/api/v1/fares/" + ENTITY_ID)
                .with(asUser("PASSENGER", USER_ID)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void createPaymentReturns201AndJson() throws Exception {
        when(paymentService.processPayment(any(), anyString()))
            .thenReturn(payment(USER_ID.toString()));

        mvc.perform(request(HttpMethod.POST, "/api/v1/payments")
                .with(asUser("PASSENGER", USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(PAYMENT_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void negativeDistanceReturns422AndUnknownRideMapsTo404() throws Exception {
        mvc.perform(request(HttpMethod.POST, "/api/v1/payments")
                .with(asUser("PASSENGER", USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(PAYMENT_JSON.replace("5.0", "-1.0")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        when(paymentService.processPayment(any(), anyString()))
            .thenThrow(new NotFoundError("Ride not found"));

        mvc.perform(request(HttpMethod.POST, "/api/v1/payments")
                .with(asUser("PASSENGER", USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(PAYMENT_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.message").value("Ride not found"));
    }

    @Test
    void incompleteRideAndDuplicatePaymentReturn409() throws Exception {
        doThrow(
                new RideNotCompletedError(
                    "Ride must be in COMPLETED state before payment"
                )
            ).when(paymentService).processPayment(any(), anyString());

        mvc.perform(request(HttpMethod.POST, "/api/v1/payments")
                .with(asUser("PASSENGER", USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(PAYMENT_JSON))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("RIDE_NOT_COMPLETED"));

        doThrow(
                new DuplicatePaymentError(
                    "A successful payment already exists for this ride"
                )
            ).when(paymentService).processPayment(any(), anyString());

        mvc.perform(request(HttpMethod.POST, "/api/v1/payments")
                .with(asUser("PASSENGER", USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(PAYMENT_JSON))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("DUPLICATE_PAYMENT"));
    }

    @Test
    void forcedPaymentDeclineReturns402WithFailureReason() throws Exception {
        PaymentDto declined = new PaymentDto(
            ENTITY_ID,
            RIDE_ID,
            null,
            USER_ID.toString(),
            OTHER_ID.toString(),
            30_000,
            "LKR",
            PaymentStatus.FAILED,
            PaymentMethod.SIMULATED_CARD,
            "test-0000",
            "Simulated card decline: transaction reference ends with 0000",
            null,
            null
        );

        when(paymentService.processPayment(any(), anyString())).thenReturn(declined);

        String failingRequest =
            PAYMENT_JSON.replace("txn-123", "test-0000");

        mvc.perform(request(HttpMethod.POST, "/api/v1/payments")
                .with(asUser("PASSENGER", USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(failingRequest))
            .andExpect(status().is(402))
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.failureReason")
                .value("Simulated card decline: transaction reference ends with 0000"));
    }

    @Test
    void rideServiceFailureMapsTo503() throws Exception {
        when(paymentService.processPayment(any(), anyString()))
            .thenThrow(
                new RideServiceUnavailableError(
                    "Ride Management Service is unavailable"
                )
            );

        mvc.perform(request(HttpMethod.POST, "/api/v1/payments")
                .with(asUser("PASSENGER", USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(PAYMENT_JSON))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.error.code")
                .value("RIDE_SERVICE_UNAVAILABLE"));
    }

    @Test
    void getPaymentReturns200AndMissingPaymentReturns404() throws Exception {
        when(paymentService.getPaymentById(ENTITY_ID))
            .thenReturn(payment(USER_ID.toString()));

        mvc.perform(request(HttpMethod.GET, "/api/v1/payments/" + ENTITY_ID)
                .with(asUser("PASSENGER", USER_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.amount").value(30000));

        when(paymentService.getPaymentById(ENTITY_ID))
            .thenThrow(new NotFoundError("Payment not found"));

        mvc.perform(request(HttpMethod.GET, "/api/v1/payments/" + ENTITY_ID)
                .with(asUser("PASSENGER", USER_ID)))
            .andExpect(status().isNotFound());
    }

    @Test
    void getPaymentsByRideReturns200AndMissingRideReturns404() throws Exception {
        when(paymentService.getPaymentByRideId(eq(RIDE_ID), anyString()))
            .thenReturn(List.of(payment(USER_ID.toString())));

        mvc.perform(request(
                HttpMethod.GET,
                "/api/v1/payments/ride/" + RIDE_ID
            ).with(asUser("PASSENGER", USER_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].rideId").value(RIDE_ID.toString()));

        when(paymentService.getPaymentByRideId(eq(RIDE_ID), anyString()))
            .thenThrow(new NotFoundError("Ride not found"));

        mvc.perform(request(
                HttpMethod.GET,
                "/api/v1/payments/ride/" + RIDE_ID
            ).with(asUser("PASSENGER", USER_ID)))
            .andExpect(status().isNotFound());
    }

    @Test
    void receiptReturns200AndMissingPaymentReturns404() throws Exception {
        when(paymentService.getReceiptByPaymentId(ENTITY_ID))
            .thenReturn(receipt(USER_ID.toString()));

        mvc.perform(request(
                HttpMethod.GET,
                "/api/v1/payments/" + ENTITY_ID + "/receipt"
            ).with(asUser("PASSENGER", USER_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentId").value(ENTITY_ID.toString()));

        when(paymentService.getReceiptByPaymentId(ENTITY_ID))
            .thenThrow(new NotFoundError("Payment not found"));

        mvc.perform(request(
                HttpMethod.GET,
                "/api/v1/payments/" + ENTITY_ID + "/receipt"
            ).with(asUser("PASSENGER", USER_ID)))
            .andExpect(status().isNotFound());
    }

    @Test
    void refundReturns200AndMapsMissingPayment() throws Exception {
        when(paymentService.refundPayment(
                ENTITY_ID, "duplicate", "ADMIN"
            )).thenReturn(payment(USER_ID.toString()));

        mvc.perform(request(
                HttpMethod.POST,
                "/api/v1/payments/" + ENTITY_ID + "/refund"
            )
                .with(asUser("ADMIN", USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"duplicate\"}"))
            .andExpect(status().isOk());

        when(paymentService.refundPayment(
                ENTITY_ID, "duplicate", "ADMIN"
            )).thenThrow(new NotFoundError("Payment not found"));

        mvc.perform(request(
                HttpMethod.POST,
                "/api/v1/payments/" + ENTITY_ID + "/refund"
            )
                .with(asUser("ADMIN", USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"duplicate\"}"))
            .andExpect(status().isNotFound());

        mvc.perform(request(
                HttpMethod.POST,
                "/api/v1/payments/" + ENTITY_ID + "/refund"
            )
                .with(asUser("ADMIN", USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\" \"}"))
            .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("routes")
    void everyEndpointRequiresAuthentication(
            String method,
            String path
    ) throws Exception {

        mvc.perform(request(HttpMethod.valueOf(method), path))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @ParameterizedTest
    @MethodSource("routes")
    void everyEndpointRejectsUnapprovedRole(
            String method,
            String path
    ) throws Exception {

        mvc.perform(
                request(HttpMethod.valueOf(method), path)
                    .with(asUser("AUDITOR", USER_ID))
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void resourceOwnershipIsEnforced() throws Exception {
        when(fareService.getEstimateById(ENTITY_ID))
            .thenReturn(estimate(OTHER_ID.toString()));

        mvc.perform(
                request(
                    HttpMethod.GET,
                    "/api/v1/fares/" + ENTITY_ID
                ).with(asUser("PASSENGER", USER_ID))
            )
            .andExpect(status().isForbidden());

        when(paymentService.getPaymentById(ENTITY_ID))
            .thenReturn(payment(OTHER_ID.toString()));

        mvc.perform(
                request(
                    HttpMethod.GET,
                    "/api/v1/payments/" + ENTITY_ID
                ).with(asUser("PASSENGER", USER_ID))
            )
            .andExpect(status().isForbidden());
    }

    static Stream<String[]> routes() {
        return Stream.of(
            new String[]{"POST", "/api/v1/fares/estimate"},
            new String[]{"GET", "/api/v1/fares/" + ENTITY_ID},
            new String[]{"POST", "/api/v1/payments"},
            new String[]{"GET", "/api/v1/payments/" + ENTITY_ID},
            new String[]{"GET", "/api/v1/payments/ride/" + RIDE_ID},
            new String[]{"GET", "/api/v1/payments/" + ENTITY_ID + "/receipt"},
            new String[]{"POST", "/api/v1/payments/" + ENTITY_ID + "/refund"}
        );
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor asUser(
            String role,
            UUID userId
    ) {
        return jwt()
            .jwt(token -> token
                .subject(userId.toString())
                .claim("userId", userId.toString())
                .claim("role", role))
            .authorities(
                new SimpleGrantedAuthority("ROLE_" + role)
            );
    }

    private static FareEstimateDto estimate(String passenger) {
        return new FareEstimateDto(
            ENTITY_ID,
            null,
            passenger,
            "A",
            "B",
            new BigDecimal("5.0"),
            10,
            BigDecimal.ONE,
            30_000,
            java.util.Map.of("totalFare", 30_000),
            null,
            "LKR"
        );
    }

    private static PaymentDto payment(String passenger) {
        return new PaymentDto(
            ENTITY_ID,
            RIDE_ID,
            null,
            passenger,
            OTHER_ID.toString(),
            30_000,
            "LKR",
            PaymentStatus.SUCCESS,
            PaymentMethod.SIMULATED_CARD,
            "txn-123",
            null,
            null,
            null
        );
    }

    private static ReceiptDto receipt(String passenger) {
        return new ReceiptDto(
            ENTITY_ID,
            RIDE_ID,
            passenger,
            OTHER_ID.toString(),
            30_000,
            "LKR",
            PaymentStatus.SUCCESS,
            PaymentMethod.SIMULATED_CARD,
            "txn-123",
            null
        );
    }
}
