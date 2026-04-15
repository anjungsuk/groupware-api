---
description: "Java 커밋 대상 파일을 스캔하여 미사용 가비지 코드를 제거하고, 운영 안정성을 고려한 커밋을 수행합니다."
---

# Java Clean Commit Guide

아래 단계를 **순서대로** 실행하세요.

---

## 1단계: 커밋 대상 파일 확인

`git diff --cached --name-only`와 `git diff --name-only`를 실행하여 staged + unstaged 변경 파일 목록을 확인합니다.

- 변경된 파일이 없으면 사용자에게 알리고 중단합니다.
- 파일 변경 규모가 큰 경우(50개 이상), 사용자에게 범위를 좁힐지 확인합니다.
- `.gitignore`에 포함된 파일, 자동 생성 파일(`**/generated/**`, `**/build/**`, `**/out/**`)은 분석 대상에서 제외합니다.

---

## 2단계: 파일 타입별 미사용 코드 스캔 및 제거

변경된 파일을 읽고, 파일 타입에 따라 다음을 검사합니다.

### Java (.java) 파일

#### 2-1. 가비지 코드 제거

- **미사용 import**: `import`된 클래스/패키지가 파일 본문에서 사용되지 않는 경우 제거합니다. 와일드카드 import(`import java.util.*`)는 명시적 import로 전환을 제안합니다.
- **미사용 지역 변수**: 메서드 내에서 선언만 되고 사용되지 않는 지역 변수를 제거합니다.
- **미사용 private 멤버**: 클래스 내부의 `private` 필드나 메서드 중 어디에서도 호출되지 않는 항목을 제거합니다.
- **Dead Code**: `return`, `throw`, `break`, `continue` 이후에 위치한 도달 불가능한 코드를 제거합니다.
- **주석 처리된 코드 블록**: 5줄 이상의 연속된 주석 처리된 코드를 식별합니다. 단, `TODO`/`FIXME` 주석과 Javadoc(`/** */`)은 제외합니다.
- **Redundant Cast**: 불필요한 타입 캐스팅을 제거합니다.
- **Deprecated API 사용**: `@Deprecated` 메서드 호출을 탐지하고 대체 API를 제안합니다.

#### 2-2. 운영 안정성 및 성능 최적화

- **Logging**: `System.out.println`, `System.err.println`을 발견하면 적절한 로거(`log.info`, `log.debug`, `log.error` 등)로 교체하거나 삭제합니다. `e.printStackTrace()`도 동일하게 `log.error("message", e)` 형태로 교체합니다.
- **Exception Handling**: 비어 있는 `catch` 블록을 발견하면 최소한 로그 출력이나 예외 전파 코드를 추가하도록 제안합니다. 단, `InterruptedException`을 catch하는 경우 `Thread.currentThread().interrupt()` 호출 여부도 확인합니다.
- **Lombok Optimization**: `@Slf4j` 등이 선언되었으나 실제 로그 코드가 없다면 어노테이션 제거를 검토합니다. `@Data`가 Entity 클래스에 사용된 경우 `@Getter`/`@Setter`로 분리를 제안합니다.
- **Resource Leak**: `try-with-resources`를 사용하지 않는 `InputStream`, `OutputStream`, `Connection`, `PreparedStatement` 등 `AutoCloseable` 리소스를 탐지합니다.
- **동시성 문제**: `SimpleDateFormat`을 공유 필드로 사용하거나, `HashMap`/`ArrayList`를 멀티스레드 환경에서 동기화 없이 사용하는 패턴을 경고합니다.
- **N+1 쿼리 의심**: JPA 연관관계에서 루프 내 연관 엔티티 접근 패턴을 탐지하고 `@EntityGraph` 또는 `fetch join` 사용을 제안합니다.
- **하드코딩된 값**: URL, 포트번호, 타임아웃 값 등 설정 파일(`application.yml` 등)로 분리해야 할 하드코딩된 값을 탐지합니다.
- **로그 레벨 적정성**: 반복문 내부 또는 대량 호출 경로에서 `log.info()`를 사용하는 경우 `log.debug()`로 변경을 제안합니다. 로그 메시지에 문자열 연결(`+`) 대신 SLF4J 플레이스홀더(`{}`)를 사용하도록 제안합니다.

#### 2-3. 코드 스타일 및 가독성 (제안만, 자동 수정하지 않음)

