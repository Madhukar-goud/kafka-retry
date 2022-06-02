import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.6.3"
	id("io.spring.dependency-management") version "1.0.11.RELEASE"
	kotlin("jvm") version "1.6.10"
	kotlin("plugin.spring") version "1.6.10"
	id("com.github.davidmc24.gradle.plugin.avro") version "1.2.0"
}

group = "com.asurint.keystone.api"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
	mavenCentral()
	gradlePluginPortal()
	maven("https://packages.confluent.io/maven/")
}
sourceSets {
	main {
		resources {
			srcDirs("src/main/avro")
		}
	}
}
//java.sourceSets.create("src/main/avro")
dependencies {
	//implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("io.github.microutils:kotlin-logging-jvm:2.1.21")
	implementation("org.apache.logging.log4j:log4j-core:2.17.0")
	implementation("io.confluent:kafka-avro-serializer:6.1.0")
	implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.17.0")
	implementation("com.jayway.jsonpath:json-path:2.7.0")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
	implementation("org.springframework.kafka:spring-kafka:2.8.6")
	implementation( "org.apache.kafka:kafka-streams")
	implementation("org.springframework.kafka:spring-kafka")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
}
configurations.implementation {
	exclude("ch.qos.logback","logback-core")
	exclude("ch.qos.logback","logback-classic")
	exclude("org.apache.logging.log4j","log4j-to-slf4j")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "1.8"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
avro {
	setGettersReturnOptional(true)
	setOptionalGettersForNullableFieldsOnly(true)
	setFieldVisibility("PUBLIC")
}