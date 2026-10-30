-- Depots
CREATE TABLE IF NOT EXISTS depots (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100)       NOT NULL,
    lat  DOUBLE PRECISION   NOT NULL,
    lng  DOUBLE PRECISION   NOT NULL
);

-- Drivers
CREATE TABLE IF NOT EXISTS drivers (
    id    BIGSERIAL PRIMARY KEY,
    name  VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(30)
);

-- Customers
CREATE TABLE IF NOT EXISTS customers (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(100)     NOT NULL,
    email   VARCHAR(150)     NOT NULL UNIQUE,
    phone   VARCHAR(30),
    address VARCHAR(255),
    lat     DOUBLE PRECISION NOT NULL,
    lng     DOUBLE PRECISION NOT NULL
);

-- Vehicles
CREATE TABLE IF NOT EXISTS vehicles (
    id        BIGSERIAL PRIMARY KEY,
    name      VARCHAR(100) NOT NULL,
    capacity  INTEGER      NOT NULL CHECK (capacity > 0),
    available BOOLEAN      NOT NULL DEFAULT TRUE,
    depot_id  BIGINT       NOT NULL REFERENCES depots(id),
    driver_id BIGINT                UNIQUE REFERENCES drivers(id)
);

-- Deliveries
CREATE TABLE IF NOT EXISTS deliveries (
    id          BIGSERIAL PRIMARY KEY,
    customer_id BIGINT    NOT NULL REFERENCES customers(id),
    quantity    INTEGER   NOT NULL CHECK (quantity > 0),
    notes       TEXT,
    timestamp   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes to boost query & benchmark performance
CREATE INDEX IF NOT EXISTS idx_deliveries_customer_id ON deliveries(customer_id);
CREATE INDEX IF NOT EXISTS idx_deliveries_timestamp ON deliveries(timestamp);
CREATE INDEX IF NOT EXISTS idx_vehicles_depot_id ON vehicles(depot_id);
CREATE INDEX IF NOT EXISTS idx_customers_coords ON customers(lat, lng);

-- Scenarios
CREATE TABLE IF NOT EXISTS scenarios (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL
);

-- Many-to-many: Scenario <-> Vehicle
CREATE TABLE IF NOT EXISTS scenario_vehicles (
    scenario_id BIGINT NOT NULL REFERENCES scenarios(id) ON DELETE CASCADE,
    vehicle_id  BIGINT NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    PRIMARY KEY (scenario_id, vehicle_id)
);

-- Many-to-many: Scenario <-> Delivery
CREATE TABLE IF NOT EXISTS scenario_deliveries (
    scenario_id BIGINT NOT NULL REFERENCES scenarios(id) ON DELETE CASCADE,
    delivery_id BIGINT NOT NULL REFERENCES deliveries(id) ON DELETE CASCADE,
    PRIMARY KEY (scenario_id, delivery_id)
);
