# CLAUDE.md
Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:

```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.
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
├── auth/          @ApplicationModule                 ← 로그인·회원가입 (AuthController/AuthService)
├── employee/      @ApplicationModule                 ← 사원·부서·직급 (Employee/Dept/Position)
├── user/          @ApplicationModule                 ← ⚠️ employee 로 대체됨. 미사용 (아래 참조)
└── vacation/      @ApplicationModule                 ← 휴가 신청 스켈레톤 (향후 확장)
```

프론트(`C:\develop\groupware-front`)의 `src/features/<도메인>` 과 1:1 대응한다.

> **`user/` 모듈과 `users` 테이블은 `employee/` + `employees` 로 대체되었다.**
> 데이터가 없음을 확인한 뒤 모듈 삭제 + `DROP TABLE users` 마이그레이션으로 정리할 것.
> 지금은 파괴적 변경을 피하려고 남겨 두었다.

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
- **`@WebMvcTest` 슬라이스가 없다**: Spring Boot 4 에서 web MVC 테스트 슬라이스가 별도 아티팩트로 분리되어 `spring-boot-test-autoconfigure` 에 `WebMvcTest` 가 들어 있지 않다. 컨트롤러 테스트는 의존성을 늘리지 말고 `MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler())` 로 작성한다 (`AuthControllerTest` 참조).

## 인증 · 회원가입 흐름

- `POST /api/v1/auth/signup` — 사번을 **서버가 채번**하고(`EMP-{연도}-{4자리}`, `employee_no_seq`),
  계정을 `PENDING` 으로 만든다. **토큰을 발급하지 않는다.**
- `POST /api/v1/auth/login` — 비밀번호가 맞아도 `status != ACTIVE` 면 로그인시키지 않는다
  (`A005` 승인 대기 / `A006` 거절). 계정 존재 여부가 드러나지 않도록 인증 실패 메시지는 통일한다.
- 부서·직급·입사일은 회원가입 시 받지 않는다. **관리자가 승인(`Employee.approve`) 시점에 배정**한다.
- 필드 단위 검증 실패는 `FieldValidationException` 으로 던진다 → `C001` + `data.{필드}`.
  프론트가 해당 입력 옆에 인라인으로 표시한다.

계약 문서: `groupware-front/docs/04_인증_API_명세.md`

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
