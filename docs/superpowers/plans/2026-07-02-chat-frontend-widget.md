# 고객 채팅 위젯 (AI 챗봇 + 1:1 상담) 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `frontend/index.html` + `frontend/assets/app.js` + `frontend/assets/styles.css`에 우하단 고정 채팅 위젯을 추가한다. 기본은 AI 챗봇 모드(`GET/POST /api/ai/chatbot*`), "상담원 연결" 버튼으로 1:1 실시간 상담 모드(`/api/chat/rooms/**` + STOMP `/ws/chat`)로 전환한다.

**Architecture:** 기존 프로젝트는 프레임워크 없는 단일 `app.js`(`state`/`elements`/render 함수) 구조를 쓴다. 새 코드도 같은 파일에 `state.chatWidget` 서브 상태와 전용 함수들을 추가하는 방식으로 통합한다(별도 모듈 파일 없음).

**Tech Stack:** Vanilla JS(ES2020+), raw WebSocket + 수동 STOMP 텍스트 프레임(라이브러리 없음, `src/main/resources/chat-test/chat-test.html` 레퍼런스 구현 재사용), 기존 `requestApi()` 헬퍼.

## Global Constraints

- 별도 프론트 빌드 도구 없음 — `frontend/index.html`을 브라우저로 직접 열어 검증한다(백엔드는 `http://localhost:8080`에서 별도 실행 중이어야 함).
- 모든 REST 호출은 기존 `requestApi(path, options)` 헬퍼를 사용한다(`frontend/assets/app.js:163`). `options.auth = true`면 `Authorization: Bearer <accessToken>` 헤더가 자동으로 붙는다.
- WebSocket 엔드포인트는 `API_BASE_URL`(`frontend/assets/app.js:1`, 기본값 `http://localhost:8080`)의 host를 사용해 `ws://` 또는 `wss://`로 변환한다. `window.location.host`를 쓰면 안 된다(프론트와 백엔드가 다른 오리진에서 서비스됨 — 63342 vs 8080).
- STOMP 프레임 송수신은 라이브러리를 추가하지 않고 `chat-test.html`과 동일한 원시 텍스트 프로토콜(`sendFrame`/`parseFrame`)을 그대로 이식한다.
- 로그인 여부 확인은 `getAccessToken()`(`app.js:127`), 로그인 유도는 `openLoginModal(message)`(`app.js:1363`)를 재사용한다.
- 관리자용 상담 콘솔(채팅방 목록/상태 변경 화면)은 이번 계획의 범위가 아니다 — 절대 만들지 않는다.
- 매 JS 편집 후 `node --check frontend/assets/app.js`로 문법 검증한다(이 프로젝트에 프론트 테스트 러너가 없어 이것이 유일한 자동 검증 수단).

---

## 파일 구조

- **Modify: `frontend/index.html`** — `<body>` 최하단(`</body>` 직전)에 채팅 위젯 마크업(토글 버튼 + 패널) 추가.
- **Modify: `frontend/assets/app.js`** — `state`에 `chatWidget` 서브 상태 추가, `elements`에 위젯 DOM 참조 추가, 챗봇/상담원 로직 함수들을 파일 끝부분에 새 섹션으로 추가, `bindEvents()`/`initialize()`에 위젯 초기화 연결.
- **Modify: `frontend/assets/styles.css`** — 위젯 전용 스타일(`.chat-widget`, `.chat-widget-panel`, `.chat-message` 등) 파일 끝에 추가.

---

## Task 1: 위젯 마크업 + 기본 스타일 (토글만 동작)

**Files:**
- Modify: `frontend/index.html` (body 끝)
- Modify: `frontend/assets/styles.css` (파일 끝에 추가)
- Modify: `frontend/assets/app.js` (state, elements, bindEvents, initialize)

**Interfaces:**
- Produces: `state.chatWidget.open`(boolean), `elements.chatWidgetRoot`/`chatWidgetToggle`/`chatWidgetPanel`/`chatWidgetClose`/`chatWidgetMessages`/`chatWidgetForm`/`chatWidgetInput`/`chatWidgetConnectButton`/`chatWidgetModeLabel`, 함수 `toggleChatWidget(open)`, `renderChatWidgetOpen()`

- [ ] **Step 1: `frontend/index.html`에 위젯 마크업 추가**

`</body>` 바로 위에 추가(다른 `<section class="view">`들과 형제 레벨, `<main>` 밖):

