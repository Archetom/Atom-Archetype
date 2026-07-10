#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.infra.rest.config;

import ${package}.infra.rest.security.TrustedHeaderAuthenticationFilter;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * Publishes an API description that reflects authentication actually available
 * in the generated application.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    static final String DEV_USER_ID_SCHEME = "devUserId";
    static final String DEV_TENANT_ID_SCHEME = "devTenantId";

    @Bean
    public OpenAPI customOpenAPI(Environment environment) {
        String applicationName = environment.getProperty("spring.application.name", "${rootArtifactId}");
        String apiVersion = environment.getProperty("atom.api.version", "v1");

        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title(applicationName + " API")
                        .version(apiVersion)
                        .description(applicationName + " REST API")
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/license/mit")))
                .components(new Components());

        if (trustedHeaderAdapterEnabled(environment)) {
            addDevelopmentHeaderSecurity(openAPI);
        }
        return openAPI;
    }

    private boolean trustedHeaderAdapterEnabled(Environment environment) {
        return environment.getProperty("atom.security.trusted-header.enabled", Boolean.class, false)
                && environment.acceptsProfiles(Profiles.of("(dev | test) & !prod"));
    }

    private void addDevelopmentHeaderSecurity(OpenAPI openAPI) {
        openAPI.getComponents()
                .addSecuritySchemes(DEV_USER_ID_SCHEME, developmentHeader(
                        TrustedHeaderAuthenticationFilter.USER_ID_HEADER,
                        "Positive actor ID accepted only by the explicitly enabled dev/test adapter"))
                .addSecuritySchemes(DEV_TENANT_ID_SCHEME, developmentHeader(
                        TrustedHeaderAuthenticationFilter.TENANT_ID_HEADER,
                        "Positive tenant ID accepted only by the explicitly enabled dev/test adapter"));
        openAPI.addSecurityItem(new SecurityRequirement()
                .addList(DEV_USER_ID_SCHEME)
                .addList(DEV_TENANT_ID_SCHEME));
    }

    private SecurityScheme developmentHeader(String headerName, String description) {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name(headerName)
                .description(description);
    }
}
