import org.gradle.internal.declarativedsl.parsing.main

plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("application")
	id("org.springframework.boot") version "3.4.0"
	id("io.spring.dependency-management") version "1.1.6"
	kotlin("plugin.jpa") version "1.9.25"
}

application {
	mainClass.set("mikhail.shell.video.hosting.ApplicationKt")
}

group = "mikhail.shell"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
	sourceCompatibility = JavaVersion.VERSION_17
    	targetCompatibility = JavaVersion.VERSION_17
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")
	runtimeOnly("com.mysql:mysql-connector-j")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation ("io.jsonwebtoken:jjwt:0.9.1")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	implementation("org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.4.1")
	// JAXB API
	implementation("javax.xml.bind:jaxb-api:2.3.1")

	// JAXB Runtime
	implementation ("org.glassfish.jaxb:jaxb-runtime:2.3.1")

	implementation ("com.google.firebase:firebase-admin:9.4.3")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
	jvmToolchain(17)
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.jar {
	manifest {
		attributes(
			"Main-Class" to "mikhail.shell.video.hosting.ApplicationKt"
		)
	}
	exclude("application.yml")
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
tasks.bootJar {
	exclude("application.yml")
}
