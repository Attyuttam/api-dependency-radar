
package com.apidependencyradar.application;

import com.apidependencyradar.infrastructure.openapi.OpenApiSpecFetcher;
import com.apidependencyradar.infrastructure.persistence.ApiDependencyEntity;
import com.apidependencyradar.infrastructure.persistence.ApiDependencyRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import com.apidependencyradar.infrastructure.persistence.AppUserEntity;
import com.apidependencyradar.infrastructure.persistence.AppUserRepository;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
class ApiDependencyPersistenceIntegrationTest {
        private static final String SOURCE_URL = "https://integration-test.example.com/openapi.yaml";
        private static final String SPEC_V1 = """
                        openapi: 3.0.3
                        info:
                          title: Integration Test API
                          version: 1.0.0
                        paths:
                          /products/{id}:
                            get:
                              operationId: getProduct
                              parameters:
                                - name: id
                                  in: path
                                  required: true
                                  schema:
                                    type: integer
                              responses:
                                '200':
                                  description: Product found
                                  content:
                                    application/json:
                                      schema:
                                        type: object
                                        properties:
                                          id:
                                            type: integer
                                          name:
                                            type: string
                        """;
        private static final String SPEC_V2 = """
                        openapi: 3.0.3
                        info:
                          title: Integration Test API
                          version: 2.0.0
                        paths:
                          /products/{id}:
                            get:
                              operationId: getProduct
                              parameters:
                                - name: id
                                  in: path
                                  required: true
                                  schema:
                                    type: integer
                              responses:
                                '200':
                                  description: Product found
                                  content:
                                    application/json:
                                      schema:
                                        type: object
                                        properties:
                                          id:
                                            type: integer
                        """;
        @Autowired
        private MockMvc mockMvc;
        private final ObjectMapper objectMapper = new ObjectMapper();
        @Autowired
        private AppUserRepository userRepository;
        @Autowired
        private ApiDependencyRepository dependencyRepository;
        @MockitoBean
        private OpenApiSpecFetcher specFetcher;
        private String createdDependencyId;

        /**
         * All test requests use the same authenticated principal without changing
         * production security.
         */
        private MockHttpServletRequestBuilder get(String url, Object... uriVariables) {
                return MockMvcRequestBuilders.get(url, uriVariables)
                                .with(user("test-user").roles("USER"));
        }

        private MockHttpServletRequestBuilder post(String url, Object... uriVariables) {
                return MockMvcRequestBuilders.post(url, uriVariables)
                                .with(user("test-user").roles("USER"));
        }

        private MockHttpServletRequestBuilder delete(String url, Object... uriVariables) {
                return MockMvcRequestBuilders.delete(url, uriVariables)
                                .with(user("test-user").roles("USER"));
        }

        @AfterEach
        void cleanUp() throws Exception {
                if (createdDependencyId != null
                                && dependencyRepository.existsById(createdDependencyId)) {
                        mockMvc.perform(
                                        delete("/api/dependencies/{id}", createdDependencyId))
                                        .andExpect(status().isNoContent());
                }
        }

        @BeforeEach
        void cleanUpExistingTestDependencies() {
                // Ensure the authenticated test principal has a database account.
                userRepository.findByEmailIgnoreCase("test-user")
                                .orElseGet(() -> userRepository.saveAndFlush(
                                                new AppUserEntity(
                                                                UUID.randomUUID().toString(),
                                                                "test-user",
                                                                "test-password-hash")));

                deleteDependenciesBySourceUrl(SOURCE_URL);
        }

        private void deleteDependenciesBySourceUrl(String sourceUrl) {
                for (ApiDependencyEntity dependency : dependencyRepository.findAllBySourceUrl(sourceUrl)) {
                        try {
                                mockMvc.perform(
                                                delete("/api/dependencies/{id}", dependency.getId()))
                                                .andExpect(status().isNoContent());
                        } catch (Exception e) {
                                throw new RuntimeException(
                                                "Failed to clean up test dependency", e);
                        }
                }
        }

