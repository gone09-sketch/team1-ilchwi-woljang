# Exception

## 목적

예외 처리는 실패 상황을 일관되게 표현하고, 클라이언트에 동일한 형식의 에러 응답을 반환하기 위해 사용한다.

예상 가능한 실패는 명확한 예외와 에러 코드로 표현한다.

## 기본 원칙

- Controller에서 try-catch를 반복하지 않는다.
- 예상 가능한 비즈니스 실패는 커스텀 예외로 표현한다.
- 예외 응답 형식은 `api-response.md`를 따른다.
- 예외 메시지는 원인을 추적할 수 있을 만큼 명확하게 작성한다.
- 예외를 catch한 뒤 아무 처리 없이 무시하지 않는다.
- 민감 정보는 예외 메시지에 포함하지 않는다.

## 예외 처리 흐름

예외 처리 기본 흐름은 아래와 같다.

1. Service에서 비즈니스 규칙 위반을 감지한다.
2. `new BusinessException(ErrorCode.X)` 형태로 예외를 발생시킨다.
3. `GlobalExceptionHandler`가 예외를 잡는다.
4. `ErrorResponse` 형식으로 변환해 클라이언트에 반환한다.

## 공통 예외 구조

비즈니스 예외의 공통 부모는 `BusinessException`으로 둔다.

에러 코드는 `ErrorCode`로 관리한다.

클라이언트에 내려가는 실패 응답은 `ErrorResponse`로 작성한다.

예시 구조:

```text
common
└── exception
    ├── BusinessException
    ├── ErrorCode
    └── handler
        └── GlobalExceptionHandler

common
└── response
    └── ErrorResponse
```

## BusinessException

`BusinessException`은 예상 가능한 비즈니스 실패를 표현하는 공통 예외다.

`BusinessException`은 `ErrorCode`를 가진다.

예시:

```java
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

## ErrorCode

`ErrorCode`는 에러 상황의 HTTP 상태와 메시지를 관리한다.

`ErrorCode`의 enum 이름을 에러 코드로 사용한다.

별도의 `code` 필드는 기본적으로 두지 않는다.

예시:

```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // global
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "요청 본문, 쿼리 파라미터, 경로 변수 검증에 실패했습니다."),
    INVALID_ENUM_VALUE(HttpStatus.BAD_REQUEST, "허용하지 않는 Enum 값입니다."),

    // auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // user
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");

    private final HttpStatus status;
    private final String message;
}
```

## ErrorResponse 변환 기준

`GlobalExceptionHandler`는 예외를 잡아 `ErrorResponse`로 변환한다.

`BusinessException`은 내부의 `ErrorCode`를 기준으로 `ErrorResponse`를 생성한다.

`ErrorResponse`의 `code` 값은 `errorCode.name()`을 사용한다.

`ErrorResponse`의 실제 필드 구성은 `api-response.md`를 따른다.

예시 흐름:

1. Service에서 `BusinessException` 발생
2. `GlobalExceptionHandler`가 예외 처리
3. 예외 안의 `ErrorCode`를 꺼냄
4. `ErrorResponse`로 변환
5. 클라이언트에 공통 에러 응답 반환

## 도메인 예외

처음에는 `new BusinessException(ErrorCode.X)` 형태로 예외를 발생시킨다.

같은 예외가 여러 곳에서 반복되거나, 예외 이름 자체가 도메인 의미를 명확히 드러내야 하는 경우 별도 도메인 예외 클래스로 분리한다.

도메인 예외 클래스는 해당 도메인의 exception 패키지에 둔다.

예시 구조:

```text
domain
└── user
    └── exception
        └── UserNotFoundException
```

예시:

```java
public class UserNotFoundException extends BusinessException {

    public UserNotFoundException() {
        super(ErrorCode.USER_NOT_FOUND);
    }
}
```

## GlobalExceptionHandler

`GlobalExceptionHandler`는 예외를 공통 에러 응답으로 변환한다.

`GlobalExceptionHandler`는 `common.exception.handler` 패키지에 둔다.

처리 대상 예시:

- `BusinessException`
- validation 예외
- 인증/인가 예외
- 예상하지 못한 서버 예외

Validation 예외는 `BusinessException`과 별도로 처리한다.

요청 값 검증 실패 처리 기준은 `validation.md`를 따르고, 응답 형식은 `api-response.md`를 따른다.

예상하지 못한 서버 예외는 내부 구현 정보를 클라이언트에 노출하지 않는다.

## 예외 사용 기준

아래 상황은 `BusinessException` 사용을 검토한다.

- 존재하지 않는 리소스
- 중복 데이터
- 권한 없는 접근
- 소유자가 아닌 사용자의 요청
- 처리할 수 없는 상태 변경
- 이미 처리된 요청

## 예외 메시지 기준

예외 메시지는 사용자가 이해할 수 있거나 개발자가 원인을 추적할 수 있게 작성한다.

단, 아래 정보는 포함하지 않는다.

- 비밀번호
- 토큰
- 인증번호
- API Key
- 내부 SQL
- 서버 파일 경로
- 스택 트레이스

## 피해야 할 예외 처리

- Controller마다 반복되는 try-catch
- 모든 실패를 `RuntimeException` 하나로 처리하는 방식
- 예외를 catch한 뒤 아무 처리 없이 무시하는 방식
- 클라이언트에 내부 구현 정보나 민감 정보를 노출하는 방식
- 실패 원인을 알 수 없는 모호한 에러 메시지