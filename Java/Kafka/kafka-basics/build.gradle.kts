plugins {
    id("java")
}

group = "io.kafka.project"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

    implementation("org.apache.kafka:kafka-clients:3.1.0")
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("org.slf4j:slf4j-simple:1.7.36")

}

tasks.test {
    useJUnitPlatform()
}