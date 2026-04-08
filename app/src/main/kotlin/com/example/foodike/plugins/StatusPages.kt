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
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.exception
import io.ktor.server.response.respond

fun Application.configureStatusPages() {
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
    }
}
