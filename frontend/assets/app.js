const API_BASE_URL = localStorage.getItem("commerceApiBaseUrl") || "http://localhost:8080";
const PAGE_SIZE = 20;
const ORDER_PAGE_SIZE = 10;
const ADMIN_ORDER_PAGE_SIZE = 10;
const ADMIN_INQUIRY_PAGE_SIZE = 10;
const POPULAR_KEYWORD_LIMIT = 10;
const DEFAULT_POPULAR_KEYWORDS = [
  "선크림",
  "노트북",
  "방울토마토",
  "바디로션",
  "우유",
  "쿠션",
  "틴트",
  "에센스",
  "크림",
  "세제"
];

const state = {
  view: "list",
  listMode: "products",
  categories: [],
  categoryLoaded: false,
  categoryMenuOpen: false,
  selectedRootId: null,
  selectedCategoryId: null,
  query: "",
  sort: "newest",
  page: 0,
  productPage: emptyPage(),
  products: [],
  productError: "",
  popularKeywords: [],
  popularKeywordError: "",
  popularKeywordLoaded: false,
  popularKeywordUpdatedAt: "",
  popularKeywordPanelTarget: "",
  categoryError: "",
  selectedProduct: null,
  detailQuantity: 1,
  cart: null,
  cartError: "",
  selectedCartIds: new Set(),
  cartSelectionReady: false,
  orders: emptyPage(ORDER_PAGE_SIZE),
  orderFilters: {
    startDate: "",
    endDate: "",
    orderStatus: "",
    keyword: ""
  },
  adminOrders: emptyPage(ADMIN_ORDER_PAGE_SIZE),
  adminOrderFilters: {
    startDate: "",
    endDate: "",
    orderStatus: "",
    productName: "",
    orderNumber: "",
    memberId: "",
    minTotalAmount: "",
    maxTotalAmount: ""
  },
  orderError: "",
  adminOrderError: "",
  adminInquiries: emptyPage(ADMIN_INQUIRY_PAGE_SIZE),
  adminInquiryError: "",
  selectedAdminInquiryId: null,
  loadingProducts: false,
  loadingPopularKeywords: false,
  loadingOrders: false,
  loadingAdminOrders: false,
  loadingAdminInquiries: false,
  chatWidget: {
    open: false,
    mode: "bot",
    messages: [],
    socket: null,
    chatRoomId: null,
    conversationId: null,
    lastReceivedMessageId: 0
  },
  adminChat: {
    rooms: [],
    activeRoomId: null,
    messages: [],
    socket: null,
    subscribed: false,
    lastReceivedMessageId: 0
  }
};

const elements = {
  categoryPanel: document.querySelector(".category-panel"),
  categoryToggle: document.getElementById("categoryToggle"),
  categoryTabs: document.getElementById("categoryTabs"),
  categoryChildren: document.getElementById("categoryChildren"),
  listTabs: document.getElementById("listTabs"),
  subTabs: document.getElementById("subTabs"),
  listView: document.getElementById("listView"),
  detailView: document.getElementById("detailView"),
  cartView: document.getElementById("cartView"),
  ordersView: document.getElementById("ordersView"),
  adminOrdersView: document.getElementById("adminOrdersView"),
  customerCenterView: document.getElementById("customerCenterView"),
  adminInquiryView: document.getElementById("adminInquiryView"),
  infoView: document.getElementById("infoView"),
  infoTitle: document.getElementById("infoTitle"),
  infoMessage: document.getElementById("infoMessage"),
  activeCategoryLabel: document.getElementById("activeCategoryLabel"),
  listTitle: document.getElementById("listTitle"),
  productCount: document.getElementById("productCount"),
  productGrid: document.getElementById("productGrid"),
  productPager: document.getElementById("productPager"),
  globalSearchForm: document.getElementById("globalSearchForm"),
  globalSearchInput: document.getElementById("globalSearchInput"),
  globalPopularKeywords: document.getElementById("globalPopularKeywords"),
  inlineSearchForm: document.getElementById("inlineSearchForm"),
  inlineSearchInput: document.getElementById("inlineSearchInput"),
  inlinePopularKeywords: document.getElementById("inlinePopularKeywords"),
  detailVisual: document.getElementById("detailVisual"),
  detailName: document.getElementById("detailName"),
  detailDescription: document.getElementById("detailDescription"),
  detailPrice: document.getElementById("detailPrice"),
  detailStock: document.getElementById("detailStock"),
  detailQuantity: document.getElementById("detailQuantity"),
  detailTotal: document.getElementById("detailTotal"),
  addDetailCart: document.getElementById("addDetailCart"),
  buyDetail: document.getElementById("buyDetail"),
  cartBadge: document.getElementById("cartBadge"),
  cartList: document.getElementById("cartList"),
  cartSubtotal: document.getElementById("cartSubtotal"),
  shippingFee: document.getElementById("shippingFee"),
  cartTotal: document.getElementById("cartTotal"),
  refreshCart: document.getElementById("refreshCart"),
  buyCart: document.getElementById("buyCart"),
  orderSearchForm: document.getElementById("orderSearchForm"),
  orderCount: document.getElementById("orderCount"),
  orderList: document.getElementById("orderList"),
  refreshOrders: document.getElementById("refreshOrders"),
  adminOrderSearchForm: document.getElementById("adminOrderSearchForm"),
  adminOrderCount: document.getElementById("adminOrderCount"),
  adminOrderList: document.getElementById("adminOrderList"),
  refreshAdminOrders: document.getElementById("refreshAdminOrders"),
  adminInquiryCount: document.getElementById("adminInquiryCount"),
  adminInquiryList: document.getElementById("adminInquiryList"),
  refreshAdminInquiries: document.getElementById("refreshAdminInquiries"),
  adminInquiryId: document.getElementById("adminInquiryId"),
  adminInquirySelection: document.getElementById("adminInquirySelection"),
  submitAdminInquiryAnswer: document.getElementById("submitAdminInquiryAnswer"),
  inquiryStatus: document.getElementById("inquiryStatus"),
  adminInquiryStatus: document.getElementById("adminInquiryStatus"),
  loginButton: document.getElementById("loginButton"),
  signupButton: document.getElementById("signupButton"),
  adminMenuButton: document.querySelector(".admin-menu-button"),
  chatWidgetToggle: document.getElementById("chatWidgetToggle"),
  chatWidgetClose: document.getElementById("chatWidgetClose"),
  chatWidgetForm: document.getElementById("chatWidgetForm"),
  chatWidgetConnectButton: document.getElementById("chatWidgetConnectButton"),
  chatWidgetPanel: document.getElementById("chatWidgetPanel"),
  chatWidgetMessages: document.getElementById("chatWidgetMessages"),
  chatWidgetModeLabel: document.getElementById("chatWidgetModeLabel"),
  chatWidgetInput: document.getElementById("chatWidgetInput"),
  adminMenuDivider: document.querySelectorAll(".admin-only"),
  adminMenuButton: document.querySelectorAll(".admin-only"),
  modalRoot: document.getElementById("modalRoot"),
  toast: document.getElementById("toast"),
  adminChatView: document.getElementById("adminChatView"),
  adminChatRoomList: document.getElementById("adminChatRoomList"),
  refreshAdminChats: document.getElementById("refreshAdminChats"),
  adminChatRoomTitle: document.getElementById("adminChatRoomTitle"),
  adminChatMessages: document.getElementById("adminChatMessages"),
  adminChatForm: document.getElementById("adminChatForm"),
  adminChatInput: document.getElementById("adminChatInput"),
  adminChatSendBtn: document.getElementById("adminChatSendBtn"),
  adminChatCompleteBtn: document.getElementById("adminChatCompleteBtn")
};

let toastTimer = null;

function emptyPage(size = PAGE_SIZE) {
  return {
    content: [],
    page: 0,
    size,
    totalElements: 0,
    totalPages: 1,
    last: true
  };
}

function getAccessToken() {
  return localStorage.getItem("accessToken") || "";
}

function getTokenPayload() {
  const token = getAccessToken();
  const payload = token.split(".")[1];
  if (!payload) {
    return null;
  }

  try {
    const base64 = payload
      .replace(/-/g, "+")
      .replace(/_/g, "/")
      .padEnd(Math.ceil(payload.length / 4) * 4, "=");
    const bytes = Uint8Array.from(atob(base64), (char) => char.charCodeAt(0));
    return JSON.parse(new TextDecoder().decode(bytes));
  } catch (error) {
    return null;
  }
}

function isAdmin() {
  return getTokenPayload()?.role === "ADMIN";
}

function setAccessToken(token) {
  if (token) {
    localStorage.setItem("accessToken", token);
  } else {
    localStorage.removeItem("accessToken");
  }
  renderAuthState();
}

async function requestApi(path, options = {}) {
  const headers = {
    Accept: "application/json",
    ...(options.headers || {})
  };

  if (options.body) {
    headers["Content-Type"] = "application/json";
  }

  if (options.auth) {
    const token = getAccessToken();
    if (!token) {
      throw new Error("로그인이 필요합니다.");
    }
    headers.Authorization = `Bearer ${token}`;
  }

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method: options.method || "GET",
      credentials: "include",
      headers,
      body: options.body ? JSON.stringify(options.body) : undefined
    });
  } catch (error) {
    throw new Error(`API 서버(${API_BASE_URL})에 연결할 수 없습니다.`);
  }

  const payload = await readResponse(response);
  if (!response.ok) {
    throw new Error(payload?.message || `API 요청이 실패했습니다. (${response.status})`);
  }

  if (payload && Object.prototype.hasOwnProperty.call(payload, "success")) {
    if (!payload.success) {
      throw new Error(payload.message || "API 요청이 실패했습니다.");
    }
    return payload.data;
  }

  return payload;
}

async function readResponse(response) {
  const contentType = response.headers.get("content-type") || "";
  if (contentType.includes("application/json")) {
    return response.json();
  }
  const text = await response.text();
  return text ? { message: text } : null;
}

async function initialize() {
  bindEvents();
  renderAuthState();
  showView("list");
  renderCart();
  await Promise.allSettled([loadCategories(), loadProducts(), loadCart(false), loadPopularKeywords()]);
}

