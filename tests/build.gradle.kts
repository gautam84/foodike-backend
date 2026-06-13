plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(project(":app"))
    testImplementation(project(":shared:auth"))
    testImplementation(project(":services:user-service"))
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.websockets)
    testImplementation(libs.ktor.server.config.yaml)
    testImplementation(libs.koin.core)
    testImplementation(libs.kotlin.test.junit)
}
