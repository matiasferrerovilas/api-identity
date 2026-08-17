package com.api.identity.exceptions;

// No extiende DomainException a propósito: esa jerarquía está sellada (permits explícito) para
// los switch exhaustivos existentes sobre errores de dominio de negocio — esta es una excepción
// de infraestructura (protección contra abuso), como AuthenticationException o
// DataIntegrityViolationException, que ErrorHandler ya maneja por fuera de esa jerarquía.
public final class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}
