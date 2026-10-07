package com.apidependencyradar.domain.detector;

import com.apidependencyradar.domain.model.ApiChange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiChangeDetectorTest {

    private final OpenApiChangeDetector detector = new OpenApiChangeDetector();

    @Test
    void shouldDetectEndpointCompletelyRemoved() {
        // /users path is present in old spec but not in new spec
        String oldSpec = """
                openapi: 3.0.0
                info:
                  title: Test API
                  version: 1.0.0
                paths:
                  /users:
                    get:
                      responses:
                        "200":
                          description: OK
                """;

        String newSpec = """
                openapi: 3.0.0
                info:
                  title: Test API
                  version: 2.0.0
                paths:
                """;
        List<ApiChange> changes = detector.detect(oldSpec, newSpec);

        assertEquals(1, changes.size());

        ApiChange change = changes.get(0);

        assertEquals("OPENAPI", change.sourceType());
        assertEquals("OPERATION_REMOVED", change.type());
        assertEquals("GET /users", change.endpoint());
        assertTrue(change.breaking());
    }

    @Test
    void shouldDetectRemovedPostOperation() {

        String oldSpec = """
                openapi: 3.0.0
                info:
                  title: Test API
                  version: 1.0.0
                paths:
                  /users:
                    post:
                      responses:
                        "200":
                          description: OK
                """;

        String newSpec = """
                openapi: 3.0.0
                info:
                  title: Test API
                  version: 2.0.0
                paths:
                """;

        List<ApiChange> changes = detector.detect(oldSpec, newSpec);

        assertEquals(1, changes.size());

        ApiChange change = changes.get(0);

        assertEquals("OPENAPI", change.sourceType());
        assertEquals("OPERATION_REMOVED", change.type());
        assertEquals("POST /users", change.endpoint());
        assertTrue(change.breaking());

    }

    @Test
    void shouldDetectMultipleRemovedOperations() {

        String oldSpec = """
                openapi: 3.0.0
                info:
                  title: Test API
                  version: 1.0.0
                paths:
                  /users:
                    get:
                      responses:
                        "200":
                          description: OK
                    post:
                      responses:
                        "201":
                          description: Created
                """;

        String newSpec = """
                openapi: 3.0.0
                info:
                  title: Test API
                  version: 2.0.0
                paths:
                """;

        List<ApiChange> changes = detector.detect(oldSpec, newSpec);

        assertEquals(2, changes.size());

        ApiChange getChange = changes.get(0);

        assertEquals("OPENAPI", getChange.sourceType());
        assertEquals("OPERATION_REMOVED", getChange.type());
        assertEquals("GET /users", getChange.endpoint());
        assertTrue(getChange.breaking());

        ApiChange postChange = changes.get(1);

        assertEquals("OPENAPI", postChange.sourceType());
        assertEquals("OPERATION_REMOVED", postChange.type());
        assertEquals("POST /users", postChange.endpoint());
        assertTrue(postChange.breaking());
    }

    @Test
    void shouldDetectNoChange() {

        String oldSpec = """
                openapi: 3.0.0
                info:
                  title: Test API
                  version: 1.0.0
                paths:
                  /users:
                    get:
                      responses:
                        "200":
                          description: OK
                """;

        String newSpec = """
                openapi: 3.0.0
                info:
                  title: Test API
                  version: 2.0.0
                paths:
                  /users:
                    get:
                      responses:
                        "200":
                          description: OK
                """;

        List<ApiChange> changes = detector.detect(oldSpec, newSpec);

        assertEquals(0, changes.size());

    }

    @Test
    void shouldDetectNewEndpoint() {

        String oldSpec = """
                openapi: 3.0.0
                info:
                  title: Test API
                  version: 1.0.0
                paths:
                    /users:
                        get:
                            responses:
                                "200":
                                    description: OK
                """;

        String newSpec = """
                openapi: 3.0.0
                info:
                  title: Test API
                  version: 2.0.0
                paths:
                    /users:
                        get:
                            responses:
                                "200":
                                    description: OK
                    /orders:
                        get:
                            responses:
                                "200":
                                    description: OK
                """;

        List<ApiChange> changes = detector.detect(oldSpec, newSpec);

        assertEquals(0, changes.size());
    }

    @Test
    void shouldDetectRemovedGetOperation() {

        String oldSpec = """
                openapi: 3.0.0
                info:
                  title: Test API
                  version: 1.0.0
                paths:
                  /users:
                    get:
                      responses:
                        "200":
                          description: OK
                """;

        String newSpec = """
                openapi: 3.0.0
                info:
                  title: Test API
                  version: 2.0.0
                paths:
                  /users:
                    post:
                      responses:
                        "200":
                          description: OK
                """;

        List<ApiChange> changes = detector.detect(oldSpec, newSpec);

        assertEquals(1, changes.size());

        ApiChange change = changes.get(0);

        assertEquals("OPENAPI", change.sourceType());
        assertEquals("OPERATION_REMOVED", change.type());
        assertEquals("GET /users", change.endpoint());
        assertTrue(change.breaking());
    }

    @Test
    void shouldDetectRequiredParameterAdded() {

        String oldSpec = """
                openapi: 3.0.0
                info:
                  title: Test API
                  version: 1.0.0
                paths:
                  /users:
                    get:
                      parameters:
                        - name: country
                          in: query
                          required: false
                          schema:
                            type: string
                      responses:
                        "200":
                          description: OK
                """;

        String newSpec = """
                openapi: 3.0.0
                info:
                  title: Test API
                  version: 2.0.0
                paths:
                  /users:
                    get:
                      parameters:
                        - name: country
                          in: query
                          required: true
                          schema:
                            type: string
                      responses:
                        "200":
                          description: OK
                """;

        List<ApiChange> changes = detector.detect(oldSpec, newSpec);

        assertEquals(1, changes.size());

        ApiChange change = changes.get(0);

        assertEquals("OPENAPI", change.sourceType());
        assertEquals("REQUIRED_PARAMETER", change.type());
        assertEquals("GET /users", change.endpoint());
        assertTrue(change.breaking());
    }

    @Test
    void shouldDetectRequestBodyPropertyBecomingRequired() {

        String oldSpec = """
                openapi: 3.0.0
                info:
                  title: Test API
                  version: 1.0.0
                paths:
                  /users:
                    post:
                      requestBody:
                        required: true
                        content:
                          application/json:
                            schema:
                              type: object
                              properties:
                                name:
                                  type: string
                      responses:
                        "201":
                          description: Created
                """;

        String newSpec = """
                openapi: 3.0.0
                info:
                  title: Test API
                  version: 2.0.0
                paths:
                  /users:
                    post:
                      requestBody:
                        required: true
                        content:
                          application/json:
                            schema:
                              type: object
                              properties:
                                name:
                                  type: string
                                email:
                                  type: string
                              required:
                                - email
                      responses:
                        "201":
                          description: Created
                """;

        List<ApiChange> changes = detector.detect(oldSpec, newSpec);

        assertEquals(1, changes.size());

        ApiChange change = changes.get(0);

        assertEquals("OPENAPI", change.sourceType());
        assertEquals("REQUIRED_REQUEST_PROPERTY", change.type());
        assertEquals("POST /users", change.endpoint());
        assertTrue(change.breaking());
    }
}