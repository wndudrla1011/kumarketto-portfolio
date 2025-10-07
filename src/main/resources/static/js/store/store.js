document.addEventListener("DOMContentLoaded", () => {
    // --- State Management Object ---
    const state = {
        product: { currentPage: 0, isLoading: false, isLastPage: false, status: "", sort: "modifiedDate,desc" },
        review: {
            currentPage: 0,
            isLoading: false,
            isLastPage: false,
            hasPhoto: false,
            sort: "createdDate,desc",
            role: "SELLER",
        },
        isReviewTabLoaded: false,
    };

    // --- DOM Element References ---
    const pathSegments = window.location.pathname.split("/");
    const userNo = pathSegments.pop() || pathSegments.pop();
    const body = document.body;
    const isOwner = body.dataset.isOwner === "true";
    const storeCard = document.getElementById("store-card");
    const tabs = document.querySelectorAll(".tab");
    const panelItems = document.getElementById("panel-items");
    const panelReviews = document.getElementById("panel-reviews");
    const productGrid = document.getElementById("product-grid");
    const reviewList = document.getElementById("review-list");
    const productLoader = document.getElementById("product-loader");
    const reviewLoader = document.getElementById("review-loader");
    const productSortSelect = document.getElementById("product-sort");
    const productStatusButtons = document.querySelectorAll(".p-status");
    const reviewSortSelect = document.getElementById("review-sort");
    const reviewPhotoCheckbox = document.getElementById("review-photo-only");
    const reviewTabs = document.querySelectorAll(".sub-tab");

    // --- Initialization ---
    if (!userNo || isNaN(userNo)) {
        storeCard.innerHTML =
            '<div class="error-message">ショップ情報を読み込めませんでした。（ユーザーIDが正しくありません）</div>';
        return;
    }
    fetchStoreInfo(userNo);
    fetchProducts();

    // --- Event Listeners ---
    productSortSelect.addEventListener("change", () =>
        handleFilterChange("product", { sort: productSortSelect.value })
    );
    reviewSortSelect.addEventListener("change", () => handleFilterChange("review", { sort: reviewSortSelect.value }));
    reviewPhotoCheckbox.addEventListener("change", () =>
        handleFilterChange("review", { hasPhoto: reviewPhotoCheckbox.checked })
    );

    productStatusButtons.forEach((button) => {
        button.addEventListener("click", () => {
            productStatusButtons.forEach((btn) => btn.classList.remove("active"));
            button.classList.add("active");
            handleFilterChange("product", { status: button.dataset.status });
        });
    });

    tabs.forEach((t) => {
        t.addEventListener("click", () => {
            tabs.forEach((x) => x.classList.remove("active"));
            t.classList.add("active");
            const key = t.dataset.key;

            panelItems.style.display = key === "items" ? "block" : "none";
            panelReviews.style.display = key === "reviews" ? "block" : "none";

            if (key === "reviews" && !state.isReviewTabLoaded) {
                state.isReviewTabLoaded = true;
                fetchReviews();
                reviewObserver.observe(reviewLoader);
            }
        });
    });

    reviewTabs.forEach((tab) => {
        tab.addEventListener("click", () => {
            const newRole = tab.dataset.role;
            if (state.review.role === newRole) return;

            // 탭 활성화 상태 변경
            reviewTabs.forEach((t) => t.classList.remove("active"));
            tab.classList.add("active");

            // handleFilterChange를 호출하여 리뷰 목록을 새로고침
            handleFilterChange("review", { role: newRole });
        });
    });

    // --- Filter Change Handler ---
    function handleFilterChange(type, newState) {
        Object.assign(state[type], { currentPage: 0, isLastPage: false, ...newState });

        const container = type === "product" ? productGrid : reviewList;
        container.innerHTML = "";

        const loader = type === "product" ? productLoader : reviewLoader;
        const observer = type === "product" ? productObserver : reviewObserver;
        loader.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> 読み込み中...';
        observer.observe(loader);

        if (type === "product") fetchProducts();
        else if (type === "review") fetchReviews();
    }

    // --- Intersection Observers for Infinite Scroll ---
    const createObserver = (loader, fetchFunction) => {
        return new IntersectionObserver(
            (entries) => {
                if (entries[0].isIntersecting) fetchFunction();
            },
            { threshold: 0.8 }
        );
    };
    const productObserver = createObserver(productLoader, fetchProducts);
    productObserver.observe(productLoader);
    const reviewObserver = createObserver(reviewLoader, fetchReviews);

    // --- Data Fetching Functions ---
    async function fetchProducts() {
        const s = state.product;
        if (s.isLoading || s.isLastPage) return;
        s.isLoading = true;
        productLoader.style.display = "block";

        const params = new URLSearchParams({ page: s.currentPage, size: 12, sort: s.sort });
        if (s.status) params.append("status", s.status);

        try {
            const response = await fetch(`/api/stores/${userNo}/products?${params.toString()}`);
            if (!response.ok) throw new Error("상품 목록 로딩 실패");
            const data = await response.json();

            if (data.empty && s.currentPage === 0) {
                productGrid.innerHTML = '<div class="empty-message">販売中の商品はありません。</div>';
                productLoader.style.display = "none";
                productObserver.unobserve(productLoader);
            } else {
                data.content.forEach((product) => productGrid.appendChild(createProductCard(product)));
            }

            s.currentPage++;
            s.isLastPage = data.last;

            if (s.isLastPage) {
                productLoader.textContent = "最後の商品です。";
                productObserver.unobserve(productLoader);
            }
        } catch (error) {
            console.error(error);
            productLoader.textContent = "商品を読み込めませんでした。";
        } finally {
            s.isLoading = false;
            if (!s.isLastPage) productLoader.style.display = "none";
        }
    }

    async function fetchReviews() {
        const s = state.review;
        if (s.isLoading || s.isLastPage) return;
        s.isLoading = true;
        reviewLoader.style.display = "block";

        const params = new URLSearchParams({
            page: s.currentPage,
            size: 10,
            sort: s.sort,
            hasPhoto: s.hasPhoto,
            role: s.role,
        });

        try {
            const response = await fetch(`/api/stores/${userNo}/reviews?${params.toString()}`);
            if (!response.ok) throw new Error("리뷰 목록 로딩 실패");
            const data = await response.json();

            if (data.empty && s.currentPage === 0) {
                reviewList.innerHTML = '<div class="empty-message">ショップ情報の読み込みに失敗しました。</div>';
                reviewLoader.style.display = "none";
                reviewObserver.unobserve(reviewLoader);
            } else {
                data.content.forEach((review) => reviewList.appendChild(createReviewCard(review)));
            }

            s.currentPage++;
            s.isLastPage = data.last;

            if (s.isLastPage) {
                reviewLoader.textContent = "最後のレビューです。";
                reviewObserver.unobserve(reviewLoader);
            }
        } catch (error) {
            console.error(error);
            reviewLoader.textContent = "レビューの読み込みに失敗しました。";
        } finally {
            s.isLoading = false;
            if (!s.isLastPage) reviewLoader.style.display = "none";
        }
    }

    // --- UI Element Creation and Update ---
    function createProductCard(product) {
        const article = document.createElement("article");
        article.className = "card";
        const statusMap = { NEW: "新規", SOLDOUT: "販売完了", RESERVED: "取引中", REPORTED: "通報商品" };
        const statusText = statusMap[product.status] || "";
        const statusBadge = statusText ? `<div class="status-badge">${statusText}</div>` : "";
        article.innerHTML = `<a href="/product/detail/${product.pid}"><div class="thumb"><img src="${
            product.imageUrl || "https://via.placeholder.com/300"
        }" alt="${product.name}" />${statusBadge}</div><div class="card-body"><div class="title">${
            product.name
        }</div><div class="price">${product.price.toLocaleString()}円</div></div></a>`;
        return article;
    }

    function createReviewCard(review) {
        const div = document.createElement("div");
        div.className = "review-card";
        const stars = "★".repeat(review.score) + "☆".repeat(5 - review.score);
        div.innerHTML = `<div class="review-avatar"><img src="${
            review.reviewerImageUrl || "/images/avatars/basic_review_profile.png"
        }" alt="리뷰 작성자 아바타"></div><div class="review-body"><div class="author">${
            review.reviewerNickname
        }</div><div class="meta"><span class="stars">${stars}</span> ·<span>${new Date(
            review.createdDate
        ).toLocaleDateString()}</span></div><div class="content">${
            review.content
        }</div><div class="product-info"><strong>取引商品:</strong> ${review.productName}</div></div>`;
        return div;
    }

    async function fetchStoreInfo(userNo) {
        try {
            const response = await fetch(`/api/stores/${userNo}`);
            if (!response.ok) throw new Error(`서버 응답 오류: ${response.status}`);
            const storeData = await response.json();
            updateStoreProfile(storeData);
        } catch (error) {
            console.error("상점 정보 로딩 실패:", error);
            storeCard.innerHTML = `<div class="error-message">ショップ情報の読み込みに失敗しました。</div>`;
        }
    }

    function updateStoreProfile(data) {
        const stats = data.stats;
        const averageRating = stats.averageRating > 0 ? stats.averageRating.toFixed(1) : "N/A";

        const actionsHtml = isOwner
            ? `<a href="/my-store" class="btn primary"><i class="fa-solid fa-store"></i> ショップを飾る</a>
               <a href="/product/write" class="btn"><i class="fa-solid fa-pen-to-square"></i> 商品登録</a>`
            : `<button class="btn"><i class="fa-regular fa-heart"></i> フォロー</button>
               <button class="btn primary"><i class="fa-regular fa-comments"></i> チャット</button>`;

        storeCard.innerHTML = `
            <div class="avatar">
                <img id="store-avatar" alt="avatar" src="${
                    data.profileImageUrl || "https://via.placeholder.com/128"
                }" />
            </div>
            <div class="store-info">
                <div id="store-name" class="store-name">${data.nickname}</div>
                <div id="store-description" class="store-description">${
                    data.description || "紹介文がありません。"
                }</div>
                <div class="store-stats">
                    <span id="store-meta-products"><i class="fa-solid fa-box-open"></i> 商品 ${
                        stats.totalProductCount
                    }</span>
                    <span id="store-meta-transactions"><i class="fa-solid fa-check-circle"></i> 取引数 ${
                        stats.transactionCount
                    }</span>
                    <span id="store-meta-rating"><i class="fa-solid fa-star"></i> ${averageRating} (${
            stats.reviewCount
        })</span>
                </div>
                <div class="store-actions">
                    ${actionsHtml}
                </div>
            </div>
        `;
        document.title = `${data.nickname}さんのショップ | クマケット`;
    }
});
