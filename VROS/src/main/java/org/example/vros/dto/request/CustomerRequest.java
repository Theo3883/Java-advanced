package org.example.vros.dto.request;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "customer")
public class CustomerRequest {

    @NotBlank(message = "{customer.name.required}")
    @Size(max = 100, message = "{customer.name.size}")
    private String name;

    @NotBlank(message = "{customer.email.required}")
    @Email(message = "{customer.email.invalid}")
    @Size(max = 150, message = "{customer.email.size}")
    private String email;

    @Size(max = 30, message = "{customer.phone.size}")
    private String phone;

    @Size(max = 255, message = "{customer.address.size}")
    private String address;

    @NotNull(message = "{customer.lat.required}")
    @DecimalMin(value = "-90.0", message = "{customer.lat.min}")
    @DecimalMax(value = "90.0", message = "{customer.lat.max}")
    private Double lat;

    @NotNull(message = "{customer.lng.required}")
    @DecimalMin(value = "-180.0", message = "{customer.lng.min}")
    @DecimalMax(value = "180.0", message = "{customer.lng.max}")
    private Double lng;
}
