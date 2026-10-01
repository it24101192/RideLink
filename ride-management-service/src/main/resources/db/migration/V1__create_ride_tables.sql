CREATE TABLE rides (
    id UUID PRIMARY KEY,
    passenger_id UUID NOT NULL,
    driver_id UUID,
    pickup_lat DOUBLE PRECISION NOT NULL,
    pickup_lng DOUBLE PRECISION NOT NULL,
    pickup_address VARCHAR(300) NOT NULL,
    dest_lat DOUBLE PRECISION NOT NULL,
    dest_lng DOUBLE PRECISION NOT NULL,
    dest_address VARCHAR(300) NOT NULL,
    status VARCHAR(24) NOT NULL,
    estimated_fare NUMERIC(12,2) NOT NULL,
    final_fare NUMERIC(12,2),
    distance_km NUMERIC(10,3) NOT NULL,
    duration_min INTEGER NOT NULL,
    cancellation_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    CONSTRAINT rides_status_check CHECK (status IN ('REQUESTED','ASSIGNED','ACCEPTED','IN_PROGRESS','COMPLETED','CANCELLED'))
);
CREATE INDEX rides_passenger_created_idx ON rides(passenger_id, created_at DESC);
CREATE INDEX rides_driver_created_idx ON rides(driver_id, created_at DESC);
CREATE INDEX rides_status_created_idx ON rides(status, created_at DESC);

CREATE TABLE ride_status_history (
    id UUID PRIMARY KEY,
    ride_id UUID NOT NULL REFERENCES rides(id),
    from_status VARCHAR(24),
    to_status VARCHAR(24) NOT NULL,
    changed_by UUID NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(500)
);
CREATE INDEX ride_status_history_ride_idx ON ride_status_history(ride_id, changed_at);

CREATE TABLE ride_payments (
    id UUID PRIMARY KEY,
    ride_id UUID NOT NULL UNIQUE REFERENCES rides(id),
    amount NUMERIC(12,2) NOT NULL,
    method VARCHAR(24) NOT NULL,
    status VARCHAR(16) NOT NULL,
    transaction_ref VARCHAR(100) NOT NULL,
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ride_payment_method_check CHECK (method = 'SIMULATED'),
    CONSTRAINT ride_payment_status_check CHECK (status IN ('PAID','FAILED'))
);
