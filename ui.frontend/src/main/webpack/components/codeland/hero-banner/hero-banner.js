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

	function initHeroBanner(el) {
		var slidesWrapper =
			el.querySelector("[data-hero-slides][data-hero-current-slide]") || el.querySelector("[data-hero-slides]");
		var prevBtn = el.querySelector("[data-hero-prev]");
		var nextBtn = el.querySelector("[data-hero-next]");
		var dotsWrapper = el.querySelector("[data-hero-dots]");

		if (!slidesWrapper) return;

		var slides = Array.prototype.slice.call(slidesWrapper.querySelectorAll("[data-hero-slide], .cl-hero-slide"));
		var total = slides.length;

		if (total === 0) return;
		if (total <= 1) return;

		var current = 0;
		var currentSlide = slidesWrapper.getAttribute("data-hero-current-slide") || "";
		var match = /slide(\d+)/.exec(currentSlide);
		if (match) {
			current = parseInt(match[1], 10);
		}
		if (!isFinite(current) || current < 0 || current >= total) {
			current = 0;
		}

		var dots = [];
		if (dotsWrapper) {
			slides.forEach(function (_slide, i) {
				var dot = document.createElement("button");
				dot.className = "cl-hero-banner__dot";
				dot.setAttribute("aria-label", "Go to slide " + (i + 1));
				dot.addEventListener("click", function () {
					goTo(i);
				});
				dotsWrapper.appendChild(dot);
				dots.push(dot);
			});
		}

		function updateNavState() {
			if (prevBtn) prevBtn.disabled = current === 0;
			if (nextBtn) nextBtn.disabled = current === total - 1;
		}

		function setActiveSlide(index) {
			current = Math.max(0, Math.min(index, total - 1));
			slides.forEach(function (slide, i) {
				var isActive = i === current;
				slide.classList.toggle("is-active", isActive);
				slide.setAttribute("aria-hidden", isActive ? "false" : "true");
			});
			dots.forEach(function (dot, i) {
				dot.classList.toggle("cl-hero-banner__dot--active", i === current);
			});
			slidesWrapper.setAttribute("data-hero-current-slide", "slide" + current);
			updateNavState();
		}

		function goTo(idx) {
			setActiveSlide(idx);
		}

		slidesWrapper.classList.add("is-initialized");
		setActiveSlide(current);

		if (prevBtn)
			prevBtn.addEventListener("click", function () {
				goTo(current - 1);
			});
		if (nextBtn)
			nextBtn.addEventListener("click", function () {
				goTo(current + 1);
			});

		el.addEventListener("keydown", function (e) {
			if (e.key === "ArrowLeft") goTo(current - 1);
			if (e.key === "ArrowRight") goTo(current + 1);
		});
	}

	function init() {
		if (isAuthorEditMode()) return;
		document.querySelectorAll(".cl-hero-banner").forEach(initHeroBanner);
	}

	if (document.readyState === "loading") {
		document.addEventListener("DOMContentLoaded", init);
	} else {
		init();
	}
})();
