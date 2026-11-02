package org.vros.api.rest.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.vros.api.mapper.DtoMapper;
import org.vros.api.dto.request.VehicleRequest;
import org.vros.api.dto.response.VehicleResponse;
import org.vros.domain.exception.ResourceNotFoundException;
import org.vros.domain.model.Depot;
import org.vros.domain.model.Driver;
import org.vros.domain.model.Vehicle;
import org.vros.application.service.DepotService;
import org.vros.application.service.DriverService;
import org.vros.application.service.VehicleService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(path = "/api/v1/vehicles", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Vehicles", description = "CRUD operations for Vehicles")
public class VehicleController {

    private final VehicleService vehicleService;
    private final DepotService depotService;
    private final DriverService driverService;

    @GetMapping
    @Operation(summary = "List all vehicles")
    public List<VehicleResponse> getAll() {
        return vehicleService.findAll().stream().map(DtoMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a vehicle by ID")
    public VehicleResponse getById(@PathVariable Long id) {
        return vehicleService.findById(id)
                .map(DtoMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
    }

    @GetMapping("/available")
    @Operation(summary = "List available vehicles")
    public List<VehicleResponse> getAvailable() {
        return vehicleService.findAvailable().stream().map(DtoMapper::toResponse).toList();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new vehicle")
    public ResponseEntity<VehicleResponse> create(@Valid @RequestBody VehicleRequest req) {
        Vehicle vehicle = buildVehicle(req);
        Vehicle saved = vehicleService.save(vehicle);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(DtoMapper.toResponse(saved));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a vehicle")
    public VehicleResponse update(@PathVariable Long id, @Valid @RequestBody VehicleRequest req) {
        Vehicle existing = vehicleService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));

        existing.setName(req.getName());
        existing.setCapacity(req.getCapacity());
        existing.setAvailable(req.isAvailable());

        if (req.getDepotId() != null) {
            Depot depot = depotService.findById(req.getDepotId())
                    .orElseThrow(() -> new ResourceNotFoundException("Depot", req.getDepotId()));
            existing.setDepot(depot);
        }

        if (req.getDriverId() != null) {
            Driver driver = driverService.findById(req.getDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Driver", req.getDriverId()));
            existing.setDriver(driver);
        } else {
            existing.setDriver(null);
        }

        return DtoMapper.toResponse(vehicleService.save(existing));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a vehicle")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (vehicleService.findById(id).isEmpty()) {
            throw new ResourceNotFoundException("Vehicle", id);
        }
        vehicleService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private Vehicle buildVehicle(VehicleRequest req) {
        Depot depot;
        if (req.getDepotId() != null) {
            depot = depotService.findById(req.getDepotId())
                    .orElseThrow(() -> new ResourceNotFoundException("Depot", req.getDepotId()));
        } else {
            depot = depotService.findAll().stream().findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("error.depot.none_available", "No depot available in database"));
        }

        Driver driver = null;
        if (req.getDriverId() != null) {
            driver = driverService.findById(req.getDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Driver", req.getDriverId()));
        }

        return Vehicle.builder()
                .name(req.getName())
                .capacity(req.getCapacity())
                .available(req.isAvailable())
                .depot(depot)
                .driver(driver)
                .build();
    }
}