function bindEvents() {
  document.addEventListener("click", handleDocumentClick);
  document.addEventListener("submit", handleDocumentSubmit);
  elements.globalSearchForm?.addEventListener("submit", handleSearchSubmit);
  elements.inlineSearchForm?.addEventListener("submit", handleSearchSubmit);
  elements.globalSearchInput?.addEventListener("focus", handleSearchFocus);
  elements.inlineSearchInput?.addEventListener("focus", handleSearchFocus);
  elements.globalSearchInput?.addEventListener("input", handleSearchInput);
  elements.inlineSearchInput?.addEventListener("input", handleSearchInput);
  elements.globalSearchInput?.addEventListener("keydown", handleSearchKeydown);
  elements.inlineSearchInput?.addEventListener("keydown", handleSearchKeydown);
  elements.orderSearchForm?.addEventListener("submit", handleOrderSearchSubmit);
  elements.adminOrderSearchForm?.addEventListener("submit", handleAdminOrderSearchSubmit);
  elements.categoryToggle?.addEventListener("click", () => setCategoryMenuOpen(!state.categoryMenuOpen));
  window.addEventListener("scroll", handleWindowScroll, { passive: true });
  elements.productPager?.addEventListener("click", (e) => {
    const btn = e.target.closest("[data-page]");
    if (btn && !btn.disabled) {
      changePage(Number(btn.dataset.page));
    }
  });
  elements.addDetailCart?.addEventListener("click", () => {
    if (state.selectedProduct) {
      addToCart(state.selectedProduct.productId, state.detailQuantity);
    }
  });
  elements.buyDetail?.addEventListener("click", () => startDirectOrder());
  elements.refreshCart?.addEventListener("click", () => loadCart(true));
  elements.buyCart?.addEventListener("click", () => startCartOrder());
  elements.refreshOrders?.addEventListener("click", () => loadOrders(true));
  elements.refreshAdminOrders?.addEventListener("click", () => loadAdminOrders(true));
  elements.refreshAdminInquiries?.addEventListener("click", () => loadAdminInquiries(true));
  elements.chatWidgetToggle?.addEventListener("click", () => toggleChatWidget(!state.chatWidget.open));
  elements.chatWidgetClose?.addEventListener("click", () => toggleChatWidget(false));
  elements.chatWidgetForm?.addEventListener("submit", handleChatWidgetSubmit);
  elements.chatWidgetConnectButton?.addEventListener("click", connectToAgent);

  document.querySelectorAll("[data-action='open-admin-chats']").forEach(btn => {
    btn.addEventListener("click", () => {
      if (isAdmin()) {
        switchView("adminChatView");
        loadAdminChatRooms();
      }
    });
  });
  
  elements.refreshAdminChats?.addEventListener("click", () => loadAdminChatRooms());
  
  elements.adminChatRoomList?.addEventListener("click", (e) => {
    const item = e.target.closest(".admin-chat-room-item");
    if (item) {
      const roomId = Number(item.dataset.roomId);
      const status = item.dataset.status;
      openAdminChatRoom(roomId, status);
    }
  });
  
  elements.adminChatForm?.addEventListener("submit", handleAdminChatSubmit);
  elements.adminChatCompleteBtn?.addEventListener("click", completeAdminChat);
}

function handleDocumentClick(event) {
  const modalClose = event.target.closest("[data-modal-close]");
  if (modalClose || event.target === elements.modalRoot) {
    closeModal();
    return;
  }

  const popularKeywordButton = event.target.closest("[data-popular-keyword]");
  if (popularKeywordButton) {
    searchProducts(popularKeywordButton.dataset.popularKeyword || "");
    return;
  }

  if (!event.target.closest(".search-box")) {
    hidePopularKeywordPanels();
  }

  const actionButton = event.target.closest("[data-action]");
  if (actionButton) {
    handleAction(actionButton);
    return;
  }

  const adminInquiryButton = event.target.closest("[data-admin-inquiry-id]");
  if (adminInquiryButton) {
    selectAdminInquiry(Number(adminInquiryButton.dataset.adminInquiryId));
    return;
  }

  const viewButton = event.target.closest("[data-view]");
  if (viewButton) {
    if (viewButton.dataset.view === "list") {
      resetListState();
      loadProducts();
      return;
    }

    showView(viewButton.dataset.view);
    if (viewButton.dataset.view === "cart") {
      loadCart(true);
    }
    if (viewButton.dataset.view === "orders") {
      loadOrders(true);
    }
    return;
  }

  const sortButton = event.target.closest("[data-sort]");
  if (sortButton) {
    state.sort = sortButton.dataset.sort;
    state.page = 0;
    renderSortButtons();
    loadProducts();
    return;
  }

  const categoryButton = event.target.closest("[data-category-id]");
  if (categoryButton) {
    selectCategory(categoryButton.dataset.categoryId, categoryButton.dataset.rootId);
    return;
  }

  const quantityButton = event.target.closest("[data-detail-quantity]");
  if (quantityButton && state.view === "detail") {
    changeDetailQuantity(Number(quantityButton.dataset.detailQuantity));
    return;
  }

  const cartQuantityButton = event.target.closest("[data-cart-quantity]");
  if (cartQuantityButton) {
    updateCartQuantity(
      Number(cartQuantityButton.dataset.cartItemId),
      Number(cartQuantityButton.dataset.cartQuantity)
    );
    return;
  }

  const cartDeleteButton = event.target.closest("[data-cart-delete]");
  if (cartDeleteButton) {
    deleteCartItem(Number(cartDeleteButton.dataset.cartDelete));
    return;
  }

  const orderCancelButton = event.target.closest("[data-order-cancel]");
  if (orderCancelButton) {
    cancelOrder(Number(orderCancelButton.dataset.orderCancel));
    return;
  }

  const orderConfirmButton = event.target.closest("[data-confirm-order]");
  if (orderConfirmButton) {
    confirmOrder(orderConfirmButton.dataset.confirmOrder);
  }
}

function handleAction(button) {
  const action = button.dataset.action;
  if (action === "home") {
    resetListState();
    loadProducts();
  } else if (action === "open-login") {
    openLoginModal();
  } else if (action === "open-signup") {
    openSignupModal();
  } else if (action === "logout") {
    logout();
  } else if (action === "open-orders") {
    showView("orders");
    loadOrders(true);
  } else if (action === "open-inquiry") {
    showView("customer");
  } else if (action === "open-admin-orders") {
    showView("adminOrders");
    loadAdminOrders(true);
  } else if (action === "open-admin-inquiries") {
    showView("adminInquiry");
    loadAdminInquiries(true);
  } else if (action === "open-product") {
    loadProductDetail(Number(button.dataset.productId));
  } else if (action === "add-cart") {
    addToCart(Number(button.dataset.productId), 1);
  } else if (action === "show-best") {
    showBestProducts();
  } else if (action === "show-promo") {
    showInfo("프로모션", "프로모션 데이터 API가 아직 없어 실제 프로모션 목록은 연결할 수 없습니다.");
  } else if (action === "show-community") {
    showInfo("커뮤니티", "커뮤니티 게시글 API가 아직 없어 실제 커뮤니티 목록은 연결할 수 없습니다.");
  }
}

async function handleDocumentSubmit(event) {
  const form = event.target;
  if (form.id === "loginForm") {
    event.preventDefault();
    await submitLogin(form);
  } else if (form.id === "signupForm") {
    event.preventDefault();
    await submitSignup(form);
  } else if (form.id === "inquiryForm") {
    event.preventDefault();
    await submitInquiry(form);
  } else if (form.id === "adminInquiryForm") {
    event.preventDefault();
    await submitAdminInquiry(form);
  }
}

function handleSearchSubmit(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const input = form === elements.globalSearchForm
    ? elements.globalSearchInput
    : elements.inlineSearchInput;
  searchProducts(input.value);
}

function handleSearchFocus(event) {
  if (event.currentTarget.value.trim()) {
    hidePopularKeywordPanels();
    return;
  }

  state.popularKeywordPanelTarget = event.currentTarget === elements.globalSearchInput ? "global" : "inline";
  renderPopularKeywords();
  loadPopularKeywords();
}

function handleSearchInput(event) {
  if (event.currentTarget.value.trim()) {
    hidePopularKeywordPanels();
    return;
  }

  state.popularKeywordPanelTarget = event.currentTarget === elements.globalSearchInput ? "global" : "inline";
  renderPopularKeywords();
}

function handleSearchKeydown(event) {
  if (event.key === "Escape") {
    hidePopularKeywordPanels();
  }
}

function searchProducts(keyword) {
  state.query = String(keyword || "").trim();
  state.selectedRootId = null;
  state.selectedCategoryId = null;
  state.listMode = "products";
  state.page = 0;
  syncSearchInputs();
  hidePopularKeywordPanels();
  showView("list");
  renderCategories();
  loadProducts();
}

function handleOrderSearchSubmit(event) {
  event.preventDefault();
  state.orderFilters = formToObject(event.currentTarget);
  loadOrders(true);
}

function handleAdminOrderSearchSubmit(event) {
  event.preventDefault();
  state.adminOrderFilters = formToObject(event.currentTarget);
  loadAdminOrders(true);
}

function handleWindowScroll() {
  if (state.view === "list" && state.categoryMenuOpen) {
    setCategoryMenuOpen(false);
  }
}

function selectCategory(categoryIdValue, rootIdValue) {
  const categoryId = categoryIdValue ? Number(categoryIdValue) : null;
  const rootId = rootIdValue ? Number(rootIdValue) : categoryId;
  state.selectedCategoryId = categoryId;
  state.selectedRootId = rootId;
  state.query = "";
  state.listMode = "products";
  state.page = 0;
  syncSearchInputs();
  renderCategories();
  loadProducts();
}

function resetListState() {
  state.view = "list";
  state.listMode = "products";
  state.selectedRootId = null;
  state.selectedCategoryId = null;
  state.query = "";
  state.sort = "newest";
  state.page = 0;
  syncSearchInputs();
  showView("list");
  renderCategories();
  renderSortButtons();
}

function showBestProducts() {
  state.view = "list";
  state.listMode = "best";
  state.selectedRootId = null;
  state.selectedCategoryId = null;
  state.query = "";
  state.sort = "newest";
  state.page = 0;
  syncSearchInputs();
  showView("list");
  renderCategories();
  loadProducts();
}

function showInfo(title, message) {
  elements.infoTitle.textContent = title;
  elements.infoMessage.textContent = message;
  showView("info");
}

async function loadCategories() {
  state.categoryError = "";
  state.categoryLoaded = false;
  renderCategories();
  try {
    const categories = await requestApi("/api/categories");
    state.categories = Array.isArray(categories) ? categories : [];
    state.categoryLoaded = true;
  } catch (error) {
    state.categoryError = error.message;
  }
  renderCategories();
}

