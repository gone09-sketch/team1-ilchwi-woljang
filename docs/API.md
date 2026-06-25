# API 명세

## 공통 규칙

### 응답 포맷

**성공**
```json
{
  "success": true,
  "message": "요청이 성공했습니다.",
  "data": { ... }
}
```

**실패**
```json
{
  "success": false,
  "code": "ERROR_CODE",
  "message": "에러 메시지",
  "errors": [{ "field": "필드명", "message": "검증 메시지" }]
}
```
> `errors`는 유효성 검사 실패 시에만 포함됩니다.

### 인증

JWT Bearer Token 방식을 사용합니다.

- `Authorization: Bearer {accessToken}` 헤더에 포함
- Access Token은 로그인/토큰 갱신 응답의 `data.accessToken`에서 획득
- Refresh Token은 `HttpOnly Cookie (refreshToken)`로 관리

| 구분 | 설명 |
|---|---|
| 🔓 공개 | 인증 없이 접근 가능 |
| 🔒 인증 필요 | 유효한 Access Token 필요 |
| 🛡 관리자 전용 | ADMIN 역할 필요 |

### 상태 표시

| 구분 | 설명 |
|---|---|
| ✅ 머지됨 | dev 브랜치에 반영된 상태 |
| 🔄 PR#N | 해당 PR이 머지되면 사용 가능 |

---

## 인증 (Auth)

### POST /api/auth/signup ✅

회원가입

| | |
|---|---|
| 인증 | 🔓 공개 |
| 상태 코드 | 201 Created |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| email | String | ✓ | 이메일 (최대 100자) |
| password | String | ✓ | 비밀번호 (8~255자, 영문+숫자+특수문자 각 1개 이상) |
| name | String | ✓ | 이름 (최대 50자) |
| phone | String | ✓ | 전화번호 (예: 010-1234-5678) |

**Response Body (`data`)**

| 필드 | 타입 | 설명 |
|---|---|---|
| memberId | Long | 회원 ID |
| email | String | 이메일 |
| name | String | 이름 |

---

### POST /api/auth/login ✅

로그인

| | |
|---|---|
| 인증 | 🔓 공개 |
| 상태 코드 | 200 OK |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| email | String | ✓ | 이메일 |
| password | String | ✓ | 비밀번호 |

**Response Body (`data`)**

| 필드 | 타입 | 설명 |
|---|---|---|
| accessToken | String | JWT Access Token |

> Refresh Token은 응답 헤더 `Set-Cookie: refreshToken=...; HttpOnly; Path=/api/auth; SameSite=Strict`로 설정됩니다.

---

### POST /api/auth/refresh ✅

Access Token 갱신

| | |
|---|---|
| 인증 | 🔓 공개 (Cookie의 Refresh Token 사용) |
| 상태 코드 | 200 OK |

**Request**

- Cookie: `refreshToken={refreshToken}`

**Response Body (`data`)**

| 필드 | 타입 | 설명 |
|---|---|---|
| accessToken | String | 새 JWT Access Token |

---

## 장바구니 (Cart)

### POST /api/carts/items ✅

장바구니 상품 추가

| | |
|---|---|
| 인증 | 🔒 인증 필요 |
| 상태 코드 | 201 Created |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| productId | Long | ✓ | 상품 ID |
| quantity | Integer | ✓ | 수량 (1 이상) |

**Response Body (`data`)**

| 필드 | 타입 | 설명 |
|---|---|---|
| cartItemId | Long | 장바구니 항목 ID |
| productId | Long | 상품 ID |
| productName | String | 상품명 |
| quantity | Integer | 수량 |

---

### GET /api/carts ✅

장바구니 조회

| | |
|---|---|
| 인증 | 🔒 인증 필요 |
| 상태 코드 | 200 OK |

**Response Body (`data`)**

