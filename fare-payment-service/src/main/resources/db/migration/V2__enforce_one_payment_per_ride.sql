ALTER TABLE payments
    ADD CONSTRAINT uk_payments_ride_id UNIQUE (ride_id);
