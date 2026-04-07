import Swiper from "swiper/bundle";

(function () {
	"use strict";

	function isAuthorEditMode() {
		var body = document.body;
		if (!body) {
			return false;
		}
		return (
			body.classList.contains("aem-AuthorLayer-Edit") ||
			body.classList.contains("aem-AuthorLayer-Design") ||
			body.classList.contains("aem-AuthorLayer-Layout")
		);
	}

	function initHeroBanner(el) {
		var slidesWrapper = el.querySelector("[data-hero-slides]");
		if (!slidesWrapper) {
			return;
		}

		var slides = slidesWrapper.querySelectorAll(".swiper-slide");
		var total = slides.length;
		if (total === 0) {
			return;
		}

		var autoplay = slidesWrapper.getAttribute("data-autoplay") === "true";
		var interval = parseInt(slidesWrapper.getAttribute("data-interval") || "5000", 10);
		var effect = (slidesWrapper.getAttribute("data-effect") || "slide").trim();
		var loop = slidesWrapper.getAttribute("data-loop") === "true";
		var hideDots = slidesWrapper.getAttribute("data-hide-dots") === "true";
		var hideNav = slidesWrapper.getAttribute("data-hide-nav") === "true";

		var prevBtn = el.querySelector("[data-hero-prev]");
		var nextBtn = el.querySelector("[data-hero-next]");
		var arrowsEl = el.querySelector(".cl-slider__arrows");
		var dotsWrapper = el.querySelector("[data-hero-dots]");

		if (arrowsEl) {
			arrowsEl.style.display = hideNav ? "none" : "";
		}
		if (dotsWrapper) {
			dotsWrapper.style.display = hideDots ? "none" : "";
		}

		if (isAuthorEditMode() || total <= 1) {
			return;
		}

		if (slidesWrapper.swiper) {
			return;
		}

		var noLoopEffects = ["cube", "cards", "creative", "flip"];
		var useLoop = loop && noLoopEffects.indexOf(effect) === -1;

		var config = {
			effect: effect,
			speed: 600,
			loop: useLoop,
			autoplay: autoplay
				? { delay: interval > 0 ? interval : 5000, disableOnInteraction: false, pauseOnMouseEnter: true }
				: false,
			pagination:
				!hideDots && dotsWrapper
					? {
							el: dotsWrapper,
							clickable: true,
							bulletClass: "cl-hero-banner__dot",
							bulletActiveClass: "cl-hero-banner__dot--active",
						}
					: false,
			navigation: !hideNav && prevBtn && nextBtn ? { prevEl: prevBtn, nextEl: nextBtn } : false,
			keyboard: { enabled: true, onlyInViewport: true },
			a11y: { prevSlideMessage: "Previous slide", nextSlideMessage: "Next slide" },
		};

		if (effect === "fade") {
			config.fadeEffect = { crossFade: true };
		}

		if (effect === "coverflow") {
			config.centeredSlides = true;
			config.slidesPerView = "auto";
			config.coverflowEffect = { rotate: 50, stretch: 0, depth: 100, modifier: 1, slideShadows: true };
		}

		new Swiper(slidesWrapper, config);
	}

	function init() {
		document.querySelectorAll(".cl-hero-banner").forEach(initHeroBanner);
	}

	if (document.readyState === "loading") {
		document.addEventListener("DOMContentLoaded", init);
	} else {
		init();
	}

	document.addEventListener("foundation-contentloaded", function (e) {
		var scope = e && e.target ? e.target : document;
		var banners = scope.querySelectorAll ? scope.querySelectorAll(".cl-hero-banner") : [];
		banners.forEach(initHeroBanner);
	});
})();
