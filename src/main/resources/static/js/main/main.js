document.addEventListener("DOMContentLoaded", () => {
    // --- 검색 관련 요소 ---
    const searchContainer = document.querySelector(".search-container");
    const searchBar = document.getElementById("search-bar");
    const searchButton = document.getElementById("search-button");
    const recentSearchesContainer = document.getElementById("recent-searches");
    const recentSearchesList = document.getElementById("recent-searches-list");
    const clearSearchesBtn = document.getElementById("clear-searches");

    // --- 필터 관련 요소 ---
    const regionSelect = document.getElementById("region");
    const prefectureSelect = document.getElementById("prefecture");
    const municipalityContainer = document.getElementById("municipality-container");
    const resetFiltersBtn = document.getElementById("reset-filters-btn");

    // 가격 범위 슬라이더 및 표시
    const priceRangeSlider = document.getElementById("price-range"); // 기존 priceRange
    const priceValueDisplay = document.getElementById("price-value"); // 기존 priceValue

    // 카테고리 (아코디언 기능과 필터 선택을 함께 관리)
    const categoryItems = document.querySelectorAll(".category-item");
    // <select id="sub-category">가 HTML에 있다면 여기에 추가:
    const subCategorySelect = document.getElementById("sub-category"); // 이 ID가 없으면 null 됨

    // --- 무한 스크롤 관련 요소 ---
    const productGrid = document.querySelector(".product-grid");
    const loaderContainer = document.querySelector(".loader-container");

    // --- 초기화 및 상태 변수 ---
    let currentPage = -1;
    let totalPages = 1;
    let isLoading = false;
    let currentSearchUrl = "/api/products"; // 필터와 검색어가 적용된 기본 URL

    // --- 정렬 ---
    const sortBySelect = document.getElementById("sort-by");

    // --- 초기 상품 로드 (필터 적용된 초기 상태로 로드) ---
    // 페이지 로드 시에는 항상 0페이지부터 시작
    loadProducts(0, currentSearchUrl);

    // --- 이벤트 리스너 ---

    // 1. 검색창 포커스 시 최근 검색어 표시
    if (searchBar) {
        searchBar.addEventListener("focus", showRecentSearches);

        // 2. 검색창에서 Enter 키를 누르면 필터/검색 실행
        searchBar.addEventListener("keypress", async (e) => {
            if (e.key === "Enter") {
                e.preventDefault(); // 기본 폼 제출 방지
                const keyword = searchBar.value.trim();

                await saveSearchKeyword(keyword); // 검색어 저장
                resetAndApplyFilters(); // 필터 적용

                recentSearchesContainer.style.display = "none"; // 검색 후 목록 숨기기
            }
        });
    }

    if (searchButton) {
        searchButton.addEventListener("click", async () => {
            const keyword = searchBar.value.trim();

            await saveSearchKeyword(keyword); // 검색어 저장
            resetAndApplyFilters(); // 필터 적용

            recentSearchesContainer.style.display = "none"; // 검색 후 목록 숨기기
        });
    }

    // 3-1 최근 검색어 전체 삭제
    if (clearSearchesBtn) {
        clearSearchesBtn.addEventListener("click", clearAllSearches);
    }

    // 3-2 최근 검색어 단건 삭제
    if (recentSearchesList) {
        recentSearchesList.addEventListener("click", async (e) => {
            const target = e.target;

            // 삭제 버튼을 클릭한 경우
            if (target.matches(".delete-search-btn")) {
                const searchId = target.dataset.id;
                if (searchId) {
                    await deleteOneSearch(searchId);
                    // 해당 항목을 삭제
                    target.closest("li").remove();
                }
            }
            // 최근 검색어 텍스트를 클릭한 경우
            else if (target.matches(".keyword-text")) {
                const keyword = target.textContent;
                searchBar.value = keyword;
                if (keyword) await saveSearchKeyword(keyword);
                resetAndApplyFilters();
                recentSearchesContainer.style.display = "none";
            }
        });
    }

    // 4. 검색창 외부 클릭 시 최근 검색어 목록 숨기기
    document.addEventListener("click", (e) => {
        // 스크립트로 만든 가짜 클릭(isTrusted: false)은 무시
        if (!e.isTrusted) {
            return;
        }

        if (searchContainer && !searchContainer.contains(e.target)) {
            recentSearchesContainer.style.display = "none";
        }
    });

    // 5. 가격 범위 슬라이더 변경 (input은 실시간, change는 드래그 끝났을 때)
    if (priceRangeSlider) {
        priceRangeSlider.addEventListener("input", () => {
            if (priceValueDisplay) {
                priceValueDisplay.textContent = `${parseInt(priceRangeSlider.value).toLocaleString()}円`;
            }
        });
        priceRangeSlider.addEventListener("change", resetAndApplyFilters); // 필터 적용
    }

    // 6. 카테고리 아코디언 메뉴 및 서브카테고리 선택
    categoryItems.forEach((item) => {
        const mainCategory = item.querySelector(".category-main");
        const subCategoryList = item.querySelector(".subcategory-list");

        // 메인 카테고리 클릭 (아코디언 기능)
        mainCategory.addEventListener("click", (e) => {
            e.preventDefault();
            categoryItems.forEach((otherItem) => {
                if (otherItem !== item && otherItem.classList.contains("active")) {
                    otherItem.classList.remove("active");
                    const otherSubList = otherItem.querySelector(".subcategory-list");
                    if (otherSubList) otherSubList.style.maxHeight = "0";
                }
            });
            item.classList.toggle("active");
            if (subCategoryList) {
                if (item.classList.contains("active")) {
                    subCategoryList.style.maxHeight = subCategoryList.scrollHeight + "px";
                } else {
                    subCategoryList.style.maxHeight = "0";
                }
            }
        });
    });

    if (subCategorySelect) {
        subCategorySelect.addEventListener("change", resetAndApplyFilters);
    }

    // 7. 지역 드롭다운 변경
    if (regionSelect) {
        regionSelect.addEventListener("change", () => {
            const selectedRegionId = regionSelect.value;

            // Prefecture(중분류) 목록 초기화
            prefectureSelect.innerHTML = '<option value="">都道府県を選択</option>';

            // Municipality(소분류) 체크박스 영역도 함께 초기화
            municipalityContainer.innerHTML = "";
            municipalityContainer.style.display = "none";

            if (!selectedRegionId) {
                prefectureSelect.disabled = true;
                return;
            }

            // 선택된 Region에 해당하는 Prefecture 목록 찾아서 채우기
            const selectedRegion = allLocations.find((r) => r.rgnId == selectedRegionId);
            if (selectedRegion && selectedRegion.prefectures) {
                selectedRegion.prefectures.forEach((pref) => {
                    const option = new Option(pref.prefName, pref.prefId);
                    prefectureSelect.add(option);
                });
                prefectureSelect.disabled = false;
            }
        });
    }

    if (prefectureSelect) {
        prefectureSelect.addEventListener("change", () => {
            const selectedRegionId = regionSelect.value;
            const selectedPrefectureId = prefectureSelect.value;

            municipalityContainer.innerHTML = "";

            if (!selectedPrefectureId) {
                municipalityContainer.style.display = "none";
                resetAndApplyFilters();
                return;
            }

            const selectedRegion = allLocations.find((r) => r.rgnId == selectedRegionId);
            const selectedPrefecture = selectedRegion?.prefectures.find((p) => p.prefId == selectedPrefectureId);

            if (
                selectedPrefecture &&
                selectedPrefecture.municipalities &&
                selectedPrefecture.municipalities.length > 0
            ) {
                municipalityContainer.style.display = "grid";

                selectedPrefecture.municipalities.forEach((muni) => {
                    const itemWrapper = document.createElement("div");
                    itemWrapper.className = "checkbox-item";

                    const checkbox = document.createElement("input");
                    checkbox.type = "checkbox";
                    checkbox.id = `muni-${muni.mid}`;
                    checkbox.name = "muniIds";
                    checkbox.value = muni.mid;

                    const label = document.createElement("label");
                    label.htmlFor = `muni-${muni.mid}`;
                    label.textContent = muni.muniName;

                    itemWrapper.appendChild(checkbox);
                    itemWrapper.appendChild(label);

                    municipalityContainer.appendChild(itemWrapper);
                });
            } else {
                municipalityContainer.style.display = "none";
            }

            resetAndApplyFilters();
        });
    }

    if (municipalityContainer) {
        municipalityContainer.addEventListener("change", (e) => {
            if (e.target.type === "checkbox") {
                resetAndApplyFilters();
            }
        });
    }

    // 8. 무한 스크롤
    if (productGrid) {
        window.addEventListener("scroll", () => {
            if (isNearBottom() && !isLoading && currentPage < totalPages - 1) {
                loadProducts(currentPage + 1, currentSearchUrl);
            }
        });
    }

    // 9. 정렬 드롭다운 변경 시 필터 다시 적용
    if (sortBySelect) {
        sortBySelect.addEventListener("change", resetAndApplyFilters);
    }

    // 10. 초기화 버튼 클릭 시 모든 필터 초기화
    if (resetFiltersBtn) {
        resetFiltersBtn.addEventListener("click", resetAllFilters);
    }

    // --- 함수 정의 ---

    // 필터 적용 전 페이지 및 스크롤 상태 초기화
    function resetAndApplyFilters() {
        currentPage = -1; // 페이지를 초기화하여 처음부터 다시 로드
        applyFilters();
    }

    // 필터 적용 함수 (모든 필터 및 검색어 통합)
    async function applyFilters() {
        const keyword = searchBar ? searchBar.value.trim() : ""; // 검색어 (search-bar)
        const checkedMuniCheckboxes = document.querySelectorAll(
            "#municipality-container input[type='checkbox']:checked"
        );
        const muniIds = Array.from(checkedMuniCheckboxes)
            .map((checkbox) => checkbox.value)
            .filter((id) => id && id !== "undefined");

        // 선택된 서브 카테고리 ID
        let selectedSubCategoryId = "";
        if (subCategorySelect) {
            selectedSubCategoryId = subCategorySelect.value;
        }

        // 가격 범위 (슬라이더의 현재 value를 maxPrice로 사용, minPrice는 0으로 고정)
        const minPrice = "0"; // HTML에 minPrice input이 없으므로 0으로 고정
        const maxPrice = priceRangeSlider ? priceRangeSlider.value : "1000000";
        const sortValue = sortBySelect ? sortBySelect.value : "default_sort";

        const hasOtherFilters = keyword || muniIds.length > 0 || selectedSubCategoryId || maxPrice !== "1000000";

        const params = new URLSearchParams();
        let targetUrl = "";

        // 정렬 파라미터 바인딩
        if (sortValue === "default_sort" && !hasOtherFilters) {
            // おすすめ順
            targetUrl = "/api/products";
        } else {
            targetUrl = "/api/products/filter";
            if (sortValue !== "default_sort") {
                const [sortField, sortDirection] = sortValue.split("_");
                params.append("sortField", sortField);
                params.append("sortDirection", sortDirection);
            }
        }

        // 요청 파라미터 바인딩
        if (keyword) params.append("keyword", keyword);
        if (muniIds.length > 0) {
            muniIds.forEach((id) => {
                params.append("muniIds", id);
            });
        }
        if (selectedSubCategoryId) params.append("subCategoryId", selectedSubCategoryId);
        if (minPrice) params.append("minPrice", minPrice);
        if (maxPrice) params.append("maxPrice", maxPrice);

        currentSearchUrl = `${targetUrl}?${params.toString()}`;
        console.log("Generated Filter URL:", currentSearchUrl);
        loadProducts(0, currentSearchUrl); // 0페이지부터 새 필터 적용하여 로드
    }

    // 모든 필터를 기본값으로 되돌리는 함수
    function resetAllFilters() {
        // 모든 UI 컨트롤을 기본 상태로 변경
        if (searchBar) searchBar.value = "";
        regionSelect.value = "";
        prefectureSelect.innerHTML = '<option value="">都道府県を選択</option>';
        prefectureSelect.disabled = true;
        municipalityContainer.innerHTML = "";
        municipalityContainer.style.display = "none";
        if (subCategorySelect) subCategorySelect.value = "";

        const maxPriceValue = priceRangeSlider.max;
        priceRangeSlider.value = maxPriceValue;
        if (priceValueDisplay) {
            priceValueDisplay.textContent = `${parseInt(maxPriceValue).toLocaleString()}円`;
        }

        sortBySelect.value = "default_sort";

        // 기존 필터 적용 함수를 호출
        // => 모든 필터가 비어있고 정렬이 default이므로, 알아서 /api/products 를 호출
        resetAndApplyFilters();
    }

    // 상품 로딩 함수
    async function loadProducts(page, baseUrl) {
        if (isLoading) return; // 이미 로딩 중이면 실행x
        isLoading = true;
        loaderContainer.style.display = "flex";

        try {
            const url = `${baseUrl}${baseUrl.includes("?") ? "&" : "?"}page=${page}`; // ✨ baseUrl 사용
            console.log("Fetching products from:", url);

            const response = await fetch(url);
            if (!response.ok) throw new Error("상품 로딩 실패: " + response.statusText);

            const pageData = await response.json();

            // 처음 로드할 때 (page=0)는 그리드를 비움
            if (page === 0) {
                productGrid.innerHTML = ""; // 첫 페이지 로드 시 기존 내용 비우기
            }

            if (pageData.content.length === 0 && page === 0) {
                productGrid.innerHTML = '<p class="no-results-message">검색 결과가 없습니다.</p>';
            } else {
                pageData.content.forEach((product) => {
                    const productCard = createProductCard(product);
                    productGrid.appendChild(productCard);
                });
            }

            // 페이지 상태 업데이트
            currentPage = pageData.number;
            totalPages = pageData.totalPages;
        } catch (error) {
            console.error("Error loading products:", error);
            if (page === 0) {
                productGrid.innerHTML = '<p class="error-message">상품을 불러오는 중 오류가 발생했습니다.</p>';
            }
        } finally {
            isLoading = false;
            loaderContainer.style.display = "none";
        }
    }

    // 기존 최근 검색어 로딩
    async function showRecentSearches() {
        try {
            const response = await fetch("/api/recent-searches");
            if (!response.ok) {
                throw new Error("최근 검색어 로딩 실패");
            }

            const keywords = await response.json();

            recentSearchesList.innerHTML = ""; // 기존 목록 초기화
            if (keywords.length > 0) {
                keywords.forEach((search) => {
                    const li = document.createElement("li");

                    // 검색어 텍스트를 담을 span 생성
                    const keywordSpan = document.createElement("span");
                    keywordSpan.className = "keyword-text";
                    keywordSpan.textContent = search.keyword;

                    // 삭제 버튼 생성
                    const deleteBtn = document.createElement("button");
                    deleteBtn.className = "delete-search-btn";
                    deleteBtn.innerHTML = '<i class="fas fa-times"></i>';
                    deleteBtn.dataset.id = search.id;

                    li.appendChild(keywordSpan);
                    li.appendChild(deleteBtn);
                    recentSearchesList.appendChild(li);
                });

                recentSearchesContainer.style.display = "block";
            }
        } catch (error) {
            console.error(error);
        }
    }

    // 선택된 최근 검색어 삭제
    async function deleteOneSearch(searchId) {
        try {
            const response = await fetch(`/api/recent-searches/${searchId}`, {
                method: "DELETE",
            });
            if (!response.ok) {
                throw new Error("최근 검색어 삭제에 실패했습니다.");
            }
            console.log(`최근 검색어 ID ${searchId} 을 삭제했습니다.`);
        } catch (error) {
            console.error(error);
        }
    }

    // 최근 검색어 전체 삭제
    async function clearAllSearches() {
        try {
            const response = await fetch("/api/recent-searches", {
                method: "DELETE",
            });
            if (response.ok) {
                recentSearchesList.innerHTML = "";
                recentSearchesContainer.style.display = "none";
            }
        } catch (error) {
            console.error("삭제 실패:", error);
        }
    }

    // 검색어 저장 함수
    async function saveSearchKeyword(keyword) {
        try {
            await fetch("/api/recent-searches", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({ keyword: keyword }),
            });
        } catch (error) {
            console.error("검색어 저장 실패:", error);
        }
    }

    // 상품 카드 생성
    function createProductCard(product) {
        const card = document.createElement("div");
        card.className = "product-card";

        // 찜 수 1개 이상만 숫자 표시
        const likeCountDisplay = product.likeCount > 0 ? `<span class="like-count">${product.likeCount}</span>` : "";

        card.innerHTML = `
        <a href="/product/detail/${product.pid}">
            <img src="${product.imageUrl}" alt="${product.name}" class="product-image">
            <div class="product-info">
                <p class="product-name">${product.name}</p>
                <p class="product-price">${product.price.toLocaleString()}円</p>
                
                <div class="product-stats">
                    <span class="heart-icon"><i class="fas fa-heart"></i></span>
                    ${likeCountDisplay}
                </div>

            </div>
        </a>
    `;
        return card;
    }

    // 페이지 끝에 가까워졌는지 확인
    function isNearBottom() {
        const buffer = 100;
        return window.innerHeight + window.scrollY >= document.body.offsetHeight - buffer;
    }

    // 가격 슬라이더 값 초기 표시
    if (priceRangeSlider && priceValueDisplay) {
        priceValueDisplay.textContent = `${parseInt(priceRangeSlider.value).toLocaleString()}円`;
    }
});
