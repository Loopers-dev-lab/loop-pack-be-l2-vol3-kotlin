# Loopers 도메인 모델: 회원(User)

## 도메인 컨텍스트

Loopers는 회원이 브랜드 상품을 탐색하고, 좋아요를 누르고, 주문하는 감성 이커머스 서비스입니다.

서비스를 이용하기 위해서는 회원으로 가입해야 하며, 다음과 같은 기능을 제공합니다.
- 회원 가입 전에도 브랜드와 상품 정보를 조회할 수 있다.
- 좋아요, 주문 등의 기능은 회원만 이용할 수 있다.

회원은 로그인 ID와 비밀번호로 식별된다.
- 별도의 로그인 API는 없다.
- 매 요청마다 헤더(`X-Loopers-LoginId`, `X-Loopers-LoginPw`)로 인증한다.

회원은 자신의 정보를 조회하고, 비밀번호를 변경할 수 있다.

---

## 도메인 모델

### 회원(User)
**Aggregate Root (POJO)**

#### 속성
| 속성 | 타입 | 설명 |
|------|------|------|
| id | Long | 식별자 (저장 후 할당) |
| loginId | LoginId | 로그인 ID (unique) |
| password | Password | 비밀번호 (해시 저장) |
| name | Name | 이름 |
| birthDate | BirthDate | 생년월일 |
| email | Email | 이메일 |
| gender | GenderType | 성별 |

#### 생성 메서드
| 메서드 | 설명 |
|--------|------|
| `register(loginId, password, name, birthDate, email, gender)` | 회원 가입 |
| `reconstitute(id, loginId, password, name, birthDate, email, gender)` | DB 복원용 |

#### 행위
| 메서드 | 설명 |
|--------|------|
| `authenticate(rawPassword, passwordEncoder)` | 비밀번호 검증 |
| `changePassword(oldPassword, newPassword, passwordEncoder)` | 비밀번호 변경 |

#### 규칙
- 비밀번호는 해시로 저장한다
- 비밀번호 변경 시 기존 비밀번호가 일치해야 한다
- 비밀번호 변경 시 새 비밀번호는 기존과 달라야 한다

---

### 로그인 ID(LoginId)
**Value Object**

#### 속성
| 속성 | 타입 | 설명 |
|------|------|------|
| value | String | 로그인 ID 값 |

#### 규칙
- 영문 대소문자와 숫자만 허용 (`^[a-zA-Z0-9]+$`)
- 10자 이내
- 중복 불가
- case-sensitive (대소문자 구분)

---

### 비밀번호(Password)
**Value Object**

#### 속성
| 속성 | 타입 | 설명 |
|------|------|------|
| value | String | 해시된 비밀번호 (private) |

#### 생성
```kotlin
Password.create(rawPassword, birthDate, passwordEncoder)
Password.fromEncoded(encodedPassword)  // DB 복원용
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
| `toEncodedString()` | 해시된 비밀번호 반환 |

---

### 이름(Name)
**Value Object**

#### 속성
| 속성 | 타입 | 설명 |
|------|------|------|
| value | String | 이름 값 |

#### 규칙
- 1자 이상
- 공백만으로 구성 불가

#### 행위
| 메서드 | 설명                                |
|--------|-----------------------------------|
| `masked()` | 마지막 글자를 `*`로 마스킹 ("홍길동" -> "홍길*") |

---

### 생년월일(BirthDate)
**Value Object**

#### 속성
| 속성 | 타입 | 설명 |
|------|------|------|
| value | LocalDate | 생년월일 값 |

#### 생성
```kotlin
BirthDate.from("1993-04-01")
BirthDate(localDate)
```

#### 규칙
- `yyyy-MM-dd` 형식
- 유효한 날짜여야 함
- 미래 날짜 불가

#### 행위
| 메서드 | 설명 |
|--------|------|
| `toCompactString()` | "YYYYMMDD" 형식 반환 |

---

### 이메일(Email)
**Value Object**

#### 속성
| 속성 | 타입 | 설명 |
|------|------|------|
| value | String | 이메일 주소 |

#### 규칙
- `xx@yy.zz` 형식

---

### 성별(GenderType)
**Enum**

| 값 | 설명 |
|----|------|
| MALE | 남성 |
| FEMALE | 여성 |

---

## Repository

```kotlin
interface UserRepository {
    fun existsByLoginId(loginId: LoginId): Boolean
    fun findByLoginId(loginId: LoginId): User?
    fun save(user: User): User
}
```