async function loadProducts() {
  state.loadingProducts = true;
  state.productError = "";
  renderProductList();

  try {
    const response = await requestApi(buildProductListPath());
    state.productPage = state.listMode === "best"
      ? pageFromArray(response)
      : normalizePage(response);
    state.products = state.productPage.content;
  } catch (error) {
    state.productError = error.message;
    state.productPage = emptyPage();
    state.products = [];
  } finally {
    state.loadingProducts = false;
    renderProductList();
  }
}

async function loadPopularKeywords() {
  if (state.loadingPopularKeywords || state.popularKeywordLoaded) {
    return;
  }

  state.loadingPopularKeywords = true;
  state.popularKeywordError = "";
  renderPopularKeywords();

  try {
    const keywords = await requestApi(`/api/search/popular?limit=${POPULAR_KEYWORD_LIMIT}`);
    state.popularKeywords = Array.isArray(keywords)
      ? keywords.filter((item) => item?.keyword).slice(0, POPULAR_KEYWORD_LIMIT)
      : [];
    state.popularKeywordLoaded = true;
    state.popularKeywordUpdatedAt = new Date().toISOString();
  } catch (error) {
    state.popularKeywordError = error.message;
    state.popularKeywords = [];
  } finally {
    state.loadingPopularKeywords = false;
    renderPopularKeywords();
  }
}

function buildProductListPath() {
  if (state.listMode === "best") {
    return `/api/products/popular?limit=${PAGE_SIZE}`;
  }

  if (state.query) {
    const params = new URLSearchParams({
      keyword: state.query,
      page: String(state.page),
      size: String(PAGE_SIZE)
    });
    params.append("sort", mapSearchSort(state.sort));
    return `/api/products/search?${params.toString()}`;
  }

  const params = new URLSearchParams({
    sort: state.sort,
    page: String(state.page),
    size: String(PAGE_SIZE)
  });

  if (state.selectedCategoryId) {
    return `/api/categories/${state.selectedCategoryId}/products?${params.toString()}`;
  }

  return `/api/products?${params.toString()}`;
}

function pageFromArray(items, fallbackSize = PAGE_SIZE) {
  const content = Array.isArray(items) ? items : [];
  return {
    content,
    page: 0,
    size: fallbackSize,
    totalElements: content.length,
    totalPages: 1,
    last: true
  };
}

function mapSearchSort(sort) {
  if (sort === "price_high") {
    return "price,desc";
  }
  if (sort === "price_low") {
    return "price,asc";
  }
  return "createdAt,desc";
}

function normalizePage(page, fallbackSize = PAGE_SIZE) {
  const content = Array.isArray(page?.content) ? page.content : [];
  return {
    content,
    page: Number(page?.page ?? 0),
    size: Number(page?.size ?? fallbackSize),
    totalElements: Number(page?.totalElements ?? content.length),
    totalPages: Math.max(Number(page?.totalPages ?? 1), 1),
    last: Boolean(page?.last ?? true)
  };
}

async function loadProductDetail(productId) {
  if (!productId) {
    return;
  }

  showView("detail");
  state.selectedProduct = null;
  state.detailQuantity = 1;
  renderDetailLoading();

  try {
    const product = await requestApi(`/api/products/${productId}`);
    state.selectedProduct = product;
    renderProductDetail();
  } catch (error) {
    showToast(error.message);
    showView("list");
  }
}

async function addToCart(productId, quantity) {
  if (!requireAuth()) {
    return;
  }

  try {
    await requestApi("/api/carts/items", {
      method: "POST",
      auth: true,
      body: { productId, quantity }
    });
    showToast("장바구니에 담았습니다.");
    await loadCart(false);
  } catch (error) {
    showToast(error.message);
  }
}

async function loadCart(showErrors) {
  state.cartError = "";
  if (!getAccessToken()) {
    state.cart = null;
    state.cartError = "로그인 후 장바구니를 조회할 수 있습니다.";
    state.selectedCartIds = new Set();
    state.cartSelectionReady = false;
    renderCart();
    return;
  }

  try {
    state.cart = await requestApi("/api/carts", { auth: true });
    syncCartSelection();
  } catch (error) {
    state.cart = null;
    state.cartError = error.message;
    if (showErrors) {
      showToast(error.message);
    }
  }
  renderCart();
}

function syncCartSelection() {
  const items = Array.isArray(state.cart?.items) ? state.cart.items : [];
  const orderableIds = items
    .filter((item) => item.orderable)
    .map((item) => Number(item.cartItemId));

  if (!state.cartSelectionReady) {
    state.selectedCartIds = new Set(orderableIds);
    state.cartSelectionReady = true;
    return;
  }

  const validIds = new Set(items.map((item) => Number(item.cartItemId)));
  state.selectedCartIds = new Set(
    Array.from(state.selectedCartIds).filter((cartId) => validIds.has(cartId))
  );
}

async function updateCartQuantity(cartItemId, nextQuantity) {
  if (nextQuantity < 1) {
    await deleteCartItem(cartItemId);
    return;
  }

  try {
    await requestApi(`/api/carts/items/${cartItemId}`, {
      method: "PATCH",
      auth: true,
      body: { quantity: nextQuantity }
    });
    await loadCart(false);
  } catch (error) {
    showToast(error.message);
  }
}

async function deleteCartItem(cartItemId) {
  try {
    await requestApi(`/api/carts/items/${cartItemId}`, {
      method: "DELETE",
      auth: true
    });
    state.selectedCartIds.delete(cartItemId);
    showToast("장바구니에서 삭제했습니다.");
    await loadCart(false);
  } catch (error) {
    showToast(error.message);
  }
}

async function startDirectOrder() {
  if (!state.selectedProduct || !requireAuth()) {
    return;
  }

  try {
    const preview = await requestApi("/api/orders/direct/preview", {
      method: "POST",
      auth: true,
      body: {
        productId: state.selectedProduct.productId,
        quantity: state.detailQuantity
      }
    });
    openOrderPreviewModal("direct", preview);
  } catch (error) {
    showToast(error.message);
  }
}

async function startCartOrder() {
  if (!requireAuth()) {
    return;
  }

  const cartIds = Array.from(state.selectedCartIds);
  if (!cartIds.length) {
    showToast("주문할 장바구니 상품을 선택해주세요.");
    return;
  }

  try {
    const params = new URLSearchParams();
    cartIds.forEach((cartId) => params.append("cartIds", String(cartId)));
    const preview = await requestApi(`/api/orders/preview?${params.toString()}`, { auth: true });
    openOrderPreviewModal("cart", preview);
  } catch (error) {
    showToast(error.message);
  }
}

async function confirmOrder(type) {
  if (!requireAuth()) {
    return;
  }

  try {
    let response;
    if (type === "direct") {
      response = await requestApi("/api/orders/direct", {
        method: "POST",
        auth: true,
        body: {
          productId: state.selectedProduct.productId,
          quantity: state.detailQuantity
        }
      });
    } else {
      response = await requestApi("/api/orders/carts", {
        method: "POST",
        auth: true,
        body: { cartIds: Array.from(state.selectedCartIds) }
      });
      state.cartSelectionReady = false;
      await loadCart(false);
    }
    closeModal();
    showToast(`주문이 완료되었습니다. 주문번호 ${response.orderNumber}`);
    await loadOrders(false);
  } catch (error) {
    showToast(error.message);
  }
}

async function loadOrders(showErrors) {
  state.orderError = "";
  if (!getAccessToken()) {
    state.orders = emptyPage(ORDER_PAGE_SIZE);
    state.orderError = "로그인 후 주문내역을 조회할 수 있습니다.";
    renderOrders();
    return;
  }

  state.loadingOrders = true;
  renderOrders();
  try {
    const params = new URLSearchParams({
      page: "0",
      size: String(ORDER_PAGE_SIZE),
      sort: "createdAt,desc"
    });
    Object.entries(state.orderFilters).forEach(([key, value]) => {
      if (value) {
        params.set(key, value);
      }
    });
    const page = await requestApi(`/api/orders?${params.toString()}`, { auth: true });
    state.orders = normalizePage(page, ORDER_PAGE_SIZE);
  } catch (error) {
    state.orderError = error.message;
    state.orders = emptyPage(ORDER_PAGE_SIZE);
    if (showErrors) {
      showToast(error.message);
    }
  } finally {
    state.loadingOrders = false;
    renderOrders();
  }
}

async function loadAdminOrders(showErrors) {
  state.adminOrderError = "";
  if (!getAccessToken()) {
    state.adminOrders = emptyPage(ADMIN_ORDER_PAGE_SIZE);
    state.adminOrderError = "관리자 로그인 후 주문내역을 조회할 수 있습니다.";
    renderAdminOrders();
    return;
  }

  if (!isAdmin()) {
    state.adminOrders = emptyPage(ADMIN_ORDER_PAGE_SIZE);
    state.adminOrderError = "관리자 권한이 필요합니다.";
    renderAdminOrders();
    if (showErrors) {
      showToast(state.adminOrderError);
    }
    return;
  }

  state.loadingAdminOrders = true;
  renderAdminOrders();
  try {
    const params = new URLSearchParams({
      page: "0",
      size: String(ADMIN_ORDER_PAGE_SIZE),
      sort: "createdAt,desc"
    });
    Object.entries(state.adminOrderFilters).forEach(([key, value]) => {
      if (value) {
        params.set(key, value);
      }
    });
    const page = await requestApi(`/api/admins/orders?${params.toString()}`, { auth: true });
    state.adminOrders = normalizePage(page, ADMIN_ORDER_PAGE_SIZE);
  } catch (error) {
    state.adminOrderError = error.message;
    state.adminOrders = emptyPage(ADMIN_ORDER_PAGE_SIZE);
    if (showErrors) {
      showToast(error.message);
    }
  } finally {
    state.loadingAdminOrders = false;
    renderAdminOrders();
  }
}

