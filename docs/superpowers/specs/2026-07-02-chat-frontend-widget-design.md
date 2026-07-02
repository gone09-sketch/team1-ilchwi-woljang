# 고객 채팅 위젯 (AI 챗봇 + 1:1 상담) 프론트엔드 설계

## 배경

백엔드에 두 가지 채팅 기능이 이미 구현되어 있다(둘 다 `origin/dev`).

- **AI 챗봇** (`domain/chatbot`): `GET /api/ai/chatbot/welcome`, `POST /api/ai/chatbot`. 인증 불필요, RAG 기반 FAQ/상품 검색 챗봇.
- **1:1 실시간 상담** (`domain/chat`): STOMP WebSocket(`/ws/chat`) + REST(`/api/chat/rooms/**`). 로그인(JWT) 필요.

프론트(`frontend/assets/app.js`, `frontend/index.html`)에는 아직 둘 다 연결되어 있지 않다. 이 스펙은 두 기능을 하나의 고객용 위젯으로 프론트에 붙이는 범위를 다룬다. 관리자용 상담 콘솔(채팅방 목록/상태 변경 UI)은 이번 범위에서 제외한다.

## 목표

- 사이트 우하단에 떠 있는 채팅 버튼 하나로 진입.
- 기본은 AI 챗봇 모드. 패널 내 "상담원 연결" 버튼으로 1:1 실시간 상담 모드로 전환.
- 기존 `app.js`의 `state`/`elements` 패턴, `API_BASE_URL`, 로그인/JWT 처리 방식을 그대로 재사용.

## 비범위 (Out of scope)

- 관리자용 채팅방 목록/상태 변경 UI (`adminInquiryView`류 백오피스 화면)
- 채팅 알림(뱃지, 푸시 등)
- 파일/이미지 전송

## 아키텍처

기존 프로젝트는 프레임워크 없는 단일 `app.js` 구조를 쓴다. 새 기능도 같은 파일 안에 상태와 렌더 함수를 추가하는 방식을 따른다(별도 모듈 분리 없음 — 기존 컨벤션 유지).

```
state.chatWidget = {
  open: false,
  mode: "bot" | "agent",       // 패널 내부 모드
  conversationId: string,      // 챗봇용, localStorage에 유지
  messages: [],                // 현재 모드의 표시용 메시지 목록
  chatRoomId: number | null,   // 상담원 모드에서만 사용
  roomStatus: "WAITING" | "IN_PROGRESS" | "COMPLETED" | null,
  socket: WebSocket | null,
  reconnectAttempts: number,
}
```

## 컴포넌트

### 1. 플로팅 버튼 & 패널 (`index.html`)

- `<div class="chat-widget">` 를 `<body>` 최하단에 추가, `position: fixed; right; bottom;`로 항상 노출(로그인 여부 무관, 뷰 전환과 무관).
- 토글 버튼 클릭 시 패널 open/close. 패널은 헤더(모드 전환 탭 또는 "상담원 연결" 버튼) + 메시지 리스트 + 입력창 구조로, 기존 `cart-item`류 카드 스타일을 재사용.

### 2. 챗봇 모드

- 패널을 처음 열면(세션당 1회) `GET /api/ai/chatbot/welcome` 호출 → 응답 텍스트를 첫 메시지로 표시.
- `conversationId`는 `crypto.randomUUID()`로 생성해 `localStorage`에 저장(백엔드 `chatbot-test.html` 레퍼런스와 동일 패턴), 새로고침해도 대화 맥락 유지.
- 메시지 전송: `POST /api/ai/chatbot { conversationId, message }` → `answer`를 봇 메시지로 추가.
- 인증 불필요.

### 3. "상담원 연결" 전환

- 버튼 클릭 시:
  1. 로그인 안 되어 있으면 기존 로그인 모달을 열고 안내 메시지 표시, 전환 중단.
  2. 로그인 되어 있으면 `GET /api/chat/rooms/my` 조회 → 404/없음이면 `POST /api/chat/rooms/my`로 생성.
  3. `chatRoomId`, `roomStatus`를 `state.chatWidget`에 저장하고 `mode`를 `"agent"`로 전환, 메시지 리스트를 상담 메시지로 교체.
  4. `GET /api/chat/rooms/{chatRoomId}/messages`로 기존 대화 이력을 불러와 표시.
  5. WebSocket 연결: `${wsProtocol}//${API_BASE_URL host}/ws/chat` → STOMP CONNECT(Authorization: Bearer accessToken) → CONNECTED 수신 시 `/sub/chat/rooms/{chatRoomId}` SUBSCRIBE.
- STOMP 프레임 송수신(SEND/MESSAGE/ERROR 파싱, 원시 텍스트 프로토콜)은 `src/main/resources/chat-test/chat-test.html`의 구현을 그대로 이식한다(라이브러리 없이 동일 프로토콜).
- `roomStatus === "COMPLETED"`면 입력창 비활성화 + 안내 문구.

### 4. 재연결 / 복구

- WebSocket `onclose` 시 지수 백오프(최대 10초) 재연결, 재연결 전 `refreshAccessToken`으로 토큰 갱신 시도(`chat-test.html`과 동일 패턴).
- 재연결/패널 재오픈 시 마지막으로 받은 `messageId`를 기준으로 `afterMessageId` 쿼리로 미수신 메시지만 복구.

## 데이터 흐름 요약

- **챗봇 모드**: 매 전송 = REST 요청 1회 → 응답 1회. 상태 없음(서버가 `conversationId`로 컨텍스트 유지).
- **상담원 모드**: 최초 REST로 방 정보 확보 → WebSocket 전환 → 이후 실시간은 STOMP, 새로고침/재연결 시에만 REST로 미수신 메시지 보충.

## 에러 처리

- REST 실패: 기존 `requestApi` 에러 처리/문구 재사용.
- WebSocket 연결 실패/끊김: 재연결 로직 + 사용자에게 "연결이 끊겼습니다. 재연결 중..." 시스템 메시지 표시.
- 미로그인 상태로 상담원 연결 시도: 안내 메시지 후 로그인 모달 유도, 패널은 챗봇 모드 유지.
- `COMPLETED` 방에 메시지 전송 시도: 입력 자체를 막아 서버 왕복 없이 차단(백엔드도 동일 정책이라 이중 방어).

## 테스트 / 검증 방법

프론트 전용 변경이라 자동화 테스트보다 브라우저 수동 검증으로 진행한다.

- [ ] 비로그인 상태에서 챗봇 대화(환영 메시지 → 질문 → 답변) 확인
- [ ] 새로고침 후에도 챗봇 대화 맥락(conversationId) 유지 확인
- [ ] 로그인 후 "상담원 연결" → 방 생성/조회 → WebSocket 연결 → 메시지 송수신 확인
- [ ] 새로고침 후 미수신 메시지 복구 확인
- [ ] 관리자가 방 상태를 `COMPLETED`로 바꾼 뒤 고객 화면에서 입력 차단 확인 (chat-test.html로 상태 변경)
- [ ] 미로그인 상태에서 "상담원 연결" 클릭 시 로그인 유도 확인
