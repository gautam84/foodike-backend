package com.example.foodike.common.exception

sealed class DomainException(
    val code: String,
    message: String,
) : RuntimeException(message)

class NotFoundException(message: String) : DomainException("NOT_FOUND", message)

class ValidationException(
    message: String,
    val validationErrors: Map<String, String> = emptyMap(),
) : DomainException("VALIDATION_ERROR", message)

class ConflictException(message: String) : DomainException("CONFLICT", message)

class UnauthorizedException(message: String = "Unauthorized") : DomainException("UNAUTHORIZED", message)

class ForbiddenException(message: String = "Forbidden") : DomainException("FORBIDDEN", message)
