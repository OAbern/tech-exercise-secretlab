plugins {
    java
    id("org.springframework.boot") version "3.4.5"
    id("io.spring.dependency-management") version "1.1.7"
    jacoco
    id("org.sonarqube") version "7.3.0.8198"
    id("com.diffplug.spotless") version "8.5.1"
    id("com.github.spotbugs") version "6.5.4"
}

group = "com.secretlab"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    maven { url = uri("https://maven.aliyun.com/repository/public") }
    maven { url = uri("https://maven.aliyun.com/repository/spring") }
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.mapstruct:mapstruct:1.6.3")
    runtimeOnly("com.h2database:h2")
    compileOnly("org.projectlombok:lombok")

    // Annotation processors — Lombok must run before MapStruct
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
    testAnnotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
}

// ─── Tests ──────────────────────────────────────────────────────────────────

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

// ─── JaCoCo ─────────────────────────────────────────────────────────────────

jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true                // CI
        csv.required = false
        html.required = true               // local: build/reports/jacoco/test/html/index.html
    }
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/MvcApplication.class",
                    "**/model/vo/**",
                    "**/model/dto/**",
                    "**/dto/**",
                    "**/vo/**",
                    "**/*ConvertorImpl.class",   // auto-generated MapStruct implementation
                )
            }
        })
    )
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                minimum = "0.90".toBigDecimal()   // code coverage >= 90%
            }
        }
    }
}

// ─── Spotless (Google Java Format) ──────────────────────────────────────────
// Run:  ./gradlew spotlessApply   — auto-reformat
//       ./gradlew spotlessCheck   — check only (wired into `check`)

spotless {
    java {
        // Enforce Google Java Format (2-space indent, 100-char line limit)
        googleJavaFormat("1.21.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// ─── SpotBugs ───────────────────────────────────────────────────────────────
// Run:  ./gradlew spotbugsMain   — analyse main sources
//       ./gradlew spotbugsTest   — analyse test sources
// Reports: build/reports/spotbugs/

spotbugs {
    toolVersion = "4.9.8"
    excludeFilter = file("config/spotbugs/exclude.xml")
    ignoreFailures = false
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    reports {
        create("html") { required = true }   // human-readable: build/reports/spotbugs/*.html
        create("xml") { required = true }    // CI-parseable: build/reports/spotbugs/*.xml
    }
}

// ─── SonarQube ──────────────────────────────────────────────────────────────
// Run:  ./gradlew sonar -Dsonar.host.url=http://... -Dsonar.token=...
//   or set env vars SONAR_HOST_URL and SONAR_TOKEN

sonar {
    properties {
        property("sonar.projectKey", "tech-exercise-secretlab")
        property("sonar.projectName", "tech-exercise-secretlab")
        property("sonar.java.source", "21")
        property("sonar.sources", "src/main/java")
        property("sonar.tests", "src/test/java")
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "${layout.buildDirectory.get()}/reports/jacoco/test/jacocoTestReport.xml"
        )
        property(
            "sonar.exclusions",
            "**/MvcApplication.java, **/model/vo/**, **/model/dto/**, **/*ConvertorImpl.java"
        )
        property(
            "sonar.coverage.exclusions",
            "**/MvcApplication.java, **/model/vo/**, **/model/dto/**, **/*ConvertorImpl.java"
        )
    }
}

tasks.named("sonar") {
    dependsOn(tasks.jacocoTestReport)
}
