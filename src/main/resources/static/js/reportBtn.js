document.addEventListener("DOMContentLoaded", () => {
    // --- 비공개 처리 버튼 로직 ---
    const hidePostBtn = document.getElementById("hidePostBtn");
    if (hidePostBtn) {
        // 이 버튼은 HTML 렌더링 시점에 이미 관리자나 소유자에게만 보이도록 처리되었으므로,
        // JavaScript에서는 버튼이 존재하는지만 확인하고 바로 클릭 이벤트를 연결합니다.
        hidePostBtn.addEventListener("click", () => {
            const productId = hidePostBtn.dataset.productId;
            const reportId = hidePostBtn.dataset.reportId;

            if (!confirm("この投稿を非公開にしますか。")) {
                return;
            }

            const params = new URLSearchParams();
            params.append("productId", productId);
            if (reportId) {
                params.append("reportId", reportId);
            }

            fetch("/report/hide", {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: params,
            }).then((response) => {
                if (response.ok) {
                    alert("非公開に設定されました。");
                    location.reload();
                } else if (response.status === 403) {
                    alert("権限がありません。");
                } else {
                    alert("失敗しました。");
                }
            });
        });
    }
});
