# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 빌드 & 실행

단일 모듈 Gradle 프로젝트 (Java 25, Spring Boot 4.0.0, Spring Modulith 2.0.5).

```bash
# 로컬 인프라 기동 (PostgreSQL 17 + Redis 7)
docker compose up -d

# 컴파일
./gradlew compileJava

# 애플리케이션 실행 (기본 SPRING_PROFILES_ACTIVE=local)
./gradlew bootRun
./gradlew bootJar && java -jar build/libs/groupware-api.jar

# 테스트
./gradlew test
./gradlew test --tests com.company.groupware.ModularityTests              # 클래스 단위
./gradlew test --tests com.company.groupware.ModularityTests.verifiesModularStructure   # 메서드 단위
```

이 저장소의 도구 환경에서 `./gradlew` 호출은 보통 `--no-daemon --console=plain` 옵션을 함께 사용한다. 데몬과 configuration cache 가 출력을 가리거나 재사용되지 않는 문제를 피하기 위함이다.

`bootJar` 는 `build/libs/groupware-api.jar` 단일 fat jar 를 생성한다 (일반 `jar` 태스크는 비활성화됨).

## 아키텍처: Spring Modulith

이 코드베이스는 [Spring Modulith](https://docs.spring.io/spring-modulith/reference/) 가 권고하는 패키지 레이아웃을 따른다. **새 패키지를 추가하기 전에 반드시 이 절을 먼저 읽어야 한다.**

```
src/main/java/com/company/groupware/
├── GroupwareApplication.java
├── common/        @ApplicationModule(type = OPEN)   ← 공용 모듈, 모든 도메인이 의존 가능
├── user/          @ApplicationModule                 ← 사용자 도메인 모듈
└── vacation/      @ApplicationModule                 ← 휴가 신청 스켈레톤 (향후 확장)
```

### 모듈 가시성 규칙 (`ModularityTests` 가 강제)

- 모듈의 **루트 패키지** = 공식 API. 다른 모듈에서 import 가능.
- 모든 **하위 패키지** = 내부 전용. 다른 모듈에서 import 시 `ApplicationModules.verify()` 가 실패한다.
- 내부 패키지명은 관례적으로 `internal/` 을 사용한다 (Modulith 가 이름 자체를 강제하진 않지만 일관성을 위해 고정).
- 각 `package-info.java` 에서 `@ApplicationModule` 으로 모듈 경계를 선언한다.

### 레이어 배치 컨벤션

| 레이어 | 위치 | 이유 |
|--------|------|------|
| Repository | `<module>/internal/` | 다른 모듈이 DB 에 직접 접근하지 못하도록 차단 |
| Service | `<module>/` (루트) | 모듈 간 호출이 일어나는 공식 API |
| Controller | `<module>/` (루트) | HTTP 진입점 — 모듈의 외부 노출 표면으로 간주 |
| Domain Entity | `<module>/` (루트) | 다른 모듈이 ID 참조나 DTO 변환 시 필요 |

모듈 간 통신은 다른 모듈의 **Service** 호출 또는 Spring `ApplicationEvent` 를 사용한다. 다른 모듈의 Controller·Repository 를 직접 호출하면 안 된다.

### Common 모듈

`common/` 은 OPEN 모듈이다. 횡단 관심사는 모두 여기에 둔다:
- `common/security/` — `SecurityConfig`, `SystemRole` enum, JWT 일체 (provider, filter, entry point, properties)
- `common/config/` — JPA Auditing, QueryDSL, OpenAPI, `SpringDocQuerydslExcluder` 워크어라운드
- `common/entity/` — `BaseEntity` (감사 필드), `BaseSoftDeleteEntity` (`deleted` 플래그 포함)
- `common/exception/` — `BusinessException`, 중앙화된 `ErrorCode` enum, `GlobalExceptionHandler`
- `common/response/` — `ApiResponse<T>` 응답 봉투, `PageResponse`
- `common/infrastructure/` — Redis 설정, `FileStorageService` 추상화 (현재 Local 구현, 추후 `app.storage.type` 으로 S3 전환)

## Spring Boot 4 / Jackson 3 호환성 이슈

이 프로젝트는 Spring Boot 4 초기 라인 위에서 동작한다. 다음 업스트림 비호환을 코드로 우회하고 있으며, **테스트 없이 제거하지 말 것**:

- **`SpringDocQuerydslExcluder`** (`common/config/`) — springdoc-openapi 2.8.x 의 `queryDslQuerydslPredicateOperationCustomizer` 빈을 제거한다. 이 빈은 Spring Data 4 에서 삭제된 `TypeInformation` 클래스를 참조하므로, 제거하지 않으면 기동 시 `NoClassDefFoundError` 가 발생한다.
- **Jackson 3 패키지 위치**: core/databind 타입은 `tools.jackson.*` 아래로 이동했다 (예: `tools.jackson.databind.ObjectMapper`). 다만 **annotation 류는 여전히** `com.fasterxml.jackson.annotation.*` 에 남아 있다. 두 경로를 통합하려 하지 말 것.
- **`spring-modulith-starter-insight` 는 의도적으로 제외**되어 있다. observability AOP 가 `JwtAuthenticationFilter` (= `OncePerRequestFilter`) 를 CGLIB 으로 감싸면서 `GenericFilterBean.init()` (final 메서드) 호출 시 NPE 가 발생해 Tomcat 기동이 깨진다. 재도입 시 보안 필터를 AOP advice 대상에서 먼저 제외해야 한다.
- **Spring Boot 4 의 Flyway**: 모듈화된 `org.springframework.boot:spring-boot-flyway` 아티팩트가 필요하다 (이미 선언됨). `flyway-core` 만으로는 자동 구성이 활성화되지 않는다.

## 주요 설정

- `app.security.jwt.*` — JWT 설정. `JwtProperties` record (`@ConfigurationProperties`) 로 바인딩.
- `app.storage.type` — `local` (기본) 또는 `s3`. `LocalFileStorageService` 는 `@ConditionalOnProperty` 로 매칭.
- 프로파일: `local` (docker-compose), `dev` (공용 개발 인프라), `prod` (AWS RDS/ElastiCache/S3, Swagger 비활성). `SPRING_PROFILES_ACTIVE` 로 전환.
- Flyway 마이그레이션 위치: `src/main/resources/db/migration/V*__*.sql`.

## 구조 변경 후 확인할 엔드포인트

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/v3/api-docs        # 200
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/swagger-ui.html    # 302
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/v1/ping        # 401 (JWT 필요)
curl -s http://localhost:8080/actuator/health                                     # {"status":"UP"}
```

`ModularityTests.verifiesModularStructure()` 는 모듈 경계 위반에 대한 표준 회귀 체크이다. 패키지를 옮긴 뒤에는 항상 이 테스트를 실행한다.

## 컨벤션

- **모든 git 커밋·PR 메시지는 한글로 작성한다** (Conventional Commit prefix `feat:` / `fix:` / `refactor:` 등은 영문 유지, Spring Boot · ObjectMapper · JWT 같은 기술 고유명사는 영문 유지).
- 커스텀 슬래시 커맨드 `/JAVA_CLEAN_COMMIT_GUIDE` (`.claude/commands/` 에 위치) 는 Java 파일 커밋 전 정리 규칙을 정의한다 — 미사용 import/지역변수 제거, `System.out` → SLF4J 교체, 빈 catch 블록 처리, 하드코딩 값 추출 등. **"미사용으로 보여도 절대 삭제하면 안 되는" 어노테이션 목록**(Spring stereotype, JPA, Jackson, MapStruct, AOP 등) 도 정리되어 있으니 Java 코드를 일괄 정리하기 전에 반드시 참조한다.