```html
  <div class="chat-widget" id="chatWidgetRoot">
    <button type="button" class="chat-widget-toggle" id="chatWidgetToggle" aria-label="채팅 상담 열기">
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M4 4h16v12H8l-4 4z"></path>
      </svg>
    </button>
    <section class="chat-widget-panel" id="chatWidgetPanel" hidden>
      <header class="chat-widget-header">
        <div>
          <strong>고객센터</strong>
          <span id="chatWidgetModeLabel">AI 챗봇</span>
        </div>
        <div class="chat-widget-header-actions">
          <button type="button" class="text-button" id="chatWidgetConnectButton">상담원 연결</button>
          <button type="button" data-modal-close-widget aria-label="닫기" id="chatWidgetClose">×</button>
        </div>
      </header>
      <div class="chat-widget-messages" id="chatWidgetMessages"></div>
      <form class="chat-widget-form" id="chatWidgetForm">
        <textarea id="chatWidgetInput" placeholder="메시지를 입력하세요." rows="1" maxlength="2000"></textarea>
        <button class="primary-button" type="submit">전송</button>
      </form>
    </section>
  </div>
</body>
```

- [ ] **Step 2: `frontend/assets/styles.css` 파일 끝에 위젯 스타일 추가**

```css
.chat-widget {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 60;
}

.chat-widget-toggle {
  display: grid;
  place-items: center;
  width: 56px;
  height: 56px;
  border: 0;
  border-radius: 50%;
  background: var(--accent);
  color: #fff;
  box-shadow: 0 12px 28px rgba(30, 41, 59, 0.22);
}

.chat-widget-toggle svg {
  width: 26px;
  height: 26px;
  stroke-width: 1.8;
}

.chat-widget-panel {
  position: absolute;
  right: 0;
  bottom: 72px;
  display: flex;
  flex-direction: column;
  width: min(360px, calc(100vw - 48px));
  height: min(520px, calc(100vh - 140px));
  border: 1px solid var(--line);
  border-radius: 16px;
  background: var(--surface);
  box-shadow: 0 20px 48px rgba(30, 41, 59, 0.18);
  overflow: hidden;
}

.chat-widget-panel[hidden] {
  display: none;
}

.chat-widget-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid var(--line);
}

.chat-widget-header strong {
  display: block;
  font-size: 14px;
}

.chat-widget-header span {
  color: var(--muted);
  font-size: 12px;
}

.chat-widget-header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat-widget-header-actions button[data-modal-close-widget] {
  border: 0;
  background: none;
  font-size: 18px;
  color: var(--muted);
}

.chat-widget-messages {
  flex: 1;
  overflow-y: auto;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chat-widget-message {
  max-width: 82%;
  padding: 9px 12px;
  border-radius: 12px;
  font-size: 13.5px;
  line-height: 1.45;
  white-space: pre-wrap;
  word-break: break-word;
}

.chat-widget-message.from-user {
  align-self: flex-end;
  background: var(--accent);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.chat-widget-message.from-other {
  align-self: flex-start;
  background: var(--surface-soft);
  color: var(--ink);
  border-bottom-left-radius: 4px;
}

.chat-widget-message.system {
  align-self: center;
  background: none;
  color: var(--muted);
  font-size: 12px;
}

.chat-widget-form {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid var(--line);
}

.chat-widget-form textarea {
  flex: 1;
  resize: none;
  max-height: 80px;
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 8px 10px;
  font: inherit;
}

.chat-widget-form textarea:disabled {
  background: var(--surface-soft);
  cursor: not-allowed;
}
```

- [ ] **Step 3: `frontend/assets/app.js`의 `state` 객체에 `chatWidget` 서브 상태 추가**

`frontend/assets/app.js:1` 근처 `state` 객체 정의 안(닫는 `};` 직전, `loadingAdminOrders: false` 다음 줄)에 콤마를 추가하고 이어서 삽입:

```js
  loadingAdminOrders: false,
  chatWidget: {
    open: false,
    mode: "bot",
    conversationId: "",
    messages: [],
    chatRoomId: null,
    roomStatus: null,
    socket: null,
    subscribed: false,
    reconnectTimer: null,
    reconnectAttempts: 0,
    manualDisconnect: false,
    lastReceivedMessageId: 0
  }
```

- [ ] **Step 4: `elements` 객체에 위젯 DOM 참조 추가**

`elements` 객체 정의 마지막 항목 뒤에 콤마를 추가하고 이어서 삽입(정확한 삽입 위치는 `elements` 객체의 닫는 `};` 직전):

