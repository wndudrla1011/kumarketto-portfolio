document.addEventListener("DOMContentLoaded", () => {
    const userNo = document.body.dataset.userNo;
    const imageForm = document.getElementById("image-form");
    const textForm = document.getElementById("text-form");
    const imagePreview = document.getElementById("image-preview");
    const imageFileInput = document.getElementById("profileImageFile");
    const descriptionInput = document.getElementById("description");
    const profileImageUrlInput = document.getElementById("profileImage");
    const goToStoreBtn = document.getElementById("go-to-store-btn");
    const successMessage = document.getElementById("success-message");
    const errorMessage = document.getElementById("error-message");

    async function loadStoreInfo() {
        if (!userNo) return;
        goToStoreBtn.href = `/stores/${userNo}`;
        try {
            const response = await fetch(`/api/stores/${userNo}`);
            if (!response.ok) {
                imagePreview.innerHTML = `<span>画像<br>選択</span>`;
                return;
            }

            const data = await response.json();
            descriptionInput.value = data.description || "";
            profileImageUrlInput.value = data.profileImageUrl || "";
            if (data.profileImageUrl) {
                imagePreview.innerHTML = `<img src="${data.profileImageUrl}" alt="">`;
            } else {
                imagePreview.innerHTML = `<span>画像<br>選択</span>`;
            }
        } catch (error) {
            console.error("상점 정보 로딩 실패:", error);
        }
    }

    imageFileInput.addEventListener("change", () => {
        const file = imageFileInput.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onload = (e) => {
                imagePreview.innerHTML = `<img src="${e.target.result}" alt="">`;
            };
            reader.readAsDataURL(file);
        }
    });

    imageForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        hideMessages();

        const file = imageFileInput.files[0];
        if (!file) {
            showMessage("error", "업로드할 이미지 파일을 선택해주세요.");
            return;
        }

        const formData = new FormData();
        formData.append("profileImageFile", file);

        try {
            const response = await fetch(`/api/stores/${userNo}/profile-image`, {
                method: "POST",
                body: formData,
            });

            if (!response.ok) {
                const errorData = await response.text();
                throw new Error(errorData || "이미지 업로드에 실패했습니다.");
            }

            const result = await response.json();
            profileImageUrlInput.value = result.profileImageUrl;
            showMessage("success", "プロフィール画像のアップロードが正常に完了しました！");
        } catch (error) {
            showMessage("error", `오류: ${error.message}`);
        }
    });

    textForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        hideMessages();

        const textData = {
            description: descriptionInput.value,
            profileImage: profileImageUrlInput.value,
        };

        try {
            const response = await fetch(`/api/stores/${userNo}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(textData),
            });

            if (!response.ok) throw new Error(await response.text());

            showMessage("success", "紹介文が正常に保存されました！ 2秒後にマイストアページへ移動します。");
            setTimeout(() => {
                window.location.href = `/stores/${userNo}`;
            }, 2000);
        } catch (error) {
            showMessage("error", `오류: ${error.message}`);
        }
    });

    function showMessage(type, text) {
        const el = type === "success" ? successMessage : errorMessage;
        el.textContent = text;
        el.style.display = "block";
    }
    function hideMessages() {
        successMessage.style.display = "none";
        errorMessage.style.display = "none";
    }

    loadStoreInfo();
});
