plugins {
	java
	id("org.springframework.boot") version "3.4.10-SNAPSHOT"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.handson"
version = "0.0.1-SNAPSHOT"
description = "Project for Spring Boot to create a prototype template as a soul for other projects to add the features in depending upon the functionalities."

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
	maven { url = uri("https://repo.spring.io/snapshot") }
}

dependencies {
	// implementations
	implementation("org.springframework.boot:spring-boot-starter-logging")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-aop")
	// Resilience4j Spring Boot 3 integration
	implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")

	// Optional: add specific modules if you want finer control
	// Comment out what you don't need
	implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.2.0")
	implementation("io.github.resilience4j:resilience4j-retry:2.2.0")
	implementation("io.github.resilience4j:resilience4j-ratelimiter:2.2.0")
	implementation("io.github.resilience4j:resilience4j-timelimiter:2.2.0")

	compileOnly("org.projectlombok:lombok")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
