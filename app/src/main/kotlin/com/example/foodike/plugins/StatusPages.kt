package com.example.foodike.plugins

import com.example.foodike.common.exception.ConflictException
import com.example.foodike.common.exception.DomainException
import com.example.foodike.common.exception.ForbiddenException
import com.example.foodike.common.exception.NotFoundException
import com.example.foodike.common.exception.UnauthorizedException
import com.example.foodike.common.exception.ValidationException
import com.example.foodike.common.model.ErrorResponse
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.log
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.UnsupportedMediaTypeException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.exception
import io.ktor.server.response.respond

fun Application.configureStatusPages() {
    val logger = log

    install(StatusPages) {
        exception<DomainException> { call: ApplicationCall, cause: DomainException ->
            val status = when (cause) {
                is ValidationException -> HttpStatusCode.BadRequest
                is NotFoundException -> HttpStatusCode.NotFound
                is ConflictException -> HttpStatusCode.Conflict
                is UnauthorizedException -> HttpStatusCode.Unauthorized
                is ForbiddenException -> HttpStatusCode.Forbidden
                else -> HttpStatusCode.InternalServerError
            }

            val details = if (cause is ValidationException) cause.validationErrors else emptyMap()
            call.respond(status, ErrorResponse(code = cause.code, message = cause.message ?: cause.code, details = details))
        }

        exception<BadRequestException> { call: ApplicationCall, cause: BadRequestException ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(code = "BAD_REQUEST", message = cause.message ?: "Malformed or invalid request body"),
            )
        }

        exception<UnsupportedMediaTypeException> { call: ApplicationCall, _: UnsupportedMediaTypeException ->
            call.respond(
                HttpStatusCode.UnsupportedMediaType,
                ErrorResponse(code = "UNSUPPORTED_MEDIA_TYPE", message = "Unsupported content type"),
            )
        }

        exception<Throwable> { call: ApplicationCall, cause: Throwable ->
            logger.error("Unhandled exception on ${call.request.local.method.value} ${call.request.local.uri}", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(code = "INTERNAL_ERROR", message = "An unexpected error occurred"),
            )
        }

        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(status, ErrorResponse(code = "NOT_FOUND", message = "Resource not found"))
        }

        status(HttpStatusCode.MethodNotAllowed) { call, status ->
            call.respond(status, ErrorResponse(code = "METHOD_NOT_ALLOWED", message = "Method not allowed for this resource"))
        }

        status(HttpStatusCode.UnsupportedMediaType) { call, status ->
            call.respond(status, ErrorResponse(code = "UNSUPPORTED_MEDIA_TYPE", message = "Unsupported content type"))
        }
    }
}
