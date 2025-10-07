document.addEventListener("DOMContentLoaded", () => {
    const messagesContainer = document.querySelector(".chat-messages");

    // 채팅창을 항상 맨 아래로 스크롤
    function scrollToBottom() {
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    scrollToBottom();
});
