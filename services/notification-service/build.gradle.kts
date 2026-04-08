plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared:common"))
    implementation(project(":shared:events"))
    implementation(project(":shared:persistence"))
    implementation(libs.koin.core)
    implementation(libs.bundles.ktor.server.base)
}
