package com.apidependencyradar.domain.detector;

import com.apidependencyradar.domain.model.ApiChange;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.oas.models.parameters.Parameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OpenApiChangeDetector implements ApiChangeDetector {

    private final OpenAPIV3Parser parser = new OpenAPIV3Parser();

    @Override
    public List<ApiChange> detect(String oldSpec, String newSpec) {

        OpenAPI oldApi = parse(oldSpec);
        OpenAPI newApi = parse(newSpec);

        List<ApiChange> changes = new ArrayList<>();

        detectRemovedOperations(oldApi, newApi, changes);

        return changes;
    }

    private OpenAPI parse(String specification) {

        var result = parser.readContents(specification, null, null);

        if (result.getOpenAPI() == null) {
            throw new IllegalArgumentException(
                    "Unable to parse OpenAPI specification");
        }

        return result.getOpenAPI();
    }

    private void detectRemovedOperations(
            OpenAPI oldApi,
            OpenAPI newApi,
            List<ApiChange> changes) {

        if (oldApi.getPaths() == null) {
            return;
        }

        for (Map.Entry<String, PathItem> entry : oldApi.getPaths().entrySet()) {

            String path = entry.getKey();

            PathItem oldPath = entry.getValue();
            PathItem newPath = newApi.getPaths() == null
                    ? null
                    : newApi.getPaths().get(path);

            if (newPath == null) {

                addAllOperationsAsRemoved(
                        path,
                        oldPath,
                        changes);

                continue;
            }

            Map<String, Operation> oldOperations = getOperations(oldPath);
            Map<String, Operation> newOperations = getOperations(newPath);

            for (Map.Entry<String, Operation> operationEntry : oldOperations.entrySet()) {

                String method = operationEntry.getKey();
                Operation oldOperation = operationEntry.getValue();
                Operation newOperation = newOperations.get(method);

                detectRemovedOperation(
                        path,
                        method,
                        oldOperation,
                        newOperation,
                        changes);

                detectRequiredParameterChanges(
                        path,
                        method,
                        oldOperation,
                        newOperation,
                        changes);

                detectRequiredRequestBodyProperties(
                        path,
                        method,
                        oldOperation,
                        newOperation,
                        changes);
            }
        }
    }

    private void detectRequiredParameterChanges(
            String path,
            String method,
            Operation oldOperation,
            Operation newOperation,
            List<ApiChange> changes) {

        if (oldOperation == null || newOperation == null) {
            return;
        }

        List<Parameter> oldParameters = oldOperation.getParameters();

        if (oldParameters == null) {
            oldParameters = List.of();
        }

        List<Parameter> newParameters = newOperation.getParameters();

        if (newParameters == null) {
            newParameters = List.of();
        }

        for (Parameter oldParameter : oldParameters) {

            Parameter newParameter = findParameter(
                    newParameters,
                    oldParameter);

            if (newParameter == null) {
                continue;
            }

            boolean wasOptional = !Boolean.TRUE.equals(
                    oldParameter.getRequired());

            boolean isRequired = Boolean.TRUE.equals(
                    newParameter.getRequired());

            if (wasOptional && isRequired) {

                changes.add(new ApiChange(
                        "OPENAPI",
                        "REQUIRED_PARAMETER",
                        method + " " + path,
                        "Parameter '" + oldParameter.getName()
                                + "' became required",
                        true));
            }
        }
    }

    private void addAllOperationsAsRemoved(
            String path,
            PathItem oldPath,
            List<ApiChange> changes) {

        Map<String, Operation> oldOperations = getOperations(oldPath);

        for (Map.Entry<String, Operation> entry : oldOperations.entrySet()) {

            detectRemovedOperation(
                    path,
                    entry.getKey(),
                    entry.getValue(),
                    null,
                    changes);
        }
    }

    private void detectRemovedOperation(
            String path,
            String method,
            Operation oldOperation,
            Operation newOperation,
            List<ApiChange> changes) {

        if (oldOperation != null && newOperation == null) {

            changes.add(new ApiChange(
                    "OPENAPI",
                    "OPERATION_REMOVED",
                    method + " " + path,
                    method + " " + path + " was removed",
                    true));
        }
    }

    private Parameter findParameter(
            List<Parameter> parameters,
            Parameter target) {

        return parameters.stream()
                .filter(parameter -> parameter.getName().equals(target.getName())
                        && parameter.getIn().equals(target.getIn()))
                .findFirst()
                .orElse(null);
    }

    private Map<String, Operation> getOperations(PathItem pathItem) {

        if (pathItem == null) {
            return Map.of();
        }

        Map<String, Operation> operations = new java.util.LinkedHashMap<>();

        if (pathItem.getGet() != null) {
            operations.put("GET", pathItem.getGet());
        }

        if (pathItem.getPost() != null) {
            operations.put("POST", pathItem.getPost());
        }

        if (pathItem.getPut() != null) {
            operations.put("PUT", pathItem.getPut());
        }

        if (pathItem.getDelete() != null) {
            operations.put("DELETE", pathItem.getDelete());
        }

        if (pathItem.getPatch() != null) {
            operations.put("PATCH", pathItem.getPatch());
        }

        if (pathItem.getHead() != null) {
            operations.put("HEAD", pathItem.getHead());
        }

        if (pathItem.getOptions() != null) {
            operations.put("OPTIONS", pathItem.getOptions());
        }

        if (pathItem.getTrace() != null) {
            operations.put("TRACE", pathItem.getTrace());
        }

        return operations;
    }

    private void detectRequiredRequestBodyProperties(
            String path,
            String method,
            Operation oldOperation,
            Operation newOperation,
            List<ApiChange> changes) {

        if (oldOperation == null || newOperation == null) {
            return;
        }

        if (oldOperation.getRequestBody() == null
                || newOperation.getRequestBody() == null) {
            return;
        }

        var oldContent = oldOperation.getRequestBody().getContent();
        var newContent = newOperation.getRequestBody().getContent();

        if (oldContent == null || newContent == null) {
            return;
        }

        var oldJsonMediaType = oldContent.get("application/json");
        var newJsonMediaType = newContent.get("application/json");

        if (oldJsonMediaType == null || newJsonMediaType == null) {
            return;
        }

        var oldSchema = oldJsonMediaType.getSchema();
        var newSchema = newJsonMediaType.getSchema();

        if (oldSchema == null || newSchema == null) {
            return;
        }

        List<String> oldRequired = getRequiredProperties(oldSchema.getRequired());
        List<String> newRequired = getRequiredProperties(newSchema.getRequired());

        for (String property : newRequired) {
            if (!oldRequired.contains(property)) {
                changes.add(new ApiChange(
                        "OPENAPI",
                        "REQUIRED_REQUEST_PROPERTY",
                        method + " " + path,
                        "Request body property '" + property + "' became required",
                        true));
            }
        }
    }

    private List<String> getRequiredProperties(List<?> required) {
        if (required == null) {
            return List.of();
        }

        return required.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }
}