async function loadAdminInquiries(showErrors) {
  state.adminInquiryError = "";
  if (!getAccessToken()) {
    state.adminInquiries = emptyPage(ADMIN_INQUIRY_PAGE_SIZE);
    state.selectedAdminInquiryId = null;
    state.adminInquiryError = "관리자 로그인 후 고객문의 내역을 조회할 수 있습니다.";
    renderAdminInquiries();
    return;
  }

  if (!isAdmin()) {
    state.adminInquiries = emptyPage(ADMIN_INQUIRY_PAGE_SIZE);
    state.selectedAdminInquiryId = null;
    state.adminInquiryError = "관리자 권한이 필요합니다.";
    renderAdminInquiries();
    if (showErrors) {
      showToast(state.adminInquiryError);
    }
    return;
  }

  state.loadingAdminInquiries = true;
  renderAdminInquiries();
  try {
    const params = new URLSearchParams({
      page: "0",
      size: String(ADMIN_INQUIRY_PAGE_SIZE),
      sort: "createdAt,desc"
    });
    const page = await requestApi(`/api/admins/inquiry?${params.toString()}`, { auth: true });
    state.adminInquiries = normalizePage(page, ADMIN_INQUIRY_PAGE_SIZE);
    if (!state.adminInquiries.content.some((inquiry) => Number(inquiry.id) === state.selectedAdminInquiryId)) {
      state.selectedAdminInquiryId = null;
    }
  } catch (error) {
    state.adminInquiryError = error.message;
    state.adminInquiries = emptyPage(ADMIN_INQUIRY_PAGE_SIZE);
    state.selectedAdminInquiryId = null;
    if (showErrors) {
      showToast(error.message);
    }
  } finally {
    state.loadingAdminInquiries = false;
    renderAdminInquiries();
  }
}

async function cancelOrder(orderId) {
  if (!requireAuth()) {
    return;
  }

  try {
    await requestApi(`/api/orders/${orderId}/cancel`, {
      method: "POST",
      auth: true
    });
    showToast("주문을 취소했습니다.");
    await loadOrders(false);
  } catch (error) {
    showToast(error.message);
  }
}

async function submitLogin(form) {
  const body = formToObject(form);
  setModalStatus("로그인 중입니다.");
  try {
    const response = await requestApi("/api/auth/login", {
      method: "POST",
      body
    });
    setAccessToken(response.accessToken);
    closeModal();
    if (isAdmin()) {
      showToast("관리자로 로그인되었습니다.");
      showView("adminOrders");
      await Promise.allSettled([loadCart(false), loadAdminOrders(false)]);
      return;
    }
    showToast("로그인되었습니다.");
    await Promise.allSettled([loadCart(false), loadOrders(false)]);
  } catch (error) {
    setModalStatus(error.message, true);
  }
}

async function submitSignup(form) {
  const body = formToObject(form);
  setModalStatus("가입 중입니다.");
  try {
    await requestApi("/api/auth/signup", {
      method: "POST",
      body
    });
    const loginResponse = await requestApi("/api/auth/login", {
      method: "POST",
      body: { email: body.email, password: body.password }
    });
    setAccessToken(loginResponse.accessToken);
    closeModal();
    showToast("회원가입과 로그인이 완료되었습니다.");
    await Promise.allSettled([loadCart(false), loadOrders(false)]);
  } catch (error) {
    setModalStatus(error.message, true);
  }
}

async function submitInquiry(form) {
  if (!requireAuth()) {
    return;
  }

  const body = formToObject(form);
  setFormStatus(elements.inquiryStatus, "문의 등록 중입니다.");
  try {
    await requestApi("/api/members/inquiry", {
      method: "POST",
      auth: true,
      body
    });
    form.reset();
    updateTextCounter("inquiryContent", "inquiryCount");
    setFormStatus(elements.inquiryStatus, "");
    showToast("고객문의가 등록되었습니다.");
  } catch (error) {
    setFormStatus(elements.inquiryStatus, error.message, true);
  }
}

async function submitAdminInquiry(form) {
  if (!requireAuth()) {
    return;
  }

  const body = formToObject(form);
  if (!body.inquiryId) {
    setFormStatus(elements.adminInquiryStatus, "답변할 문의를 선택하세요.", true);
    return;
  }

  setFormStatus(elements.adminInquiryStatus, "답변 등록 중입니다.");
  try {
    await requestApi("/api/admins/inquiry", {
      method: "POST",
      auth: true,
      body: {
        inquiryId: Number(body.inquiryId),
        answer: body.answer
      }
    });
    form.reset();
    state.selectedAdminInquiryId = null;
    updateTextCounter("adminAnswer", "adminAnswerCount");
    setFormStatus(elements.adminInquiryStatus, "");
    showToast("관리자 답변이 등록되었습니다.");
    await loadAdminInquiries(false);
  } catch (error) {
    setFormStatus(elements.adminInquiryStatus, error.message, true);
  }
}

function requireAuth() {
  if (getAccessToken()) {
    return true;
  }
  openLoginModal("로그인이 필요한 기능입니다.");
  return false;
}

function logout() {
  setAccessToken("");
  state.cart = null;
  state.cartSelectionReady = false;
  state.selectedCartIds = new Set();
  state.orders = emptyPage(ORDER_PAGE_SIZE);
  state.adminOrders = emptyPage(ADMIN_ORDER_PAGE_SIZE);
  state.adminOrderError = "";
  state.adminInquiries = emptyPage(ADMIN_INQUIRY_PAGE_SIZE);
  state.adminInquiryError = "";
  state.selectedAdminInquiryId = null;
  renderCart();
  renderOrders();
  renderAdminOrders();
  renderAdminInquiries();
  showToast("로그아웃되었습니다.");
}

function showView(view) {
  state.view = view;
  if (view !== "list") {
    state.categoryMenuOpen = false;
  }
  updateCategoryMenuVisibility();
  elements.listView.classList.toggle("active", view === "list");
  elements.detailView.classList.toggle("active", view === "detail");
  elements.cartView.classList.toggle("active", view === "cart");
  elements.ordersView.classList.toggle("active", view === "orders");
  elements.adminOrdersView.classList.toggle("active", view === "adminOrders");
  elements.customerCenterView.classList.toggle("active", view === "customer");
  elements.adminInquiryView.classList.toggle("active", view === "adminInquiry");
  elements.infoView.classList.toggle("active", view === "info");
  renderNavState();
}

function setCategoryMenuOpen(open) {
  state.categoryMenuOpen = Boolean(open) && state.view === "list";
  updateCategoryMenuVisibility();
}

function updateCategoryMenuVisibility() {
  const showCategoryPanel = state.view === "list" && state.categoryMenuOpen;
  if (elements.categoryPanel) {
    elements.categoryPanel.hidden = !showCategoryPanel;
    elements.categoryPanel.classList.toggle("is-hidden", !showCategoryPanel);
  }
  if (elements.categoryToggle) {
    elements.categoryToggle.hidden = state.view !== "list";
    elements.categoryToggle.classList.toggle("active", showCategoryPanel);
    elements.categoryToggle.setAttribute("aria-expanded", String(showCategoryPanel));
    elements.categoryToggle.setAttribute("aria-label", showCategoryPanel ? "카테고리 닫기" : "카테고리 열기");
  }
}

function renderNavState() {
  document.querySelectorAll(".nav-button").forEach((button) => {
    const active = (
      button.dataset.view === "list" && state.view === "list" && state.listMode === "products"
    ) || (
      button.dataset.action === "show-best" && state.view === "list" && state.listMode === "best"
    ) || (
      button.dataset.action === "show-promo" && state.view === "info" && elements.infoTitle.textContent === "프로모션"
    ) || (
      button.dataset.action === "show-community" && state.view === "info" && elements.infoTitle.textContent === "커뮤니티"
    );
    button.classList.toggle("active", active);
  });
}

function renderAuthState() {
  const loggedIn = Boolean(getAccessToken());
  const admin = loggedIn && isAdmin();
  elements.loginButton.textContent = loggedIn ? "로그아웃" : "로그인";
  elements.loginButton.dataset.action = loggedIn ? "logout" : "open-login";
  elements.signupButton.textContent = loggedIn ? "주문내역" : "회원가입";
  elements.signupButton.dataset.action = loggedIn ? "open-orders" : "open-signup";
  elements.adminMenuButton.forEach(el => el.hidden = !admin);
}

function renderPopularKeywords() {
  const panels = [
    { target: "global", element: elements.globalPopularKeywords },
    { target: "inline", element: elements.inlinePopularKeywords }
  ];

  panels.forEach(({ target, element }) => {
    if (!element) {
      return;
    }

    const active = state.popularKeywordPanelTarget === target;
    const hasContent = state.loadingPopularKeywords
      || state.popularKeywordError
      || state.popularKeywordLoaded
      || state.popularKeywords.length;
    element.hidden = !active || !hasContent;

    if (!hasContent) {
      element.innerHTML = "";
      return;
    }

    element.innerHTML = popularKeywordPanelTemplate();
  });
}

function popularKeywordPanelTemplate() {
  if (state.loadingPopularKeywords) {
    return `<div class="popular-keywords-empty">인기 검색어를 불러오는 중입니다.</div>`;
  }

  if (state.popularKeywordError) {
    return `<div class="popular-keywords-empty error">${escapeHtml(state.popularKeywordError)}</div>`;
  }

  const updatedAt = formatPopularKeywordTime(state.popularKeywordUpdatedAt);
  const keywords = buildPopularKeywordItems();
  return `
    <div class="popular-keywords-heading">
      <strong>인기검색어</strong>
      <span>${updatedAt ? `${escapeHtml(updatedAt)} 기준` : ""}</span>
    </div>
    <ol class="popular-keywords-list">
      ${keywords.map(popularKeywordItemTemplate).join("")}
    </ol>
  `;
}

function buildPopularKeywordItems() {
  const seen = new Set();
  const keywords = [];
  [...state.popularKeywords.map((item) => item.keyword), ...DEFAULT_POPULAR_KEYWORDS].forEach((keyword) => {
    const value = String(keyword || "").trim();
    if (!value || seen.has(value)) {
      return;
    }
    seen.add(value);
    keywords.push({ keyword: value });
  });
  return keywords.slice(0, POPULAR_KEYWORD_LIMIT);
}

function popularKeywordItemTemplate(item, index) {
  const keyword = String(item.keyword || "").trim();
  return `
    <li>
      <button class="popular-keyword-button" type="button" data-popular-keyword="${escapeHtml(keyword)}">
        <span class="popular-keyword-rank">${index + 1}</span>
        <span class="popular-keyword-text">${escapeHtml(keyword)}</span>
      </button>
    </li>
  `;
}

function hidePopularKeywordPanels() {
  state.popularKeywordPanelTarget = "";
  renderPopularKeywords();
}

