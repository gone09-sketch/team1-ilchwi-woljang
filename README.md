# 일취월장

팀 프로젝트 `일취월장`은 상품 조회, 장바구니, 주문, 문의, 실시간 상담, AI 챗봇을 제공하는 Spring Boot 기반 이커머스 API 서버입니다.

현재 `dev` 브랜치는 기본 커머스 기능 위에 실시간 통신, 인증/인가, 주문/상품 조회 성능 개선, 동시성 제어, 인기 데이터 캐싱을 고도화한 상태입니다.

## 팀 구성 및 고도화 구현

| 이름 | 담당 영역 | 구현 내용 |
| --- | --- | --- |
| <nobr>양지원</nobr> | 실시간 통신 및 인증/인가 | STOMP 기반 WebSocket 채팅, JWT 기반 STOMP `CONNECT` 인증, 채팅방 `SUBSCRIBE` 권한 검증, HTTP API JWT 인증 |
| <nobr>한예진</nobr> | 관리자 주문 내역 인덱싱 | Querydsl 기반 관리자 주문 동적 검색, 주문번호 정확 일치 검색, 상품명 Full-Text 검색, 주문 조회 성능 검증 문서화 |
| <nobr>박송이</nobr> | 동시성 문제 | 상품 재고 차감 시 비관적 락을 적용하여 동시 주문 상황의 재고 정합성 보장 |
| <nobr>전용운</nobr> | 인기검색어 및 인기상품 캐싱 | Redis Cache 기반 인기검색어/인기상품 캐싱, 스케줄러 기반 캐시 웜업, 캐시 장애 시 DB 조회 fallback |
| <nobr>민병준</nobr> | 상품 목록 인덱싱 | 상품 목록 조회용 복합 인덱스, 상품명 Full-Text 인덱스 자동 생성, 가격/카테고리/상태 조건 조회 최적화 |

## 주요 기능

- 회원가입, 로그인, JWT Access Token/Refresh Token 재발급
- 상품 목록, 상품 상세, 카테고리별 상품, 가격 범위, 상품명 검색, 인기 상품 조회
- 장바구니 상품 추가, 조회, 수량 변경, 삭제
- 바로 주문, 장바구니 주문, 주문 미리보기, 주문 취소, 주문 내역 조회
- 관리자 주문 목록 검색 및 주문 상세 조회
- 회원 문의 등록, 관리자 답변 등록
- STOMP WebSocket 기반 실시간 채팅
- Spring AI OpenAI 기반 상품 추천/FAQ 챗봇
- 인기검색어 및 인기상품 Redis 캐싱

## 기술 스택

- Java 17
- Spring Boot 4.1.0
- Spring MVC, Spring Data JPA, Spring Security
- Spring WebSocket, STOMP
- Spring Cache, Redis
- Spring AI OpenAI
- Querydsl 5.0.0
- MySQL, H2(test)
- JWT: JJWT 0.13.0
- Gradle
- Lombok

## 프로젝트 구조

```text
src/main/java/com/team1ilchwiwoljang
├── common
│   ├── config        # Security, WebSocket, Cache, Querydsl, JPA 설정
│   ├── entity        # 공통 BaseEntity
│   ├── exception     # 공통 예외 및 에러 응답
│   ├── response      # 공통 API 응답
│   └── security      # JWT 인증 필터, 토큰 provider, @Auth
└── domain
    ├── auth          # 회원가입, 로그인, 토큰 재발급
    ├── cart          # 장바구니
    ├── category      # 카테고리
    ├── chat          # 실시간 채팅
    ├── chatbot       # AI 챗봇
    ├── inquiry       # 문의
    ├── member        # 회원
    ├── order         # 주문 및 관리자 주문 검색
    ├── product       # 상품 조회, 검색, 재고
    └── search        # 검색어 및 인기검색어
```

## 고도화 포인트

### 실시간 채팅 및 인증/인가

