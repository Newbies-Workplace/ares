import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktorVersion = "2.0.1"
val koinVersion = "3.2.0"
val logbackVersion = "1.2.11"
val valiktorVersion = "0.12.0"
val exposedVersion = "0.38.2"
val hikariVersion = "5.0.1"
val flywayVersion = "8.5.11"
val h2Version = "2.1.210"
val junitVersion = "5.8.2"
val testContainers = "1.17.2"
val kGraphQLVersion = "0.17.14"
val kotlinGraphQL = "5.4.1"

plugins {
    application
    kotlin("jvm") version "1.6.21"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.6.21"
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
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.3")

    // ktor
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-client-apache-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-serialization-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // database
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client:3.0.4")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.flywaydb:flyway-mysql:$flywayVersion")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")

    // graphql
    implementation("com.expediagroup:graphql-kotlin-server:$kotlinGraphQL")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.3")

    // di
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-ktor:$koinVersion")

    implementation("org.sejda.imageio:webp-imageio:0.1.6")
    implementation("com.aventrix.jnanoid:jnanoid:2.0.0")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    // validation
    implementation("org.valiktor:valiktor-core:$valiktorVersion")

    // metrics
    implementation("io.micrometer:micrometer-registry-prometheus:1.9.0")

    // tests
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.testcontainers:mariadb:$testContainers")
    testImplementation("org.testcontainers:junit-jupiter:$testContainers")
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