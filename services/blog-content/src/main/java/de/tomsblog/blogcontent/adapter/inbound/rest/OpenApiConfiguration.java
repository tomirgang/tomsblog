package de.tomsblog.blogcontent.adapter.inbound.rest;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for the Blog Content Service.
 *
 * @req SWR-024
 */
@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI blogContentOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Blog Content Service API")
                        .description("REST API for managing blog posts, tags, translations, and sources "
                                + "in a multi-tenant blog platform.")
                        .version("0.1.0")
                        .contact(new Contact().name("Toms Blog").url("https://github.com/tomsblog"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")));
    }

    @Bean
    public OperationCustomizer tenantHeaderCustomizer() {
        return (operation, handlerMethod) -> {
            operation.addParametersItem(new Parameter()
                    .in("header")
                    .name("X-Tenant-Id")
                    .description("UUID of the tenant")
                    .required(true)
                    .schema(new io.swagger.v3.oas.models.media.StringSchema().format("uuid")));
            return operation;
        };
    }
}