- **매직 넘버**: 의미를 알 수 없는 숫자 리터럴은 `private static final` 상수로 추출을 제안합니다.
- **메서드 길이**: 50줄 이상의 메서드는 분리를 제안합니다.
- **파라미터 수**: 5개 이상의 파라미터를 가진 메서드는 DTO 또는 Builder 패턴 적용을 제안합니다.
- **네이밍 컨벤션**: Java 표준 위반을 탐지합니다 — 상수는 `UPPER_SNAKE_CASE`, 클래스는 `PascalCase`, 메서드/변수는 `camelCase`.

#### 2-4. 보안 검사

- **SQL Injection**: 문자열 연결(`+`)로 작성된 SQL 쿼리를 탐지하고 `PreparedStatement` 또는 파라미터 바인딩(`:param`, `?`) 사용을 제안합니다.
- **민감 정보 노출**: 비밀번호, API 키, 시크릿 토큰 등이 소스 코드에 하드코딩된 경우 경고합니다.
- **XSS 취약점**: 사용자 입력을 escape 없이 직접 출력하는 패턴을 탐지합니다.
- **안전하지 않은 역직렬화**: `ObjectInputStream`을 검증 없이 사용하는 경우 경고합니다.

### 설정 파일 (XML, YAML, Properties, Gradle)

- **application.yml / properties**: 운영 환경에 부적절한 설정을 탐지합니다 — `debug=true`, `spring.jpa.show-sql=true`, `spring.jpa.hibernate.ddl-auto=create` 또는 `create-drop` 등.
- **build.gradle.kts**: SNAPSHOT 의존성이 운영 브랜치에 포함된 경우 경고합니다. `implementation` 대신 `api`로 불필요하게 노출된 의존성, 사용되지 않는 플러그인, 중복 의존성 선언도 탐지합니다.
- **logback.xml / logback-spring.xml**: 운영 프로파일에서 `DEBUG` 레벨 로깅이 루트 로거에 활성화된 경우 경고합니다.

### 공통 규칙

- **주석 처리된 코드 블록**: 5줄 이상의 연속된 주석 처리된 코드가 있으면 사용자에게 알립니다. 단, TODO/FIXME 주석은 제외합니다.

---

## 3단계: 변경사항 리포트

제거한 항목을 파일별로 요약하여 사용자에게 보여줍니다. **자동 수정 항목**과 **수동 검토 제안 항목**을 구분합니다.

```
📋 Clean Commit Report
──────────────────────────

📁 src/main/java/com/example/UserService.java
  ✅ 자동 수정:
    - 제거된 import: java.util.ArrayList, java.util.HashMap
    - 제거된 미사용 변수: tempResult (Line 34)
    - System.out.println → log.info 교체 (Line 45)
  ⚠️ 수동 검토 필요:
    - 빈 catch 블록에 로그 추가 권장 (Line 82)
    - 매직 넘버 30 → 상수 추출 권장 (Line 91)

📁 src/main/resources/application.yml
  ⚠️ 경고: spring.jpa.show-sql=true (운영 환경 확인 필요)

📊 요약
  ✅ 자동 수정: 4건
  ⚠️ 수동 검토 필요: 3건
  🔒 보안 이슈: 0건
```

변경사항이 없으면 "클린 코드입니다! 미사용 코드가 없습니다." 라고 알립니다.

---

## 4단계: 롤백 안전장치 및 빌드 검증

자동 수정 적용 후, 커밋 전에 다음을 확인합니다.

- `git diff`로 최종 변경 내용을 확인합니다.
- 가능하면 `./gradlew compileJava`를 실행하여 컴파일 오류가 없는지 확인합니다.
- 가능하면 `./gradlew test`를 실행하여 기존 테스트가 통과하는지 확인합니다.
- 문제가 발생하면 `git checkout -- <file>` 또는 `git stash`로 즉시 롤백합니다.

---

## 5단계: 파일 스테이징 및 커밋

1. 변경된 모든 파일을 `git add`로 스테이징합니다 (파일명을 명시적으로 지정).
2. 커밋 메시지를 다음 형식으로 작성합니다.

### 커밋 메시지 형식

```
<type>: <기능 요약>

- 미사용 import 제거 (UserService, OrderController)
- 빈 catch 블록 로그 추가 (PaymentService)
- System.out.println → SLF4J 로거 교체

Co-Authored-By: Claude <claude@anthropic.com>
```

### 커밋 타입

| 타입 | 용도 |
|------|------|
| `feat:` | 새로운 기능 추가 |
| `fix:` | 버그 수정 |
| `refactor:` | 코드 리팩토링 (기능 변경 없음) |
| `style:` | 코드 포맷팅, 세미콜론 등 (동작 변경 없음) |
| `chore:` | 빌드 설정, 패키지 매니저 등 |
| `docs:` | 문서 수정 |
| `test:` | 테스트 코드 추가/수정 |
| `perf:` | 성능 개선 |