```js
  chatWidgetRoot: document.getElementById("chatWidgetRoot"),
  chatWidgetToggle: document.getElementById("chatWidgetToggle"),
  chatWidgetPanel: document.getElementById("chatWidgetPanel"),
  chatWidgetClose: document.getElementById("chatWidgetClose"),
  chatWidgetMessages: document.getElementById("chatWidgetMessages"),
  chatWidgetForm: document.getElementById("chatWidgetForm"),
  chatWidgetInput: document.getElementById("chatWidgetInput"),
  chatWidgetConnectButton: document.getElementById("chatWidgetConnectButton"),
  chatWidgetModeLabel: document.getElementById("chatWidgetModeLabel")
```

- [ ] **Step 5: 토글/렌더 함수 추가**

파일 맨 끝에 새 섹션을 추가한다(기존 마지막 함수 뒤):

```js
// ===== Chat Widget =====

function toggleChatWidget(open) {
  state.chatWidget.open = open;
  elements.chatWidgetPanel.hidden = !open;
  if (open && state.chatWidget.messages.length === 0) {
    initChatBotMode();
  }
}

function renderChatWidgetMessages() {
  // message.type은 호출부에서 이미 "chat-widget-message from-user" 형태로
  // 전체 클래스 문자열을 넘긴다 — 여기서 prefix를 다시 붙이면 클래스가 중복된다.
  elements.chatWidgetMessages.innerHTML = state.chatWidget.messages
    .map((message) => `
      <div class="${message.type}">${escapeHtml(message.text)}</div>
    `)
    .join("");
  elements.chatWidgetMessages.scrollTop = elements.chatWidgetMessages.scrollHeight;
}

function addChatWidgetMessage(text, type) {
  state.chatWidget.messages.push({ text, type });
  renderChatWidgetMessages();
}
```

- [ ] **Step 6: `bindEvents()`와 `initialize()`에 위젯 이벤트 연결**

`frontend/assets/app.js`의 `bindEvents()` 함수(`app.js:224` 부근) 안, 마지막 `elements.refreshAdminOrders.addEventListener(...)` 다음 줄에 추가:

```js
  elements.chatWidgetToggle.addEventListener("click", () => toggleChatWidget(!state.chatWidget.open));
  elements.chatWidgetClose.addEventListener("click", () => toggleChatWidget(false));
```

- [ ] **Step 7: 문법 검증**

Run: `cd "/Users/han-yejin/Documents/CH5 플러스 1취월장/team1-ilchwi-woljang" && node --check frontend/assets/app.js`
Expected: 에러 없이 종료(출력 없음).

- [ ] **Step 8: 브라우저 수동 검증**

1. 백엔드가 `http://localhost:8080`에서 실행 중인지 확인.
2. `frontend/index.html`을 브라우저로 연다.
3. 우하단에 원형 채팅 버튼이 보이는지 확인.
4. 버튼 클릭 → 패널이 열리는지 확인(현재는 빈 화면이어도 정상 — Task 2에서 챗봇 연결).
5. × 버튼 클릭 → 패널이 닫히는지 확인.

- [ ] **Step 9: Commit**

```bash
git add frontend/index.html frontend/assets/app.js frontend/assets/styles.css
git commit -m "feat(chat): 채팅 위젯 토글 UI 뼈대 추가"
```

---

## Task 2: AI 챗봇 모드 연동

**Files:**
- Modify: `frontend/assets/app.js`

**Interfaces:**
- Consumes: `state.chatWidget`(Task 1), `elements.chatWidget*`(Task 1), `addChatWidgetMessage(text, type)`(Task 1), `renderChatWidgetMessages()`(Task 1), `requestApi(path, options)`(`app.js:163`), `escapeHtml`(기존 유틸)
- Produces: `initChatBotMode()`, `sendChatBotMessage(text)`, `getOrCreateConversationId()`

- [ ] **Step 1: `getOrCreateConversationId()` 추가**

"Chat Widget" 섹션에 추가:

```js
function getOrCreateConversationId() {
  let conversationId = localStorage.getItem("chatbotConversationId");
  if (!conversationId) {
    conversationId = crypto.randomUUID();
    localStorage.setItem("chatbotConversationId", conversationId);
  }
  return conversationId;
}
```

- [ ] **Step 2: `initChatBotMode()` 추가 (환영 메시지 로드)**

