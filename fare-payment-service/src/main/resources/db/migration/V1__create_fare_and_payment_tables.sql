CREATE TABLE fare_estimates (
    id BINARY(16) NOT NULL PRIMARY KEY,
    ride_request_id BINARY(16) NULL,
    passenger_id VARCHAR(100) NOT NULL,
    pickup_location VARCHAR(500) NOT NULL,
    destination_location VARCHAR(500) NOT NULL,
    distance_km DECIMAL(12, 3) NOT NULL,
    duration_minutes INT NOT NULL,
    surge_multiplier DECIMAL(3, 2) NOT NULL DEFAULT 1.00,
    estimated_fare INT NOT NULL COMMENT 'Integer LKR cents',
    breakdown JSON NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_fare_estimates_ride_request_id (ride_request_id),
    INDEX idx_fare_estimates_passenger_id (passenger_id)
);

CREATE TABLE payments (
    id BINARY(16) NOT NULL PRIMARY KEY,
    ride_id BINARY(16) NOT NULL,
    fare_estimate_id BINARY(16) NULL,
    passenger_id VARCHAR(100) NOT NULL,
    driver_id VARCHAR(100) NOT NULL,
    amount INT NOT NULL COMMENT 'Integer cents in currency units',
    currency CHAR(3) NOT NULL DEFAULT 'LKR',
    status VARCHAR(16) NOT NULL,
    payment_method VARCHAR(24) NOT NULL,
    transaction_ref VARCHAR(100) NOT NULL UNIQUE,
    failure_reason VARCHAR(500) NULL,
    refund_reason VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_payments_fare_estimate FOREIGN KEY (fare_estimate_id) REFERENCES fare_estimates(id),
    INDEX idx_payments_ride_id (ride_id),
    INDEX idx_payments_passenger_id (passenger_id)
);
