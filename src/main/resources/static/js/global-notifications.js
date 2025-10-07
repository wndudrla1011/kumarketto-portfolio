document.addEventListener("DOMContentLoaded", () => {
    // 이 스크립트는 사용자가 로그인했을 때만 실행되어야 합니다.
    // 웹소켓 연결을 시도하는 것으로 이를 확인할 수 있습니다.
    // 서버의 인터셉터가 세션이 없으면 연결을 거부할 것입니다.
    connectGlobalWebSocket();
});

let globalWebSocket = null;

function connectGlobalWebSocket() {
    // 여러 개의 연결을 생성하는 것을 방지합니다.
    if (globalWebSocket && globalWebSocket.readyState === WebSocket.OPEN) {
        return;
    }

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.host;
    globalWebSocket = new WebSocket(`${protocol}//${host}/ws/chat`);

    globalWebSocket.onopen = () => {
        console.log("알림을 위한 전역 웹소켓 연결이 수립되었습니다.");
    };

    globalWebSocket.onmessage = (event) => {
        // 만약 현재 페이지가 채팅 페이지라면 아무것도 하지 않습니다.
        // 채팅 페이지 자체의 웹소켓 로직이 메시지를 처리할 것입니다.
        if (window.location.pathname.endsWith('/chat')) {
            return;
        }

        const message = JSON.parse(event.data);

        // 시스템 메시지가 아닌, 수신된 메시지에 대해서만 알림을 표시하고 싶습니다.
        if (message && message.chatId && message.senderId) {
            // 상대방 닉네임과 같은 추가 정보가 필요합니다.
            // 서버에서 "알림" 전용 페이로드를 보내는 것이 더 최적화된 방법이겠지만,
            // 지금은 채팅방 상세 정보를 가져오는 것이 안정적인 방법입니다.
            fetchChatRoomAndShowNotification(message);
        }
    };

    globalWebSocket.onclose = () => {
        console.log("전역 웹소켓 연결이 닫혔습니다. 5초 후 재연결을 시도합니다...");
        setTimeout(connectGlobalWebSocket, 5000); // 간단한 재연결 로직
    };

    globalWebSocket.onerror = (error) => {
        console.error("전역 웹소켓 오류:", error);
        globalWebSocket.close(); // onclose의 재연결 로직을 트리거합니다.
    };
}

async function fetchChatRoomAndShowNotification(message) {
    try {
        // 모든 채팅방 목록을 가져와서 수신된 메시지의 chatId와 일치하는 방을 찾습니다.
        const response = await fetch(`${window.location.origin}/api/chat/my-rooms`);
        if (!response.ok) return;

        const rooms = await response.json();
        const relevantRoom = rooms.find(room => room.chatId === message.chatId);

        if (relevantRoom) {
            showNotification(relevantRoom, message);
        }
    } catch (error) {
        console.error("알림을 위한 채팅방 정보 조회 실패:", error);
    }
}


/**
 * 알림 메시지 내용을 메시지 타입에 따라 가공하는 함수
 * MessageType enum의 모든 케이스를 처리하도록 확장되었습니다.
 */
function formatNotificationContent(messageData) {
    try {
        // 일부 시스템 메시지는 content에 JSON 형식의 추가 정보를 담고 있습니다.
        const data = (messageData.content && (messageData.content.startsWith('{') || messageData.content.startsWith('[')))
            ? JSON.parse(messageData.content)
            : null;

        switch (messageData.messageType) {
            case 'TEXT':
                return messageData.content;
            case 'IMAGE':
                return '写真を送りました。';

            // --- システム 메시지 타입 ---
            case 'TRANSACTION_REQUEST':
                return `${data?.buyerNickname || '相手'}様から購入リクエストが届きました。`;
            case 'TRANSACTION_TYPE_SELECT':
                return '取引方法（直接取引/宅配便）を選択してください。';
            case 'PAYMENT_METHOD_SELECT':
                return 'お支払い方法を選択してください。';
            case 'SHIPPING_INFO_REQUEST':
                return 'お支払いが完了しました。送り状情報を入力してください。';
            case 'CASH_PAYMENT_SELECTED':
                return '相手が現地決済を選択しました。約束を調整してください。';
            case 'ITEM_RECEIVED_CHECK':
                return '販売者が商品を発送しました。受け取り確認をしてください。';
            case 'PURCHASE_CONFIRM_REQUEST':
                return '商品を受け取りましたか？購入確定を進めてください。';
            case 'REVIEW_REQUEST':
                return '取引が完了しました。レビューを残してください！';

            default:
                // 위에 정의되지 않은 다른 타입이나 예외 상황을 위한 기본 메시지
                return '新しい通知が届きました。';
        }
    } catch (e) {
        console.error("알림 메시지 파싱 오류:", e, "원본 메시지:", messageData);
        // 오류 발생 시 안전한 기본 메시지 반환
        return "새로운 알림을 확인하세요.";
    }
}





function showNotification(roomData, messageData) {
    let container = document.getElementById('notification-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'notification-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = 'notification-toast';

    // 알림을 클릭하면 해당 채팅방으로 이동합니다.
    toast.onclick = () => {
        window.location.href = `${window.location.origin}/chat?chatId=${roomData.chatId}`;
    };

    const time = new Date(messageData.createdDate);
    const timeString = time.getHours().toString().padStart(2, '0') + ':' + time.getMinutes().toString().padStart(2, '0');
    const placeholderImg = "https://placehold.co/100x100/E9ECEF/ADB5BD?text=Kumarket";

    const content = formatNotificationContent(messageData);

        toast.innerHTML = `
            <img class="notification-img" src="${roomData.productImageUrl || placeholderImg}" alt="Product Image" onerror="this.src='${placeholderImg}'">
            <div class="notification-details">
                <div class="notification-header">
                    <span class="notification-nickname">${roomData.opponentNickname}</span>
                    <span class="notification-time">${timeString}</span>
                </div>
                <div class="notification-content">${content}</div>
            </div>
        `;

    container.appendChild(toast);

    // 7초 후에 알림이 자동으로 사라지게 합니다.
    setTimeout(() => {
        toast.classList.add('closing');
        // 애니메이션이 끝나면 DOM에서 요소를 제거합니다.
        toast.addEventListener('animationend', () => {
            toast.remove();
        });
    }, 7000);
}