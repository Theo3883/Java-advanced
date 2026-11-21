package org.vros.api.rest.v1.assembler;

import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.vros.api.dto.response.CustomerResponse;
import org.vros.api.mapper.DtoMapper;
import org.vros.api.rest.v1.CustomerController;
import org.vros.api.rest.v1.DeliveryController;
import org.vros.domain.model.Customer;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Spring HATEOAS assembler for Customer.
 *
 * <p>Administrative discovery links are only exposed to callers holding
 * {@code ROLE_ADMIN}: a CUSTOMER must not be invited to follow a link that would
 * immediately answer {@code 403 Forbidden}.</p>
 */
@Component
public class CustomerModelAssembler implements RepresentationModelAssembler<Customer, CustomerResponse> {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    @Override
    public CustomerResponse toModel(Customer customer) {
        CustomerResponse response = DtoMapper.toResponse(customer);

        // self
        response.add(linkTo(methodOn(CustomerController.class).getById(customer.getId())).withSelfRel());

        // all-customers (administrative discovery only)
        if (hasRoleAdmin()) {
            response.add(linkTo(methodOn(CustomerController.class).getAll()).withRel("all-customers"));
        }

        // deliveries (related resource discovery; ownership is enforced server-side)
        response.add(linkTo(methodOn(DeliveryController.class).getByCustomer(customer.getId())).withRel("deliveries"));

        return response;
    }

    private boolean hasRoleAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ROLE_ADMIN::equals);
    }
}
