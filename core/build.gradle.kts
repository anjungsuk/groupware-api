val queryDslVersion: String by project

dependencies {
    api(project(":common"))
    api(project(":infra"))

    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.postgresql:postgresql")
    api("org.springframework.boot:spring-boot-flyway")
    api("org.flywaydb:flyway-core")
    api("org.flywaydb:flyway-database-postgresql")

    // JpaAuditingConfig에서 SecurityContextHolder/Authentication 참조
    // 런타임은 security 모듈이 제공, core는 컴파일만 필요
    compileOnly("org.springframework.security:spring-security-core")

    // QueryDSL (Jakarta)
    api("com.querydsl:querydsl-jpa:${queryDslVersion}:jakarta")
    annotationProcessor("com.querydsl:querydsl-apt:${queryDslVersion}:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")
}

val generated = layout.buildDirectory.dir("generated/querydsl")

tasks.withType<JavaCompile>().configureEach {
    options.generatedSourceOutputDirectory.set(generated.get().asFile)
}

sourceSets {
    named("main") {
        java.srcDirs(generated)
    }
}

tasks.named<Delete>("clean") {
    delete(generated)
}
