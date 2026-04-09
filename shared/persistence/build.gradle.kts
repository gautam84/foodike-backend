plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":shared:common"))
    api(libs.exposed.core)
    api(libs.exposed.jdbc)
    api(libs.exposed.java.time)
    api(libs.hikari.cp)
    runtimeOnly(libs.h2)
    runtimeOnly(libs.postgresql)
}