| 필드 | 타입 | 설명 |
|---|---|---|
| items | CartItemResponse[] | 장바구니 항목 목록 |
| cartTotalPrice | Long | 장바구니 전체 금액 |

**CartItemResponse**

| 필드 | 타입 | 설명 |
|---|---|---|
| cartItemId | Long | 장바구니 항목 ID |
| productId | Long | 상품 ID |
| productName | String | 상품명 |
| productPrice | Integer | 상품 단가 |
| quantity | Integer | 수량 |
| itemTotalPrice | Long | 항목 합계 금액 |
| stock | Integer | 재고 |
| productStatus | String | 상품 상태 (`ON_SALE` / `SOLD_OUT` / `DISCONTINUED`) |
| orderable | Boolean | 주문 가능 여부 |

---

## 카테고리 (Category)

### GET /api/categories ✅

카테고리 전체 목록 조회 (계층 구조)

| | |
|---|---|
| 인증 | 🔓 공개 |
| 상태 코드 | 200 OK |

**Response Body (`data`)** — `CategoryResponse[]`

| 필드 | 타입 | 설명 |
|---|---|---|
| categoryId | Long | 카테고리 ID |
| name | String | 카테고리명 |
| children | CategoryResponse[] | 하위 카테고리 목록 (최대 1단계) |

---

### GET /api/categories/{categoryId}/products ✅

카테고리별 상품 목록 조회

| | |
|---|---|
| 인증 | 🔓 공개 |
| 상태 코드 | 200 OK |

**Path Variable**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| categoryId | Long | 카테고리 ID |

