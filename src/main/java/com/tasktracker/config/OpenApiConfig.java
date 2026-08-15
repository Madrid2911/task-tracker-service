package com.tasktracker.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@OpenAPIDefinition(security = @SecurityRequirement(name = "basicAuth"))
@SecurityScheme(name = "basicAuth", type = io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP,
        scheme = "basic", in = SecuritySchemeIn.HEADER)
public class OpenApiConfig {
}
