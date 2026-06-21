# Entity

## 목적

Entity는 DB 테이블과 비즈니스 상태를 표현한다.

Entity는 JPA가 관리하는 객체이므로 API 응답, 화면 표시, 요청 검증 같은 역할을 함께 맡지 않는다.

## 기본 원칙

- Entity를 API 응답으로 직접 반환하지 않는다.
- Entity에 요청 DTO나 응답 DTO를 의존시키지 않는다.
- Entity는 DB 상태와 도메인 상태 변경을 표현한다.
- 화면 표시용 로직이나 API 응답 조립 로직은 Entity에 두지 않는다.
- Entity 이름과 위치는 Naming, Package Structure 문서를 따른다.

## Lombok 사용 기준

Entity에서는 아래 Lombok 사용을 허용한다.

- `@Getter`
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)`

Entity에서는 아래 Lombok 사용을 피한다.

- `@Setter`
- `@Data`
- `@AllArgsConstructor`

프로덕션 코드에서 Entity 생성은 정적 팩토리 메서드를 우선 사용한다.

`@Builder`는 프로덕션 Entity 생성 규칙을 대체하는 용도로 사용하지 않는다.

테스트 데이터 생성이 반복되어 불편한 경우에만 테스트용 Builder 또는 Fixture 사용을 검토한다.

## 필드

- 필드는 가능한 `private`으로 선언한다.
- 외부에서 직접 값을 바꾸지 못하도록 Setter를 무분별하게 열어두지 않는다.
- 반드시 필요한 값은 생성 시점에 받는다.
- nullable 여부, unique 여부, 길이 제한은 DB 제약조건과 비즈니스 규칙을 함께 고려한다.
- 민감 정보는 로그나 응답으로 노출되지 않도록 주의한다.

## 공통 시간 필드

생성 시각, 수정 시각이 필요한 Entity는 공통 BaseEntity 상속을 우선 검토한다.

공통 시간 필드는 `createdAt`, `updatedAt` 이름을 사용한다.

예시:

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

BaseEntity는 여러 도메인 Entity가 공통으로 사용하므로 `common.entity` 패키지에 둔다.

## 생성자와 생성 메서드

JPA를 위해 기본 생성자가 필요하다.

기본 생성자는 `@NoArgsConstructor(access = AccessLevel.PROTECTED)` 사용을 우선한다.

프로덕션 코드에서 Entity 생성은 정적 팩토리 메서드를 우선 사용한다.

정적 팩토리 메서드는 Entity 생성 규칙을 드러내기 위해 사용한다.

예시:

```java
public static User create(String email, String encodedPassword, String nickname) {
    User user = new User();
    user.email = email;
    user.encodedPassword = encodedPassword;
    user.nickname = nickname;
    user.status = UserStatus.ACTIVE;
    return user;
}
```

## 상태 변경 메서드

Entity 상태 변경은 의미 있는 메서드로 표현한다.

단순 Setter보다 비즈니스 의도가 드러나는 메서드를 우선 사용한다.

예시:

```java
public void changeNickname(String nickname) {
    this.nickname = nickname;
}

public void withdraw() {
    this.status = UserStatus.WITHDRAWN;
}
```

피해야 할 예시:

```java
user.setStatus(UserStatus.WITHDRAWN);
```

## 연관관계

연관관계는 필요한 경우에만 설정한다.

모든 연관관계는 `FetchType.LAZY`를 기본으로 사용한다.

양방향 연관관계는 꼭 필요한 경우에만 사용한다.

단순히 id만 필요하다면 연관관계 대신 `{domain}Id` 보관도 검토할 수 있다.

컬렉션 연관관계는 외부에서 직접 수정하지 못하게 관리한다.

## Enum

Entity 상태를 표현하는 값은 문자열이나 숫자보다 enum 사용을 우선 검토한다.

JPA enum 매핑은 `EnumType.STRING`을 사용한다.

예시:

```java
@Enumerated(EnumType.STRING)
private UserStatus status;
```

`EnumType.ORDINAL`은 enum 순서가 바뀌면 데이터 의미가 깨질 수 있으므로 사용하지 않는다.

## equals와 hashCode

Entity의 `equals`, `hashCode`는 신중하게 작성한다.

처음에는 직접 오버라이드하지 않는 것을 기본으로 한다.

필요한 경우 식별자 기준으로 작성하되, 영속화 전후 상태 차이를 고려한다.

## toString

Entity의 `toString`은 신중하게 작성한다.

연관관계 필드를 포함하면 순환 참조나 지연 로딩 문제가 생길 수 있다.

비밀번호, 토큰, 인증번호 같은 민감 정보는 포함하지 않는다.

## 피해야 할 Entity

- 모든 필드에 public setter가 열려 있는 Entity
- `@Setter`, `@Data`를 사용하는 Entity
- API 응답을 직접 담당하는 Entity
- 요청 DTO를 받아서 검증하는 Entity
- 화면 표시용 문자열을 조립하는 Entity
- 비즈니스 의미 없는 단순 데이터 박스 Entity
- `EnumType.ORDINAL`을 사용하는 Entity