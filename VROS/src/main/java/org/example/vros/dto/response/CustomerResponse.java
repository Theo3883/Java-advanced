package org.example.vros.dto.response;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.*;
import org.springframework.hateoas.RepresentationModel;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JacksonXmlRootElement(localName = "customer")
public class CustomerResponse extends RepresentationModel<CustomerResponse> {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private Double lat;
    private Double lng;
}