function renderCategories() {
  renderListFilters();

  if (state.categoryError) {
    elements.categoryTabs.innerHTML = `<div class="empty error">${escapeHtml(state.categoryError)}</div>`;
    elements.categoryChildren.innerHTML = "";
    return;
  }

  if (!state.categories.length) {
    if (state.categoryLoaded) {
      elements.categoryTabs.innerHTML = "";
      elements.categoryChildren.innerHTML = "";
      return;
    }
    elements.categoryTabs.innerHTML = `<span class="skeleton">카테고리를 불러오는 중입니다.</span>`;
    elements.categoryChildren.innerHTML = "";
    return;
  }

  const rootButtons = [
    ...state.categories.map((category) => categoryButtonTemplate(
      category.name,
      category.categoryId,
      category.categoryId,
      state.selectedRootId === category.categoryId
    ))
  ];
  elements.categoryTabs.innerHTML = rootButtons.join("");

  const selectedRoot = state.categories.find((category) => category.categoryId === state.selectedRootId);
  elements.categoryChildren.innerHTML = selectedRoot
    ? selectedCategoryTemplate(selectedRoot)
    : allCategoriesTemplate();
}

function renderListFilters() {
  if (!elements.listTabs || !elements.subTabs) {
    return;
  }

  elements.listTabs.innerHTML = "";
  elements.subTabs.innerHTML = "";
}

function categoryButtonTemplate(label, categoryId, rootId, active) {
  return `
    <button
      class="category-button${active ? " active" : ""}"
      type="button"
      data-category-id="${escapeHtml(String(categoryId ?? ""))}"
      data-root-id="${escapeHtml(String(rootId ?? ""))}"
    >${escapeHtml(label)}</button>
  `;
}

function allCategoriesTemplate() {
  return state.categories.map((category) => categoryColumnTemplate(category)).join("");
}

function selectedCategoryTemplate(category) {
  const childLinks = Array.isArray(category.children) && category.children.length
    ? category.children.map((child) => `
      <button class="category-link${state.selectedCategoryId === child.categoryId ? " active" : ""}" type="button" data-category-id="${child.categoryId}" data-root-id="${category.categoryId}">
        ${escapeHtml(child.name)}
      </button>
    `).join("")
    : "";

  return `
    <div class="category-group selected">
      <button class="category-group-heading${state.selectedCategoryId === category.categoryId ? " active" : ""}" type="button" data-category-id="${category.categoryId}" data-root-id="${category.categoryId}">
        전체 보기
      </button>
      ${childLinks}
    </div>
  `;
}

function categoryColumnTemplate(category) {
  const links = Array.isArray(category.children) && category.children.length
    ? category.children.map((child) => `
      <button class="category-link${state.selectedCategoryId === child.categoryId ? " active" : ""}" type="button" data-category-id="${child.categoryId}" data-root-id="${category.categoryId}">
        ${escapeHtml(child.name)}
      </button>
    `).join("")
    : "";

  return `
    <div class="category-group">
      <button class="category-group-heading${state.selectedCategoryId === category.categoryId ? " active" : ""}" type="button" data-category-id="${category.categoryId}" data-root-id="${category.categoryId}">
        ${escapeHtml(category.name)}
      </button>
      <button class="category-link${state.selectedCategoryId === category.categoryId ? " active" : ""}" type="button" data-category-id="${category.categoryId}" data-root-id="${category.categoryId}">
        전체
      </button>
      ${links}
    </div>
  `;
}

function listTabTemplate(label, categoryId, rootId, active) {
  return `
    <button class="list-tab${active ? " active" : ""}" type="button" data-category-id="${escapeHtml(String(categoryId ?? ""))}" data-root-id="${escapeHtml(String(rootId ?? ""))}">
      ${escapeHtml(label)}
    </button>
  `;
}

function subTabTemplate(label, categoryId, rootId, active) {
  return `
    <button class="sub-tab${active ? " active" : ""}" type="button" data-category-id="${escapeHtml(String(categoryId ?? ""))}" data-root-id="${escapeHtml(String(rootId ?? ""))}">
      ${escapeHtml(label)}
    </button>
  `;
}

function renderProductList() {
  renderListHeading();
  renderSortButtons();
  renderPager();
  renderNavState();

  if (state.loadingProducts) {
    elements.productGrid.innerHTML = `<div class="empty">상품을 불러오는 중입니다.</div>`;
    return;
  }

  if (state.productError) {
    elements.productGrid.innerHTML = `<div class="empty error">${escapeHtml(state.productError)}</div>`;
    return;
  }

  if (!state.products.length) {
    elements.productGrid.innerHTML = `<div class="empty">조건에 맞는 상품이 없습니다.</div>`;
    return;
  }

  elements.productGrid.innerHTML = state.products.map(productCardTemplate).join("");
}

function renderListHeading() {
  const activeCategory = findCategoryById(state.selectedCategoryId);
  elements.activeCategoryLabel.textContent = state.query
    ? "검색 결과"
    : activeCategory
      ? "카테고리 상품"
      : "전체 상품";
  elements.listTitle.textContent = state.listMode === "best"
    ? "베스트"
    : state.query
      ? `"${state.query}" 검색`
      : activeCategory?.name || "전체상품";
  elements.productCount.textContent = formatNumber(state.productPage.totalElements);
}

function renderSortButtons() {
  document.querySelectorAll(".sort-button").forEach((button) => {
    button.classList.toggle("active", button.dataset.sort === state.sort);
  });
}

function renderPager() {
  if (!elements.productPager) return;
  const current = state.productPage.page;
  const total = state.productPage.totalPages;

  if (total <= 1 && !state.loadingProducts && state.products.length === 0) {
    elements.productPager.innerHTML = "";
    return;
  }

  const windowSize = 5;
  let start = Math.max(0, current - Math.floor(windowSize / 2));
  let end = Math.min(total - 1, start + windowSize - 1);

  if (end - start + 1 < windowSize) {
    start = Math.max(0, end - windowSize + 1);
  }

  let html = "";
  const prevDisabled = current <= 0 || state.loadingProducts;
  html += `<button type="button" class="pager-nav" data-page="${current - 1}" ${prevDisabled ? 'disabled' : ''} aria-label="이전 페이지">
    <svg viewBox="0 0 24 24"><path d="M15 18l-6-6 6-6"/></svg>
  </button>`;

  for (let i = start; i <= end; i++) {
    const active = i === current;
    html += `<button type="button" class="pager-number ${active ? 'active' : ''}" data-page="${i}" ${state.loadingProducts ? 'disabled' : ''}>${i + 1}</button>`;
  }

  const nextDisabled = current >= total - 1 || state.loadingProducts;
  html += `<button type="button" class="pager-nav" data-page="${current + 1}" ${nextDisabled ? 'disabled' : ''} aria-label="다음 페이지">
    <svg viewBox="0 0 24 24"><path d="M9 18l6-6-6-6"/></svg>
  </button>`;

  elements.productPager.innerHTML = html;
}

function productCardTemplate(product) {
  const productId = product.productId;
  const orderable = isOrderable(product);
  const popularBadge = state.listMode === "best"
    ? `<span class="product-popular-badge">인기상품</span>`
    : "";
  return `
    <article class="product-card">
      <button class="product-thumb" type="button" data-action="open-product" data-product-id="${productId}" aria-label="상세 보기">
        ${productVisual(product)}
      </button>
      <div class="product-body">
        <div class="product-badges">
          <span class="product-status">${escapeHtml(statusLabel(product))}</span>
          ${popularBadge}
        </div>
        <h2>${escapeHtml(product.name || "이름 없는 상품")}</h2>
        <p class="product-description">${escapeHtml(product.description || "상품 설명이 없습니다.")}</p>
        <strong class="product-price">${formatMoney(product.price)}</strong>
      </div>
      <div class="product-actions">
        <button class="secondary-button" type="button" data-action="add-cart" data-product-id="${productId}" ${orderable ? "" : "disabled"}>장바구니</button>
        <button class="primary-button" type="button" data-action="open-product" data-product-id="${productId}">바로구매</button>
      </div>
    </article>
  `;
}

function renderDetailLoading() {
  elements.detailVisual.innerHTML = "";
  elements.detailName.textContent = "상품을 불러오는 중입니다.";
  elements.detailDescription.textContent = "";
  elements.detailPrice.textContent = formatMoney(0);
  elements.detailStock.textContent = "-";
  renderDetailQuantity();
}

function renderProductDetail() {
  const product = state.selectedProduct;
  if (!product) {
    return;
  }

  elements.detailVisual.innerHTML = productVisual(product);
  elements.detailName.textContent = product.name || "이름 없는 상품";
  elements.detailDescription.textContent = product.description || "상품 설명이 없습니다.";
  elements.detailPrice.textContent = formatMoney(product.price);
  elements.detailStock.textContent = `${formatNumber(product.stock)}개 / ${statusLabel(product)}`;
  renderDetailQuantity();
}

function renderDetailQuantity() {
  const price = Number(state.selectedProduct?.price || 0);
  elements.detailQuantity.textContent = state.detailQuantity;
  elements.detailTotal.textContent = formatMoney(price * state.detailQuantity);
}

function changeDetailQuantity(delta) {
  const nextQuantity = state.detailQuantity + delta;
  const maxStock = Number(state.selectedProduct?.stock || 1);
  state.detailQuantity = Math.max(1, Math.min(nextQuantity, Math.max(maxStock, 1)));
  renderDetailQuantity();
}

function renderCart() {
  const items = Array.isArray(state.cart?.items) ? state.cart.items : [];
  const selectedItems = items.filter((item) => state.selectedCartIds.has(Number(item.cartItemId)));
  const subtotal = selectedItems.reduce((sum, item) => sum + Number(item.itemTotalPrice || 0), 0);
  const shippingFee = 0;
  elements.cartBadge.textContent = formatNumber(items.reduce((sum, item) => sum + Number(item.quantity || 0), 0));
  elements.cartSubtotal.textContent = formatMoney(subtotal);
  elements.shippingFee.textContent = formatMoney(shippingFee);
  elements.cartTotal.textContent = formatMoney(subtotal + shippingFee);

  if (state.cartError) {
    elements.cartList.innerHTML = `<div class="empty${getAccessToken() ? " error" : ""}">${escapeHtml(state.cartError)}</div>`;
    return;
  }

  if (!items.length) {
    elements.cartList.innerHTML = `<div class="empty">장바구니가 비어 있습니다.</div>`;
    return;
  }

  const allOrderableIds = items.filter((item) => item.orderable).map((item) => Number(item.cartItemId));
  const allChecked = allOrderableIds.length > 0 && allOrderableIds.every((cartId) => state.selectedCartIds.has(cartId));

  elements.cartList.innerHTML = `
    <div class="cart-selection-bar">
      <label><input type="checkbox" data-cart-select-all ${allChecked ? "checked" : ""}> 전체 선택</label>
      <span>선택 <strong>${formatNumber(selectedItems.length)}</strong>개</span>
      <button type="button" class="secondary-button compact" data-cart-delete-selected>선택 삭제</button>
    </div>
    ${items.map(cartItemTemplate).join("")}
  `;
}

