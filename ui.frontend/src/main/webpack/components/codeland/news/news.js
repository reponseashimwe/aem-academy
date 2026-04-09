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

	function formatDate(raw) {
		if (!raw) return "";
		var d = new Date(raw);
		if (isNaN(d.getTime())) return raw;
		return d.toLocaleDateString("en-US", { year: "numeric", month: "long", day: "numeric" });
	}

	function buildSlide(course, defaultImg) {
		var tags = (course.tags || [])
			.map(function (t) {
				return '<span class="cl-news-card__tag">' + t + "</span>";
			})
			.join("");

		var imgSrc = course.fileReference || defaultImg || "";
		var img = imgSrc
			? '<img src="' + imgSrc + '" alt="' + (course.title || "") + '" loading="lazy" />'
			: "";

		return (
			'<div class="swiper-slide">' +
			'<article class="cl-news-card">' +
			'<div class="cl-news-card__image"><a class="cl-news-card__image-link" href="' + (course.link || "#") + '">' + img + "</a></div>" +
			'<div class="cl-news-card__body">' +
			'<time class="cl-news-card__date">' + (course.formattedStartDate || formatDate(course.startDate) || "") + "</time>" +
			'<div class="cl-news-card__tags">' + tags + "</div>" +
			'<h3 class="cl-news-card__title"><a class="cl-news-card__title-link" href="' + (course.link || "#") + '">' + (course.title || "") + "</a></h3>" +
			'<p class="cl-news-card__desc">' + (course.abstract || "") + "</p>" +
			'<a href="' + (course.showMoreLink || course.link || "#") + '" class="cl-news-card__link">Show More <i class="cl-icon-arrow_forward"></i></a>' +
			"</div>" +
			"</article>" +
			"</div>"
		);
	}

	function initSlider(el, count) {
		var slidesEl = el.querySelector("[data-news-slides]");
		if (!slidesEl || count === 0) return;
		if (slidesEl.swiper) return;

		var dotsWrapper = el.querySelector("[data-news-dots]");
		var prevBtn     = el.querySelector("[data-news-prev]");
		var nextBtn     = el.querySelector("[data-news-next]");

		var slideWidth   = 279 + 24;
		var visibleCount = Math.floor(slidesEl.offsetWidth / slideWidth) || 1;
		var loopEnabled  = slidesEl.dataset.newsLoop !== "false" && count > visibleCount;

		new Swiper(slidesEl, {
			slidesPerView: "auto",
			spaceBetween: 24,
			slidesPerGroup: 2,
			loop: loopEnabled,
			watchOverflow: true,
			pagination: dotsWrapper
				? {
						el: dotsWrapper,
						clickable: true,
						bulletClass: "cl-slider__dot cl-slider__dot--dark",
						bulletActiveClass: "cl-slider__dot--active",
				  }
				: false,
			navigation: prevBtn && nextBtn ? { prevEl: prevBtn, nextEl: nextBtn } : false,
			keyboard: { enabled: true, onlyInViewport: true },
			a11y: { prevSlideMessage: "Previous slide", nextSlideMessage: "Next slide" },
		});
	}

	function renderCourses(el, courses, defaultImg) {
		var slidesEl = el.querySelector("[data-news-slides]");
		if (!slidesEl) return;
		var wrapper = slidesEl.querySelector(".swiper-wrapper");
		if (!wrapper) return;

		if (slidesEl.swiper) {
			slidesEl.swiper.destroy(true, true);
		}

		if (!courses || courses.length === 0) {
			wrapper.innerHTML = '<p class="cl-news__empty">No courses match the selected filter.</p>';
			return;
		}
		wrapper.innerHTML = courses.map(function (c) { return buildSlide(c, defaultImg); }).join("");
		initSlider(el, courses.length);
	}

	function fetchCourses(url, sortKey) {
		var fetchUrl = sortKey && sortKey !== "default"
			? url + "?sortBy=" + encodeURIComponent(sortKey)
			: url;
		return fetch(fetchUrl).then(function (res) {
			if (!res.ok) throw new Error("HTTP " + res.status);
			return res.json();
		});
	}

	function filterByTags(courses, activeTags) {
		if (!activeTags || activeTags.length === 0) return courses;
		return courses.filter(function (c) {
			var courseTags = c.tags || [];
			return activeTags.some(function (t) {
				return courseTags.indexOf(t) !== -1;
			});
		});
	}

	function initNewsSlider(el) {

		var coursesUrl = el.dataset.coursesUrl;
		if (!coursesUrl) return;

		var defaultImg = el.dataset.defaultImg || "";
		var allCourses  = [];  // full list from server — updated on each sort re-fetch
		var activeTags  = []; // visitor-selected tags (OR) — preserved across sort changes

		var sortWrap    = el.querySelector("[data-news-sort-wrap]");
		var sortSelect  = el.querySelector("[data-news-sort]");
		var tagFilterEl = el.querySelector("[data-news-tag-filter]");
		var slidesEl    = el.querySelector("[data-news-slides]");
		if (!slidesEl) return;
		var wrapper = slidesEl.querySelector(".swiper-wrapper");
		if (!wrapper) return;

		// ── Tag chips ────────────────────────────────────────────────────────
		function buildTagChips(courses) {
			var tagSet = Object.create(null);
			courses.forEach(function (c) {
				(c.tags || []).forEach(function (t) { tagSet[t] = true; });
			});
			var tags = Object.keys(tagSet);
			if (tags.length === 0) return;

			var html = '<button class="cl-news__tag-chip cl-news__tag-chip--active" data-tag="">All</button>';
			tags.forEach(function (t) {
				html += '<button class="cl-news__tag-chip" data-tag="' + t + '">' + t + "</button>";
			});
			tagFilterEl.innerHTML = html;

			var allBtn = tagFilterEl.querySelector('[data-tag=""]');

			tagFilterEl.addEventListener("click", function (e) {
				var btn = e.target.closest("[data-tag]");
				if (!btn) return;

				if (btn === allBtn) {
					// "All" clears every other selection
					activeTags = [];
					tagFilterEl.querySelectorAll("[data-tag]").forEach(function (b) {
						b.classList.toggle("cl-news__tag-chip--active", b === allBtn);
					});
				} else {
					// Toggle this tag in the active set (OR logic)
					var tag = btn.dataset.tag;
					var idx = activeTags.indexOf(tag);
					if (idx === -1) {
						activeTags.push(tag);
					} else {
						activeTags.splice(idx, 1);
					}
					btn.classList.toggle("cl-news__tag-chip--active", idx === -1);

					// Keep "All" active only when nothing else is selected
					allBtn.classList.toggle("cl-news__tag-chip--active", activeTags.length === 0);
				}

				renderCourses(el, filterByTags(allCourses, activeTags), defaultImg);
			});
		}

		// ── Sort select ──────────────────────────────────────────────────────
		function initSortSelect() {
			if (!sortSelect) return;
			sortSelect.addEventListener("change", function () {
				var sortKey = this.value;
				fetchCourses(coursesUrl, sortKey)
					.then(function (data) {
						allCourses = data.courses || [];
						renderCourses(el, filterByTags(allCourses, activeTags), defaultImg);
					})
					.catch(function (err) {
						console.error("News/Courses slider: sort fetch failed", err);
					});
			});
		}

		// ── Init Swiper on server-rendered slides ────────────────────────────
		// Slides are rendered server-side in slider.html (same pattern as hero-slides).
		// JS only needs to initialise Swiper on the existing DOM; it does NOT inject slides.
		var serverSlideCount = wrapper.querySelectorAll(".swiper-slide").length;
		if (serverSlideCount > 0 && !slidesEl.swiper) {
			initSlider(el, serverSlideCount);
		}

		// ── Fetch metadata for sort / filter controls ────────────────────────
		// Still needed so sort/filter UI can be shown and dynamic re-sorting works.
		fetchCourses(coursesUrl, null)
			.then(function (data) {
				allCourses = data.courses || [];

				if (allCourses.length === 0 && serverSlideCount === 0) {
					el.style.display = "none";
					return;
				}

				if (sortWrap && data.showSortControl) {
					sortWrap.style.display = "";
				}

				if (tagFilterEl && data.showTagFilter) {
					tagFilterEl.style.display = "";
					buildTagChips(allCourses);
				}

				initSortSelect();
			})
			.catch(function (err) {
				console.error("News/Courses slider: failed to load", err);
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

	document.addEventListener("foundation-contentloaded", function (e) {
		var scope = e && e.target ? e.target : document;
		var els;
		if (scope.nodeType === 1 && scope.matches && scope.matches(".cl-news")) {
			els = [scope];
		} else {
			els = scope.querySelectorAll ? Array.prototype.slice.call(scope.querySelectorAll(".cl-news")) : [];
		}
		els.forEach(initNewsSlider);
	});
})();
