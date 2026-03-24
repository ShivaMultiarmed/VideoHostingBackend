plugins {
    val kotlin = "2.3.20"
	kotlin("jvm") version kotlin
	kotlin("plugin.spring") version kotlin
	id("application")
	id("org.springframework.boot") version "4.0.4"
	id("io.spring.dependency-management") version "1.1.6"
	kotlin("plugin.jpa") version kotlin
}

application {
	mainClass = "mikhail.shell.video.hosting.ApplicationKt"
}

group = "mikhail.shell"
version = "2.0.0"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
	sourceCompatibility = JavaVersion.VERSION_25
	targetCompatibility = JavaVersion.VERSION_25
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")
	implementation("org.springframework.boot:spring-boot-starter-mail")
	runtimeOnly("com.mysql:mysql-connector-j")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation ("io.jsonwebtoken:jjwt:0.9.1")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	implementation("org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.4.1")
	// JAXB API
	implementation("javax.xml.bind:jaxb-api:2.3.1")

	// JAXB Runtime
	implementation ("org.glassfish.jaxb:jaxb-runtime:2.3.1")

	implementation ("com.google.firebase:firebase-admin:9.8.0")

	implementation("net.coobird:thumbnailator:0.4.8")

    val jave = "3.5.0"
    implementation("ws.schild:jave-core:$jave")
    implementation("ws.schild:jave-nativebin-linux64:$jave")

    implementation("org.apache.commons:commons-imaging:1.0.0-alpha6")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
	jvmToolchain(25)
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
