import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktorVersion = "2.0.0-beta-1"
val koinVersion = "3.1.6"
val kotlinVersion = "1.6.10"
val logbackVersion = "1.2.11"
val valiktorVersion = "0.12.0"
val exposedVersion = "0.37.3"
val hikariVersion = "5.0.1"
val flywayVersion = "8.5.7"
val h2Version = "2.1.210"
val junitVersion = "5.8.2"
val testContainers = "1.16.3"
val kGraphQLVersion = "0.17.14"
val kotlinGraphQL = "5.3.2"

plugins {
    application
    kotlin("jvm") version "1.6.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.6.20"
}

group = "pl.newbies"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

repositories {
    mavenCentral()
}

dependencies {
    // kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.2")

    // ktor
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-host-common:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")

    // database
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")

    // graphql
    implementation("com.expediagroup:graphql-kotlin-server:$kotlinGraphQL")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.2")

    runtimeOnly("org.mariadb.jdbc:mariadb-java-client:3.0.4")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.flywaydb:flyway-mysql:$flywayVersion")

    // di
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-ktor:$koinVersion")

    implementation("org.sejda.imageio:webp-imageio:0.1.6")
    implementation("com.aventrix.jnanoid:jnanoid:2.0.0")

    // validation
    implementation("org.valiktor:valiktor-core:$valiktorVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-serialization:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    // tests
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.testcontainers:mariadb:$testContainers")
    testImplementation("org.testcontainers:junit-jupiter:$testContainers")
    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
}

java {
    sourceCompatibility = JavaVersion.toVersion("11")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}