package org.example.vros.controller.v1.assembler;

import org.example.vros.controller.v1.CustomerController;
import org.example.vros.controller.v1.DeliveryController;
import org.example.vros.dto.mapper.DtoMapper;
import org.example.vros.dto.response.CustomerResponse;
import org.example.vros.model.Customer;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Spring HATEOAS RepresentationModelAssembler for Customer.
 * Attaches hypermedia navigation links to customer resources:
 * - self: link to customer by ID (/api/v1/customers/{id})
 * - all-customers: link to customer collection (/api/v1/customers)
 * - deliveries: link to discover deliveries belonging to this customer (/api/v1/deliveries/customer/{id})
 */
@Component
public class CustomerModelAssembler implements RepresentationModelAssembler<Customer, CustomerResponse> {

    @Override
    public CustomerResponse toModel(Customer customer) {
        CustomerResponse response = DtoMapper.toResponse(customer);

        // self
        response.add(linkTo(methodOn(CustomerController.class).getById(customer.getId())).withSelfRel());

        // all-customers
        response.add(linkTo(methodOn(CustomerController.class).getAll()).withRel("all-customers"));

        // deliveries (related resource discovery)
        response.add(linkTo(methodOn(DeliveryController.class).getByCustomer(customer.getId())).withRel("deliveries"));

        return response;
    }
}
