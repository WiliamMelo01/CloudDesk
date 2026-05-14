package wiliammelo.clouddesk.shared;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void createsOpenApiDefinitionWithBearerAuth() {
        var openApi = new OpenApiConfig().cloudDeskOpenApi();

        assertThat(openApi.getInfo().getTitle()).isEqualTo("CloudDesk API");
        assertThat(openApi.getInfo().getVersion()).isEqualTo("v1");
        assertThat(openApi.getComponents().getSecuritySchemes())
                .containsKey(OpenApiConfig.BEARER_AUTH);
        assertThat(openApi.getComponents().getSecuritySchemes().get(OpenApiConfig.BEARER_AUTH).getScheme())
                .isEqualTo("bearer");
    }
}
