package com.apidependencyradar.domain.model;

/*
example:

sourceType = OPENAPI
type       = OPERATION_REMOVED
endpoint   = GET /users
description = GET /users was removed
breaking   = true
*/
public record ApiChange(
        String sourceType,
        String type,
        String endpoint,
        String description,
        boolean breaking) {
}