```js
async function initChatBotMode() {
  state.chatWidget.mode = "bot";
  state.chatWidget.conversationId = getOrCreateConversationId();
  elements.chatWidgetModeLabel.textContent = "AI 챗봇";
  elements.chatWidgetInput.disabled = false;
  try {
    const welcomeText = await requestApi("/api/ai/chatbot/welcome");
    addChatWidgetMessage(welcomeText, "chat-widget-message from-other");
  } catch (error) {
    addChatWidgetMessage(`챗봇 연결에 실패했습니다: ${error.message}`, "chat-widget-message system");
  }
}
```

> `requestApi`는 `success`가 포함된 `ApiResponse`면 `payload.data`를 반환한다. `GET /api/ai/chatbot/welcome`은 `ApiResponse<String>`을 반환하므로 `welcomeText`는 문자열 그대로다.

- [ ] **Step 3: `sendChatBotMessage(text)` 추가**

```js
async function sendChatBotMessage(text) {
  addChatWidgetMessage(text, "chat-widget-message from-user");
  try {
    const response = await requestApi("/api/ai/chatbot", {
      method: "POST",
      body: { conversationId: state.chatWidget.conversationId, message: text }
    });
    addChatWidgetMessage(response.answer, "chat-widget-message from-other");
  } catch (error) {
    addChatWidgetMessage(`전송 실패: ${error.message}`, "chat-widget-message system");
  }
}
```

- [ ] **Step 4: 폼 제출을 모드에 따라 분기하는 핸들러 추가**

```js
function handleChatWidgetSubmit(event) {
  event.preventDefault();
  const text = elements.chatWidgetInput.value.trim();
  if (!text) return;
  elements.chatWidgetInput.value = "";
  if (state.chatWidget.mode === "bot") {
    sendChatBotMessage(text);
  } else {
    sendAgentChatMessage(text);
  }
}
```

> `sendAgentChatMessage`는 Task 4에서 정의한다. Task 2~3 단계에서는 `mode`가 항상 `"bot"`이라 아직 호출되지 않는다.

- [ ] **Step 5: `bindEvents()`에 폼 제출 연결**

`bindEvents()`에 Task 1에서 추가한 두 줄 다음에 추가:

```js
  elements.chatWidgetForm.addEventListener("submit", handleChatWidgetSubmit);
```

- [ ] **Step 6: 문법 검증**

Run: `node --check frontend/assets/app.js`
Expected: 에러 없음.

- [ ] **Step 7: 브라우저 수동 검증**

1. `frontend/index.html`을 새로고침(하드 리프레시)한다.
2. 채팅 버튼 클릭 → "안녕하세요. 일취월장 고객센터 AI 챗봇입니다..." 환영 메시지가 보이는지 확인.
3. 입력창에 "노트북 추천해줘" 입력 후 전송 → 내 메시지가 우측(파란 말풍선)에, 챗봇 답변이 좌측에 표시되는지 확인.
4. 브라우저 새로고침 후 다시 패널을 열어도 이전 대화가 안 보이는 건 정상(메시지는 세션 로컬 상태일 뿐 서버 조회 API 없음) — 단, `localStorage`의 `chatbotConversationId`가 유지되므로 챗봇이 이전 맥락을 참고해 답하는지 확인(예: "방금 뭐라고 했지?").

- [ ] **Step 8: Commit**

```bash
git add frontend/assets/app.js
git commit -m "feat(chat): AI 챗봇 모드 연동"
```

---

## Task 3: "상담원 연결" — 채팅방 확보 및 이력 로드

**Files:**
- Modify: `frontend/assets/app.js`

**Interfaces:**
- Consumes: `getAccessToken()`(`app.js:127`), `openLoginModal(message)`(`app.js:1363`), `requestApi`, `state.chatWidget`, `addChatWidgetMessage`
- Produces: `connectToAgent()`, `formatChatHistoryMessage(message)`, `state.chatWidget.chatRoomId`/`roomStatus` 채움

- [ ] **Step 1: 채팅방 확보 함수 추가**

```js
async function fetchOrCreateChatRoom() {
  try {
    return await requestApi("/api/chat/rooms/my", { auth: true });
  } catch (error) {
    return requestApi("/api/chat/rooms/my", { method: "POST", auth: true });
  }
}
```

> 백엔드는 회원당 채팅방이 없으면 조회 시 예외를 던진다(`ChatRoomService.getMyChatRoom`). 조회 실패 시 생성으로 폴백한다.

- [ ] **Step 2: 메시지 이력을 위젯 표시용으로 변환하는 함수 추가**

