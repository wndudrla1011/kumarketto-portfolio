document.addEventListener("DOMContentLoaded", () => {
    // --- 상태 관리 ---
    let currentTransactionId = null;
    let pollingInterval = null;
    const TEST_PRODUCT_ID = 50; // 테스트할 상품 ID
    const API_BASE_URL = "http://localhost:8088";

    // --- 모달 관리 ---
    function openModal() {
        modalOverlay.classList.add("active");
        initializeChatFlow();
    }

    function closeModal() {
        modalOverlay.classList.remove("active");
        const wrapper = document.getElementById("modal-content-wrapper");
        if (wrapper) wrapper.innerHTML = "";
        if (pollingInterval) clearInterval(pollingInterval);
    }

    // --- UI 초기화 및 헬퍼 함수 ---
    function initializeChatFlow() {
        const modalContentWrapper = document.getElementById("modal-content-wrapper");
        modalContentWrapper.innerHTML = `
            <div class="chat-app-container">
                <div class="chat-container">
                    <header class="chat-header"><h3>판매자 화면</h3></header>
                    <main id="seller-messages" class="chat-messages"></main>
                </div>
                <div class="chat-container">
                    <header class="chat-header"><h3>구매자 화면</h3></header>
                    <main id="buyer-messages" class="chat-messages"></main>
                </div>
            </div>`;
        renderInitialUI();
    }

    function addMessage(userType, element) {
        const containerId = userType === "buyer" ? "buyer-messages" : "seller-messages";
        const container = document.getElementById(containerId);
        if (container) {
            container.appendChild(element);
            container.scrollTop = container.scrollHeight;
        }
    }

    function createSystemMessage(text) {
        const div = document.createElement("div");
        div.className = "message system-message";
        div.textContent = text;
        return div;
    }

    function createMessageBubble(templateId, userType) {
        const template = document.getElementById(templateId);
        if (!template) {
            console.error(`Template with id "${templateId}" not found.`);
            return createSystemMessage("UI 템플릿을 찾을 수 없습니다.");
        }
        const bubble = document.importNode(template.content, true);
        const wrapper = document.createElement("div");
        wrapper.className = userType === "buyer" ? "message message-buyer" : "message message-seller";
        wrapper.appendChild(bubble);
        return wrapper;
    }

    // --- API 호출 헬퍼 ---
    async function apiCall(endpoint, options = {}) {
        try {
            const response = await fetch(`${API_BASE_URL}${endpoint}`, {
                ...options,
                headers: { "Content-Type": "application/json", ...options.headers },
                credentials: "include",
            });
            if (!response.ok) {
                if (response.redirected) {
                    alert("로그인이 필요합니다. 로그인 페이지로 이동합니다.");
                    window.location.href = response.url;
                    throw new Error("Redirected to login page");
                }
                const errorText = await response.text();
                throw new Error(`API Error ${response.status}: ${errorText}`);
            }
            if (response.headers.get("content-type")?.includes("application/json")) {
                return response.json();
            }
        } catch (error) {
            console.error("API Error:", error);
            if (error.message !== "Redirected to login page") {
                alert(`오류가 발생했습니다: ${error.message}`);
            }
            throw error;
        }
    }

    // --- 각 단계별 핸들러 함수들 ---

    // 1. 구매 요청
    async function handlePurchaseRequest(e) {
        e.target.disabled = true;
        addMessage("buyer", createSystemMessage("구매 요청 중..."));
        try {
            const data = await apiCall("/api/transactions", {
                method: "POST",
                body: JSON.stringify({ productId: TEST_PRODUCT_ID }),
            });
            currentTransactionId = data.transactionId;
            addMessage("buyer", createSystemMessage("구매를 요청했습니다."));
            addMessage("seller", createSystemMessage("구매 요청이 도착했습니다."));
            renderSellerApprovalUI();
        } catch (error) {
            e.target.disabled = false;
            addMessage("buyer", createSystemMessage("구매 요청에 실패했습니다."));
        }
    }

    // 2. 판매자 승인
    async function handleApproval(e) {
        if (e.target.tagName !== "BUTTON") return;
        const status = e.target.dataset.action === "approve" ? "APPROVED" : "REJECTED";
        e.currentTarget.closest(
            ".message"
        ).innerHTML = `<div class="content"><p class="system-message">처리 중...</p></div>`;
        try {
            await apiCall(`/api/transactions/${currentTransactionId}/approval`, {
                method: "PATCH",
                body: JSON.stringify({ status }),
            });
            const message = status === "APPROVED" ? "승인" : "거절";
            addMessage("seller", createSystemMessage(`거래를 ${message}했습니다.`));
            addMessage("buyer", createSystemMessage(`판매자가 거래를 ${message}했습니다.`));
            if (status === "APPROVED") renderDeliveryChoiceUI();
        } catch (error) {
            addMessage("seller", createSystemMessage(`처리 실패: ${error.message}`));
        }
    }

    // 3-A. 거래 방식 선택
    function handleDeliveryChoice(e) {
        if (e.target.tagName !== "BUTTON") return;
        const deliveryType = e.target.dataset.deliveryType;
        e.currentTarget.closest(
            ".message"
        ).innerHTML = `<div class="content"><p class="system-message">처리 중...</p></div>`;
        if (deliveryType === "DELIVERY_SERVICE") {
            updateTransactionTypeAndProceed(deliveryType, "CARD");
        } else {
            renderPaymentChoiceUI();
        }
    }

    // 3-B. 결제 방식 선택
    function handlePaymentChoice(e) {
        if (e.target.tagName !== "BUTTON") return;
        const paymentType = e.target.dataset.paymentType;
        e.currentTarget.closest(
            ".message"
        ).innerHTML = `<div class="content"><p class="system-message">처리 중...</p></div>`;
        updateTransactionTypeAndProceed("DIRECT_TRADE", paymentType);
    }

    // 3-C. 서버에 타입 업데이트 및 다음 단계 진행
    async function updateTransactionTypeAndProceed(deliveryService, paymentMethod) {
        try {
            await apiCall(`/api/transactions/${currentTransactionId}/type`, {
                method: "PATCH",
                body: JSON.stringify({ deliveryService, paymentMethod }),
            });
            if (paymentMethod === "CARD") {
                addMessage("buyer", createSystemMessage("카드 결제를 선택했습니다."));
                renderBuyerPaymentUI();
            } else {
                addMessage("buyer", createSystemMessage("현금 결제를 선택했습니다."));
                addMessage("seller", createSystemMessage("구매자가 현금 결제를 선택했습니다. 거래를 준비해주세요."));
                renderBuyerConfirmUI();
            }
        } catch (error) {
            addMessage("buyer", createSystemMessage("처리 중 오류가 발생했습니다."));
        }
    }

    // 4. (카드) 결제 시작
    function handlePaymentInit(e) {
        e.target.disabled = true;
        window.open(`/payment?transactionId=${currentTransactionId}`, "paymentPopup", "width=500,height=700");
        addMessage("buyer", createSystemMessage("결제 팝업창을 열었습니다."));
        addMessage("seller", createSystemMessage("구매자가 결제를 진행 중입니다."));
        startPollingForPaymentStatus();
    }

    // 5. (카드) 결제 상태 폴링
    function startPollingForPaymentStatus() {
        if (pollingInterval) clearInterval(pollingInterval);
        pollingInterval = setInterval(async () => {
            try {
                const transaction = await apiCall(`/api/transactions/${currentTransactionId}`);
                if (transaction.status === "PAID") {
                    clearInterval(pollingInterval);
                    pollingInterval = null;
                    addMessage("buyer", createSystemMessage("결제가 성공적으로 확인되었습니다."));
                    addMessage("seller", createSystemMessage("구매자의 결제가 확인되었습니다."));

                    if (transaction.deliveryService === "DELIVERY_SERVICE") {
                        renderSellerShippingUI();
                    } else {
                        renderBuyerConfirmUI();
                    }
                }
            } catch (error) {
                console.error("결제 상태 확인 중 오류:", error);
                clearInterval(pollingInterval);
                addMessage("buyer", createSystemMessage("결제 상태 확인에 실패했습니다."));
            }
        }, 3000);
    }

    // 6. 운송장 입력 모달 열기 및 처리
    function handleShippingInputModal() {
        const template = document.getElementById("shipping-input-modal-template");
        const modalNode = document.importNode(template.content, true);
        const shippingModal = modalNode.querySelector(".shipping-modal-overlay");
        document.body.appendChild(shippingModal);

        const form = shippingModal.querySelector("#shipping-form");
        form.addEventListener("submit", async (e) => {
            e.preventDefault();
            const courier = shippingModal.querySelector("#courier-select").value;
            const trackingNumber = shippingModal.querySelector("#tracking-number-input").value;
            const errorMessageDiv = shippingModal.querySelector("#shipping-error-message");
            if (!courier || !trackingNumber) {
                errorMessageDiv.textContent = "택배사와 운송장 번호를 모두 입력해주세요.";
                return;
            }
            try {
                errorMessageDiv.textContent = "";
                shippingModal.querySelector("#shipping-confirm-btn").disabled = true;

                await apiCall(`/api/transactions/${currentTransactionId}/shipment`, {
                    method: "POST",
                    body: JSON.stringify({ courier, trackingNumber }),
                });

                document.body.removeChild(shippingModal);
                addMessage("seller", createSystemMessage("운송장 정보를 등록했습니다."));
                addMessage("buyer", createSystemMessage("판매자가 상품을 발송했습니다."));
                renderBuyerConfirmUI();
            } catch (error) {
                errorMessageDiv.textContent = `등록 실패: ${error.message}`;
                shippingModal.querySelector("#shipping-confirm-btn").disabled = false;
            }
        });
        shippingModal.querySelector("#shipping-cancel-btn").addEventListener("click", () => {
            document.body.removeChild(shippingModal);
        });
    }

    // 7. 구매 확정
    async function handlePurchaseConfirm(e) {
        e.target.disabled = true;
        const messageBubble = e.target.closest(".message");
        messageBubble.innerHTML = `<div class="content"><p class="system-message">구매 확정 처리 중...</p></div>`;

        try {
            // alert 대신 실제 백엔드 API를 호출
            await apiCall(`/api/transactions/${currentTransactionId}/confirm`, {
                method: "POST",
            });

            // API 호출 성공 시, 최종 메시지 표시
            addMessage("buyer", createSystemMessage("거래가 성공적으로 완료되었습니다."));
            addMessage("seller", createSystemMessage("구매자가 구매를 확정하여 거래가 종료되었습니다."));
        } catch (error) {
            // 실패 시 에러 메시지를 표시
            addMessage("buyer", createSystemMessage(`구매 확정 실패: ${error.message}`));
            // 원래 버튼으로 되돌리는 로직 (선택사항)
            messageBubble.innerHTML = ""; // 기존 메시지 삭제
            renderBuyerConfirmUI(); // 버튼 UI 다시 렌더링
        }
    }

    // --- UI 렌더링 함수들 ---
    function renderInitialUI() {
        const bubble = createMessageBubble("buyer-request-template", "buyer");
        bubble.querySelector("button").addEventListener("click", handlePurchaseRequest);
        addMessage("buyer", bubble);
    }

    function renderSellerApprovalUI() {
        const bubble = createMessageBubble("seller-approval-template", "seller");
        bubble.addEventListener("click", handleApproval);
        addMessage("seller", bubble);
    }

    function renderDeliveryChoiceUI() {
        const bubble = createMessageBubble("buyer-delivery-choice-template", "buyer");
        bubble.addEventListener("click", handleDeliveryChoice);
        addMessage("buyer", bubble);
    }

    function renderPaymentChoiceUI() {
        const bubble = createMessageBubble("buyer-payment-choice-template", "buyer");
        bubble.addEventListener("click", handlePaymentChoice);
        addMessage("buyer", bubble);
    }

    function renderBuyerPaymentUI() {
        const bubble = createMessageBubble("buyer-payment-template", "buyer");
        bubble.querySelector("button").addEventListener("click", handlePaymentInit);
        addMessage("buyer", bubble);
    }

    function renderSellerShippingUI() {
        const bubble = createMessageBubble("seller-shipping-template", "seller");
        bubble.querySelector("button").addEventListener("click", handleShippingInputModal);
        addMessage("seller", bubble);
    }

    function renderBuyerConfirmUI() {
        const bubble = createMessageBubble("buyer-confirm-template", "buyer");
        bubble.querySelector("button").addEventListener("click", handlePurchaseConfirm);
        addMessage("buyer", bubble);
    }
});
