# Loopers 도메인 모델: 회원(User)

## 도메인 컨텍스트

Loopers는 회원이 브랜드 상품을 탐색하고, 좋아요를 누르고, 주문하는 감성 이커머스 서비스 입니다.

서비스를 이용하기 위해서는 회원으로 가입해야 해야하며, 다음과 같은 기능을 제공합니다.
- 회원 가입 전에도 브랜드와 상품 정보를 조회할 수 있다.
- 좋아요, 주문 등의 기능은 회원만 이용할 수 있다.

회원은 로그인 ID와 비밀번호로 식별된다.
- 별도의 로그인 API는 없다.
- 매 요청마다 헤더(`X-Loopers-LoginId`, `X-Loopers-LoginPw`)로 인증한다.

회원은 자신의 정보를 조회하고, 비밀번호를 변경할 수 있다.

---

## 도메인 모델

### 회원(User)
**Aggregate Root / JPA Entity**

#### 속성
| 속성 | 타입 | JPA | 설명 |
|------|------|-----|------|
| id | Long | `@Id @GeneratedValue` | 식별자 (PK) |
| loginId | LoginId | `@Embedded` | 로그인 ID (unique) |
| password | Password | `@Embedded` | 비밀번호 (해시 저장) |
| name | Name | `@Embedded` | 이름 |
| birthDate | BirthDate | `@Embedded` | 생년월일 |
| email | Email | `@Embedded` | 이메일 |
| gender | Gender | `@Embedded` | 성별 |
| createdAt | ZonedDateTime | BaseEntity 상속 | 가입 일시 |
| updatedAt | ZonedDateTime | BaseEntity 상속 | 수정 일시 |

#### 행위
| 메서드 | 설명 |
|--------|------|
| `register(loginId: LoginId, password: Password, name: Name, birthDate: BirthDate, email: Email, gender: Gender)` | 회원 가입 (static factory), 이미 유효한 VO를 받는다 |
| `authenticate(rawPassword, passwordEncoder)` | 비밀번호 검증 |
| `changePassword(oldPassword, newPassword, passwordEncoder)` | 비밀번호 변경 |

#### 규칙
- 비밀번호는 해시로 저장한다
- 비밀번호 변경 시 기존 비밀번호가 일치해야 한다
- 비밀번호 변경 시 새 비밀번호는 기존과 달라야 한다

---

### 로그인 ID(LoginId)
**Value Object / @Embeddable**

#### 속성
| 속성 | 타입 | 컬럼 | 설명 |
|------|------|------|------|
| value | String | `login_id` (unique) | 로그인 ID 값 |

#### 규칙
- 영문 대소문자와 숫자만 허용 (`^[a-zA-Z0-9]+$`)
- **10자 이내**
- 중복 불가
- **case-sensitive** (대소문자 구분)

---

### 비밀번호(Password)
**Value Object / @Embeddable**

#### 속성
| 속성 | 타입 | 컬럼 | 설명 |
|------|------|------|------|
| value | String | `password` | 해시된 비밀번호 |

#### 생성
```kotlin
Password.create(rawPassword, birthDate, passwordEncoder)
```

#### 규칙
- 8자 이상 16자 이하
- 영문 대소문자, 숫자, 특수문자만 허용
- 허용 특수문자: `!@#$%^&*()_+-=`
- 생년월일(YYYYMMDD) 포함 불가

#### 행위
| 메서드 | 설명 |
|--------|------|
| `matches(rawPassword, passwordEncoder)` | 비밀번호 일치 여부 확인 |

---

### 이름(Name)
**Value Object / @Embeddable**

#### 속성
| 속성 | 타입 | 컬럼 | 설명 |
|------|------|------|------|
| value | String | `name` | 이름 값 |

#### 규칙
- 1자 이상

#### 행위
| 메서드 | 설명 |
|--------|------|
| `masked()` | 마지막 글자를 `*`로 마스킹하여 반환 |

#### 마스킹 규칙
- "홍길동" → "홍길*"
- "김" → "*"

---

### 생년월일(BirthDate)
**Value Object / @Embeddable**

#### 속성
| 속성 | 타입 | 컬럼 | 설명 |
|------|------|------|------|
| value | LocalDate | `birth_date` | 생년월일 값 |

#### 규칙
- `yyyy-MM-dd` 형식
- 유효한 날짜여야 함
- 미래 날짜 불가

#### 행위
| 메서드 | 설명 |
|--------|------|
| `toCompactString()` | "YYYYMMDD" 형식 문자열 반환 (비밀번호 검증용) |

---

### 이메일(Email)
**Value Object / @Embeddable**

#### 속성
| 속성 | 타입 | 컬럼 | 설명 |
|------|------|------|------|
| value | String | `email` | 이메일 주소 |

#### 규칙
- `xx@yy.zz` 형식 (기본 이메일 형식)

---

### 성별(Gender)
**Value Object / @Embeddable**

#### 속성
| 속성 | 타입 | 컬럼 | 설명 |
|------|------|------|------|
| value | GenderType | `gender` | 성별 값 |

#### GenderType (Enum)
| 값 | 설명 |
|----|------|
| MALE | 남성 |
| FEMALE | 여성 |

#### 규칙
- 필수 값 (null 불가)

---

## Repository

```kotlin
interface UserRepository {
    fun existsByLoginId(loginId: LoginId): Boolean
    fun findByLoginId(loginId: LoginId): User?
    fun save(user: User): User
}
```

---

## 예외(Exception)

| 예외 | 발생 조건 |
|------|----------|
| `DuplicateLoginIdException` | 이미 존재하는 로그인 ID로 가입 시도 |
| `InvalidFormatException` | 포맷 오류 (loginId, password, name, birthDate, email, gender) |
| `AuthenticationFailedException` | 인증 실패 (비밀번호 불일치, 사용자 없음). 보안상 사용자 존재 여부를 노출하지 않기 위해 동일 예외로 처리한다. |
| `SamePasswordException` | 새 비밀번호가 기존과 동일 |
| `UserNotFoundException` | 존재하지 않는 회원 조회 |