package com.ridelink.account_service.account;

import java.util.UUID;

public record UserIdentityResponse(UUID userId, String role, String status) { }
