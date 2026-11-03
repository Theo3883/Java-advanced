package org.vros.api.rest.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.vros.api.mapper.DtoMapper;
import org.vros.api.dto.request.ScenarioRequest;
import org.vros.api.dto.response.ScenarioResponse;
import org.vros.domain.exception.ResourceNotFoundException;
import org.vros.domain.model.Delivery;
import org.vros.domain.model.Scenario;
import org.vros.domain.model.Vehicle;
import org.vros.application.service.DeliveryService;
import org.vros.application.service.ScenarioService;
import org.vros.application.service.VehicleService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping(path = "/api/v1/scenarios", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Scenarios", description = "CRUD operations for Scenarios (VRP problem definitions)")
public class ScenarioController {

    private final ScenarioService scenarioService;
    private final VehicleService vehicleService;
    private final DeliveryService deliveryService;

    @GetMapping
    @Operation(summary = "List all scenarios")
    public List<ScenarioResponse> getAll() {
        return scenarioService.findAll().stream().map(DtoMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a scenario by ID (with vehicles and deliveries)")
    public ScenarioResponse getById(@PathVariable Long id) {
        return scenarioService.findByIdWithAssociations(id)
                .map(DtoMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Scenario", id));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new scenario")
    public ResponseEntity<ScenarioResponse> create(@Valid @RequestBody ScenarioRequest req) {
        Scenario scenario = buildScenario(req);
        Scenario saved = scenarioService.save(scenario);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(DtoMapper.toResponse(saved));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a scenario")
    public ScenarioResponse update(@PathVariable Long id, @Valid @RequestBody ScenarioRequest req) {
        Scenario updated = buildScenario(req);
        return DtoMapper.toResponse(scenarioService.update(id, updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a scenario")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        scenarioService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Search scenarios by name keyword")
    public List<ScenarioResponse> search(@RequestParam String keyword) {
        return scenarioService.searchByName(keyword).stream().map(DtoMapper::toResponse).toList();
    }

    private Scenario buildScenario(ScenarioRequest req) {
        Set<Vehicle> vehicles = new HashSet<>();
        if (req.getVehicleIds() != null) {
            for (Long vid : req.getVehicleIds()) {
                vehicles.add(vehicleService.findById(vid)
                        .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vid)));
            }
        }

        Set<Delivery> deliveries = new HashSet<>();
        if (req.getDeliveryIds() != null) {
            for (Long did : req.getDeliveryIds()) {
                deliveries.add(deliveryService.findById(did)
                        .orElseThrow(() -> new ResourceNotFoundException("Delivery", did)));
            }
        }

        return Scenario.builder()
                .name(req.getName())
                .vehicles(vehicles)
                .deliveries(deliveries)
                .build();
    }
}
