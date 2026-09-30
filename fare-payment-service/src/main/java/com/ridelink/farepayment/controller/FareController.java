package com.ridelink.farepayment.controller;

import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.ridelink.farepayment.dto.FareEstimateDto;
import com.ridelink.farepayment.dto.FareEstimateRequest;
import com.ridelink.farepayment.error.UnauthorizedError;
import com.ridelink.farepayment.security.AuthenticatedUser;
import com.ridelink.farepayment.service.FareService;

@RestController
@RequestMapping("/api/v1/fares")
@Tag(name = "Fares")
@SecurityRequirement(name = "bearerAuth")
public class FareController {
    private final FareService fareService;

    public FareController(FareService fareService) { this.fareService = fareService; }

    @PostMapping("/estimate")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a fare estimate")
    public FareEstimateDto estimate(@Valid @RequestBody FareEstimateRequest request,
                                    Authentication authentication) {
        AuthenticatedUser user = AuthenticatedUser.from(authentication);
        if (!user.isPassenger() && !user.isAdmin() && !user.isDriver()) {
            throw new UnauthorizedError("Passenger or driver role is required");
        }
        return fareService.estimateFare(request.withPassengerId(user.userId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve a fare estimate by ID")
    public FareEstimateDto getById(@PathVariable UUID id, Authentication authentication) {
        AuthenticatedUser user = AuthenticatedUser.from(authentication);
        FareEstimateDto estimate = fareService.getEstimateById(id);
        ensureOwnerOrAdmin(user, estimate.passengerId());
        return estimate;
    }

    private static void ensureOwnerOrAdmin(AuthenticatedUser user, String ownerId) {
        if (!user.isAdmin() && !user.userId().equals(ownerId)) {
            throw new UnauthorizedError("You may only access your own fare estimates");
        }
    }
}