```js
function formatChatHistoryMessage(message) {
  const isMine = message.senderRole !== "ADMIN";
  return {
    text: message.content,
    type: `chat-widget-message ${isMine ? "from-user" : "from-other"}`
  };
}
```

> `senderRole`이 `"ADMIN"`이 아니면(즉 `MEMBER`) 내가 보낸 메시지로 간주해 오른쪽에 표시한다. 고객 위젯에서는 상대방이 항상 `ADMIN`이기 때문에 이 판단으로 충분하다.

- [ ] **Step 3: `connectToAgent()` 추가 (REST로 방/이력 확보까지, STOMP 연결은 Task 4)**

```js
async function connectToAgent() {
  if (!getAccessToken()) {
    addChatWidgetMessage("상담원 연결을 위해 로그인이 필요합니다.", "chat-widget-message system");
    openLoginModal("상담원 연결을 위해 로그인이 필요합니다.");
    return;
  }

  try {
    const room = await fetchOrCreateChatRoom();
    state.chatWidget.chatRoomId = room.chatRoomId;
    state.chatWidget.roomStatus = room.status;
    state.chatWidget.mode = "agent";
    state.chatWidget.messages = [];
    elements.chatWidgetModeLabel.textContent = "상담원 연결";

    const history = await requestApi(`/api/chat/rooms/${room.chatRoomId}/messages`, { auth: true });
    history.forEach((message) => {
      state.chatWidget.lastReceivedMessageId = Math.max(state.chatWidget.lastReceivedMessageId, message.messageId);
      const formatted = formatChatHistoryMessage(message);
      state.chatWidget.messages.push(formatted);
    });
    renderChatWidgetMessages();

    updateChatWidgetInputEnabled();
    connectAgentSocket();
  } catch (error) {
    addChatWidgetMessage(`상담원 연결 실패: ${error.message}`, "chat-widget-message system");
  }
}

function updateChatWidgetInputEnabled() {
  const isCompleted = state.chatWidget.mode === "agent" && state.chatWidget.roomStatus === "COMPLETED";
  elements.chatWidgetInput.disabled = isCompleted;
  elements.chatWidgetInput.placeholder = isCompleted
    ? "상담이 종료된 채팅방입니다."
    : "메시지를 입력하세요.";
}
```

> `connectAgentSocket()`은 Task 4에서 정의한다. 이 태스크 시점에는 함수가 없어 브라우저 콘솔에 `ReferenceError`가 뜬다 — Step 6 수동 검증은 Task 4 완료 후 함께 진행해도 무방하지만, 아래처럼 임시 스텁을 넣어 이 태스크 단독으로도 검증 가능하게 한다.

- [ ] **Step 4: 임시 스텁 추가 (Task 4에서 실제 구현으로 교체됨)**

```js
function connectAgentSocket() {
  addChatWidgetMessage("(WebSocket 연결은 다음 단계에서 구현됩니다.)", "chat-widget-message system");
}
```

- [ ] **Step 5: 연결 버튼 이벤트 연결**

`bindEvents()`에 추가:

```js
  elements.chatWidgetConnectButton.addEventListener("click", () => connectToAgent());
```

- [ ] **Step 6: 문법 검증**

Run: `node --check frontend/assets/app.js`
Expected: 에러 없음.

- [ ] **Step 7: 브라우저 수동 검증**

1. 로그아웃 상태에서 채팅 위젯을 열고 "상담원 연결" 클릭 → "로그인이 필요합니다" 시스템 메시지와 함께 로그인 모달이 뜨는지 확인.
2. 로그인 후 "상담원 연결" 클릭 → 모드 라벨이 "상담원 연결"로 바뀌고, 방이 처음이면 빈 이력, 이전에 `chat-test.html`로 메시지를 보낸 적이 있으면 그 이력이 표시되는지 확인.
3. `(WebSocket 연결은 다음 단계에서 구현됩니다.)` 시스템 메시지가 보이면 정상(Task 4에서 대체됨).

- [ ] **Step 8: Commit**

```bash
git add frontend/assets/app.js
git commit -m "feat(chat): 상담원 연결 시 채팅방 확보 및 이력 로드"
```

---

## Task 4: STOMP WebSocket 연결 — CONNECT/SUBSCRIBE/실시간 수신

**Files:**
- Modify: `frontend/assets/app.js`

