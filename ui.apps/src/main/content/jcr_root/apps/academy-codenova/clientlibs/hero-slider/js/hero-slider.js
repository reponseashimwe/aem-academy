;(function () {
	if (typeof window === "undefined") {
		return;
	}

	function initHeroSwiper(containerEl) {
		if (!window.Swiper || !containerEl) {
			// Swiper library not present – avoid breaking authoring
			return null;
		}

		// Avoid multiple initialisations on the same element
		if (containerEl._cnHeroSwiper) {
			return containerEl._cnHeroSwiper;
		}

		var autoplayAttr = containerEl.getAttribute("data-autoplay");
		var autoplayEnabled = autoplayAttr === "true" || autoplayAttr === "";
		var interval = parseInt(containerEl.getAttribute("data-interval") || "5000", 10);

		var loopAttr = containerEl.getAttribute("data-wrap");
		var loop = loopAttr === "true" || loopAttr === "";

		var currentEl = containerEl.querySelector(".cn-hero-slider__counter-current");
		var totalEl = containerEl.querySelector(".cn-hero-slider__counter-total");
		var paginationEl = containerEl.querySelector(".cn-hero-slider__pagination");

		var swiper = new window.Swiper(containerEl, {
			loop: loop,
			autoplay: autoplayEnabled
				? {
						delay: interval > 0 ? interval : 5000,
						disableOnInteraction: false,
				  }
				: false,
			pagination: paginationEl
				? {
						el: paginationEl,
						clickable: true,
				  }
				: undefined,
		});

		function updateCounter(swiperInstance) {
			if (!currentEl || !totalEl) {
				return;
			}
			var index = typeof swiperInstance.realIndex === "number" ? swiperInstance.realIndex : swiperInstance.activeIndex || 0;
			currentEl.textContent = String(index + 1);
			// total is rendered from Sling Model (sliderModel.totalSlides)
		}

		updateCounter(swiper);

		swiper.on("slideChange", function () {
			updateCounter(swiper);
		});

		var prevButtons = containerEl.querySelectorAll("[data-cn-hero-prev]");
		prevButtons.forEach(function (btn) {
			btn.addEventListener("click", function (event) {
				event.preventDefault();
				swiper.slidePrev();
			});
		});

		var nextButtons = containerEl.querySelectorAll("[data-cn-hero-next]");
		nextButtons.forEach(function (btn) {
			btn.addEventListener("click", function (event) {
				event.preventDefault();
				swiper.slideNext();
			});
		});

		containerEl._cnHeroSwiper = swiper;

		return swiper;
	}

	function initAllHeroSliders() {
		var sliders = document.querySelectorAll(".cn-hero-slider__carousel.swiper");
		if (!sliders || !sliders.length) {
			return;
		}
		sliders.forEach(function (el) {
			initHeroSwiper(el);
		});
	}

	if (document.readyState === "loading") {
		document.addEventListener("DOMContentLoaded", initAllHeroSliders);
	} else {
		initAllHeroSliders();
	}

	// Re-init when components are dynamically loaded in AEM authoring, but safely idempotent
	if (window.jQuery) {
		window.jQuery(document).on("foundation-contentloaded", initAllHeroSliders);
	}
})();
