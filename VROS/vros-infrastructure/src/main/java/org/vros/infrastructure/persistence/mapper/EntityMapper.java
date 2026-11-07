package org.vros.infrastructure.persistence.mapper;

import org.vros.domain.model.*;
import org.vros.infrastructure.persistence.entity.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Static bidirectional mappers between JPA entities and domain models.
 * Must be called inside an active transaction so lazy proxies are initialized.
 *
 * <p>Nested associations are mapped "shallow" (one level, no back-reference) to avoid
 * infinite object graphs (e.g. Vehicle.driver &lt;-&gt; Driver.vehicle).</p>
 */
public final class EntityMapper {

    private EntityMapper() {}

    // ---------- Coordinates ----------
    public static Coordinates toDomain(CoordinatesEmbeddable e) {
        if (e == null) return null;
        return new Coordinates(e.getLat(), e.getLng());
    }

    public static CoordinatesEmbeddable toEntity(Coordinates d) {
        if (d == null) return null;
        return new CoordinatesEmbeddable(d.getLat(), d.getLng());
    }

    // ---------- User ----------
    public static User toDomain(UserEntity e) {
        if (e == null) return null;
        return User.builder()
                .id(e.getId())
                .keycloakSubject(e.getKeycloakSubject())
                .email(e.getEmail())
                .displayName(e.getDisplayName())
                .enabled(e.isEnabled())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    public static UserEntity toEntity(User d) {
        if (d == null) return null;
        return UserEntity.builder()
                .id(d.getId())
                .keycloakSubject(d.getKeycloakSubject())
                .email(d.getEmail())
                .displayName(d.getDisplayName())
                .enabled(d.isEnabled())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    /** Lightweight JPA reference by id, without triggering cascades or loading associations. */
    private static UserEntity userRef(User d) {
        if (d == null) return null;
        UserEntity e = new UserEntity();
        e.setId(d.getId());
        e.setKeycloakSubject(d.getKeycloakSubject());
        e.setEmail(d.getEmail());
        e.setDisplayName(d.getDisplayName());
        e.setEnabled(d.isEnabled());
        return e;
    }

    // ---------- Customer ----------
    public static Customer toDomain(CustomerEntity e) {
        if (e == null) return null;
        Customer c = new Customer(e.getName(), e.getEmail(), e.getPhone(), e.getAddress(), toDomain(e.getCoordinates()));
        c.setId(e.getId());
        c.setUser(toDomain(e.getUser()));
        return c;
    }

    /**
     * Customer graph including its deliveries. Deliveries are mapped without their
     * customer back-reference to keep the object graph acyclic.
     */
    public static Customer toDomainWithDeliveries(CustomerEntity e) {
        if (e == null) return null;
        Customer c = toDomain(e);
        if (e.getDeliveries() != null) {
            c.setDeliveries(e.getDeliveries().stream()
                    .map(EntityMapper::toDomainWithoutCustomer)
                    .collect(Collectors.toList()));
        }
        return c;
    }

    public static CustomerEntity toEntity(Customer d) {
        if (d == null) return null;
        CustomerEntity e = new CustomerEntity(d.getName(), d.getEmail(), d.getPhone(), d.getAddress(), toEntity(d.getCoordinates()));
        e.setId(d.getId());
        e.setUser(userRef(d.getUser()));
        return e;
    }

    private static CustomerEntity customerRef(Customer d) {
        if (d == null) return null;
        CustomerEntity e = new CustomerEntity(d.getName(), d.getEmail(), d.getPhone(), d.getAddress(), toEntity(d.getCoordinates()));
        e.setId(d.getId());
        return e;
    }

    // ---------- Delivery ----------
    public static Delivery toDomain(DeliveryEntity e) {
        if (e == null) return null;
        Delivery d = toDomainWithoutCustomer(e);
        d.setCustomer(toDomain(e.getCustomer()));
        return d;
    }

    private static Delivery toDomainWithoutCustomer(DeliveryEntity e) {
        if (e == null) return null;
        Delivery d = new Delivery();
        d.setId(e.getId());
        d.setAssignedDriver(toDomainShallow(e.getAssignedDriver()));
        d.setQuantity(e.getQuantity());
        d.setNotes(e.getNotes());
        d.setTimestamp(e.getTimestamp());
        return d;
    }

    public static DeliveryEntity toEntity(Delivery d) {
        if (d == null) return null;
        DeliveryEntity e = new DeliveryEntity();
        e.setId(d.getId());
        e.setCustomer(customerRef(d.getCustomer()));
        e.setAssignedDriver(driverRef(d.getAssignedDriver()));
        e.setQuantity(d.getQuantity());
        e.setNotes(d.getNotes());
        e.setTimestamp(d.getTimestamp());
        return e;
    }

    // ---------- Depot ----------
    public static Depot toDomain(DepotEntity e) {
        if (e == null) return null;
        Depot d = new Depot();
        d.setId(e.getId());
        d.setName(e.getName());
        d.setCoordinates(toDomain(e.getCoordinates()));
        if (e.getVehicles() != null) {
            d.setVehicles(e.getVehicles().stream()
                    .map(EntityMapper::toDomainShallow)
                    .collect(Collectors.toList()));
        }
        return d;
    }

    public static DepotEntity toEntity(Depot d) {
        if (d == null) return null;
        DepotEntity e = new DepotEntity();
        e.setId(d.getId());
        e.setName(d.getName());
        e.setCoordinates(toEntity(d.getCoordinates()));
        return e;
    }

    private static DepotEntity depotRef(Depot d) {
        if (d == null) return null;
        DepotEntity e = new DepotEntity();
        e.setId(d.getId());
        e.setName(d.getName());
        return e;
    }

    // ---------- Driver ----------
    public static Driver toDomain(DriverEntity e) {
        if (e == null) return null;
        Driver d = toDomainShallow(e);
        d.setVehicle(toDomainShallow(e.getVehicle()));
        return d;
    }

    /** Driver without its vehicle back-reference. */
    private static Driver toDomainShallow(DriverEntity e) {
        if (e == null) return null;
        Driver d = new Driver(e.getName(), e.getEmail(), e.getPhone());
        d.setId(e.getId());
        d.setUser(toDomain(e.getUser()));
        return d;
    }

    public static DriverEntity toEntity(Driver d) {
        if (d == null) return null;
        DriverEntity e = new DriverEntity(d.getName(), d.getEmail(), d.getPhone());
        e.setId(d.getId());
        e.setUser(userRef(d.getUser()));
        return e;
    }

    private static DriverEntity driverRef(Driver d) {
        if (d == null) return null;
        DriverEntity e = new DriverEntity(d.getName(), d.getEmail(), d.getPhone());
        e.setId(d.getId());
        return e;
    }

    // ---------- Scenario ----------
    public static Scenario toDomain(ScenarioEntity e) {
        if (e == null) return null;
        Scenario s = new Scenario();
        s.setId(e.getId());
        s.setName(e.getName());
        if (e.getVehicles() != null) {
            s.setVehicles(e.getVehicles().stream().map(EntityMapper::toDomainShallow).collect(Collectors.toSet()));
        }
        if (e.getDeliveries() != null) {
            s.setDeliveries(e.getDeliveries().stream().map(EntityMapper::toDomainWithoutCustomer).collect(Collectors.toSet()));
        }
        return s;
    }

    public static ScenarioEntity toEntity(Scenario d) {
        if (d == null) return null;
        ScenarioEntity e = new ScenarioEntity();
        e.setId(d.getId());
        e.setName(d.getName());
        if (d.getVehicles() != null) {
            e.setVehicles(d.getVehicles().stream().map(EntityMapper::toEntity).collect(Collectors.toSet()));
        }
        if (d.getDeliveries() != null) {
            e.setDeliveries(d.getDeliveries().stream().map(EntityMapper::toEntity).collect(Collectors.toSet()));
        }
        return e;
    }

    // ---------- Vehicle ----------
    public static Vehicle toDomain(VehicleEntity e) {
        if (e == null) return null;
        Vehicle v = toDomainShallow(e);
        v.setDepot(toDomain(e.getDepot()));
        v.setDriver(toDomainShallow(e.getDriver()));
        return v;
    }

    /** Vehicle without driver/depot back-references (safe for nested mappings). */
    private static Vehicle toDomainShallow(VehicleEntity e) {
        if (e == null) return null;
        Vehicle v = new Vehicle();
        v.setId(e.getId());
        v.setName(e.getName());
        v.setCapacity(e.getCapacity());
        v.setAvailable(e.isAvailable());
        return v;
    }

    public static VehicleEntity toEntity(Vehicle d) {
        if (d == null) return null;
        VehicleEntity e = new VehicleEntity();
        e.setId(d.getId());
        e.setName(d.getName());
        e.setCapacity(d.getCapacity());
        e.setAvailable(d.isAvailable());
        e.setDepot(depotRef(d.getDepot()));
        e.setDriver(driverRef(d.getDriver()));
        return e;
    }

    // ---------- Batch helpers ----------
    public static List<Customer> toCustomerDomainList(List<CustomerEntity> entities) {
        if (entities == null) return new ArrayList<>();
        return entities.stream().map(EntityMapper::toDomain).collect(Collectors.toList());
    }

    public static List<Delivery> toDeliveryDomainList(List<DeliveryEntity> entities) {
        if (entities == null) return new ArrayList<>();
        return entities.stream().map(EntityMapper::toDomain).collect(Collectors.toList());
    }

    public static List<Depot> toDepotDomainList(List<DepotEntity> entities) {
        if (entities == null) return new ArrayList<>();
        return entities.stream().map(EntityMapper::toDomain).collect(Collectors.toList());
    }

    public static List<Driver> toDriverDomainList(List<DriverEntity> entities) {
        if (entities == null) return new ArrayList<>();
        return entities.stream().map(EntityMapper::toDomain).collect(Collectors.toList());
    }

    public static List<Vehicle> toVehicleDomainList(List<VehicleEntity> entities) {
        if (entities == null) return new ArrayList<>();
        return entities.stream().map(EntityMapper::toDomain).collect(Collectors.toList());
    }

    public static List<Scenario> toScenarioDomainList(List<ScenarioEntity> entities) {
        if (entities == null) return new ArrayList<>();
        return entities.stream().map(EntityMapper::toDomain).collect(Collectors.toList());
    }
}