**Interfaces:**
- Consumes: `state.chatWidget.chatRoomId`, `getAccessToken()`, `formatChatHistoryMessage`(Task 3, MESSAGE 프레임 바디에도 동일 shape 적용), `addChatWidgetMessage`
- Produces: `connectAgentSocket()`(Task 3의 스텁을 대체), `sendStompFrame(command, headers, body)`, `parseStompFrame(rawFrame)`, `handleStompFrameData(data)`, `sendAgentChatMessage(text)`(Task 2의 `handleChatWidgetSubmit`에서 호출)

- [ ] **Step 1: Task 3의 스텁 `connectAgentSocket()`을 제거**

`function connectAgentSocket() { addChatWidgetMessage(...); }` 블록을 삭제한다.

- [ ] **Step 2: STOMP 프레임 송수신 헬퍼 추가 (`chat-test.html` 이식)**

```js
function sendStompFrame(command, headers = {}, body = "") {
  const headerLines = Object.entries(headers).map(([key, value]) => `${key}:${value}`).join("\n");
  state.chatWidget.socket.send(`${command}\n${headerLines}\n\n${body} `);
}

function parseStompFrame(rawFrame) {
  const splitIndex = rawFrame.indexOf("\n\n");
  const headerPart = splitIndex >= 0 ? rawFrame.slice(0, splitIndex) : rawFrame;
  const body = splitIndex >= 0 ? rawFrame.slice(splitIndex + 2) : "";
  const headerLines = headerPart.split("\n");
  const command = headerLines.shift();
  const headers = {};
  headerLines.forEach((line) => {
    const colonIndex = line.indexOf(":");
    if (colonIndex > 0) headers[line.slice(0, colonIndex)] = line.slice(colonIndex + 1);
  });
  return { command, headers, body };
}
```

- [ ] **Step 3: WebSocket URL 구성 함수 추가**

```js
function buildChatWebSocketUrl() {
  const apiUrl = new URL(API_BASE_URL);
  const wsProtocol = apiUrl.protocol === "https:" ? "wss:" : "ws:";
  return `${wsProtocol}//${apiUrl.host}/ws/chat`;
}
```

- [ ] **Step 4: 수신 프레임 처리 함수 추가**

```js
function handleStompFrameData(data) {
  data.split(" ").filter((frame) => frame.trim() !== "").forEach((rawFrame) => {
    const frame = parseStompFrame(rawFrame);
    if (frame.command === "CONNECTED") {
      subscribeAgentRoom();
      state.chatWidget.reconnectAttempts = 0;
      recoverMissedAgentMessages();
      return;
    }
    if (frame.command === "MESSAGE") {
      const message = JSON.parse(frame.body);
      state.chatWidget.lastReceivedMessageId = Math.max(state.chatWidget.lastReceivedMessageId, message.messageId);
      const formatted = formatChatHistoryMessage(message);
      state.chatWidget.messages.push(formatted);
      renderChatWidgetMessages();
      return;
    }
    if (frame.command === "ERROR") {
      addChatWidgetMessage(`상담 연결 오류: ${frame.body}`, "chat-widget-message system");
    }
  });
}

function subscribeAgentRoom() {
  if (state.chatWidget.subscribed) return;
  sendStompFrame("SUBSCRIBE", {
    id: "chat-widget-subscription",
    destination: `/sub/chat/rooms/${state.chatWidget.chatRoomId}`,
    ack: "auto"
  });
  state.chatWidget.subscribed = true;
}

async function recoverMissedAgentMessages() {
  if (!state.chatWidget.lastReceivedMessageId) return;
  try {
    const missed = await requestApi(
      `/api/chat/rooms/${state.chatWidget.chatRoomId}/messages?afterMessageId=${state.chatWidget.lastReceivedMessageId}`,
      { auth: true }
    );
    missed.forEach((message) => {
      state.chatWidget.lastReceivedMessageId = Math.max(state.chatWidget.lastReceivedMessageId, message.messageId);
      state.chatWidget.messages.push(formatChatHistoryMessage(message));
    });
    if (missed.length > 0) renderChatWidgetMessages();
  } catch (error) {
    addChatWidgetMessage(`미수신 메시지 복구 실패: ${error.message}`, "chat-widget-message system");
  }
}
```

- [ ] **Step 5: `connectAgentSocket()` 실제 구현**

```js
function connectAgentSocket() {
  const accessToken = getAccessToken();
  if (!accessToken || !state.chatWidget.chatRoomId) return;
  if (state.chatWidget.socket && state.chatWidget.socket.readyState === WebSocket.OPEN) return;

  state.chatWidget.manualDisconnect = false;
  state.chatWidget.subscribed = false;
  const socket = new WebSocket(buildChatWebSocketUrl());
  state.chatWidget.socket = socket;

  socket.onopen = () => {
    sendStompFrame("CONNECT", {
      "accept-version": "1.2",
      "heart-beat": "10000,10000",
      Authorization: `Bearer ${accessToken}`
    });
  };
  socket.onmessage = (event) => handleStompFrameData(event.data);
  socket.onerror = () => {
    addChatWidgetMessage("상담 연결에 오류가 발생했습니다.", "chat-widget-message system");
  };
  socket.onclose = () => {
    state.chatWidget.subscribed = false;
    if (!state.chatWidget.manualDisconnect) scheduleAgentReconnect();
  };
}

