plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":shared:common"))
    implementation(libs.java.jwt)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
}
