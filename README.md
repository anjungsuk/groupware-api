# GroupWare API

사내 그룹웨어 백엔드 API. Spring Boot 4.x / Java 25 LTS 기반 **Spring Modulith** 아키텍처.

## 🧱 기술 스택

| 분류 | 스택 |
|------|------|
| Runtime | Java 25 LTS, Spring Boot 4.0.x |
| Architecture | Spring Modulith 2.0.5 (단일 모듈 + 패키지 경계 검증) |
| Persistence | PostgreSQL 17, JPA (Hibernate 7), QueryDSL 5.1 (Jakarta), Flyway |
| Cache/Session | Redis 7 (Lettuce) |
| Security | Spring Security, JWT (jjwt 0.12) |
| API Doc | springdoc-openapi 2.8.6 (OpenAPI 3.1) |
| Build | Gradle Kotlin DSL (단일 모듈) |
| Cloud | AWS (RDS, ElastiCache, S3) — 추후 연동 |

## 📦 모듈 구조 (Spring Modulith)

```
src/main/java/com/company/groupware/
├── GroupwareApplication.java
├── common/                    ← @ApplicationModule(type=OPEN) 모든 도메인이 자유롭게 의존 가능
│   ├── config/                ← OpenAPI, JpaAuditing, QueryDsl, springdoc 호환 설정
│   ├── entity/                ← BaseEntity, BaseSoftDeleteEntity (감사 필드 표준)
│   ├── exception/             ← BusinessException, ErrorCode, GlobalExceptionHandler
│   ├── infrastructure/        ← Redis, FileStorage 추상화 (Local ↔ S3)
│   ├── response/              ← ApiResponse, PageResponse
│   ├── security/              ← SecurityConfig, SystemRole, JWT 묶음
│   └── web/                   ← HealthController 등 공용 웹 진입점
├── user/                      ← @ApplicationModule (도메인)
│   ├── User.java              ← 외부 노출 API
│   └── internal/              ← UserRepository (모듈 내부 캡슐화)
└── vacation/                  ← @ApplicationModule (스켈레톤, 향후 확장)
    └── internal/
```

**Spring Modulith 적용 포인트**

- 각 도메인 패키지의 `package-info.java`에 `@ApplicationModule` 선언으로 모듈 경계를 명시한다.
- `common` 은 `Type.OPEN` — 모든 도메인이 의존 허용.
- 각 도메인의 `internal` 패키지는 모듈 외부에서 직접 참조 불가 (Modulith 가 컴파일·테스트 단계에서 강제).
- `ApplicationModules.of(GroupwareApplication.class).verify()` 테스트로 의존 위반을 자동 검증한다 (`ModularityTests`).
- 향후 MSA 분리 시 모듈 단위로 분해하는 길을 열어둔다 (단일 모듈 → 다중 모듈 → 다중 서비스).

## 🚀 로컬 실행

### 1. 인프라 기동

```bash
docker compose up -d
```

→ PostgreSQL (5432), Redis (6379) 컨테이너 기동.

### 2. 환경변수

```bash
cp .env.example .env
# JWT_SECRET 은 운영시 반드시 재생성
```

### 3. 빌드 & 실행

```bash
./gradlew bootRun
# 또는
./gradlew bootJar && java -jar build/libs/groupware-api.jar
```

### 4. 동작 확인

```bash
curl http://localhost:8080/actuator/health         # 상태 확인 (인증 불필요)
curl http://localhost:8080/api/v1/ping             # 401 반환 (JWT 필요)
open http://localhost:8080/swagger-ui.html         # Swagger UI
curl http://localhost:8080/v3/api-docs             # OpenAPI 3.1 스펙 JSON
```

## 🌐 프로파일

| Profile | 용도 | DB/Redis | Swagger |
|---------|------|----------|---------|
| `local` | 개인 PC 개발 | docker-compose | 활성 |
| `dev` | 공용 개발 서버 | 공용 Dev 인프라 | 활성 |
| `prod` | 운영 | AWS RDS/ElastiCache, S3 | 비활성 |

변경은 `SPRING_PROFILES_ACTIVE` 환경변수 또는 JVM 옵션 `-Dspring.profiles.active=prod`.

## 🧪 모듈 검증 테스트

```bash
./gradlew test --tests com.company.groupware.ModularityTests
```

- `verifiesModularStructure()` — 모듈 간 의존 위반, 순환 참조, internal 침범 검출.
- `writeDocumentationSnippets()` — `build/spring-modulith-docs/` 에 PlantUML 다이어그램 자동 생성.

## 🔑 핵심 원칙

1. **나중에 바꾸기 어려운 것부터 먼저** — 환경 분리 → DB → Security → 모듈 경계
2. **`open-in-view: false`, `ddl-auto: validate`** — 운영 안정성 기본값
3. **BaseEntity / BaseSoftDeleteEntity** — 감사 필드 표준화 (createdAt/By, updatedAt/By, deleted)
4. **ApiResponse<T> + ErrorCode Enum** — 응답 포맷 단일화, 도메인별 에러 코드 체계
5. **Storage 추상화** — Local 구현체 기본, S3 구현체 추가 시 `app.storage.type=s3`로 전환
6. **Modulith 경계 강제** — 모듈 외부에서 `internal` 침범 금지, ApplicationModules 테스트로 회귀 방지

## 🧭 다음 단계

- [ ] Auth 컨트롤러 (로그인/토큰 재발급) 구현 — `user` 또는 신규 `auth` 모듈
- [ ] Vacation 도메인 본격 구현 (휴가 정책, 결재 라인)
- [ ] Organization / Department 도메인 모듈 추가
- [ ] S3 FileStorageService 구현체 추가 (AWS SDK v2)
- [ ] Notification 모듈 (메일/푸시) — Modulith Event Externalization 활용
- [ ] GitHub Actions CI/CD 파이프라인
- [ ] `spring-modulith-starter-insight` 재도입 (필터 CGLIB 충돌 해소 후)
