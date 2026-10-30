package org.example.vros.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.vros.dto.mapper.DtoMapper;
import org.example.vros.dto.request.DeliveryRequest;
import org.example.vros.dto.response.DeliveryResponse;
import org.example.vros.exception.ResourceNotFoundException;
import org.example.vros.model.Customer;
import org.example.vros.model.Delivery;
import org.example.vros.service.interfaces.CustomerService;
import org.example.vros.service.interfaces.DeliveryService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(path = "/api/v1/deliveries", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Deliveries", description = "CRUD operations for Deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final CustomerService customerService;

    @GetMapping
    @Operation(summary = "List all deliveries")
    public List<DeliveryResponse> getAll() {
        return deliveryService.findAll().stream().map(DtoMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a delivery by ID")
    public DeliveryResponse getById(@PathVariable Long id) {
        return deliveryService.findById(id)
                .map(DtoMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", id));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "List deliveries for a customer")
    public List<DeliveryResponse> getByCustomer(@PathVariable Long customerId) {
        return deliveryService.findByCustomer(customerId).stream().map(DtoMapper::toResponse).toList();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new delivery")
    public ResponseEntity<DeliveryResponse> create(@Valid @RequestBody DeliveryRequest req) {
        Customer customer = customerService.findById(req.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", req.getCustomerId()));

        Delivery delivery = Delivery.builder()
                .customer(customer)
                .quantity(req.getQuantity())
                .notes(req.getNotes())
                .timestamp(req.getTimestamp() != null ? req.getTimestamp() : LocalDateTime.now())
                .build();

        Delivery saved = deliveryService.save(delivery);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(DtoMapper.toResponse(saved));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a delivery")
    public DeliveryResponse update(@PathVariable Long id, @Valid @RequestBody DeliveryRequest req) {
        Delivery existing = deliveryService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", id));

        if (!existing.getCustomer().getId().equals(req.getCustomerId())) {
            Customer customer = customerService.findById(req.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer", req.getCustomerId()));
            existing.setCustomer(customer);
        }

        existing.setQuantity(req.getQuantity());
        existing.setNotes(req.getNotes());
        if (req.getTimestamp() != null) {
            existing.setTimestamp(req.getTimestamp());
        }

        return DtoMapper.toResponse(deliveryService.save(existing));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a delivery")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (deliveryService.findById(id).isEmpty()) {
            throw new ResourceNotFoundException("Delivery", id);
        }
        deliveryService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
