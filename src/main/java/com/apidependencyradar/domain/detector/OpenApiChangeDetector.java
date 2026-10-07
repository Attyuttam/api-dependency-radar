package com.apidependencyradar.domain.detector;

import com.apidependencyradar.domain.model.ApiChange;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.Schema;

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

                detectRemovedRequestBodyProperties(
                        path,
                        method,
                        oldOperation,
                        newOperation,
                        changes);

                detectRemovedResponseProperties(
                        path,
                        method,
                        oldOperation,
                        newOperation,
                        changes);

                detectRequiredResponseProperties(
                        path,
                        method,
                        oldOperation,
                        newOperation,
                        changes);

                detectRequestParameterTypeChanges(
                        path,
                        method,
                        oldOperation,
                        newOperation,
                        changes);

                detectRequestEnumValueChanges(
                        path,
                        method,
                        oldOperation,
                        newOperation,
                        changes);

                detectRequestPropertyTypeChanges(
                        path,
                        method,
                        oldOperation,
                        newOperation,
                        changes);

                detectResponsePropertyTypeChanges(
                        path,
                        method,
                        oldOperation,
                        newOperation,
                        changes);

                detectResponseEnumValueChanges(
                        path,
                        method,
                        oldOperation,
                        newOperation,
                        changes);

                detectRemovedResponseStatusCodes(
                        path,
                        method,
                        oldOperation,
                        newOperation,
                        changes);
            }
        }
    }

    private void detectRemovedRequestBodyProperties(
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

        if (oldSchema.getProperties() == null) {
            return;
        }

        var newProperties = newSchema.getProperties() == null
                ? Map.of()
                : newSchema.getProperties();

        for (Object property : oldSchema.getProperties().keySet()) {

            if (!(property instanceof String propertyName)) {
                continue;
            }

            if (!newProperties.containsKey(propertyName)) {
                changes.add(new ApiChange(
                        "OPENAPI",
                        "REQUEST_PROPERTY_REMOVED",
                        method + " " + path,
                        "Request body property '" + propertyName + "' was removed",
                        true));
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

        var oldSchema = getJsonRequestBodySchema(oldOperation);
        var newSchema = getJsonRequestBodySchema(newOperation);

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

    private void detectRemovedResponseProperties(
            String path,
            String method,
            Operation oldOperation,
            Operation newOperation,
            List<ApiChange> changes) {

        var oldSchema = getJsonResponseSchema(oldOperation);
        var newSchema = getJsonResponseSchema(newOperation);

        if (oldSchema == null || newSchema == null) {
            return;
        }
        var newProperties = newSchema.getProperties() == null
                ? Map.of()
                : newSchema.getProperties();

        for (Object property : oldSchema.getProperties().keySet()) {

            if (!(property instanceof String propertyName)) {
                continue;
            }

            if (!newProperties.containsKey(propertyName)) {
                changes.add(new ApiChange(
                        "OPENAPI",
                        "RESPONSE_PROPERTY_REMOVED",
                        method + " " + path,
                        "Response property '" + propertyName + "' was removed",
                        true));
            }
        }
    }

    private void detectRequiredResponseProperties(
            String path,
            String method,
            Operation oldOperation,
            Operation newOperation,
            List<ApiChange> changes) {

        var oldSchema = getJsonResponseSchema(oldOperation);
        var newSchema = getJsonResponseSchema(newOperation);

        if (oldSchema == null || newSchema == null) {
            return;
        }

        List<String> oldRequired = getRequiredProperties(oldSchema.getRequired());

        List<String> newRequired = getRequiredProperties(newSchema.getRequired());

        for (String property : newRequired) {

            if (!oldRequired.contains(property)) {

                changes.add(new ApiChange(
                        "OPENAPI",
                        "REQUIRED_RESPONSE_PROPERTY",
                        method + " " + path,
                        "Response property '" + property + "' became required",
                        true));
            }
        }
    }

    private void detectRequestParameterTypeChanges(
            String path,
            String method,
            Operation oldOperation,
            Operation newOperation,
            List<ApiChange> changes) {

        if (oldOperation == null || newOperation == null) {
            return;
        }

        List<Parameter> oldParameters = oldOperation.getParameters() == null
                ? List.of()
                : oldOperation.getParameters();

        List<Parameter> newParameters = newOperation.getParameters() == null
                ? List.of()
                : newOperation.getParameters();

        for (Parameter oldParameter : oldParameters) {

            Parameter newParameter = findParameter(newParameters, oldParameter);

            if (newParameter == null
                    || oldParameter.getSchema() == null
                    || newParameter.getSchema() == null) {
                continue;
            }

            String oldType = oldParameter.getSchema().getType();
            String newType = newParameter.getSchema().getType();

            if (!java.util.Objects.equals(oldType, newType)) {
                changes.add(new ApiChange(
                        "OPENAPI",
                        "REQUEST_PARAMETER_TYPE_CHANGED",
                        method + " " + path,
                        "Parameter '" + oldParameter.getName()
                                + "' changed type from '" + oldType
                                + "' to '" + newType + "'",
                        true));
            }
        }
    }

    private void detectRequestPropertyTypeChanges(
            String path,
            String method,
            Operation oldOperation,
            Operation newOperation,
            List<ApiChange> changes) {

        var oldSchema = getJsonRequestBodySchema(oldOperation);
        var newSchema = getJsonRequestBodySchema(newOperation);

        if (oldSchema == null || newSchema == null
                || oldSchema.getProperties() == null
                || newSchema.getProperties() == null) {
            return;
        }

        for (Object property : oldSchema.getProperties().keySet()) {

            if (!(property instanceof String propertyName)) {
                continue;
            }

            var oldProperty = oldSchema.getProperties().get(propertyName);
            var newProperty = newSchema.getProperties().get(propertyName);

            if (oldProperty == null || newProperty == null) {
                continue;
            }

            String oldType = oldProperty.getType();
            String newType = newProperty.getType();

            if (!java.util.Objects.equals(oldType, newType)) {
                changes.add(new ApiChange(
                        "OPENAPI",
                        "REQUEST_PROPERTY_TYPE_CHANGED",
                        method + " " + path,
                        "Request property '" + propertyName
                                + "' changed type from '" + oldType
                                + "' to '" + newType + "'",
                        true));
            }
        }
    }

    private void detectResponsePropertyTypeChanges(
            String path,
            String method,
            Operation oldOperation,
            Operation newOperation,
            List<ApiChange> changes) {

        var oldSchema = getJsonResponseSchema(oldOperation);
        var newSchema = getJsonResponseSchema(newOperation);

        if (oldSchema == null || newSchema == null
                || oldSchema.getProperties() == null
                || newSchema.getProperties() == null) {
            return;
        }

        for (Object property : oldSchema.getProperties().keySet()) {

            if (!(property instanceof String propertyName)) {
                continue;
            }

            var oldProperty = oldSchema.getProperties().get(propertyName);
            var newProperty = newSchema.getProperties().get(propertyName);

            if (oldProperty == null || newProperty == null) {
                continue;
            }

            String oldType = oldProperty.getType();
            String newType = newProperty.getType();

            if (!java.util.Objects.equals(oldType, newType)) {
                changes.add(new ApiChange(
                        "OPENAPI",
                        "RESPONSE_PROPERTY_TYPE_CHANGED",
                        method + " " + path,
                        "Response property '" + propertyName
                                + "' changed type from '" + oldType
                                + "' to '" + newType + "'",
                        true));
            }
        }
    }

    private void detectRequestEnumValueChanges(
            String path,
            String method,
            Operation oldOperation,
            Operation newOperation,
            List<ApiChange> changes) {

        if (oldOperation == null || newOperation == null) {
            return;
        }

        List<Parameter> oldParameters = oldOperation.getParameters() == null
                ? List.of()
                : oldOperation.getParameters();

        List<Parameter> newParameters = newOperation.getParameters() == null
                ? List.of()
                : newOperation.getParameters();

        for (Parameter oldParameter : oldParameters) {

            Parameter newParameter = findParameter(newParameters, oldParameter);

            if (newParameter == null
                    || oldParameter.getSchema() == null
                    || newParameter.getSchema() == null) {
                continue;
            }

            var oldEnum = oldParameter.getSchema().getEnum();
            var newEnum = newParameter.getSchema().getEnum();

            if (oldEnum == null || newEnum == null) {
                continue;
            }

            for (Object value : oldEnum) {
                if (!newEnum.contains(value)) {
                    changes.add(new ApiChange(
                            "OPENAPI",
                            "REQUEST_ENUM_VALUE_REMOVED",
                            method + " " + path,
                            "Enum value '" + value + "' was removed from parameter '"
                                    + oldParameter.getName() + "'",
                            true));
                }
            }
        }
    }

    private void detectResponseEnumValueChanges(
            String path,
            String method,
            Operation oldOperation,
            Operation newOperation,
            List<ApiChange> changes) {

        var oldSchema = getJsonResponseSchema(oldOperation);
        var newSchema = getJsonResponseSchema(newOperation);

        if (oldSchema == null || newSchema == null
                || oldSchema.getProperties() == null
                || newSchema.getProperties() == null) {
            return;
        }

        for (Object property : oldSchema.getProperties().keySet()) {

            if (!(property instanceof String propertyName)) {
                continue;
            }

            var oldProperty = oldSchema.getProperties().get(propertyName);
            var newProperty = newSchema.getProperties().get(propertyName);

            if (oldProperty == null || newProperty == null) {
                continue;
            }

            var oldEnum = oldProperty.getEnum();
            var newEnum = newProperty.getEnum();

            if (oldEnum == null || newEnum == null) {
                continue;
            }

            for (Object value : oldEnum) {
                if (!newEnum.contains(value)) {
                    changes.add(new ApiChange(
                            "OPENAPI",
                            "RESPONSE_ENUM_VALUE_REMOVED",
                            method + " " + path,
                            "Enum value '" + value
                                    + "' was removed from response property '"
                                    + propertyName + "'",
                            true));
                }
            }
        }
    }

    private void detectRemovedResponseStatusCodes(
            String path,
            String method,
            Operation oldOperation,
            Operation newOperation,
            List<ApiChange> changes) {

        if (oldOperation == null
                || oldOperation.getResponses() == null
                || newOperation == null) {
            return;
        }

        var newResponses = newOperation.getResponses();

        for (String statusCode : oldOperation.getResponses().keySet()) {

            if (!newResponses.containsKey(statusCode)) {
                changes.add(new ApiChange(
                        "OPENAPI",
                        "RESPONSE_STATUS_REMOVED",
                        method + " " + path,
                        "Response status '" + statusCode + "' was removed",
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

    private Schema<?> getJsonResponseSchema(
            Operation operation) {

        if (operation == null || operation.getResponses() == null) {
            return null;
        }

        var response = operation.getResponses().get("200");

        if (response == null || response.getContent() == null) {
            return null;
        }

        var jsonMediaType = response.getContent().get("application/json");

        if (jsonMediaType == null) {
            return null;
        }

        return jsonMediaType.getSchema();
    }

    private Schema<?> getJsonRequestBodySchema(
            Operation operation) {

        if (operation == null || operation.getRequestBody() == null) {
            return null;
        }

        var content = operation.getRequestBody().getContent();

        if (content == null) {
            return null;
        }

        var jsonMediaType = content.get("application/json");

        if (jsonMediaType == null) {
            return null;
        }

        return jsonMediaType.getSchema();
    }
}