package com.booking.booking.security

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
    info = @Info(
        contact = @Contact(
            name = "mmaqbour",
            email = "maqbour.moh@gmail.com",
            url = "https://mohamedmqdev.netlify.app/"
        ),
        description = "OpenApi documentation for Ticket Event App",
        title = "OpenApi specification - mmaqbour",
        version = "1.0",
        license = @License(
            name = "Mohamed Maqbour License",
            url = "https://mohamedmqdev.netlify.app/"
        )
    ),
    servers = {
        @Server(
            description = "local ENV",
            url = "https://mohamedmqdev.netlify.app/"
        ),
        @Server(
            description = "local PROD",
            url = "https://mohamedmqdev.netlify.app/"
        )
    }
)
public class OpenApiConfig {
    
}