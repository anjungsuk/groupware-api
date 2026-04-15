dependencies {
    api(project(":common"))

    api("org.springframework.boot:spring-boot-starter-data-redis")

    // Spring Boot 4 — Jackson 3 (tools.jackson.*)
    api("org.springframework.boot:spring-boot-starter-jackson")
}