**Query Parameter** (PR #64 머지 후 추가)

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| sort | String | newest | 정렬 기준 |
| page | Integer | 0 | 페이지 번호 (0-based) |
| size | Integer | 20 | 페이지 크기 |

**Response Body (`data`)**

- 현재 (dev): `ProductResponse[]` (배열)
- PR #64 머지 후: `PageResponse<ProductResponse>` (페이지네이션 객체) ← ⚠️ Breaking Change

**ProductResponse**

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | 상품 ID |
| name | String | 상품명 |
| price | Integer | 가격 |
| stock | Integer | 재고 |
| status | String | 상태 (`ON_SALE` / `SOLD_OUT` / `DISCONTINUED`) |
| description | String | 상품 설명 |

---

## 문의 (Inquiry)

### POST /api/members/inquiry ✅

문의 등록

| | |
|---|---|
| 인증 | 🔒 인증 필요 |
| 상태 코드 | 201 Created |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| title | String | ✓ | 제목 (최대 100자) |
| content | String | ✓ | 내용 (최대 1000자) |

**Response Body (`data`)**

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | 문의 ID |
| memberId | Long | 회원 ID |
| title | String | 제목 |
| content | String | 내용 |
| status | String | 상태 (`PENDING` / `ANSWERED`) |
| createdAt | DateTime | 등록일시 |

---

### POST /api/admins/inquiry 🔄 PR#54

관리자 문의 답변

| | |
|---|---|
| 인증 | 🛡 관리자 전용 |
| 상태 코드 | 200 OK |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| inquiryId | Long | ✓ | 답변할 문의 ID |
| answer | String | ✓ | 답변 내용 (최대 1000자) |

**Response Body (`data`)**

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | 문의 ID |
| memberId | Long | 회원 ID |
| title | String | 제목 |
| content | String | 내용 |
| adminId | Long | 답변 관리자 ID |
| answer | String | 답변 내용 |
| status | String | 상태 (`ANSWERED`) |
| createdAt | DateTime | 등록일시 |
| answeredAt | DateTime | 답변일시 |

---

## 주문 (Order)

### GET /api/orders/carts/preview 🔄 PR#68

장바구니 주문서 미리보기

| | |
|---|---|
| 인증 | 🔒 인증 필요 |
| 상태 코드 | 200 OK |

**Query Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| cartIds | Long[] | - | 선택할 장바구니 항목 ID. 미입력 시 전체 장바구니 대상 |

**Response Body (`data`)**

| 필드 | 타입 | 설명 |
|---|---|---|
| orderItems | OrderPreviewItemResponse[] | 주문 항목 미리보기 목록 |
| totalOrderAmount | Long | 주문 합계 금액 |

**OrderPreviewItemResponse**

| 필드 | 타입 | 설명 |
|---|---|---|
| productName | String | 상품명 |
| productPrice | Long | 단가 |
| quantity | Integer | 수량 |
| productTotalAmount | Long | 항목 합계 금액 |

---

### POST /api/orders/direct ✅

단건 직접 주문 (상품 상세 → 바로 구매)

| | |
|---|---|
| 인증 | 🔒 인증 필요 |
| 상태 코드 | 201 Created |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| productId | Long | ✓ | 상품 ID |
| quantity | Integer | ✓ | 수량 (1 이상) |

**Response Body (`data`)**

| 필드 | 타입 | 설명 |
|---|---|---|
| orderId | Long | 주문 ID |
| orderNumber | String | 주문 번호 |
| orderStatus | String | 주문 상태 |
| totalAmount | Long | 주문 총액 |
| orderItems | OrderItemResponse[] | 주문 상품 목록 |

**OrderItemResponse**

| 필드 | 타입 | 설명 |
|---|---|---|
| productName | String | 상품명 (주문 시점 스냅샷) |
| productPrice | Long | 단가 (주문 시점 스냅샷) |
| quantity | Long | 수량 |
| totalPrice | Long | 합계 금액 |

---

### PATCH /api/orders/{orderId}/status 🔄 PR#61

주문 상태 변경

| | |
|---|---|
| 인증 | 🔒 인증 필요 |
| 상태 코드 | 200 OK |

**Path Variable**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| orderId | Long | 주문 ID |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| orderStatus | String | ✓ | 변경할 주문 상태 (`OrderStatus` enum 값) |

**Response Body (`data`)** — `null`

---

## 상품 (Product)

### GET /api/products 🔄 PR#64

상품 목록 조회

| | |
|---|---|
| 인증 | 🔓 공개 |
| 상태 코드 | 200 OK |

**Query Parameter**

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| sort | String | newest | 정렬 기준 |
| page | Integer | 0 | 페이지 번호 (0-based) |
| size | Integer | 20 | 페이지 크기 |

**Response Body (`data`)** — `PageResponse<ProductResponse>`

| 필드 | 타입 | 설명 |
|---|---|---|
| content | ProductResponse[] | 상품 목록 |
| page | Integer | 현재 페이지 번호 (0-based) |
| size | Integer | 페이지 크기 |
| totalElements | Long | 전체 상품 수 |
| totalPages | Integer | 전체 페이지 수 |
| last | Boolean | 마지막 페이지 여부 |

---

### GET /api/products/{productId} 🔄 PR#65

상품 상세 조회

| | |
|---|---|
| 인증 | 🔓 공개 |
| 상태 코드 | 200 OK |

**Path Variable**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| productId | Long | 상품 ID |

**Response Body (`data`)**

| 필드 | 타입 | 설명 |
|---|---|---|
| productId | Long | 상품 ID |
| name | String | 상품명 |
| description | String | 상품 설명 |
| price | Integer | 가격 |
| stock | Integer | 재고 |
| status | String | 상태 (`ON_SALE` / `SOLD_OUT` / `DISCONTINUED`) |
| categoryId | Long | 카테고리 ID (카테고리 없으면 null) |
| categoryName | String | 카테고리명 (카테고리 없으면 null) |

---

### GET /api/products/search 🔄 PR#59

상품 검색

| | |
|---|---|
| 인증 | 🔓 공개 |
| 상태 코드 | 200 OK |

**Query Parameter**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| keyword | String | ✓ | - | 검색어 |
| page | Integer | - | 0 | 페이지 번호 (0-based) |
| size | Integer | - | 20 | 페이지 크기 |
| sort | String | - | createdAt,desc | 정렬 기준 |

**Response Body (`data`)**

| 필드 | 타입 | 설명 |
|---|---|---|
| products | ProductSearchItemResponse[] | 검색 결과 목록 |
| page | Integer | 현재 페이지 번호 |
| size | Integer | 페이지 크기 |
| totalElements | Long | 전체 검색 결과 수 |
| totalPages | Integer | 전체 페이지 수 |
| hasNext | Boolean | 다음 페이지 존재 여부 |

**ProductSearchItemResponse**

| 필드 | 타입 | 설명 |
|---|---|---|
| productId | Long | 상품 ID |
| productName | String | 상품명 |
| productPrice | Integer | 가격 |
| stock | Integer | 재고 |
| productStatus | String | 상태 (`ON_SALE` / `SOLD_OUT` / `DISCONTINUED`) |
| orderable | Boolean | 주문 가능 여부 |

---

## ⚠️ 명세 충돌 정리

### 1. 상품 응답 필드명 불일치

같은 상품 데이터를 표현하는 필드명이 엔드포인트마다 다릅니다.

| 개념 | 목록 `GET /api/products` | 카테고리별 `GET /api/categories/{id}/products` | 상세 `GET /api/products/{id}` | 검색 `GET /api/products/search` |
|---|---|---|---|---|
| 상품 ID 키 | `id` | `id` | `productId` | `productId` |
| 상품명 키 | `name` | `name` | `name` | `productName` |
| 가격 키 | `price` | `price` | `price` | `productPrice` |
| 상태 키 | `status` | `status` | `status` | `productStatus` |
| 주문 가능 | 없음 | 없음 | 없음 | `orderable` |
| 카테고리 정보 | 없음 | 없음 | `categoryId`, `categoryName` | 없음 |

**영향:** 프런트에서 엔드포인트마다 다른 키를 매핑해야 합니다.

---

### 2. 페이지네이션 래퍼 불일치

상품 목록(PR#64)과 검색(PR#59)이 다른 페이지네이션 구조를 사용합니다.

| 항목 | 목록 `PageResponse` (PR#64) | 검색 `ProductSearchResponse` (PR#59) |
|---|---|---|
| 데이터 배열 키 | `content` | `products` |
| 마지막 페이지 키 | `last` (마지막이면 true) | `hasNext` (다음 있으면 true, 의미 반전) |

**영향:** 두 엔드포인트가 공통 페이지네이션 컴포넌트를 공유할 수 없습니다.

---

### 3. 카테고리별 상품 조회 Breaking Change

PR #64 머지 전후로 `GET /api/categories/{categoryId}/products`의 응답 타입이 바뀝니다.

| | 응답 타입 |
|---|---|
| 현재 (dev) | `List<ProductResponse>` — 배열 직접 반환 |
| PR #64 머지 후 | `PageResponse<ProductResponse>` — 페이지네이션 객체 |

**영향:** 이 엔드포인트를 사용하는 코드가 있다면 PR #64 머지 시 수정이 필요합니다.

---

### 4. 병합 충돌 예고

아래 파일들은 여러 PR이 동시에 수정하고 있어 머지 순서에 따라 코드 충돌이 발생합니다.

| 파일 | 충돌하는 PR | 원인 |
|---|---|---|
| `ProductController.java` | #59, #64, #65 | 모두 빈 클래스에서 각자 메서드 추가 |
| `OrderController.java` | #61, #68 | 같은 베이스에서 각자 메서드 추가 |
| `ErrorCode.java` | #54, #59, #60, #61, #64, #68 | 모두 새 에러 코드 추가 |

**권장:** 위 파일이 포함된 PR은 머지 전에 `dev` 최신 상태를 반드시 rebase하고, 충돌 발생 시 직접 해소해야 합니다.
