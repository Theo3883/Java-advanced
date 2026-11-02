package org.vros.infrastructure.persistence.mapper;

import org.vros.domain.model.*;
import org.vros.infrastructure.persistence.entity.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Static bidirectional mappers between JPA entities and domain models.
 * Must be called inside an active transaction so lazy proxies are initialized.
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

    // ---------- Customer ----------
    public static Customer toDomain(CustomerEntity e) {
        if (e == null) return null;
        Customer c = new Customer(e.getName(), e.getEmail(), e.getPhone(), e.getAddress(), toDomain(e.getCoordinates()));
        c.setId(e.getId());
        if (e.getDeliveries() != null) {
            c.setDeliveries(e.getDeliveries().stream().map(EntityMapper::toDomain).collect(Collectors.toList()));
        }
        return c;
    }

    public static CustomerEntity toEntity(Customer d) {
        if (d == null) return null;
        CustomerEntity e = new CustomerEntity(d.getName(), d.getEmail(), d.getPhone(), d.getAddress(), toEntity(d.getCoordinates()));
        e.setId(d.getId());
        return e;
    }

    // ---------- Delivery ----------
    public static Delivery toDomain(DeliveryEntity e) {
        if (e == null) return null;
        Delivery d = new Delivery();
        d.setId(e.getId());
        d.setCustomer(toDomain(e.getCustomer()));
        d.setQuantity(e.getQuantity());
        d.setNotes(e.getNotes());
        d.setTimestamp(e.getTimestamp());
        return d;
    }

    public static DeliveryEntity toEntity(Delivery d) {
        if (d == null) return null;
        DeliveryEntity e = new DeliveryEntity();
        e.setId(d.getId());
        e.setCustomer(toEntity(d.getCustomer()));
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
            d.setVehicles(e.getVehicles().stream().map(EntityMapper::toDomain).collect(Collectors.toList()));
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

    // ---------- Driver ----------
    public static Driver toDomain(DriverEntity e) {
        if (e == null) return null;
        Driver d = new Driver(e.getName(), e.getEmail(), e.getPhone());
        d.setId(e.getId());
        d.setVehicle(toDomain(e.getVehicle()));
        return d;
    }

    public static DriverEntity toEntity(Driver d) {
        if (d == null) return null;
        DriverEntity e = new DriverEntity(d.getName(), d.getEmail(), d.getPhone());
        e.setId(d.getId());
        e.setVehicle(toEntity(d.getVehicle()));
        return e;
    }

    // ---------- Scenario ----------
    public static Scenario toDomain(ScenarioEntity e) {
        if (e == null) return null;
        Scenario s = new Scenario();
        s.setId(e.getId());
        s.setName(e.getName());
        if (e.getVehicles() != null) {
            s.setVehicles(e.getVehicles().stream().map(EntityMapper::toDomain).collect(Collectors.toSet()));
        }
        if (e.getDeliveries() != null) {
            s.setDeliveries(e.getDeliveries().stream().map(EntityMapper::toDomain).collect(Collectors.toSet()));
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
        Vehicle v = new Vehicle();
        v.setId(e.getId());
        v.setName(e.getName());
        v.setCapacity(e.getCapacity());
        v.setAvailable(e.isAvailable());
        v.setDepot(toDomain(e.getDepot()));
        v.setDriver(toDomain(e.getDriver()));
        return v;
    }

    public static VehicleEntity toEntity(Vehicle d) {
        if (d == null) return null;
        VehicleEntity e = new VehicleEntity();
        e.setId(d.getId());
        e.setName(d.getName());
        e.setCapacity(d.getCapacity());
        e.setAvailable(d.isAvailable());
        e.setDepot(toEntity(d.getDepot()));
        e.setDriver(toEntity(d.getDriver()));
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
