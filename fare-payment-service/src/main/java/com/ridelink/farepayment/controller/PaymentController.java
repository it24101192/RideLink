package com.ridelink.farepayment.controller;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ridelink.farepayment.dto.PaymentDto;
import com.ridelink.farepayment.dto.PaymentRequest;
import com.ridelink.farepayment.dto.ReceiptDto;
import com.ridelink.farepayment.dto.RefundRequest;
import com.ridelink.farepayment.error.UnauthorizedError;
import com.ridelink.farepayment.security.AuthenticatedUser;
import com.ridelink.farepayment.service.PaymentService;
import com.ridelink.farepayment.model.PaymentStatus;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) { this.paymentService = paymentService; }

    @PostMapping
    @Operation(summary = "Process a simulated payment for a ride")
    public ResponseEntity<PaymentDto> process(@Valid @RequestBody PaymentRequest request, Authentication authentication) {
        AuthenticatedUser user = AuthenticatedUser.from(authentication);
        ensureRequestIdentity(user, request);
        PaymentDto payment = paymentService.processPayment(request);
        HttpStatus status = payment.status() == PaymentStatus.FAILED
            ? HttpStatus.PAYMENT_REQUIRED : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(payment);
    }

    @GetMapping("/ride/{rideId}")
    @Operation(summary = "List payment records for a ride")
    public List<PaymentDto> getByRideId(@PathVariable UUID rideId, Authentication authentication) {
        AuthenticatedUser user = AuthenticatedUser.from(authentication);
        List<PaymentDto> results = paymentService.getPaymentByRideId(rideId);
        results.forEach(payment -> ensureOwnerOrAdmin(user, payment.passengerId(), payment.driverId()));
        return results;
    }

    @GetMapping("/{id}/receipt")
    @Operation(summary = "Retrieve a payment receipt")
    public ReceiptDto getReceipt(@PathVariable UUID id, Authentication authentication) {
        AuthenticatedUser user = AuthenticatedUser.from(authentication);
        ReceiptDto receipt = paymentService.getReceiptByPaymentId(id);
        ensureOwnerOrAdmin(user, receipt.passengerId(), receipt.driverId());
        return receipt;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve a payment by ID")
    public PaymentDto getById(@PathVariable UUID id, Authentication authentication) {
        AuthenticatedUser user = AuthenticatedUser.from(authentication);
        PaymentDto payment = paymentService.getPaymentById(id);
        ensureOwnerOrAdmin(user, payment.passengerId(), payment.driverId());
        return payment;
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Refund a successful payment (admin only)")
    public PaymentDto refund(@PathVariable UUID id, @Valid @RequestBody RefundRequest request,
                             Authentication authentication) {
        AuthenticatedUser user = AuthenticatedUser.from(authentication);
        if (!user.isAdmin()) throw new UnauthorizedError("Only an admin can refund payments");
        return paymentService.refundPayment(id, request.reason(), "ADMIN");
    }

    private static void ensureRequestIdentity(AuthenticatedUser user, PaymentRequest request) {
        if (user.isAdmin()) return;
        if (user.isPassenger() && user.userId().equals(request.passengerId())) return;
        if (user.isDriver() && user.userId().equals(request.driverId())) return;
        throw new UnauthorizedError("Payment identity must match the authenticated passenger or driver");
    }

    private static void ensureOwnerOrAdmin(AuthenticatedUser user, String passengerId, String driverId) {
        if (user.isAdmin() || user.userId().equals(passengerId) || user.userId().equals(driverId)) return;
        throw new UnauthorizedError("You may only access payment records for your rides");
    }
}
