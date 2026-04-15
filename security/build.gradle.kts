val jjwtVersion: String by project

dependencies {
    api(project(":common"))
    api(project(":core"))

    api("org.springframework.boot:spring-boot-starter-security")

    api("io.jsonwebtoken:jjwt-api:${jjwtVersion}")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:${jjwtVersion}")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:${jjwtVersion}")
}