- WebSocket/STOMP endpoint: `/ws/chat`
- 메시지 발행 destination: `/pub/chat/rooms/{chatRoomId}/messages`
- 메시지 구독 destination: `/sub/chat/rooms/{chatRoomId}`
- STOMP `CONNECT` frame의 `Authorization: Bearer {accessToken}` header를 검증합니다.
- `MEMBER`는 본인 채팅방만 구독할 수 있고, `ADMIN`은 모든 회원 채팅방을 구독할 수 있습니다.
- STOMP 처리 중 발생한 인증/인가 오류는 전용 error handler를 통해 STOMP `ERROR` frame으로 응답합니다.

### 관리자 주문 내역 검색 최적화

- 관리자 주문 API는 `GET /api/admins/orders`에서 상태, 기간, 금액, 주문번호, 회원 ID, 상품명 조건을 조합해 검색합니다.
- 주문번호는 인덱스를 활용하기 위해 부분 검색이 아닌 정확 일치 검색으로 처리합니다.
- 상품명 검색은 MySQL `MATCH ... AGAINST` Full-Text 검색을 사용하고, 테스트용 H2 환경에서는 `LIKE` 검색으로 fallback합니다.
- 사용자 주문 내역 조회는 `(member_id, created_at DESC, id DESC)` 복합 인덱스를 기준으로 최신순 페이징을 최적화합니다.

### 재고 동시성 제어

- 상품 재고 조회 시 `PESSIMISTIC_WRITE` 락을 사용합니다.
- 주문 생성 과정에서 동일 상품에 대한 동시 재고 차감 요청이 들어와도 재고가 음수가 되지 않도록 보호합니다.
- JPA 2차 캐시 영향을 피하기 위해 락 조회 쿼리에 cache bypass hint를 적용합니다.

### 인기 데이터 캐싱

- 인기 상품 cache name: `popularProducts`
- 인기 검색어 cache name: `popularKeywords`
- Redis CacheManager를 사용하며 기본 TTL은 30분입니다.
- 인기 상품/검색어는 9분 30초 주기의 스케줄러로 캐시를 미리 갱신합니다.
- Redis 장애 시 캐시 예외를 무시하고 DB 조회로 fallback합니다.

### 상품 목록 인덱싱

- `products` 테이블 복합 인덱스
  - `idx_product_status_created_at`: 판매 상태 + 최신순 조회
  - `idx_product_category_status_created_at`: 카테고리 + 판매 상태 + 최신순 조회
  - `idx_product_status_price`: 판매 상태 + 가격 범위 조회
- 상품명 검색용 Full-Text 인덱스
  - `idx_product_name_fulltext`
  - 애플리케이션 시작 시 MySQL `information_schema.statistics`를 확인하고 없으면 자동 생성합니다.

## 주요 API

