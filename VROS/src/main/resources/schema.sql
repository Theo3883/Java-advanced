-- ============================================================
-- VROS Schema — executed on schema init
-- ============================================================

DROP TABLE IF EXISTS deliveries CASCADE;
DROP TABLE IF EXISTS vehicles    CASCADE;
DROP TABLE IF EXISTS drivers     CASCADE;
DROP TABLE IF EXISTS customers   CASCADE;
DROP TABLE IF EXISTS depots      CASCADE;

-- Depots
CREATE TABLE depots (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100)       NOT NULL,
    lat  DOUBLE PRECISION   NOT NULL,
    lng  DOUBLE PRECISION   NOT NULL
);

-- Drivers
CREATE TABLE drivers (
    id    BIGSERIAL PRIMARY KEY,
    name  VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(30)
);

-- Customers
CREATE TABLE customers (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(100)     NOT NULL,
    email   VARCHAR(150)     NOT NULL UNIQUE,
    phone   VARCHAR(30),
    address VARCHAR(255),
    lat     DOUBLE PRECISION NOT NULL,
    lng     DOUBLE PRECISION NOT NULL
);

-- Vehicles
CREATE TABLE vehicles (
    id        BIGSERIAL PRIMARY KEY,
    name      VARCHAR(100) NOT NULL,
    capacity  INTEGER      NOT NULL CHECK (capacity > 0),
    available BOOLEAN      NOT NULL DEFAULT TRUE,
    depot_id  BIGINT       NOT NULL REFERENCES depots(id),
    driver_id BIGINT                UNIQUE REFERENCES drivers(id)
);

-- Deliveries
CREATE TABLE deliveries (
    id          BIGSERIAL PRIMARY KEY,
    customer_id BIGINT    NOT NULL REFERENCES customers(id),
    quantity    INTEGER   NOT NULL CHECK (quantity > 0),
    notes       TEXT,
    timestamp   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes to boost query & benchmark performance
CREATE INDEX idx_deliveries_customer_id ON deliveries(customer_id);
CREATE INDEX idx_deliveries_timestamp ON deliveries(timestamp);
CREATE INDEX idx_vehicles_depot_id ON vehicles(depot_id);
CREATE INDEX idx_customers_coords ON customers(lat, lng);
