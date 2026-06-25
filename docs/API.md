# API 명세

이 문서는 현재 API 목록과 구현 코드 기준의 API 계약 초안이다.
성공/실패 응답은 모두 [공통 응답](#공통-응답) wrapper를 사용한다.
아래 `Response Data` 예시는 wrapper의 `data` 안에 들어가는 값만 보여준다.

`🔄 PR#N` 표시가 없는 API는 `dev` 브랜치에 머지된 상태다.
`🔄 PR#N` 표시가 있는 API는 해당 PR이 머지되어야 사용 가능하다.

## 목차

- [공통 API 규칙](#공통-api-규칙)
- [API 목록](#api-목록)
- [Enum](#enum)
- [공통 에러 코드](#공통-에러-코드)
- [도메인 에러 코드 카탈로그](#도메인-에러-코드-카탈로그)
- [인증 API](#인증-api)
- [장바구니 API](#장바구니-api)
- [카테고리 API](#카테고리-api)
- [문의 API](#문의-api)
- [주문 API](#주문-api)
- [상품 API](#상품-api)
- [명세 충돌 정리](#명세-충돌-정리)

## 공통 API 규칙

### Base URL

- 로컬 실행 기준: `http://localhost:8080`
- 모든 REST API prefix: `/api`
- 요청/응답 Content-Type: `application/json; charset=UTF-8`
- 금액 단위: 원화 정수. 소수점 금액은 사용하지 않는다.
- 일시 형식: ISO 8601 문자열. 예: `2026-06-22T09:30:00`

### 인증

JWT Bearer 토큰을 사용한다.

```http
Authorization: Bearer {accessToken}
```

- Access Token은 로그인 응답 `data.accessToken`에서 획득한다.
- Refresh Token은 로그인 응답 헤더 `Set-Cookie: refreshToken=...; HttpOnly; Path=/api/auth; SameSite=Strict`로 설정된다.
- 토큰 갱신 시 Cookie의 refreshToken을 자동으로 서버로 전달한다.

인증이 필요 없는 API:

- `POST /api/auth/signup`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `GET /api/categories`
- `GET /api/categories/{categoryId}/products`
- `GET /api/products` 🔄 PR#64
- `GET /api/products/{productId}` 🔄 PR#65
- `GET /api/products/search` 🔄 PR#59

관리자 API는 `ADMIN` 역할이 필요하다.

- `POST /api/admins/inquiry` 🔄 PR#54

### 공통 응답

성공과 실패 모두 같은 wrapper를 사용한다.

성공 응답:

```json
{
  "success": true,
  "message": "요청이 성공했습니다.",
  "data": {}
}
```

실패 응답:

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

`data`가 `null`이면 응답 JSON에서 생략된다.
`errors`는 유효성 검사 실패 시에만 포함된다.

### 페이지네이션

목록 API는 기본적으로 다음 query parameter를 사용한다.

| 이름 | 타입 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `page` | int | `0` | 0부터 시작하는 페이지 번호 |
| `size` | int | `20` | 페이지 크기 |

> ⚠️ 두 가지 페이지 응답 형식이 혼재한다. [명세 충돌 정리](#명세-충돌-정리) 참고.

`PageResponse` 형식 (`GET /api/products` 등):

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 120,
  "totalPages": 6,
  "last": true
}
```

`ProductSearchResponse` 형식 (`GET /api/products/search`):

```json
{
  "products": [],
  "page": 0,
  "size": 20,
  "totalElements": 120,
  "totalPages": 6,
  "last": true
}
```

---

## API 목록

| 상태 | 도메인 | 이름 | Method | Path |
| --- | --- | --- | --- | --- |
| ✅ | 인증 | 회원가입 | `POST` | `/api/auth/signup` |
| ✅ | 인증 | 로그인 | `POST` | `/api/auth/login` |
| ✅ | 인증 | 토큰 갱신 | `POST` | `/api/auth/refresh` |
| ✅ | 장바구니 | 장바구니 상품 추가 | `POST` | `/api/carts/items` |
| ✅ | 장바구니 | 장바구니 조회 | `GET` | `/api/carts` |
| ✅ | 카테고리 | 카테고리 목록 조회 | `GET` | `/api/categories` |
| ✅ | 카테고리 | 카테고리별 상품 목록 조회 | `GET` | `/api/categories/{categoryId}/products` |
| ✅ | 문의 | 문의 등록 | `POST` | `/api/members/inquiry` |
| ✅ | 주문 | 직접 주문 (바로 구매) | `POST` | `/api/orders/direct` |
| 🔄 PR#69 | 주문 | 바로 구매 주문서 미리보기 | `POST` | `/api/orders/direct/preview` |
| 🔄 PR#54 | 문의 | 관리자 문의 답변 | `POST` | `/api/admins/inquiry` |
| 🔄 PR#59 | 상품 | 상품 검색 | `GET` | `/api/products/search` |
| 🔄 PR#61 | 주문 | 주문 상태 변경 | `PATCH` | `/api/orders/{orderId}/status` |
| 🔄 PR#64 | 상품 | 상품 목록 조회 | `GET` | `/api/products` |
| 🔄 PR#65 | 상품 | 상품 상세 조회 | `GET` | `/api/products/{productId}` |
| 🔄 PR#68 | 주문 | 장바구니 주문서 미리보기 | `GET` | `/api/orders/preview` |

---

## Enum

### MemberRole

| 값 | 설명 |
| --- | --- |
| `MEMBER` | 일반 회원 |
| `ADMIN` | 관리자 |

### ProductStatus

| 값 | 설명 |
| --- | --- |
| `ON_SALE` | 판매 중 |
| `OUT_OF_STOCK` | 품절 |
| `STOPPED` | 판매 중지 |

### OrderStatus

| 값 | 설명 |
| --- | --- |
| `PENDING` | 주문 대기 |
| `PAID` | 결제 완료 |
| `CANCELLED` | 주문 취소 |

### InquiryStatus

| 값 | 설명 |
| --- | --- |
| `WAITING` | 답변 대기 |
| `ANSWERED` | 답변 완료 |
| `CLOSED` | 문의 종료 |

---

## 공통 에러 코드

| 코드 | HTTP | 설명 |
| --- | --- | --- |
| `VALIDATION_FAILED` | 400 | 요청 본문, 쿼리 파라미터, 경로 변수 검증 실패 |
| `INVALID_ENUM_VALUE` | 400 | 허용하지 않는 Enum 값 |
| `UNAUTHORIZED` | 401 | 인증 토큰 누락 또는 인증 실패 |
| `FORBIDDEN` | 403 | 권한 없음 |

---

## 도메인 에러 코드 카탈로그

| 코드 | HTTP | 설명 | 도입 시점 |
| --- | --- | --- | --- |
| `DUPLICATE_EMAIL` | 409 | 이미 사용 중인 이메일 | ✅ |
| `MEMBER_NOT_FOUND` | 404 | 회원 없음 | ✅ |
| `PRODUCT_NOT_FOUND` | 404 | 상품 없음 | ✅ |
| `INVALID_QUANTITY` | 400 | 수량이 1개 미만 | ✅ |
| `CART_ITEM_QUANTITY_EXCEEDED` | 400 | 장바구니 수량이 재고 초과 | ✅ |
| `DUPLICATE_CART_ITEM` | 409 | 이미 장바구니에 담긴 상품 | ✅ |
| `CATEGORY_NOT_FOUND` | 404 | 카테고리 없음 | ✅ |
| `OUT_OF_STOCK` | 400 | 재고 부족 | ✅ |
| `INQUIRY_NOT_FOUND` | 404 | 문의 없음 | 🔄 PR#54 |
| `ALREADY_ANSWERED_INQUIRY` | 400 | 이미 답변 완료된 문의 | 🔄 PR#54 |
| `ORDER_NOT_FOUND` | 404 | 주문 없음 | 🔄 PR#61 |
| `INVALID_ORDER_STATUS` | 400 | 변경할 수 없는 주문 상태 | 🔄 PR#61 |
| `CART_ITEM_NOT_FOUND` | 404 | 장바구니 상품 없음 | 🔄 PR#68 |
| `EMPTY_ORDER_PREVIEW` | 400 | 주문할 상품 없음 | 🔄 PR#68 |
| `NOT_ORDERABLE_PRODUCT` | 400 | 주문할 수 없는 상품 포함 | 🔄 PR#68 |

---

## 인증 API

### 회원가입

회원을 생성한다.

- Method: `POST`
- Path: `/api/auth/signup`
- 인증: 불필요
- HTTP Status: `201 Created`

#### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `email` | String | Y | 이메일. 최대 100자, UNIQUE |
| `password` | String | Y | 비밀번호. 8~255자, 영문·숫자·특수문자 각 1개 이상 |
| `name` | String | Y | 이름. 최대 50자 |
| `phone` | String | Y | 전화번호. 예: `010-1234-5678` |

```json
{
  "email": "user@example.com",
  "password": "Password1!",
  "name": "홍길동",
  "phone": "010-1234-5678"
}
```

#### Response Data

```json
{
  "memberId": 1,
  "email": "user@example.com",
  "name": "홍길동"
}
```

#### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `VALIDATION_FAILED` | 400 | 이메일 형식 오류, 비밀번호 정책 위반, 필수 값 누락 |
| `DUPLICATE_EMAIL` | 409 | 이미 가입된 이메일 |

---

### 로그인

이메일과 비밀번호를 검증하고 JWT access token을 발급한다.
Refresh token은 `HttpOnly Cookie`로 설정된다.

- Method: `POST`
- Path: `/api/auth/login`
- 인증: 불필요
- HTTP Status: `200 OK`

#### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `email` | String | Y | 이메일 |
| `password` | String | Y | 비밀번호 |

```json
{
  "email": "user@example.com",
  "password": "Password1!"
}
```

#### Response Headers

```http
Set-Cookie: refreshToken={token}; HttpOnly; Path=/api/auth; SameSite=Strict
```

#### Response Data

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

#### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `VALIDATION_FAILED` | 400 | 이메일 누락 또는 형식 오류, 비밀번호 누락 |
| `UNAUTHORIZED` | 401 | 이메일 또는 비밀번호 불일치 |

---

### 토큰 갱신

Cookie의 refresh token을 검증하고 새 access token을 발급한다.

- Method: `POST`
- Path: `/api/auth/refresh`
- 인증: 불필요 (Cookie의 `refreshToken` 필요)
- HTTP Status: `200 OK`

#### Request

- Cookie: `refreshToken={refreshToken}`

#### Response Headers

```http
Set-Cookie: refreshToken={newToken}; HttpOnly; Path=/api/auth; SameSite=Strict
```

#### Response Data

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

#### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | refresh token 누락, 만료, 유효하지 않은 토큰 |

---

## 장바구니 API

### 장바구니 상품 추가

로그인한 회원이 상품을 장바구니에 추가한다.
동일 상품이 이미 장바구니에 있으면 `DUPLICATE_CART_ITEM`을 반환한다.

- Method: `POST`
- Path: `/api/carts/items`
- 인증: 필요
- HTTP Status: `201 Created`

#### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `productId` | Long | Y | 상품 ID |
| `quantity` | Integer | Y | 수량 (1 이상) |

```json
{
  "productId": 10,
  "quantity": 2
}
```

#### Response Data

```json
{
  "cartItemId": 5,
  "productId": 10,
  "productName": "노트북 파우치",
  "quantity": 2
}
```

#### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `VALIDATION_FAILED` | 400 | 필수 값 누락 |
| `INVALID_QUANTITY` | 400 | 수량이 1 미만 |
| `CART_ITEM_QUANTITY_EXCEEDED` | 400 | 요청 수량이 재고 초과 |
| `UNAUTHORIZED` | 401 | 토큰 누락 |
| `PRODUCT_NOT_FOUND` | 404 | 상품 없음 |
| `DUPLICATE_CART_ITEM` | 409 | 이미 장바구니에 담긴 상품 |

---

### 장바구니 조회

로그인한 회원의 장바구니를 조회한다.

- Method: `GET`
- Path: `/api/carts`
- 인증: 필요
- HTTP Status: `200 OK`

#### Response Data

```json
{
  "items": [
    {
      "cartItemId": 5,
      "productId": 10,
      "productName": "노트북 파우치",
      "productPrice": 25000,
      "quantity": 2,
      "itemTotalPrice": 50000,
      "stock": 10,
      "productStatus": "ON_SALE",
      "orderable": true
    }
  ],
  "cartTotalPrice": 50000
}
```

#### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 누락 |

---

## 카테고리 API

### 카테고리 목록 조회

카테고리 전체 목록을 계층 구조로 조회한다.

- Method: `GET`
- Path: `/api/categories`
- 인증: 불필요
- HTTP Status: `200 OK`

#### Response Data

```json
[
  {
    "categoryId": 1,
    "name": "전자기기",
    "children": [
      {
        "categoryId": 2,
        "name": "노트북",
        "children": []
      }
    ]
  }
]
```

#### Errors

없음

---

### 카테고리별 상품 목록 조회

특정 카테고리에 속한 상품 목록을 조회한다.

- Method: `GET`
- Path: `/api/categories/{categoryId}/products`
- 인증: 불필요
- HTTP Status: `200 OK`

> ⚠️ PR#64 머지 후 응답 형식이 배열 → `PageResponse` 객체로 변경된다. [명세 충돌 정리](#명세-충돌-정리) 참고.

#### Path Variables

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| `categoryId` | Long | 카테고리 ID |

#### Query Parameters (PR#64 머지 후 추가)

| 이름 | 타입 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `sort` | String | `newest` | 정렬 기준 |
| `page` | int | `0` | 페이지 번호 |
| `size` | int | `20` | 페이지 크기 |

#### Response Data (현재 dev)

```json
[
  {
    "id": 10,
    "name": "노트북 파우치",
    "price": 25000,
    "stock": 10,
    "status": "ON_SALE",
    "description": "슬림한 노트북 파우치입니다."
  }
]
```

#### Response Data (PR#64 머지 후)

```json
{
  "content": [
    {
      "id": 10,
      "name": "노트북 파우치",
      "price": 25000,
      "stock": 10,
      "status": "ON_SALE",
      "description": "슬림한 노트북 파우치입니다."
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

#### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `CATEGORY_NOT_FOUND` | 404 | 카테고리 없음 |

---

## 문의 API

### 문의 등록

로그인한 회원이 문의를 등록한다.

- Method: `POST`
- Path: `/api/members/inquiry`
- 인증: 필요
- HTTP Status: `201 Created`

#### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `title` | String | Y | 제목. 최대 100자 |
| `content` | String | Y | 내용. 최대 1000자 |

```json
{
  "title": "배송 관련 문의",
  "content": "주문 후 배송은 얼마나 걸리나요?"
}
```

#### Response Data

```json
{
  "id": 20,
  "memberId": 1,
  "title": "배송 관련 문의",
  "content": "주문 후 배송은 얼마나 걸리나요?",
  "status": "WAITING",
  "createdAt": "2026-06-22T09:30:00"
}
```

#### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `VALIDATION_FAILED` | 400 | 제목 또는 내용 누락, 길이 초과 |
| `UNAUTHORIZED` | 401 | 토큰 누락 |

---

### 관리자 문의 답변 🔄 PR#54

관리자가 문의에 답변한다.

- Method: `POST`
- Path: `/api/admins/inquiry`
- 인증: 필요 (`ADMIN` 역할)
- HTTP Status: `200 OK`

#### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `inquiryId` | Long | Y | 답변할 문의 ID |
| `answer` | String | Y | 답변 내용. 최대 1000자 |

```json
{
  "inquiryId": 20,
  "answer": "배송은 주문 후 영업일 기준 2~3일 소요됩니다."
}
```

#### Response Data

```json
{
  "id": 20,
  "memberId": 1,
  "title": "배송 관련 문의",
  "content": "주문 후 배송은 얼마나 걸리나요?",
  "adminId": 2,
  "answer": "배송은 주문 후 영업일 기준 2~3일 소요됩니다.",
  "status": "ANSWERED",
  "createdAt": "2026-06-22T09:30:00",
  "answeredAt": "2026-06-22T11:00:00"
}
```

#### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `VALIDATION_FAILED` | 400 | 문의 ID 또는 답변 내용 누락, 길이 초과 |
| `UNAUTHORIZED` | 401 | 토큰 누락 |
| `FORBIDDEN` | 403 | ADMIN 역할 없음 |
| `INQUIRY_NOT_FOUND` | 404 | 문의 없음 |
| `ALREADY_ANSWERED_INQUIRY` | 400 | 이미 답변 완료된 문의 |

---

## 주문 API

### 장바구니 주문서 미리보기 🔄 PR#68

장바구니 상품을 기준으로 주문서 미리보기를 조회한다.
`cartIds`가 없으면 회원의 전체 장바구니를 대상으로 조회하고, `cartIds`가 있으면 선택된 항목만 조회한다.

- Method: `GET`
- Path: `/api/orders/preview`
- 인증: 필요
- HTTP Status: `200 OK`

#### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `cartIds` | Long[] | N | 선택할 장바구니 항목 ID 목록. 미입력 시 전체 장바구니 대상 |

예시: `GET /api/orders/preview?cartIds=1&cartIds=3`

#### Response Data

```json
{
  "orderItems": [
    {
      "cartId": 1,
      "productId": 10,
      "productName": "노트북 파우치",
      "productPrice": 25000,
      "quantity": 2,
      "productTotalAmount": 50000
    }
  ],
  "totalOrderAmount": 50000
}
```

#### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 누락 |
| `CART_ITEM_NOT_FOUND` | 404 | 요청한 cartIds 중 존재하지 않는 항목 |
| `EMPTY_ORDER_PREVIEW` | 400 | 미리볼 상품이 없음 |
| `NOT_ORDERABLE_PRODUCT` | 400 | 주문할 수 없는 상품 포함 |

---

### 바로 구매 주문서 미리보기 🔄 PR#69

상품 상세 페이지에서 바로 주문하기 전 주문 금액과 상품 정보를 미리 확인한다.
실제 주문을 저장하거나 상품 재고를 차감하지 않는다.

- Method: `POST`
- Path: `/api/orders/direct/preview`
- 인증: 필요
- HTTP Status: `200 OK`

#### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `productId` | Long | Y | 상품 ID |
| `quantity` | Integer | Y | 수량 (1 이상) |

```json
{
  "productId": 10,
  "quantity": 2
}
```

#### Response Data

```json
{
  "orderItems": [
    {
      "productId": 10,
      "productName": "노트북 파우치",
      "productPrice": 25000,
      "quantity": 2,
      "productTotalAmount": 50000
    }
  ],
  "totalOrderAmount": 50000
}
```

#### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `VALIDATION_FAILED` | 400 | 필수 값 누락 또는 수량이 1 미만 |
| `NOT_ORDERABLE_PRODUCT` | 400 | 판매 중이 아닌 상품 |
| `OUT_OF_STOCK` | 400 | 재고 부족 |
| `UNAUTHORIZED` | 401 | 토큰 누락 |
| `PRODUCT_NOT_FOUND` | 404 | 상품 없음 |

---

### 직접 주문 (바로 구매)

상품 상세 페이지에서 단건 상품을 바로 구매한다.

- Method: `POST`
- Path: `/api/orders/direct`
- 인증: 필요
- HTTP Status: `201 Created`

#### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `productId` | Long | Y | 상품 ID |
| `quantity` | Integer | Y | 수량 (1 이상) |

```json
{
  "productId": 10,
  "quantity": 1
}
```

#### Response Data

```json
{
  "orderId": 100,
  "orderNumber": "ORD-20260622-001",
  "orderStatus": "PENDING",
  "totalAmount": 25000,
  "orderItems": [
    {
      "productName": "노트북 파우치",
      "productPrice": 25000,
      "quantity": 1,
      "totalPrice": 25000
    }
  ]
}
```

#### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `VALIDATION_FAILED` | 400 | 필수 값 누락 |
| `OUT_OF_STOCK` | 400 | 재고 부족 |
| `UNAUTHORIZED` | 401 | 토큰 누락 |
| `PRODUCT_NOT_FOUND` | 404 | 상품 없음 |

---

### 주문 상태 변경 🔄 PR#61

주문 상태를 변경한다.

- Method: `PATCH`
- Path: `/api/orders/{orderId}/status`
- 인증: 필요
- HTTP Status: `200 OK`

#### Path Variables

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| `orderId` | Long | 주문 ID |

#### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `orderStatus` | OrderStatus | Y | 변경할 주문 상태 |

```json
{
  "orderStatus": "PAID"
}
```

#### Response Data

```json
null
```

#### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `VALIDATION_FAILED` | 400 | 주문 상태 누락 |
| `INVALID_ORDER_STATUS` | 400 | 변경할 수 없는 주문 상태 |
| `UNAUTHORIZED` | 401 | 토큰 누락 |
| `ORDER_NOT_FOUND` | 404 | 주문 없음 |

---

## 상품 API

### 상품 목록 조회 🔄 PR#64

판매 중인 상품 목록을 페이지네이션으로 조회한다.

- Method: `GET`
- Path: `/api/products`
- 인증: 불필요
- HTTP Status: `200 OK`

#### Query Parameters

| 이름 | 타입 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `sort` | String | `newest` | 정렬 기준 |
| `page` | int | `0` | 페이지 번호 (0-based) |
| `size` | int | `20` | 페이지 크기 |

#### Response Data

```json
{
  "content": [
    {
      "id": 10,
      "name": "노트북 파우치",
      "price": 25000,
      "stock": 10,
      "status": "ON_SALE",
      "description": "슬림한 노트북 파우치입니다."
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

> ⚠️ 페이지 마지막 여부 키가 `last`이다. 검색 API의 `hasNext`와 의미가 반전된다. [명세 충돌 정리](#명세-충돌-정리) 참고.

#### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `VALIDATION_FAILED` | 400 | 페이지 파라미터 오류 |

---

### 상품 상세 조회 🔄 PR#65

상품 상세 정보를 조회한다.

- Method: `GET`
- Path: `/api/products/{productId}`
- 인증: 불필요
- HTTP Status: `200 OK`

#### Path Variables

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| `productId` | Long | 상품 ID |

#### Response Data

```json
{
  "productId": 10,
  "name": "노트북 파우치",
  "description": "슬림한 노트북 파우치입니다.",
  "price": 25000,
  "stock": 10,
  "status": "ON_SALE",
  "categoryId": 2,
  "categoryName": "노트북"
}
```

> ⚠️ 상품 ID 필드명이 `productId`이다. 상품 목록 API의 `id`와 다르다. [명세 충돌 정리](#명세-충돌-정리) 참고.

#### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `PRODUCT_NOT_FOUND` | 404 | 상품 없음 |

---

### 상품 검색 🔄 PR#59

키워드로 상품을 검색한다.

- Method: `GET`
- Path: `/api/products/search`
- 인증: 불필요
- HTTP Status: `200 OK`

#### Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| `keyword` | String | Y | - | 검색어 |
| `page` | int | N | `0` | 페이지 번호 |
| `size` | int | N | `20` | 페이지 크기 |
| `sort` | String | N | `createdAt,desc` | 정렬 기준 |

#### Response Data

```json
{
  "products": [
    {
      "id": 10,
      "name": "노트북 파우치",
      "price": 25000,
      "stock": 10,
      "status": "ON_SALE",
      "orderable": true
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

#### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `VALIDATION_FAILED` | 400 | keyword 누락 |

---

## 명세 충돌 정리

### 1. 상품 응답 필드명 — ✅ PR#59에서 해결됨

| 개념 | 목록 `GET /api/products` | 카테고리별 | 상세 `GET /api/products/{id}` | 검색 `GET /api/products/search` |
|---|---|---|---|---|
| 상품 ID 키 | `id` | `id` | `productId` | `id` ✅ |
| 상품명 키 | `name` | `name` | `name` | `name` ✅ |
| 가격 키 | `price` | `price` | `price` | `price` ✅ |
| 상태 키 | `status` | `status` | `status` | `status` ✅ |
| 주문 가능 여부 | 없음 | 없음 | 없음 | `orderable` |

> 상세 API(`productId`)는 PR#65에서 미해결. **관련 이슈:** #71

---

### 2. 페이지네이션 래퍼 불일치 — 부분 해결

| 항목 | 목록 `PageResponse` | 검색 `ProductSearchResponse` |
|---|---|---|
| 데이터 배열 키 | `content` | `products` (도메인 의미 유지) |
| 마지막 페이지 키 | `last` | `last` ✅ |

> 배열 키는 의도적으로 다르게 유지. `last` 키는 PR#59에서 통일됨. **관련 이슈:** #71

---

### 3. 카테고리별 상품 조회 Breaking Change

PR#64 머지 시 `GET /api/categories/{categoryId}/products`의 응답 타입이 변경된다.

| | 응답 타입 |
|---|---|
| 현재 (dev) | `ProductResponse[]` (배열) |
| PR#64 머지 후 | `PageResponse<ProductResponse>` (페이지네이션 객체) |

**영향:** 이 엔드포인트를 사용하는 코드가 있다면 PR#64 머지 시 수정이 필요하다.
**관련 이슈:** #71

---

### 4. 병합 충돌 예고

아래 파일들은 여러 PR이 동시에 수정 중이다. 머지 전 반드시 `dev` 기준으로 rebase해야 한다.

| 파일 | 충돌하는 PR |
|---|---|
| `ProductController.java` | #59, #64, #65 (모두 빈 클래스에서 메서드 추가) |
| `OrderController.java` | #61, #68 |
| `ErrorCode.java` | #54, #59, #60, #61, #64, #68 |
