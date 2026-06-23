# API Response

## 목적

API 응답 형식을 일관되게 유지한다.

클라이언트가 성공 응답과 실패 응답을 예측 가능하게 처리할 수 있도록 한다.

## 기본 원칙

- Entity를 API 응답으로 직접 반환하지 않는다.
- 성공 응답은 `ApiResponse<T>`로 감싼다.
- 실패 응답은 `ErrorResponse`로 반환한다.
- 응답 DTO 이름은 Naming 문서를 따른다.
- 에러 코드와 예외 처리 기준은 `exception.md`를 따른다.

## 성공 응답

성공 응답은 `ApiResponse<T>`를 사용한다.

기본 필드:

- `success`
- `message`
- `data`

`message` 기본값은 `"요청이 성공했습니다."`를 사용한다.

`data`가 `null`인 경우 JSON 응답에서 제외된다.

예시:

```json
{
  "success": true,
  "message": "요청이 성공했습니다.",
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "nickname": "닉네임"
  }
}
```

본문이 필요 없는 성공 응답은 204 No Content를 사용할 수 있다.

## 실패 응답

실패 응답은 `ErrorResponse`를 사용한다.

기본 필드:

- `success`
- `code`
- `message`

`code` 값은 `ErrorCode.name()`을 사용한다.

예시:

```json
{
  "success": false,
  "code": "USER_NOT_FOUND",
  "message": "사용자를 찾을 수 없습니다."
}
```

예상하지 못한 서버 오류는 내부 구현 정보를 노출하지 않는다.

## Validation 실패 응답

요청 값 검증 실패는 필요한 경우 필드별 에러 정보를 포함할 수 있다.

필드별 에러 정보는 아래 값을 포함한다.

- `field`
- `message`

예시:

```json
{
  "success": false,
  "code": "VALIDATION_FAILED",
  "message": "요청 본문, 쿼리 파라미터, 경로 변수 검증에 실패했습니다.",
  "errors": [
    {
      "field": "email",
      "message": "이메일 형식이 올바르지 않습니다."
    }
  ]
}
```

필드별 에러 목록은 필요한 경우에만 포함한다.

## HTTP 상태 코드 기준

- 조회 성공: 200 OK
- 생성 성공: 201 Created
- 수정 성공: 200 OK 또는 204 No Content
- 삭제 성공: 204 No Content
- 요청 값 오류: 400 Bad Request
- 인증 실패: 401 Unauthorized
- 권한 부족: 403 Forbidden
- 리소스 없음: 404 Not Found
- 중복 데이터: 409 Conflict
- 서버 오류: 500 Internal Server Error

## 응답 작성 기준

- 성공 응답과 실패 응답의 구조를 임의로 섞지 않는다.
- 204 응답에는 본문을 포함하지 않는다.

## 피해야 할 응답

- Entity를 그대로 반환하는 응답
- API마다 다른 성공/실패 응답 구조
- 민감 정보를 포함하는 응답
- 내부 예외 메시지나 스택 트레이스를 그대로 노출하는 응답
- 204 응답에 본문을 포함하는 구조
