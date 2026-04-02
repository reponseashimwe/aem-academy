import Swiper from "swiper/bundle";

(function () {
	"use strict";

	function isAuthorEditMode() {
		var body = document.body;
		if (!body) return false;
		return (
			body.classList.contains("aem-AuthorLayer-Edit") ||
			body.classList.contains("aem-AuthorLayer-Design") ||
			body.classList.contains("aem-AuthorLayer-Layout")
		);
	}

	function initNewsSlider(el) {
		var slidesEl = el.querySelector("[data-news-slides]");
		if (!slidesEl) return;

		var slides = slidesEl.querySelectorAll(".swiper-slide");
		if (slides.length === 0) return;

		var dotsWrapper = el.querySelector("[data-news-dots]");
		var prevBtn = el.querySelector("[data-news-prev]");
		var nextBtn = el.querySelector("[data-news-next]");
		var loopEnabled = slidesEl.dataset.newsLoop !== "false";

		if (isAuthorEditMode() || slides.length <= 1) return;
		if (slidesEl.swiper) return;

		new Swiper(slidesEl, {
			slidesPerView: "auto",
			spaceBetween: 24,
			// Advance 2 slides per click → 6 slides produces 3 pagination dots
			slidesPerGroup: 2,
			loop: loopEnabled,
			// Reuse shared dot classes from _slider.scss (--dark variant for light bg)
			pagination: dotsWrapper
				? {
						el: dotsWrapper,
						clickable: true,
						bulletClass: "cl-slider__dot cl-slider__dot--dark",
						bulletActiveClass: "cl-slider__dot--active",
					}
				: false,
			navigation:
				prevBtn && nextBtn ? { prevEl: prevBtn, nextEl: nextBtn } : false,
			keyboard: { enabled: true, onlyInViewport: true },
			a11y: { prevSlideMessage: "Previous slide", nextSlideMessage: "Next slide" },
		});
	}

	function init() {
		document.querySelectorAll(".cl-news").forEach(initNewsSlider);
	}

	if (document.readyState === "loading") {
		document.addEventListener("DOMContentLoaded", init);
	} else {
		init();
	}

	// Re-init after AEM author-mode content load
	document.addEventListener("foundation-contentloaded", function (e) {
		var scope = e && e.target ? e.target : document;
		if (scope.querySelectorAll) {
			scope.querySelectorAll(".cl-news").forEach(initNewsSlider);
		}
	});
})();
