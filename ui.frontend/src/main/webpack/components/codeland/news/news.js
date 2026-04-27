import Swiper from "swiper/bundle";

(function () {
	"use strict";

	function isAuthorEditMode() {
		var body = document.body;
		return body && (
			body.classList.contains("aem-AuthorLayer-Edit") ||
			body.classList.contains("aem-AuthorLayer-Design") ||
			body.classList.contains("aem-AuthorLayer-Layout")
		);
	}

	function initNewsSlider(el) {
		var slidesEl = el.querySelector("[data-news-slides]");
		if (!slidesEl) return;

		var wrapper = slidesEl.querySelector(".swiper-wrapper");
		if (!wrapper) return;

		var count = wrapper.querySelectorAll(".swiper-slide").length;
		if (count === 0) return;

		if (isAuthorEditMode() || count <= 1) return;
		if (slidesEl.swiper) return;

		var dotsWrapper = el.querySelector("[data-news-dots]");
		var prevBtn     = el.querySelector("[data-news-prev]");
		var nextBtn     = el.querySelector("[data-news-next]");

		new Swiper(slidesEl, {
			slidesPerView: "auto",
			spaceBetween: 24,
			loop: slidesEl.dataset.newsLoop !== "false" && count > 1,
			pagination: dotsWrapper ? {
				el: dotsWrapper,
				clickable: true,
				bulletClass: "cl-slider__dot cl-slider__dot--dark",
				bulletActiveClass: "cl-slider__dot--active",
			} : false,
			navigation: prevBtn && nextBtn ? { prevEl: prevBtn, nextEl: nextBtn } : false,
			keyboard: { enabled: true, onlyInViewport: true },
			a11y: { prevSlideMessage: "Previous slide", nextSlideMessage: "Next slide" },
		});

		var tagFilter = el.querySelector("[data-news-tag-filter]");
		if (tagFilter) {
			tagFilter.addEventListener("click", function (e) {
				var chip = e.target.closest("[data-tag]");
				if (!chip) return;
				var url = new URL(window.location.href);
				var tag = chip.dataset.tag;
				if (tag) url.searchParams.set("tag", tag);
				else url.searchParams.delete("tag");
				window.location.href = url.toString();
			});
		}

		var sortSelect = el.querySelector("[data-news-sort]");
		if (sortSelect) {
			sortSelect.addEventListener("change", function () {
				var url = new URL(window.location.href);
				url.searchParams.set("sortBy", this.value);
				window.location.href = url.toString();
			});
		}
	}

	function init() {
		document.querySelectorAll(".cl-news").forEach(initNewsSlider);
	}

	if (document.readyState === "loading") {
		document.addEventListener("DOMContentLoaded", init);
	} else {
		init();
	}

	document.addEventListener("foundation-contentloaded", function (e) {
		var scope = e && e.target ? e.target : document;
		var els = scope.nodeType === 1 && scope.matches && scope.matches(".cl-news")
			? [scope]
			: (scope.querySelectorAll ? Array.from(scope.querySelectorAll(".cl-news")) : []);
		els.forEach(initNewsSlider);
	});
})();
