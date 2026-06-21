# DTO

## 목적

DTO는 API 요청/응답 또는 계층 간 데이터 전달을 위해 사용한다.

Entity를 외부에 직접 노출하지 않고, 필요한 데이터만 명확하게 전달하는 것을 목표로 한다.

## 기본 원칙

- Entity를 API 요청/응답으로 직접 사용하지 않는다.
- Request DTO와 Response DTO를 분리한다.
- DTO는 가능한 불변 객체로 작성한다.
- DTO 이름은 Naming 문서를 따른다.
- DTO 위치는 Package Structure 문서를 따른다.

## Request DTO

Request DTO는 클라이언트가 서버로 보내는 요청 값을 담는다.

- 요청 값의 형식 검증은 Request DTO에서 처리한다.
- `@NotBlank`, `@NotNull`, `@Email`, `@Size` 같은 Bean Validation을 사용한다.
- 비즈니스 규칙 검증은 Request DTO가 아니라 Service에서 처리한다.
- Request DTO는 Entity로 직접 노출하거나 저장하지 않는다.

예시:

```java
public record UserCreateRequest(
        @NotBlank String email,
        @NotBlank String password,
        @NotBlank String nickname
) {
}
```

## Response DTO

Response DTO는 서버가 클라이언트에 반환할 값을 담는다.

- 클라이언트에 필요한 값만 포함한다.
- 비밀번호, 토큰, 인증번호 같은 민감 정보는 포함하지 않는다.
- Entity를 그대로 반환하지 않고 Response DTO로 변환한다.
- 단순 변환은 `from()` 정적 팩토리 메서드를 우선 사용한다.

예시:

```java
public record UserResponse(
        Long userId,
        String email,
        String nickname
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname()
        );
    }
}
```

## DTO와 Entity 변환

단순한 Entity → Response DTO 변환은 Response DTO의 `from()` 메서드를 우선 사용한다.

Entity → Response DTO 변환은 기본적으로 Service 계층에서 수행한다.

Controller는 Service가 반환한 DTO를 그대로 응답하는 것을 우선한다.

Request DTO는 Entity 생성에 필요한 값을 전달할 뿐, 비즈니스 규칙이 들어간 Entity 생성 로직을 직접 가지지 않는다.

복잡한 변환이 반복되면 별도 Mapper 도입을 검토한다.

## record 사용 기준

Request DTO와 Response DTO는 `record` 사용을 우선 검토한다.

`record`는 값을 변경할 수 없는 불변 객체를 쉽게 만들 수 있어 DTO에 적합하다.

단, 다음 경우에는 class 사용을 검토할 수 있다.

- 기본 생성자가 필요한 라이브러리를 사용하는 경우
- 필드 기본값이나 복잡한 생성 로직이 필요한 경우
- 요청 값을 단계적으로 조립해야 하는 경우

## 피해야 할 DTO

- Entity와 필드가 완전히 동일한 DTO
- 하나의 DTO를 요청과 응답에 같이 사용하는 구조
- 모든 API에서 재사용하려는 지나치게 큰 공통 DTO
- 민감 정보를 포함하는 Response DTO
- 비즈니스 로직을 포함하는 DTO