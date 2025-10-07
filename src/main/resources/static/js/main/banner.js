document.addEventListener("DOMContentLoaded", () => {
    const bannerContainer = document.getElementById("banner-container");
    if (bannerContainer) {
        const carousel = document.getElementById("banner-carousel");
        const prevButton = document.getElementById("prev-btn");
        const nextButton = document.getElementById("next-btn");
        const dotsContainer = document.getElementById("banner-dots");
        let autoSlideInterval;

        // ▼▼▼ 임시 샘플 데이터 (나중에 이 부분만 API 호출로 변경) ▼▼▼
        const sampleBannerData = [
            {
                imageUrl:
                    "https://images.pexels.com/photos/7679720/pexels-photo-7679720.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=1",
            },
            {
                imageUrl:
                    "https://images.pexels.com/photos/271816/pexels-photo-271816.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=1",
            },
            {
                imageUrl:
                    "https://images.pexels.com/photos/356056/pexels-photo-356056.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=1",
            },
        ];

        // 샘플 데이터로 배너 UI 생성
        if (!sampleBannerData || sampleBannerData.length === 0) {
            bannerContainer.style.display = "none";
            return;
        }

        let bannerHTML = "";
        let dotsHTML = "";
        sampleBannerData.forEach((banner, index) => {
            bannerHTML += `<div class="banner-item" style="background-image: url('${banner.imageUrl}')"></div>`;
            dotsHTML += `<button class="banner-dot" data-index="${index}"></button>`;
        });

        carousel.innerHTML = bannerHTML;
        dotsContainer.innerHTML = dotsHTML;
        // ▲▲▲ 임시 샘플 데이터 끝 ▲▲▲

        // 이하 캐러셀 기능 초기화 및 동작 로직 (이전과 동일)
        const items = carousel.querySelectorAll(".banner-item");
        const dots = dotsContainer.querySelectorAll(".banner-dot");
        const itemCount = items.length;
        let currentIndex = 0;

        if (itemCount > 1) {
            dots.forEach((dot) => {
                dot.addEventListener("click", (e) => {
                    const index = parseInt(e.target.dataset.index);
                    goToSlide(index);
                });
            });

            nextButton.addEventListener("click", () => {
                goToSlide((currentIndex + 1) % itemCount);
            });
            prevButton.addEventListener("click", () => {
                goToSlide((currentIndex - 1 + itemCount) % itemCount);
            });

            carousel.addEventListener("mouseenter", stopAutoSlide);
            carousel.addEventListener("mouseleave", startAutoSlide);

            updateUI();
            startAutoSlide();
        } else {
            prevButton.style.display = "none";
            nextButton.style.display = "none";
            dotsContainer.style.display = "none";
        }

        function goToSlide(index) {
            carousel.scrollTo({ left: items[0].offsetWidth * index, behavior: "smooth" });
            currentIndex = index;
            updateUI();
        }

        function updateUI() {
            dots.forEach((dot, i) => dot.classList.toggle("active", i === currentIndex));
        }

        function startAutoSlide() {
            stopAutoSlide();
            autoSlideInterval = setInterval(() => nextButton.click(), 3000);
        }
        function stopAutoSlide() {
            clearInterval(autoSlideInterval);
        }
    }
});