function disconnectAgentSocket() {
  state.chatWidget.manualDisconnect = true;
  clearTimeout(state.chatWidget.reconnectTimer);
  if (state.chatWidget.socket && state.chatWidget.socket.readyState === WebSocket.OPEN) {
    sendStompFrame("DISCONNECT", {});
    state.chatWidget.socket.close();
  }
  state.chatWidget.socket = null;
}

function scheduleAgentReconnect() {
  state.chatWidget.reconnectAttempts += 1;
  const delay = Math.min(1000 * state.chatWidget.reconnectAttempts, 10000);
  addChatWidgetMessage(`연결이 끊겼습니다. ${delay / 1000}초 후 재연결합니다.`, "chat-widget-message system");
  clearTimeout(state.chatWidget.reconnectTimer);
  state.chatWidget.reconnectTimer = setTimeout(() => connectAgentSocket(), delay);
}
```

- [ ] **Step 6: 메시지 전송 함수 추가 (`handleChatWidgetSubmit`이 호출)**

```js
function sendAgentChatMessage(text) {
  if (state.chatWidget.roomStatus === "COMPLETED") {
    addChatWidgetMessage("상담이 종료된 채팅방에는 메시지를 보낼 수 없습니다.", "chat-widget-message system");
    return;
  }
  if (!state.chatWidget.socket || state.chatWidget.socket.readyState !== WebSocket.OPEN) {
    addChatWidgetMessage("상담원과 연결되어 있지 않습니다.", "chat-widget-message system");
    return;
  }
  sendStompFrame(
    "SEND",
    { destination: `/pub/chat/rooms/${state.chatWidget.chatRoomId}/messages`, "content-type": "application/json" },
    JSON.stringify({ content: text })
  );
}
```

> 전송한 메시지는 서버가 저장 후 구독 브로드캐스트로 다시 돌려주므로(Task 4 Step 4의 `MESSAGE` 처리), 여기서 화면에 직접 추가하지 않는다 — 중복 렌더 방지.

- [ ] **Step 7: 패널을 닫을 때 소켓을 끊지 않도록 확인 (연결 유지 정책)**

`toggleChatWidget(open)`(Task 1)은 그대로 둔다 — 패널을 닫아도 실시간 수신을 유지해야 하므로 `disconnectAgentSocket()`을 호출하지 않는다. 이 스텝은 코드 변경 없음, 의도 확인용 체크리스트 항목이다.

- [ ] **Step 8: 문법 검증**

Run: `node --check frontend/assets/app.js`
Expected: 에러 없음.

- [ ] **Step 9: 브라우저 수동 검증 (양방향 확인)**

1. 브라우저 탭 A: `frontend/index.html`에서 로그인 후 "상담원 연결" → 콘솔에 에러 없이 연결되는지 확인(개발자 도구 Network 탭에서 `ws://localhost:8080/ws/chat` 101 Switching Protocols 확인).
2. `http://localhost:8080/chat-test.html`을 탭 B에서 열고 관리자 계정으로 로그인, 같은 `chatRoomId`를 입력해 STOMP 연결 후 메시지 전송.
3. 탭 A 위젯에 방금 보낸 관리자 메시지가 실시간으로(좌측 말풍선) 표시되는지 확인.
4. 탭 A에서 메시지를 보내고 탭 B(`chat-test.html`)에 표시되는지 확인.
5. 탭 A를 새로고침 후 다시 "상담원 연결" → 이전 대화 전체가 이력으로 다시 보이는지 확인.

- [ ] **Step 10: Commit**

```bash
git add frontend/assets/app.js
git commit -m "feat(chat): STOMP WebSocket 연결 및 실시간 메시지 송수신"
```

---

## Task 5: 재연결 견고성 + COMPLETED 상태 처리 마무리

**Files:**
- Modify: `frontend/assets/app.js`

