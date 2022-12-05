import com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer
import com.expediagroup.graphql.plugin.gradle.graphql
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateTestClientTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktorVersion = "2.1.0"
val koinVersion = "3.2.0"
val logbackVersion = "1.4.0"
val valiktorVersion = "0.12.0"
val exposedVersion = "0.39.2"
val hikariVersion = "5.0.1"
val flywayVersion = "9.8.3"
val h2Version = "2.1.210"
val junitVersion = "5.9.0"
val testContainersVersion = "1.17.3"
val kotlinGraphQLVersion = "6.2.2"
val kotlinDateTimeVersion = "0.4.0"
val mariadbClientVersion = "3.1.0"
val jacksonJsr310Version = "2.14.1"
val webpImageIoVersion = "0.1.6"
val jNanoIdVersion = "2.0.0"
val apacheCommonsLang3Version = "3.12.0"
val micrometerPrometheusVersion = "1.9.3"

plugins {
    application
    kotlin("jvm") version "1.7.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.10"
    id("com.expediagroup.graphql") version "6.3.0"
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
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinDateTimeVersion")

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
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client:$mariadbClientVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.flywaydb:flyway-mysql:$flywayVersion")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")

    // graphql
    implementation("com.expediagroup:graphql-kotlin-server:$kotlinGraphQLVersion")
    implementation("com.expediagroup:graphql-kotlin-hooks-provider:$kotlinGraphQLVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonJsr310Version")

    // di
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-ktor:$koinVersion")

    implementation("org.sejda.imageio:webp-imageio:$webpImageIoVersion")
    implementation("com.aventrix.jnanoid:jnanoid:$jNanoIdVersion")
    implementation("org.apache.commons:commons-lang3:$apacheCommonsLang3Version")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    // validation
    implementation("org.valiktor:valiktor-core:$valiktorVersion")

    // metrics
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerPrometheusVersion")

    // tests
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")
    testImplementation("com.expediagroup:graphql-kotlin-ktor-client:$kotlinGraphQLVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.testcontainers:mariadb:$testContainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testContainersVersion")
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

// generates classes needed for graphql tests
tasks.withType<GraphQLGenerateTestClientTask> {
    dependsOn("graphqlGenerateSDL")

    schemaFile.set(file("${project.buildDir}/schema.graphql"))
    queryFileDirectory.set(file("src/test/resources/graphql"))
    packageName.set("pl.newbies.generated")
    serializer.set(GraphQLSerializer.KOTLINX)
}

// generates schema.graphql needed for graphql tests
graphql {
    schema {
        packages = listOf("pl.newbies")
    }
}