function cartItemTemplate(item) {
  const cartItemId = Number(item.cartItemId);
  const checked = state.selectedCartIds.has(cartItemId);
  return `
    <article class="cart-item">
      <input type="checkbox" data-cart-toggle="${cartItemId}" ${checked ? "checked" : ""} ${item.orderable ? "" : "disabled"}>
      <div class="cart-item-visual">${productIcon({ name: item.productName, productId: item.productId })}</div>
      <div>
        <h2>${escapeHtml(item.productName || "이름 없는 상품")}</h2>
        <p>단가 ${formatMoney(item.productPrice)} · ${escapeHtml(statusLabel({ status: item.productStatus, stock: item.stock, orderable: item.orderable }))}</p>
        <div class="cart-quantity">
          <button type="button" data-cart-quantity="${Math.max(1, Number(item.quantity) - 1)}" data-cart-item-id="${cartItemId}">−</button>
          <span>${formatNumber(item.quantity)}</span>
          <button type="button" data-cart-quantity="${Number(item.quantity) + 1}" data-cart-item-id="${cartItemId}">+</button>
        </div>
      </div>
      <div class="cart-item-price">
        <button type="button" data-cart-delete="${cartItemId}" aria-label="삭제">×</button>
        <strong>${formatMoney(item.itemTotalPrice)}</strong>
      </div>
    </article>
  `;
}

function renderOrders() {
  elements.orderCount.textContent = formatNumber(state.orders.totalElements);

  if (state.loadingOrders) {
    elements.orderList.innerHTML = `<div class="empty">주문내역을 불러오는 중입니다.</div>`;
    return;
  }

  if (state.orderError) {
    elements.orderList.innerHTML = `<div class="empty${getAccessToken() ? " error" : ""}">${escapeHtml(state.orderError)}</div>`;
    return;
  }

  if (!state.orders.content.length) {
    elements.orderList.innerHTML = `<div class="empty">주문내역이 없습니다.</div>`;
    return;
  }

  elements.orderList.innerHTML = state.orders.content.map((order) => `
    <article class="order-history-card">
      <div class="order-primary">
        <strong>${formatDateOnly(order.createdAt)} 주문</strong>
        <h2>${escapeHtml(order.productName || order.representativeProductName || "주문 상품")}</h2>
        <p>주문번호 ${escapeHtml(order.orderNumber || String(order.orderId || "-"))}</p>
      </div>
      <span class="order-status-badge ${orderStatusClass(order.orderStatus)}">${escapeHtml(orderStatusLabel(order.orderStatus))}</span>
      <div class="order-number-block">
        <span>주문번호</span>
        <strong>${escapeHtml(order.orderNumber || String(order.orderId || "-"))}</strong>
      </div>
      <div class="order-amount-block">
        <span>주문금액</span>
        <strong>${formatMoney(order.totalAmount)}</strong>
      </div>
      <div class="order-actions-block">
        <button type="button" class="secondary-button compact">주문 상세보기</button>
        ${order.orderStatus === "CANCELLED" ? "" : `<button type="button" class="text-button danger" data-order-cancel="${order.orderId}">주문 취소</button>`}
      </div>
    </article>
  `).join("");
}

function renderAdminOrders() {
  elements.adminOrderCount.textContent = formatNumber(state.adminOrders.totalElements);

  if (state.loadingAdminOrders) {
    elements.adminOrderList.innerHTML = `<div class="empty">관리자 주문내역을 불러오는 중입니다.</div>`;
    return;
  }

  if (state.adminOrderError) {
    elements.adminOrderList.innerHTML = `<div class="empty error">${escapeHtml(state.adminOrderError)}</div>`;
    return;
  }

  if (!state.adminOrders.content.length) {
    elements.adminOrderList.innerHTML = `<div class="empty">조건에 맞는 주문이 없습니다.</div>`;
    return;
  }

  elements.adminOrderList.innerHTML = state.adminOrders.content.map((order) => `
    <article class="admin-order-card">
      <div class="order-primary">
        <strong>${formatDateOnly(order.createdAt)} 주문</strong>
        <h2>${escapeHtml(order.orderNumber || String(order.orderId || "-"))}</h2>
        <p>회원 ID ${escapeHtml(String(order.memberId || "-"))}</p>
      </div>
      <span class="order-status-badge ${orderStatusClass(order.orderStatus)}">${escapeHtml(orderStatusLabel(order.orderStatus))}</span>
      <div class="order-amount-block">
        <span>총 결제금액</span>
        <strong>${formatMoney(order.totalAmount)}</strong>
      </div>
      <div class="order-amount-block">
        <span>PG 금액</span>
        <strong>${formatMoney(order.pgAmount)}</strong>
      </div>
      <div class="order-number-block">
        <span>결제일</span>
        <strong>${formatDate(order.paidAt)}</strong>
      </div>
      <div class="order-number-block">
        <span>취소일</span>
        <strong>${formatDate(order.canceledAt)}</strong>
      </div>
    </article>
  `).join("");
}

function renderAdminInquiries() {
  elements.adminInquiryCount.textContent = formatNumber(state.adminInquiries.totalElements);

  if (state.loadingAdminInquiries) {
    elements.adminInquiryList.innerHTML = `<div class="empty">고객문의 내역을 불러오는 중입니다.</div>`;
    renderAdminInquiryForm();
    return;
  }

  if (state.adminInquiryError) {
    elements.adminInquiryList.innerHTML = `<div class="empty error">${escapeHtml(state.adminInquiryError)}</div>`;
    renderAdminInquiryForm();
    return;
  }

  if (!state.adminInquiries.content.length) {
    elements.adminInquiryList.innerHTML = `<div class="empty">접수된 고객문의가 없습니다.</div>`;
    renderAdminInquiryForm();
    return;
  }

  elements.adminInquiryList.innerHTML = state.adminInquiries.content.map(adminInquiryCardTemplate).join("");
  renderAdminInquiryForm();
}

function adminInquiryCardTemplate(inquiry) {
  const inquiryId = Number(inquiry.id);
  const answered = inquiry.status === "ANSWERED";
  const selected = state.selectedAdminInquiryId === inquiryId;

  return `
    <article class="admin-inquiry-card${selected ? " active" : ""}">
      <div class="admin-inquiry-card-header">
        <div>
          <strong>${escapeHtml(inquiry.title || "제목 없는 문의")}</strong>
          <span>문의 ID ${escapeHtml(String(inquiry.id || "-"))} · 회원 ID ${escapeHtml(String(inquiry.memberId || "-"))}</span>
        </div>
        <span class="order-status-badge ${answered ? "paid" : "pending"}">${escapeHtml(inquiryStatusLabel(inquiry.status))}</span>
      </div>
      <p class="admin-inquiry-content">${escapeHtml(inquiry.content || "")}</p>
      <div class="admin-inquiry-meta">
        <span>등록일 ${formatDate(inquiry.createdAt)}</span>
        ${answered ? `<span>답변일 ${formatDate(inquiry.answeredAt)}</span>` : ""}
      </div>
      ${answered ? `
        <div class="admin-inquiry-answer">
          <span>답변</span>
          <p>${escapeHtml(inquiry.answer || "")}</p>
        </div>
      ` : `
        <button class="secondary-button compact" type="button" data-admin-inquiry-id="${inquiryId}">답변하기</button>
      `}
    </article>
  `;
}

function renderAdminInquiryForm() {
  const selected = state.adminInquiries.content.find((inquiry) => Number(inquiry.id) === state.selectedAdminInquiryId);
  const answerTextarea = document.getElementById("adminAnswer");
  const canAnswer = Boolean(selected && selected.status !== "ANSWERED");

  elements.adminInquiryId.value = canAnswer ? selected.id : "";
  elements.adminInquirySelection.textContent = canAnswer
    ? `문의 ID ${selected.id} · ${selected.title || "제목 없는 문의"}`
    : "답변할 문의를 선택하세요.";
  if (answerTextarea) {
    answerTextarea.disabled = !canAnswer;
    if (!canAnswer) {
      answerTextarea.value = "";
      updateTextCounter("adminAnswer", "adminAnswerCount");
    }
  }
  elements.submitAdminInquiryAnswer.disabled = !canAnswer;
}

function selectAdminInquiry(inquiryId) {
  const inquiry = state.adminInquiries.content.find((item) => Number(item.id) === inquiryId);
  if (!inquiry || inquiry.status === "ANSWERED") {
    return;
  }

  state.selectedAdminInquiryId = inquiryId;
  setFormStatus(elements.adminInquiryStatus, "");
  renderAdminInquiries();
  document.getElementById("adminAnswer")?.focus();
}

function openLoginModal(message = "") {
  openModal(`
    <form class="modal-card" id="loginForm">
      <div class="modal-heading">
        <h2>일반회원 로그인</h2>
        <button type="button" data-modal-close aria-label="닫기">×</button>
      </div>
      ${message ? `<p class="modal-message">${escapeHtml(message)}</p>` : ""}
      <label>이메일<input name="email" type="email" required placeholder="이메일을 입력해주세요."></label>
      <label>비밀번호<input name="password" type="password" required placeholder="비밀번호를 입력해주세요."></label>
      <p class="modal-status" id="modalStatus"></p>
      <button class="primary-button modal-submit" type="submit">로그인</button>
    </form>
  `);
}

function openSignupModal() {
  openModal(`
    <form class="modal-card" id="signupForm">
      <div class="modal-heading">
        <h2>회원정보 입력</h2>
        <button type="button" data-modal-close aria-label="닫기">×</button>
      </div>
      <label>이름<input name="name" required maxlength="50"></label>
      <label>이메일<input name="email" type="email" required maxlength="100"></label>
      <label>비밀번호<input name="password" type="password" required minlength="8" placeholder="영문/숫자/특수문자 포함 8자 이상"></label>
      <label>핸드폰 번호<input name="phone" required placeholder="010-1234-5678"></label>
      <p class="modal-status" id="modalStatus"></p>
      <button class="primary-button modal-submit" type="submit">가입하기</button>
    </form>
  `);
}

function openInquiryModal() {
  openModal(`
    <form class="modal-card" id="inquiryForm">
      <div class="modal-heading">
        <h2>고객문의</h2>
        <button type="button" data-modal-close aria-label="닫기">×</button>
      </div>
      <label>제목<input name="title" required maxlength="100"></label>
      <label>내용<textarea name="content" required maxlength="1000" rows="6"></textarea></label>
      <p class="modal-status" id="modalStatus"></p>
      <button class="primary-button modal-submit" type="submit">문의 등록</button>
    </form>
  `);
}

