val queryDslVersion: String by project

dependencies {
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("jakarta.servlet:jakarta.servlet-api")

    // AccessDeniedException 컴파일용 — 런타임은 security 모듈이 제공
    compileOnly("org.springframework.security:spring-security-core")

    compileOnly("org.slf4j:slf4j-api")

    // BaseEntity/BaseSoftDeleteEntity에 대한 Q클래스 생성
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