### 커밋 분리 기준

- 기능 변경(`feat:`, `fix:`)과 클린업(`refactor:`)은 **별도 커밋**으로 분리합니다.
- 하나의 커밋에 10개 이상의 파일 변경이 포함되면 모듈/기능 단위로 분리를 제안합니다.

---

## 주의사항 및 예외 규칙 (필독)

### 절대 제거 금지

다음 코드는 IDE에서 미사용으로 표시되더라도 **절대 제거하지 마세요**. 프레임워크가 런타임에 리플렉션/프록시를 통해 사용합니다.

| 대상 | 예시 |
|------|------|
| **Spring Bean** | `@Component`, `@Service`, `@Repository`, `@Configuration`, `@Bean`, `@Controller`, `@RestController` |
| **JPA Entity/DTO 필드** | `@Entity`, `@Column`, `@Transient`, `@Embeddable`, `@MappedSuperclass` |
| **Spring 스케줄링/이벤트** | `@Scheduled`, `@EventListener`, `@Async`, `@Cacheable`, `@CacheEvict` |
| **직렬화 필드** | `serialVersionUID`, `@JsonProperty`, `@JsonIgnore`, `@JsonCreator` |
| **MapStruct/ModelMapper** | `@Mapper` 인터페이스 메서드 — 프레임워크가 구현체를 자동 생성 |
| **Feign Client** | `@FeignClient` 인터페이스 메서드 — 프록시로 호출됨 |
| **MyBatis Mapper** | `@Mapper` 인터페이스 메서드 — XML 또는 어노테이션 기반 바인딩 |
| **AOP Aspect** | `@Aspect`, `@Around`, `@Before`, `@After`, `@Pointcut` |
| **Validation** | `@Valid`, `@NotNull`, `@Size` 등 Bean Validation 어노테이션 |
| **Spring Security** | `@PreAuthorize`, `@Secured`, `@RolesAllowed` |

### 절대 건드리지 말 것

- `@SuppressWarnings("unused")` 어노테이션이 있는 코드
- `// noinspection unused` 주석이 있는 코드

### 보수적 접근 대상

| 대상 | 이유 |
|------|------|
| **테스트 코드** (`src/test`) | 테스트 픽스처, Mock 데이터 등 의도적 미사용 코드가 존재할 수 있습니다. **절대 자동 제거하지 마세요.** |
| **`protected` 멤버** | 하위 클래스에서 사용될 수 있으므로 현재 파일만으로 미사용을 판단하지 않습니다. |
| **`public` 멤버** | 라이브러리/모듈의 `public` 메서드는 외부에서 사용될 수 있으므로 제거하지 않습니다. |
| **인터페이스 구현 메서드** | `@Override`로 강제된 빈 메서드는 유지합니다. |
| **Javadoc 주석** | 문서화 주석(`/** */`)은 주석 처리된 코드와 구분하여 제거 대상에서 제외합니다. |
| **TODO/FIXME 주석** | 의도적으로 남긴 것이므로 5줄 규칙에서 제외합니다. |

- **의심스러운 경우 제거하지 않고 사용자에게 확인을 요청하세요.**

---

## 멀티 모듈 프로젝트 고려사항

- 모듈 간 의존성을 고려하여 `public` 멤버 제거 시 다른 모듈에서의 사용 여부를 반드시 확인합니다.
- 각 모듈의 `build.gradle.kts` 및 `settings.gradle.kts`의 `include` 구성을 참조하여 의존 관계를 파악합니다.
- 모듈별로 커밋을 분리하여 변경 추적을 용이하게 합니다.

---

## CI/CD 연동 권장 사항

| 항목 | 설명 |
|------|------|
| **Pre-commit Hook** | 이 가이드의 규칙을 `pre-commit` 훅으로 등록하여 커밋 시 자동 검사를 수행합니다. |
| **정적 분석 도구 병행** | SpotBugs, PMD, Checkstyle, SonarQube 등 기존 도구와 역할을 분담합니다. |
| **PR 리뷰 체크리스트** | 클린업 결과 리포트를 PR 설명에 포함하도록 권장합니다. |

---

*이 가이드는 Java 프로젝트의 코드 품질과 운영 안정성을 높이기 위한 목적으로 작성되었습니다. 프로젝트 특성에 맞게 규칙을 조정하여 사용하세요.*
