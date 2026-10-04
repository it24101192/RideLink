ALTER TABLE rides ADD COLUMN service_area VARCHAR(100);
CREATE INDEX rides_service_area_idx ON rides(service_area);
