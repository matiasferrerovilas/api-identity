package com.api.identity.exceptions;

public record ErrorResponse(String statusCode, String title, String detail) {
}
