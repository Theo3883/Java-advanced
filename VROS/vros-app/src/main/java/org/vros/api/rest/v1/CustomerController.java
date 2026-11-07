package org.vros.api.rest.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.vros.api.rest.v1.assembler.CustomerModelAssembler;
import org.vros.api.dto.request.CustomerRequest;
import org.vros.api.dto.response.CustomerResponse;
import org.vros.domain.exception.ConflictException;
import org.vros.domain.exception.ResourceNotFoundException;
import org.vros.domain.model.Customer;
import org.vros.domain.model.Coordinates;
import org.vros.application.service.CustomerService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(
        path = "/api/v1/customers",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE}
)
@RequiredArgsConstructor
@Tag(name = "Customers", description = "CRUD operations for Customers with HATEOAS link discovery")
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerModelAssembler customerModelAssembler;

    @GetMapping
    @Operation(summary = "List all customers")
    @ApiResponse(responseCode = "200", description = "OK")
    public List<CustomerResponse> getAll() {
        return customerService.findAll().stream()
                .map(customerModelAssembler::toModel)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @ownershipChecker.isCustomerOwner(authentication, #id)")
    @Operation(summary = "Get a customer by ID with HATEOAS discovery links")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found",
                    content = @Content(schema = @Schema(implementation = CustomerResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    public CustomerResponse getById(
            @Parameter(description = "Customer ID") @PathVariable Long id) {
        return customerService.findById(id)
                .map(customerModelAssembler::toModel)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @Operation(summary = "Create a new customer")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "409", description = "Email already in use", content = @Content)
    })
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest req) {
        customerService.findByEmail(req.getEmail()).ifPresent(existing -> {
            throw new ConflictException(req.getEmail());
        });

        Customer customer = Customer.builder()
                .name(req.getName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .address(req.getAddress())
                .coordinates(new Coordinates(req.getLat(), req.getLng()))
                .build();

        Customer saved = customerService.save(customer);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(customerModelAssembler.toModel(saved));
    }

    @PutMapping(value = "/{id}", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ADMIN') or @ownershipChecker.isCustomerOwner(authentication, #id)")
    @Operation(summary = "Update a customer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Email conflict", content = @Content)
    })
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest req) {
        Customer existing = customerService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));

        // Check email conflict with OTHER customers
        customerService.findByEmail(req.getEmail()).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new ConflictException(req.getEmail());
            }
        });

        existing.setName(req.getName());
        existing.setEmail(req.getEmail());
        existing.setPhone(req.getPhone());
        existing.setAddress(req.getAddress());
        existing.setCoordinates(new Coordinates(req.getLat(), req.getLng()));

        return customerModelAssembler.toModel(customerService.save(existing));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a customer")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (customerService.findById(id).isEmpty()) {
            throw new ResourceNotFoundException("Customer", id);
        }
        customerService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Search customers by name keyword")
    public List<CustomerResponse> search(@RequestParam String keyword) {
        return customerService.searchByName(keyword).stream()
                .map(customerModelAssembler::toModel)
                .toList();
    }
}