        @Test
        void userCannotAccessAnotherUsersDependency() throws Exception {
                // User A is created by the existing @BeforeEach fixture.
                AppUserEntity owner = userRepository
                                .findByEmailIgnoreCase("test-user")
                                .orElseThrow();

                // Create a second, independent account for User B.
                String otherEmail = "isolation-user@example.test";

                AppUserEntity otherUser = userRepository
                                .findByEmailIgnoreCase(otherEmail)
                                .orElseGet(() -> userRepository.saveAndFlush(
                                                new AppUserEntity(
                                                                UUID.randomUUID().toString(),
                                                                otherEmail,
                                                                "test-password-hash")));

                // Persist a dependency owned by User A.
                String dependencyId = UUID.randomUUID().toString();
                createdDependencyId = dependencyId;

                dependencyRepository.saveAndFlush(
                                new ApiDependencyEntity(
                                                dependencyId,
                                                owner,
                                                "Isolation Test API",
                                                null,
                                                "https://integration-test.example.com/isolation-openapi.yaml",
                                                "OPENAPI",
                                                null,
                                                SPEC_V1));

                // User B cannot read User A's dependency.
                mockMvc.perform(
                                get("/api/dependencies/{id}", dependencyId)
                                                .with(user(otherUser.getEmail()).roles("USER")))
                                .andExpect(status().isNotFound());

                // User B cannot read its change history.
                mockMvc.perform(
                                get("/api/dependencies/{id}/changes", dependencyId)
                                                .with(user(otherUser.getEmail()).roles("USER")))
                                .andExpect(status().isNotFound());

                // User B cannot trigger a check on the dependency.
                mockMvc.perform(
                                post("/api/dependencies/{id}/check", dependencyId)
                                                .with(user(otherUser.getEmail()).roles("USER")))
                                .andExpect(status().isNotFound());

                // User B cannot delete the dependency.
                mockMvc.perform(
                                delete("/api/dependencies/{id}", dependencyId)
                                                .with(user(otherUser.getEmail()).roles("USER")))
                                .andExpect(status().isNotFound());

                // User B's list must not expose User A's dependency.
                MvcResult listResult = mockMvc.perform(
                                get("/api/dependencies")
                                                .with(user(otherUser.getEmail()).roles("USER")))
                                .andExpect(status().isOk())
                                .andReturn();

                JsonNode dependencies = objectMapper.readTree(
                                listResult.getResponse().getContentAsString());

                assertTrue(dependencies.isArray());
                for (JsonNode dependency : dependencies) {
                        assertFalse(dependencyId.equals(dependency.path("id").asText()));
                }

                // Confirm User A's dependency still exists and remains accessible.
                mockMvc.perform(get("/api/dependencies/{id}", dependencyId))
                                .andExpect(status().isOk());
        }

        @Test
        void breakingChangeIsPersistedAndReturnedByChangeHistory() throws Exception {
                // Registration fetch returns version 1; the subsequent check returns version 2.
                when(specFetcher.fetch(SOURCE_URL))
                                .thenReturn(SPEC_V1, SPEC_V2);
                String requestBody = objectMapper.writeValueAsString(
                                java.util.Map.of(
                                                "name", "Integration Test API",
                                                "providerName", "Test Provider",
                                                "sourceUrl", SOURCE_URL,
                                                "description", "Temporary integration test"));
                // 1. Register version 1 and save its baseline in PostgreSQL.
                MvcResult createResult = mockMvc.perform(
                                post("/api/dependencies")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(requestBody))
                                .andExpect(status().isCreated())
                                .andReturn();
                JsonNode created = objectMapper.readTree(
                                createResult.getResponse().getContentAsString());
                createdDependencyId = created.get("id").asText();
                assertTrue(dependencyRepository.existsById(createdDependencyId));
                // 2. Check against version 2, where the response property "name" is removed.
                MvcResult checkResult = mockMvc.perform(
                                post("/api/dependencies/{id}/check", createdDependencyId))
                                .andExpect(status().isOk())
                                .andReturn();
                JsonNode check = objectMapper.readTree(
                                checkResult.getResponse().getContentAsString());
                assertEquals(createdDependencyId, check.get("dependencyId").asText());
                assertTrue(check.get("breakingChangesDetected").asInt() >= 1);
                boolean breakingPropertyRemovalFound = false;
                for (JsonNode change : check.get("changes")) {
                        if ("RESPONSE_PROPERTY_REMOVED".equals(
                                        change.path("type").asText())
                                        && change.path("breaking").asBoolean()
                                        && change.path("description").asText().contains("name")) {
                                breakingPropertyRemovalFound = true;
                                break;
                        }
                }
                assertTrue(
                                breakingPropertyRemovalFound,
                                "Expected the removed response property 'name' "
                                                + "to be reported as a breaking change");
                // 3. Read change history through the actual REST endpoint.
                MvcResult historyResult = mockMvc.perform(
                                get("/api/dependencies/{id}/changes", createdDependencyId))
                                .andExpect(status().isOk())
                                .andReturn();
                JsonNode history = objectMapper.readTree(
                                historyResult.getResponse().getContentAsString());
                boolean persistedChangeFound = false;
                for (JsonNode change : history) {
                        if ("RESPONSE_PROPERTY_REMOVED".equals(
                                        change.path("type").asText())
                                        && change.path("breaking").asBoolean()
                                        && change.path("description").asText().contains("name")) {
                                persistedChangeFound = true;
                                break;
                        }
                }
                assertTrue(
                                persistedChangeFound,
                                "Expected the breaking change to be persisted "
                                                + "and returned by the history endpoint");
                // 4. Verify the dependency's change counts were updated in PostgreSQL.
                MvcResult dependencyResult = mockMvc.perform(
                                get("/api/dependencies/{id}", createdDependencyId))
                                .andExpect(status().isOk())
                                .andReturn();
                JsonNode dependency = objectMapper.readTree(
                                dependencyResult.getResponse().getContentAsString());
                assertTrue(dependency.get("changeCount").asLong() >= 1);
                assertTrue(dependency.get("breakingChangeCount").asLong() >= 1);
                assertTrue(dependency.hasNonNull("lastCheckedAt"));
        }

