plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass = "com.example.foodike.ApplicationKt"
}

dependencies {
    implementation(project(":shared:common"))
    implementation(project(":shared:events"))
    implementation(project(":shared:auth"))
    implementation(project(":shared:persistence"))
    implementation(project(":services:user-service"))
    implementation(project(":services:restaurant-service"))
    implementation(project(":services:order-service"))
    implementation(project(":services:payment-service"))
    implementation(project(":services:notification-service"))
    implementation(project(":services:tracking-service"))

    implementation(libs.bundles.ktor.server.base)
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.logback.classic)
}