function openOrderPreviewModal(type, preview) {
  const items = Array.isArray(preview?.orderItems) ? preview.orderItems : [];
  const totalAmount = Number(preview?.totalOrderAmount || 0);
  openModal(`
    <div class="modal-card order-sheet-modal">
      <div class="modal-heading">
        <h2>주문서</h2>
        <button type="button" data-modal-close aria-label="닫기">×</button>
      </div>
      <p class="screen-helper">주문 전 상품과 결제 금액을 확인합니다.</p>
      <div class="order-sheet-layout">
        <section class="order-sheet-items">
          <h3>주문 예정 상품</h3>
          <div class="order-sheet-list">
            ${items.map((item) => `
              <article class="order-sheet-item">
                <div>
                  <strong>${escapeHtml(item.productName || "상품")}</strong>
                  <span>상품 단가 ${formatMoney(item.productPrice)}</span>
                  <span>수량 ${formatNumber(item.quantity)}개</span>
                </div>
                <div>
                  <strong>${formatMoney(item.productTotalAmount)}</strong>
                  <span>상품 합계</span>
                </div>
              </article>
            `).join("")}
          </div>
        </section>

        <aside class="order-sheet-summary">
          <h3>결제 예정 금액</h3>
          <div><span>전체 상품 금액</span><strong>${formatMoney(totalAmount)}</strong></div>
          <div class="summary-total"><span>최종 결제 금액</span><strong>${formatMoney(totalAmount)}</strong></div>
          <button class="primary-button" type="button" data-confirm-order="${type}">주문하기</button>
        </aside>
      </div>
    </div>
  `);
}

function openModal(html) {
  elements.modalRoot.innerHTML = html;
  elements.modalRoot.hidden = false;
}

function closeModal() {
  elements.modalRoot.hidden = true;
  elements.modalRoot.innerHTML = "";
}

function setModalStatus(message, error = false) {
  const status = document.getElementById("modalStatus");
  if (!status) {
    return;
  }
  status.textContent = message;
  status.classList.toggle("error", error);
}

function setFormStatus(element, message, error = false) {
  if (!element) {
    return;
  }
  element.textContent = message;
  element.classList.toggle("error", error);
}

function updateTextCounter(textareaId, counterId) {
  const textarea = document.getElementById(textareaId);
  const counter = document.getElementById(counterId);
  if (!textarea || !counter) {
    return;
  }
  counter.textContent = `${formatNumber(textarea.value.length)} / ${formatNumber(textarea.maxLength || 1000)}`;
}

function formToObject(form) {
  return Object.fromEntries(new FormData(form).entries());
}

function findCategoryById(categoryId) {
  if (!categoryId) {
    return null;
  }
  for (const category of state.categories) {
    if (category.categoryId === categoryId) {
      return category;
    }
    const child = category.children?.find((item) => item.categoryId === categoryId);
    if (child) {
      return child;
    }
  }
  return null;
}

function changePage(page) {
  if (page < 0 || page === state.page || page >= state.productPage.totalPages) {
    return;
  }
  state.page = page;
  loadProducts();
}

function isOrderable(product) {
  return Boolean(product.orderable ?? (product.status === "ON_SALE" && Number(product.stock) > 0));
}

function statusLabel(product) {
  if (product.orderable === true) {
    return "판매중";
  }
  if (product.status === "ON_SALE") {
    return Number(product.stock) > 0 ? "판매중" : "품절";
  }
  if (product.status === "OUT_OF_STOCK" || product.status === "SOLD_OUT") {
    return "품절";
  }
  if (product.status === "STOPPED") {
    return "판매중지";
  }
  return product.status || "상태 미확인";
}

function getImageUrl(imageUrl) {
  const path = imageUrl || "/images/default-product.png";
  if (path.startsWith("http")) {
    return path;
  }
  return `${API_BASE_URL}${path}`;
}

function productVisual(product) {
  if (product?.imageUrl) {
    const alt = product.name || "상품 이미지";
    return `<img class="product-image" src="${escapeHtml(getImageUrl(product.imageUrl))}" alt="${escapeHtml(alt)}" loading="lazy">`;
  }
  return productIcon(product);
}

function productIcon(product) {
  const icons = [
    '<rect x="78" y="58" width="64" height="112" rx="16"></rect><path d="M96 58V36h28v22"></path><path d="M88 102h44"></path>',
    '<rect x="54" y="78" width="112" height="72" rx="14"></rect><path d="M70 98h80M70 118h80M70 138h80"></path>',
    '<path d="M70 148h86"></path><rect x="70" y="120" width="86" height="28" rx="7"></rect><path d="M92 120V56h36v64M86 56h48"></path>',
    '<rect x="48" y="82" width="124" height="76" rx="12"></rect><path d="M82 82c8-26 48-26 56 0"></path><path d="M76 122h68"></path>',
    '<path d="M76 84h68v72H76z"></path><path d="M84 62h52v22H84zM82 116h56"></path>',
    '<rect x="56" y="62" width="108" height="108" rx="10"></rect><path d="M56 94h108M86 62v32M86 126h48"></path>'
  ];
  const seed = `${product?.name || ""}${product?.productId || ""}`;
  const path = icons[hashString(seed) % icons.length];
  return `<svg viewBox="0 0 220 220" aria-hidden="true">${path}</svg>`;
}

function hashString(value) {
  return Array.from(String(value)).reduce((hash, char) => (
    ((hash << 5) - hash + char.charCodeAt(0)) >>> 0
  ), 0);
}

function syncSearchInputs() {
  elements.globalSearchInput.value = state.query;
  elements.inlineSearchInput.value = state.query;
}

function formatMoney(value) {
  return `${formatNumber(Number(value || 0))}원`;
}

function formatNumber(value) {
  return new Intl.NumberFormat("ko-KR").format(Number(value || 0));
}

function formatPopularKeywordTime(value) {
  if (!value) {
    return "";
  }

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false
  }).format(new Date(value));
}

function formatDate(value) {
  if (!value) {
    return "-";
  }
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  }).format(new Date(value));
}

function formatDateOnly(value) {
  if (!value) {
    return "-";
  }
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit"
  }).format(new Date(value));
}

function orderStatusLabel(status) {
  const labels = {
    PENDING: "주문대기",
    PAID: "주문완료",
    CANCELLED: "주문취소"
  };
  return labels[status] || status || "상태 미확인";
}

function inquiryStatusLabel(status) {
  const labels = {
    WAITING: "답변대기",
    ANSWERED: "답변완료"
  };
  return labels[status] || status || "상태 미확인";
}

function orderStatusClass(status) {
  if (status === "CANCELLED") {
    return "cancelled";
  }
  if (status === "PENDING") {
    return "pending";
  }
  return "paid";
}

function showToast(message) {
  clearTimeout(toastTimer);
  elements.toast.textContent = message;
  elements.toast.hidden = false;
  toastTimer = setTimeout(() => {
    elements.toast.hidden = true;
  }, 3200);
}