        @Test
        void failedCheckPreservesBaselineAndChangeHistory() throws Exception {
                // 1. Registers a dependency using SPEC_V1.
                when(specFetcher.fetch(SOURCE_URL))
                                .thenReturn(SPEC_V1);
                String requestBody = objectMapper.writeValueAsString(
                                java.util.Map.of(
                                                "name", "Failure Safety Test API",
                                                "providerName", "Test Provider",
                                                "sourceUrl", SOURCE_URL,
                                                "description", "Failure safety integration test"));
                MvcResult createResult = mockMvc.perform(
                                post("/api/dependencies")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(requestBody))
                                .andExpect(status().isCreated())
                                .andReturn();
                JsonNode created = objectMapper.readTree(
                                createResult.getResponse().getContentAsString());
                createdDependencyId = created.get("id").asText();
                assertTrue(dependencyRepository.existsById(createdDependencyId));
                // 2. Successfully checks SPEC_V2 and confirms a change is persisted.
                when(specFetcher.fetch(SOURCE_URL))
                                .thenReturn(SPEC_V2);
                MvcResult checkResult = mockMvc.perform(
                                post("/api/dependencies/{id}/check", createdDependencyId))
                                .andExpect(status().isOk())
                                .andReturn();
                JsonNode check = objectMapper.readTree(
                                checkResult.getResponse().getContentAsString());
                assertEquals(createdDependencyId, check.get("dependencyId").asText());
                assertEquals(1, check.get("breakingChangesDetected").asInt());
                // Snapshot state before failure through existing endpoints
                MvcResult initialHistoryResult = mockMvc.perform(
                                get("/api/dependencies/{id}/changes", createdDependencyId))
                                .andExpect(status().isOk())
                                .andReturn();
                JsonNode historyBeforeFailure = objectMapper.readTree(
                                initialHistoryResult.getResponse().getContentAsString());
                assertEquals(1, historyBeforeFailure.size());
                MvcResult initialDependencyResult = mockMvc.perform(
                                get("/api/dependencies/{id}", createdDependencyId))
                                .andExpect(status().isOk())
                                .andReturn();
                JsonNode dependencyBeforeFailure = objectMapper.readTree(
                                initialDependencyResult.getResponse().getContentAsString());
                long changeCountBeforeFailure = dependencyBeforeFailure.get("changeCount").asLong();
                long breakingChangeCountBeforeFailure = dependencyBeforeFailure.get("breakingChangeCount").asLong();
                String lastCheckedAtBeforeFailure = dependencyBeforeFailure.get("lastCheckedAt").asText();
                assertEquals(1, changeCountBeforeFailure);
                assertEquals(1, breakingChangeCountBeforeFailure);
                // 3. Configures the mocked OpenApiSpecFetcher to fail on the next check.
                when(specFetcher.fetch(SOURCE_URL))
                                .thenThrow(new IllegalStateException("Failed to fetch OpenAPI specification"));
                // 4. Calls POST /api/dependencies/{id}/check and verifies the expected HTTP
                // error response.
                mockMvc.perform(
                                post("/api/dependencies/{id}/check", createdDependencyId))
                                .andExpect(status().isBadGateway());
                // 5. Verifies through the existing API endpoints that the previously valid
                // state and change history remain intact.
                MvcResult dependencyResultAfterFailure = mockMvc.perform(
                                get("/api/dependencies/{id}", createdDependencyId))
                                .andExpect(status().isOk())
                                .andReturn();
                JsonNode dependencyAfterFailure = objectMapper.readTree(
                                dependencyResultAfterFailure.getResponse().getContentAsString());
                assertEquals(changeCountBeforeFailure, dependencyAfterFailure.get("changeCount").asLong());
                assertEquals(breakingChangeCountBeforeFailure,
                                dependencyAfterFailure.get("breakingChangeCount").asLong());
                assertEquals(lastCheckedAtBeforeFailure, dependencyAfterFailure.get("lastCheckedAt").asText());
                assertEquals("ACTIVE", dependencyAfterFailure.get("status").asText());
                MvcResult historyResultAfterFailure = mockMvc.perform(
                                get("/api/dependencies/{id}/changes", createdDependencyId))
                                .andExpect(status().isOk())
                                .andReturn();
                JsonNode historyAfterFailure = objectMapper.readTree(
                                historyResultAfterFailure.getResponse().getContentAsString());
                assertEquals(historyBeforeFailure.size(), historyAfterFailure.size());
                assertEquals(
                                historyBeforeFailure.get(0).get("id").asText(),
                                historyAfterFailure.get(0).get("id").asText());
                assertEquals(
                                historyBeforeFailure.get(0).get("type").asText(),
                                historyAfterFailure.get(0).get("type").asText());
                // Verify baseline spec in repository remains at SPEC_V2
                ApiDependencyEntity entity = dependencyRepository.findById(createdDependencyId).orElseThrow();
                assertEquals(SPEC_V2, entity.getBaselineSpec());
                // 6. Cleans up the registered dependency.
                mockMvc.perform(
                                delete("/api/dependencies/{id}", createdDependencyId))
                                .andExpect(status().isNoContent());
                createdDependencyId = null;
        }