| 영역 | Method | Endpoint | 설명 |
| --- | --- | --- | --- |
| Auth | `POST` | `/api/auth/signup` | 회원가입 |
| Auth | `POST` | `/api/auth/login` | 로그인 |
| Auth | `POST` | `/api/auth/refresh` | Access Token 재발급 |
| Product | `GET` | `/api/products` | 상품 목록 조회 |
| Product | `GET` | `/api/products/{productId}` | 상품 상세 조회 |
| Product | `GET` | `/api/products/search` | 상품명 검색 |
| Product | `GET` | `/api/products/search-fulltext` | 상품명 Full-Text 검색 |
| Product | `GET` | `/api/products/popular` | 인기 상품 조회 |
| Search | `GET` | `/api/search/popular` | 인기검색어 조회 |
| Cart | `POST` | `/api/carts/items` | 장바구니 상품 추가 |
| Cart | `GET` | `/api/carts` | 장바구니 조회 |
| Cart | `PATCH` | `/api/carts/items/{cartItemId}` | 장바구니 수량 변경 |
| Cart | `DELETE` | `/api/carts/items/{cartItemId}` | 장바구니 상품 삭제 |
| Order | `GET` | `/api/orders/preview` | 장바구니 주문 미리보기 |
| Order | `POST` | `/api/orders/direct/preview` | 바로 주문 미리보기 |
| Order | `POST` | `/api/orders/direct` | 바로 주문 |
| Order | `POST` | `/api/orders/carts` | 장바구니 주문 |
| Order | `POST` | `/api/orders/{orderId}/cancel` | 주문 취소 |
| Order | `GET` | `/api/orders` | 내 주문 내역 조회 |
| Admin Order | `GET` | `/api/admins/orders` | 관리자 주문 검색 |
| Admin Order | `GET` | `/api/admins/orders/{orderId}` | 관리자 주문 상세 조회 |
| Chat | `POST` | `/api/chat/rooms/my` | 내 채팅방 생성/조회 |
| Chat | `GET` | `/api/chat/rooms/my` | 내 채팅방 조회 |
| Chat | `GET` | `/api/chat/rooms` | 관리자 채팅방 목록 조회 |
| Chat | `GET` | `/api/chat/rooms/{chatRoomId}/messages` | 채팅 메시지 조회 |
| Chat | `PATCH` | `/api/chat/rooms/{chatRoomId}/status` | 채팅방 상태 변경 |
| Chatbot | `GET` | `/api/ai/chatbot/welcome` | 챗봇 웰컴 메시지 |
| Chatbot | `POST` | `/api/ai/chatbot` | 챗봇 질의 |
| Inquiry | `POST` | `/api/members/inquiry` | 회원 문의 등록 |
| Inquiry | `POST` | `/api/admins/inquiry` | 관리자 문의 답변 |

## 실행 방법

### 1. 환경 변수 준비

`.env.example`을 참고해 프로젝트 루트에 `.env` 파일을 생성합니다.

```properties
DB_URL=jdbc:mysql://localhost:3306/team1_db?serverTimezone=UTC
DB_USERNAME=
DB_PASSWORD=

JPA_DDL_AUTO=update
JPA_SHOW_SQL=true
JPA_FORMAT_SQL=true

JWT_SECRET=change-this-secret-to-at-least-32-characters
JWT_ACCESS_TOKEN_EXPIRATION=1h
JWT_REFRESH_TOKEN_EXPIRATION=14d

COOKIE_SECURE=false

OPENAI_API_KEY=your-openai-api-key
REDIS_HOST=localhost
REDIS_PORT=6379
CHAT_WEBSOCKET_ALLOWED_ORIGIN_PATTERNS=http://localhost:8080
```

### 2. MySQL 및 Redis 실행

로컬 환경에서 MySQL과 Redis가 실행 중이어야 합니다.

- MySQL: `DB_URL`에 지정한 데이터베이스 생성 필요
- Redis: 기본값 `localhost:6379`

### 3. 애플리케이션 실행

```bash
./gradlew bootRun
```

Windows PowerShell에서는 다음 명령을 사용할 수 있습니다.

```powershell
.\gradlew.bat bootRun
```

### 4. 테스트 실행

```bash
./gradlew test
```

Windows PowerShell에서는 다음 명령을 사용할 수 있습니다.

```powershell
.\gradlew.bat test
```

## 문서

- [API 문서](docs/API.md)
- [데이터베이스 인덱싱 설계 및 검증 가이드](docs/indexing_guide.md)
- [주문 내역 동적 필터 검색 성능 및 부하 테스트 가이드](docs/order_search_perf_test.md)
- [코드 컨벤션](docs/CODE-CONVENTION.md)
- [Git 컨벤션](docs/GIT-CONVENTION.md)

## 개발 규칙

- 공통 응답은 `ApiResponse` 형식을 사용합니다.
- 인증된 HTTP API에서는 `@Auth AuthMember`로 현재 사용자를 주입합니다.
- 테스트 환경에서는 Redis 대신 `ConcurrentMapCacheManager`를 사용합니다.
- 인덱스 추가는 실제 조회 조건과 정렬 조건이 확인된 경우에만 적용합니다.
- 변경 후 `git status --short`와 `./gradlew test`로 변경 범위와 테스트 결과를 확인합니다.