function escapeHtml(value) {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

document.addEventListener("change", (event) => {
  const cartToggle = event.target.closest("[data-cart-toggle]");
  if (cartToggle) {
    const cartItemId = Number(cartToggle.dataset.cartToggle);
    if (cartToggle.checked) {
      state.selectedCartIds.add(cartItemId);
    } else {
      state.selectedCartIds.delete(cartItemId);
    }
    renderCart();
    return;
  }

  const selectAll = event.target.closest("[data-cart-select-all]");
  if (selectAll) {
    const items = Array.isArray(state.cart?.items) ? state.cart.items : [];
    state.selectedCartIds = selectAll.checked
      ? new Set(items.filter((item) => item.orderable).map((item) => Number(item.cartItemId)))
      : new Set();
    state.cartSelectionReady = true;
    renderCart();
  }
});

document.addEventListener("click", (event) => {
  const selectedDelete = event.target.closest("[data-cart-delete-selected]");
  if (!selectedDelete) {
    return;
  }
  const ids = Array.from(state.selectedCartIds);
  if (!ids.length) {
    showToast("삭제할 상품을 선택해주세요.");
    return;
  }
  Promise.all(ids.map((cartId) => requestApi(`/api/carts/items/${cartId}`, {
    method: "DELETE",
    auth: true
  })))
    .then(() => {
      state.selectedCartIds = new Set();
      showToast("선택한 상품을 삭제했습니다.");
      return loadCart(false);
    })
    .catch((error) => showToast(error.message));
});

document.addEventListener("input", (event) => {
  if (event.target.id === "inquiryContent") {
    updateTextCounter("inquiryContent", "inquiryCount");
  }
  if (event.target.id === "adminAnswer") {
    updateTextCounter("adminAnswer", "adminAnswerCount");
  }
});

initialize();

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

async function initChatBotMode() {
  state.chatWidget.mode = "bot";
  state.chatWidget.conversationId = Date.now().toString();
  elements.chatWidgetModeLabel.textContent = "AI 챗봇";
  elements.chatWidgetConnectButton.hidden = false;
  
  addChatWidgetMessage("AI 챗봇과 연결 중입니다...", "chat-widget-message system");
  
  try {
    const response = await requestApi("/api/ai/chatbot/welcome");
    state.chatWidget.messages = []; // Clear system message
    addChatWidgetMessage(response, "chat-widget-message from-other");
  } catch (error) {
    state.chatWidget.messages = [];
    addChatWidgetMessage("AI 챗봇에 연결할 수 없습니다.", "chat-widget-message system");
  }
}

async function handleChatWidgetSubmit(event) {
  event.preventDefault();
  const text = elements.chatWidgetInput.value.trim();
  if (!text) return;
  
  elements.chatWidgetInput.value = "";
  
  if (state.chatWidget.mode === "bot") {
    addChatWidgetMessage(text, "chat-widget-message from-user");
    try {
      const response = await requestApi("/api/ai/chatbot", {
        method: "POST",
        body: {
          conversationId: state.chatWidget.conversationId,
          message: text
        }
      });
      addChatWidgetMessage(response.answer, "chat-widget-message from-other");
    } catch (error) {
      addChatWidgetMessage("오류가 발생했습니다: " + error.message, "chat-widget-message system");
    }
  } else if (state.chatWidget.mode === "agent") {
    if (state.chatWidget.socket && state.chatWidget.socket.connected) {
      state.chatWidget.socket.publish({
        destination: `/pub/chat/rooms/${state.chatWidget.chatRoomId}/messages`,
        body: JSON.stringify({ content: text })
      });
    } else {
      addChatWidgetMessage("연결이 끊어졌습니다. 잠시 후 다시 시도해주세요.", "chat-widget-message system");
    }
  }
}

async function connectToAgent() {
  if (!requireAuth()) {
    toggleChatWidget(false);
    return;
  }
  
  elements.chatWidgetConnectButton.hidden = true;
  state.chatWidget.mode = "agent";
  elements.chatWidgetModeLabel.textContent = "상담원 연결";
  
  state.chatWidget.messages = [];
  addChatWidgetMessage("상담원과 연결 중입니다...", "chat-widget-message system");
  
  let chatRoomId;
  let roomStatus;
  try {
    const room = await requestApi("/api/chat/rooms/my", { method: "POST", auth: true });
    chatRoomId = room.chatRoomId;
    roomStatus = room.status;
  } catch (error) {
    try {
      const room = await requestApi("/api/chat/rooms/my", { auth: true });
      chatRoomId = room.chatRoomId;
      roomStatus = room.status;
    } catch (e) {
      addChatWidgetMessage("채팅방을 생성하거나 불러오는데 실패했습니다.", "chat-widget-message system");
      return;
    }
  }
  
  state.chatWidget.chatRoomId = chatRoomId;
  
  if (roomStatus === "COMPLETED") {
    elements.chatWidgetInput.disabled = true;
  } else {
    elements.chatWidgetInput.disabled = false;
  }
  
  try {
    const messages = await requestApi(`/api/chat/rooms/${chatRoomId}/messages`, { auth: true });
    
    state.chatWidget.messages = [];
    let lastId = 0;
    messages.forEach(msg => {
      const type = msg.senderRole === "ADMIN" ? "chat-widget-message from-other" : "chat-widget-message from-user";
      state.chatWidget.messages.push({ text: msg.message, type });
      if (msg.messageId > lastId) lastId = msg.messageId;
    });
    
    state.chatWidget.lastReceivedMessageId = lastId;
    
    if (state.chatWidget.messages.length === 0) {
      addChatWidgetMessage("상담이 시작되었습니다. 메시지를 남겨주세요.", "chat-widget-message system");
    } else {
      renderChatWidgetMessages();
    }
    
    connectStompClient(chatRoomId);
  } catch (error) {
    addChatWidgetMessage("채팅 이력을 불러오는데 실패했습니다.", "chat-widget-message system");
  }
}

function connectStompClient(chatRoomId) {
  if (state.chatWidget.socket) {
    state.chatWidget.socket.deactivate();
  }
  
  const brokerURL = API_BASE_URL.replace(/^http/, 'ws') + '/ws/chat';
  
  const client = new window.StompJs.Client({
    brokerURL: brokerURL,
    connectHeaders: {
      Authorization: `Bearer ${getAccessToken()}`
    },
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
    onConnect: async () => {
      try {
        const url = `/api/chat/rooms/${chatRoomId}/messages?afterMessageId=${state.chatWidget.lastReceivedMessageId}`;
        const messages = await requestApi(url, { auth: true });
        messages.forEach(msg => {
          const type = msg.senderRole === "ADMIN" ? "chat-widget-message from-other" : "chat-widget-message from-user";
          if (msg.messageId > state.chatWidget.lastReceivedMessageId) {
            state.chatWidget.lastReceivedMessageId = msg.messageId;
            addChatWidgetMessage(msg.message, type);
          }
        });
      } catch (e) {
        // Ignore fetch errors during reconnect
      }

      client.subscribe(`/sub/chat/rooms/${chatRoomId}`, (message) => {
        const data = JSON.parse(message.body);
        const type = data.senderRole === "ADMIN" ? "chat-widget-message from-other" : "chat-widget-message from-user";
        if (data.messageId > state.chatWidget.lastReceivedMessageId) {
          state.chatWidget.lastReceivedMessageId = data.messageId;
          addChatWidgetMessage(data.message, type);
        }
      });
      state.chatWidget.subscribed = true;
    },
    onStompError: (frame) => {
      const code = frame.headers['message'];
      if (code === 'COMPLETED_CHAT_ROOM_MESSAGE_NOT_ALLOWED') {
        addChatWidgetMessage("상담이 종료되어 메시지를 보낼 수 없습니다.", "chat-widget-message system");
        elements.chatWidgetInput.disabled = true;
      } else {
        addChatWidgetMessage("메시지 전송에 실패했습니다.", "chat-widget-message system");
      }
    }
  });

  client.activate();
  state.chatWidget.socket = client;
}

// ====== Admin Chat ======

async function loadAdminChatRooms() {
  if (!isAdmin()) return;
  try {
    const rooms = await requestApi("/api/chat/rooms", { auth: true });
    state.adminChat.rooms = rooms || [];
    renderAdminChatRooms();
  } catch (error) {
    showToast("채팅방 목록을 불러오는데 실패했습니다.");
  }
}

function renderAdminChatRooms() {
  if (!elements.adminChatRoomList) return;
  if (state.adminChat.rooms.length === 0) {
    elements.adminChatRoomList.innerHTML = `<div class="empty">진행중인 채팅이 없습니다.</div>`;
    return;
  }
  
  elements.adminChatRoomList.innerHTML = state.adminChat.rooms.map(room => {
    const isActive = room.chatRoomId === state.adminChat.activeRoomId;
    const isCompleted = room.status === "COMPLETED";
    return `
      <div class="admin-chat-room-item ${isActive ? "active" : ""}" data-room-id="${room.chatRoomId}" data-status="${room.status}">
        <div class="room-id">
          방 #${room.chatRoomId}
          <span class="room-status ${isCompleted ? "" : "active"}">${isCompleted ? "종료됨" : "상담중"}</span>
        </div>
        <div class="room-status">회원 ID: ${room.memberId}</div>
      </div>
    `;
  }).join("");
}

async function openAdminChatRoom(chatRoomId, status) {
  state.adminChat.activeRoomId = chatRoomId;
  state.adminChat.lastReceivedMessageId = 0;
  state.adminChat.messages = [];
  
  renderAdminChatRooms();
  
  elements.adminChatRoomTitle.textContent = `채팅방 #${chatRoomId}`;
  
  if (status === "COMPLETED") {
    if (elements.adminChatCompleteBtn) elements.adminChatCompleteBtn.hidden = true;
    if (elements.adminChatInput) elements.adminChatInput.disabled = true;
    if (elements.adminChatSendBtn) elements.adminChatSendBtn.disabled = true;
  } else {
    if (elements.adminChatCompleteBtn) elements.adminChatCompleteBtn.hidden = false;
    if (elements.adminChatInput) elements.adminChatInput.disabled = false;
    if (elements.adminChatSendBtn) elements.adminChatSendBtn.disabled = false;
  }
  
  try {
    const messages = await requestApi(`/api/chat/rooms/${chatRoomId}/messages`, { auth: true });
    let lastId = 0;
    messages.forEach(msg => {
      const type = msg.senderRole === "ADMIN" ? "chat-widget-message from-user" : "chat-widget-message from-other";
      state.adminChat.messages.push({ text: msg.message, type });
      if (msg.messageId > lastId) lastId = msg.messageId;
    });
    
    state.adminChat.lastReceivedMessageId = lastId;
    renderAdminChatMessages();
    connectAdminStompClient(chatRoomId);
  } catch (error) {
    if (elements.adminChatMessages) {
      elements.adminChatMessages.innerHTML = `<div class="empty">메시지를 불러오는데 실패했습니다.</div>`;
    }
  }
}

function renderAdminChatMessages() {
  if (!elements.adminChatMessages) return;
  if (state.adminChat.messages.length === 0) {
    elements.adminChatMessages.innerHTML = `<div class="empty">메시지가 없습니다.</div>`;
    return;
  }
  
  elements.adminChatMessages.innerHTML = state.adminChat.messages.map(msg => `
    <div class="${msg.type}">${escapeHtml(msg.text)}</div>
  `).join("");
  
  elements.adminChatMessages.scrollTop = elements.adminChatMessages.scrollHeight;
}

function addAdminChatMessage(text, type) {
  state.adminChat.messages.push({ text, type });
  renderAdminChatMessages();
}

function connectAdminStompClient(chatRoomId) {
  if (state.adminChat.socket) {
    state.adminChat.socket.deactivate();
  }
  
  const brokerURL = API_BASE_URL.replace(/^http/, 'ws') + '/ws/chat';
  
  const client = new window.StompJs.Client({
    brokerURL: brokerURL,
    connectHeaders: { Authorization: `Bearer ${getAccessToken()}` },
    reconnectDelay: 5000,
    onConnect: async () => {
      client.subscribe(`/sub/chat/rooms/${chatRoomId}`, (message) => {
        if (state.adminChat.activeRoomId !== chatRoomId) return;
        const data = JSON.parse(message.body);
        const type = data.senderRole === "ADMIN" ? "chat-widget-message from-user" : "chat-widget-message from-other";
        if (data.messageId > state.adminChat.lastReceivedMessageId) {
          state.adminChat.lastReceivedMessageId = data.messageId;
          addAdminChatMessage(data.message, type);
        }
      });
      state.adminChat.subscribed = true;
    }
  });

  client.activate();
  state.adminChat.socket = client;
}

async function handleAdminChatSubmit(event) {
  event.preventDefault();
  if (!state.adminChat.activeRoomId) return;
  
  const text = elements.adminChatInput.value.trim();
  if (!text) return;
  
  elements.adminChatInput.value = "";
  
  if (state.adminChat.socket && state.adminChat.socket.connected) {
    state.adminChat.socket.publish({
      destination: `/pub/chat/rooms/${state.adminChat.activeRoomId}/messages`,
      body: JSON.stringify({ content: text })
    });
  } else {
    showToast("연결이 끊어졌습니다. 잠시 후 다시 시도해주세요.");
  }
}

async function completeAdminChat() {
  if (!state.adminChat.activeRoomId) return;
  
  try {
    await requestApi(`/api/chat/rooms/${state.adminChat.activeRoomId}/status`, {
      method: "PATCH",
      auth: true,
      body: { status: "COMPLETED" }
    });
    showToast("상담이 종료되었습니다.");
    loadAdminChatRooms();
    
    if (elements.adminChatCompleteBtn) elements.adminChatCompleteBtn.hidden = true;
    if (elements.adminChatInput) elements.adminChatInput.disabled = true;
    if (elements.adminChatSendBtn) elements.adminChatSendBtn.disabled = true;
  } catch (error) {
    showToast("상담 종료에 실패했습니다.");
  }
}