        @Test
        void createWithMissingOrBlankNameReturnsBadRequest() throws Exception {
                String requestBody = objectMapper.writeValueAsString(
                                java.util.Map.of(
                                                "name", "   ",
                                                "sourceUrl", SOURCE_URL));
                MvcResult result = mockMvc.perform(
                                post("/api/dependencies")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(requestBody))
                                .andExpect(status().isBadRequest())
                                .andReturn();
                JsonNode error = objectMapper.readTree(result.getResponse().getContentAsString());
                assertEquals(400, error.get("status").asInt());
                assertEquals("Request validation failed", error.get("message").asText());
                assertTrue(error.get("details").toString().contains("name"));
        }

        @Test
        void createWithMissingOrBlankSourceUrlReturnsBadRequest() throws Exception {
                String requestBody = objectMapper.writeValueAsString(
                                java.util.Map.of(
                                                "name", "Valid Name",
                                                "sourceUrl", "  "));
                MvcResult result = mockMvc.perform(
                                post("/api/dependencies")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(requestBody))
                                .andExpect(status().isBadRequest())
                                .andReturn();
                JsonNode error = objectMapper.readTree(result.getResponse().getContentAsString());
                assertEquals(400, error.get("status").asInt());
                assertEquals("Request validation failed", error.get("message").asText());
                assertTrue(error.get("details").toString().contains("sourceUrl"));
        }

        @Test
        void createWithMalformedUrlReturnsBadRequest() throws Exception {
                when(specFetcher.fetch("://malformed-url"))
                                .thenThrow(new IllegalArgumentException("Invalid URL"));
                String requestBody = objectMapper.writeValueAsString(
                                java.util.Map.of(
                                                "name", "Valid Name",
                                                "sourceUrl", "://malformed-url"));
                MvcResult result = mockMvc.perform(
                                post("/api/dependencies")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(requestBody))
                                .andExpect(status().isBadRequest())
                                .andReturn();
                JsonNode error = objectMapper.readTree(result.getResponse().getContentAsString());
                assertEquals(400, error.get("status").asInt());
                assertEquals("Invalid URL", error.get("message").asText());
        }

        @Test
        void createWithUnsupportedSchemeOrSsrfUrlReturnsBadRequest() throws Exception {
                when(specFetcher.fetch("http://insecure.example.com/openapi.yaml"))
                                .thenThrow(new IllegalArgumentException("Only HTTPS URLs are allowed"));
                String requestBody = objectMapper.writeValueAsString(
                                java.util.Map.of(
                                                "name", "Insecure API",
                                                "sourceUrl", "http://insecure.example.com/openapi.yaml"));
                MvcResult result = mockMvc.perform(
                                post("/api/dependencies")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(requestBody))
                                .andExpect(status().isBadRequest())
                                .andReturn();
                JsonNode error = objectMapper.readTree(result.getResponse().getContentAsString());
                assertEquals(400, error.get("status").asInt());
                assertEquals("Only HTTPS URLs are allowed", error.get("message").asText());
        }

