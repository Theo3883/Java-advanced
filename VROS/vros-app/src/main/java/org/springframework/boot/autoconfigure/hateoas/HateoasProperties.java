package org.springframework.boot.autoconfigure.hateoas;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.hateoas")
public class HateoasProperties extends org.springframework.boot.hateoas.autoconfigure.HateoasProperties {
}
