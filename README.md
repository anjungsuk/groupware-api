# GroupWare API

사내 그룹웨어 백엔드 API. Spring Boot 4.x / Java 25 LTS 기반 멀티모듈 아키텍처.

## 🧱 기술 스택

| 분류 | 스택 |
|------|------|
| Runtime | Java 25 LTS, Spring Boot 4.0.x |
| Persistence | PostgreSQL 17, JPA (Hibernate 7), QueryDSL 5.1 (Jakarta), Flyway |
| Cache/Session | Redis 7 (Lettuce) |
| Security | Spring Security, JWT (jjwt 0.12) |
| Build | Gradle Kotlin DSL |
| Cloud | AWS (RDS, ElastiCache, S3) — 추후 연동 |

## 📦 모듈 구조

```
groupware-api/
├── api/         ← 실행 진입점, REST 컨트롤러, 환경별 yml
├── security/    ← SecurityConfig, JWT Provider/Filter
├── core/        ← 도메인 엔티티, JPA/QueryDSL 설정, Flyway
├── infra/       ← Redis, FileStorage 추상화 (Local ↔ S3)
└── common/      ← ApiResponse, ErrorCode, Exception, BaseEntity
```

**의존 방향** (단방향):

```
api → security → core → infra → common
              ↘︎ core ↗
```

## 🚀 로컬 실행

### 1. 인프라 기동

```bash
docker compose up -d
```

→ PostgreSQL (5432), Redis (6379) 컨테이너 기동.

### 2. 환경변수

```bash
cp .env.example .env
# JWT_SECRET은 운영시 반드시 재생성
```

### 3. 빌드 & 실행

```bash
./gradlew :api:bootRun
# 또는
./gradlew build && java -jar api/build/libs/groupware-api.jar
```

### 4. 헬스체크

```bash
curl http://localhost:8080/api/v1/ping
curl http://localhost:8080/actuator/health
```

## 🌐 프로파일

| Profile | 용도 | DB/Redis |
|---------|------|----------|
| `local` | 개인 PC 개발 | docker-compose |
| `dev` | 공용 개발 서버 | 공용 Dev 인프라 |
| `prod` | 운영 | AWS RDS/ElastiCache, S3 |

변경은 `SPRING_PROFILES_ACTIVE` 환경변수 또는 JVM 옵션 `-Dspring.profiles.active=prod`.

## 🔑 핵심 원칙 (AA Guide 기반)

1. **나중에 바꾸기 어려운 것부터 먼저** — 환경 분리 → DB → Security → 공통 컴포넌트
2. **`open-in-view: false`, `ddl-auto: validate`** — 운영 안정성 기본값
3. **BaseEntity / BaseSoftDeleteEntity** — 감사 필드 표준화 (createdAt/By, updatedAt/By, deleted)
4. **ApiResponse<T> + ErrorCode Enum** — 응답 포맷 단일화, 도메인별 에러 코드 체계
5. **Storage 추상화** — Local 구현체 기본, S3 구현체 추가 시 `app.storage.type=s3`로 전환

## 🧭 다음 단계

- [ ] Auth 컨트롤러 (로그인/토큰 재발급) 구현
- [ ] Organization / Department 도메인 추가
- [ ] S3 FileStorageService 구현체 추가 (AWS SDK v2)
- [ ] Spring Batch 모듈 추가
- [ ] Notification 모듈 (메일/푸시)
- [ ] GitHub Actions CI/CD 파이프라인
- [ ] OpenAPI (springdoc-openapi) 문서 자동화