        @Test
        void createWithNonOpenApiContentReturnsBadRequest() throws Exception {
                when(specFetcher.fetch(SOURCE_URL))
                                .thenReturn("<html><body>Not an OpenAPI document</body></html>");
                String requestBody = objectMapper.writeValueAsString(
                                java.util.Map.of(
                                                "name", "HTML API",
                                                "sourceUrl", SOURCE_URL));
                MvcResult result = mockMvc.perform(
                                post("/api/dependencies")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(requestBody))
                                .andExpect(status().isBadRequest())
                                .andReturn();
                JsonNode error = objectMapper.readTree(result.getResponse().getContentAsString());
                assertEquals(400, error.get("status").asInt());
                assertEquals("The URL did not return a valid OpenAPI specification", error.get("message").asText());
        }

        @Test
        void getWithNonExistentIdReturnsNotFound() throws Exception {
                MvcResult result = mockMvc.perform(
                                get("/api/dependencies/{id}", "00000000-0000-0000-0000-000000000000"))
                                .andExpect(status().isNotFound())
                                .andReturn();
                JsonNode error = objectMapper.readTree(result.getResponse().getContentAsString());
                assertEquals(404, error.get("status").asInt());
                assertTrue(error.get("message").asText().contains("API dependency not found"));
        }

        @Test
        void getWithBlankOrMalformedIdReturnsBadRequest() throws Exception {
                MvcResult result = mockMvc.perform(
                                get("/api/dependencies/{id}", " "))
                                .andExpect(status().isBadRequest())
                                .andReturn();
                JsonNode error = objectMapper.readTree(result.getResponse().getContentAsString());
                assertEquals(400, error.get("status").asInt());
        }

        @Test
        void unexpectedInternalErrorReturns500WithoutLeakingDetails() throws Exception {
                when(specFetcher.fetch(SOURCE_URL))
                                .thenThrow(new RuntimeException(
                                                "Critical database connection failure: secret_password"));
                String requestBody = objectMapper.writeValueAsString(
                                java.util.Map.of(
                                                "name", "Crashing API",
                                                "sourceUrl", SOURCE_URL));
                MvcResult result = mockMvc.perform(
                                post("/api/dependencies")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(requestBody))
                                .andExpect(status().isInternalServerError())
                                .andReturn();
                String rawResponse = result.getResponse().getContentAsString();
                JsonNode error = objectMapper.readTree(rawResponse);
                assertEquals(500, error.get("status").asInt());
                assertEquals("Internal Server Error", error.get("error").asText());
                assertEquals("An unexpected internal error occurred", error.get("message").asText());
                assertEquals(0, error.get("details").size());
                assertFalse(rawResponse.contains("secret_password"));
                assertFalse(rawResponse.contains("Exception"));
                assertFalse(rawResponse.contains("at com.apidependencyradar"));
        }

        @Test
        void createWithDuplicateSourceUrlReturnsConflict() throws Exception {
                String duplicateSourceUrl = "https://integration-test.example.com/openapi-duplicate-"
                                + java.util.UUID.randomUUID() + ".yaml";
                when(specFetcher.fetch(duplicateSourceUrl)).thenReturn(SPEC_V1);
                String requestBody = objectMapper.writeValueAsString(
                                java.util.Map.of(
                                                "name", "Duplicate Test API",
                                                "providerName", "Test Provider",
                                                "sourceUrl", duplicateSourceUrl,
                                                "description", "Duplicate registration test"));
                // First registration should succeed.
                MvcResult createResult = mockMvc.perform(
                                post("/api/dependencies")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(requestBody))
                                .andExpect(status().isCreated())
                                .andReturn();
                JsonNode created = objectMapper.readTree(
                                createResult.getResponse().getContentAsString());
                createdDependencyId = created.get("id").asText();
                assertTrue(dependencyRepository.existsById(createdDependencyId));
                // Registering the same URL again should return 409 Conflict.
                MvcResult duplicateResult = mockMvc.perform(
                                post("/api/dependencies")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(requestBody))
                                .andExpect(status().isConflict())
                                .andReturn();
                JsonNode error = objectMapper.readTree(
                                duplicateResult.getResponse().getContentAsString());
                assertEquals(409, error.get("status").asInt());
                assertTrue(error.get("message").asText()
                                .contains("source URL already exists"));
                // The duplicate attempt must not fetch or create another dependency.
                org.mockito.Mockito.verify(
                                specFetcher, org.mockito.Mockito.times(1))
                                .fetch(duplicateSourceUrl);
                assertEquals(
                                1L,
                                dependencyRepository.findAll().stream()
                                                .filter(dependency -> duplicateSourceUrl
                                                                .equals(dependency.getSourceUrl()))
                                                .count());
        }
}