**Interfaces:**
- Consumes: Task 4의 모든 함수, `state.chatWidget.roomStatus`
- Produces: `handleChatWidgetSubmit`(Task 2) 내 `COMPLETED` 방지 확인, `updateChatWidgetInputEnabled()`(Task 3) 재사용 확인

- [ ] **Step 1: `sendAgentChatMessage` 호출 전 입력창 비활성화가 실제로 전송을 막는지 `handleChatWidgetSubmit` 재확인**

Task 2의 `handleChatWidgetSubmit`은 `elements.chatWidgetInput.value`가 비었으면 이미 return한다. `disabled` 텍스트영역은 사용자가 타이핑할 수 없어 자연히 빈 값이 되므로 추가 코드 변경은 없다. 다만 `connectToAgent()`가 방 조회 직후 `updateChatWidgetInputEnabled()`를 호출하는지 Task 3 Step 3 코드를 재확인한다 — 이미 호출하고 있다.

- [ ] **Step 2: `chat-test.html`로 방 상태를 `COMPLETED`로 바꾼 뒤 위젯이 이를 반영하도록, `MESSAGE` 프레임 처리에 상태 갱신 로직은 없다는 점 확인**

현재 설계상 방 상태 변경은 실시간으로 push되지 않는다(백엔드가 상태 변경을 STOMP로 broadcast하지 않음, `ChatRoomController.changeChatRoomStatus`는 REST 응답만 반환). 따라서 위젯은 **패널을 다시 열 때(`connectToAgent` 재호출 시)만** 최신 상태를 반영한다. 이는 스펙에 명시된 범위 내 동작이므로 추가 구현 없이 아래 문서화 스텝만 진행한다.

`connectToAgent()` 함수(Task 3) 바로 위에 주석 추가:

```js
/*
 * 채팅방 상태(WAITING/IN_PROGRESS/COMPLETED) 변경은 실시간으로 push되지 않는다.
 * 관리자가 상태를 바꾼 뒤에는 고객이 "상담원 연결"을 다시 눌러(즉 connectToAgent 재호출)
 * 최신 상태를 반영해야 한다. 실시간 상태 push는 이번 범위 밖이다.
 */
async function connectToAgent() {
```

- [ ] **Step 3: 문법 검증**

Run: `node --check frontend/assets/app.js`
Expected: 에러 없음.

- [ ] **Step 4: 브라우저 수동 검증**

1. 탭 A(고객 위젯)에서 상담원 연결 상태 유지.
2. 탭 B(`chat-test.html`)에서 관리자로 로그인, 해당 채팅방 상태를 `WAITING → IN_PROGRESS → COMPLETED`로 변경.
3. 탭 A에서 메시지 전송 시도 → 이 시점엔 아직 `COMPLETED`를 모르므로 서버가 거부할 수 있다(정상, 백엔드가 최종 방어선). 에러 메시지가 사용자에게 시스템 메시지로 표시되는지 확인.
4. 탭 A 위젯을 닫았다가 다시 열고 "상담원 연결" 재클릭 → `roomStatus`가 `COMPLETED`로 갱신되고 입력창이 비활성화되는지 확인.
5. 네트워크 탭에서 WebSocket을 강제로 끊는다(개발자 도구 → Network → WS 연결 우클릭 불가하면 백엔드 서버를 잠시 재시작) → `scheduleAgentReconnect`가 동작해 "재연결합니다" 시스템 메시지가 뜨는지 확인.
6. 탭 A가 끊겨 있는 동안 탭 B(`chat-test.html`)에서 같은 방으로 메시지를 1~2개 보낸다.
7. 탭 A가 자동 재연결되면 `CONNECTED` 프레임 처리에서 `recoverMissedAgentMessages()`가 호출되어, 끊긴 동안 탭 B가 보낸 메시지가 `afterMessageId` 조회로 화면에 나타나는지 확인 — 이것이 스펙의 "미수신 메시지 복구" 검증 항목이다.

- [ ] **Step 5: Commit**

```bash
git add frontend/assets/app.js
git commit -m "docs(chat): 상담 상태 반영 시점 문서화"
```

---

## 최종 점검 (모든 태스크 완료 후)

- [ ] `node --check frontend/assets/app.js` 통과
- [ ] `docs/superpowers/specs/2026-07-02-chat-frontend-widget-design.md`의 "테스트 / 검증 방법" 체크리스트 6항목 전부 실제로 브라우저에서 확인
- [ ] `git log --oneline`으로 이번 계획의 커밋들이 의도한 단위로 쪼개져 있는지 확인
