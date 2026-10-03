package com.ridelink.farepayment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ridelink.farepayment.domain.FareCalculator;
import com.ridelink.farepayment.dto.FareEstimateRequest;
import com.ridelink.farepayment.error.NotFoundError;
import com.ridelink.farepayment.error.ValidationError;
import com.ridelink.farepayment.model.FareEstimate;
import com.ridelink.farepayment.repository.port.FareEstimateRepository;

@ExtendWith(MockitoExtension.class)
class FareServiceTest {
    @Mock FareEstimateRepository estimates;
    private FareService service;

    @BeforeEach
    void setUp() {
        service = new FareService(new FareCalculator(), estimates);
    }

    @Test
    void estimatesAndPersistsFare() {
        when(estimates.create(any(FareEstimate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var request = new FareEstimateRequest(UUID.randomUUID(), "1001", "A", "B",
            5, 10, new BigDecimal("1.5"));

        var result = service.estimateFare(request);

        assertEquals(105_000, result.estimatedFare());
        assertEquals("LKR", result.currency());
        assertEquals(105_000L, result.breakdown().get("totalFare"));
        verify(estimates).create(any(FareEstimate.class));
    }

    @Test
    void rejectsZeroDistanceAndNegativeDuration() {
        var request = new FareEstimateRequest(null, "1001", "A", "B", 0, 0, null);
        assertThrows(ValidationError.class, () -> service.estimateFare(request));
        var invalidDuration = new FareEstimateRequest(null, "1001", "A", "B", 1, -1, null);
        assertThrows(ValidationError.class, () -> service.estimateFare(invalidDuration));
    }

    @Test
    void looksUpEstimateOrThrowsNotFound() {
        UUID id = UUID.randomUUID();
        when(estimates.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundError.class, () -> service.getEstimateById(id));
    }
}
