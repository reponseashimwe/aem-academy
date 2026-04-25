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

	function buildItem(course, defaultImg) {
		var imgSrc = course.fileReference || course.fileOriginal || defaultImg || "";
		var imgSrcSet = course.fileSrcSet || "";
		var imgSizes = course.fileSizes || "(max-width: 768px) 88vw, 279px";
		var img = imgSrc
			? '<picture>' +
				(imgSrcSet ? '<source srcset="' + imgSrcSet + '" sizes="' + imgSizes + '"/>' : '') +
				'<img src="' + imgSrc + '" alt="' + (course.title || "") + '" loading="lazy"/>' +
			  '</picture>'
			: "";

		var tags = (course.tags || [])
			.map(function (t) {
				return '<span class="cl-course-list__tag">' + t + "</span>";
			})
			.join("");

		var dateLabel = course.formattedStartDate || (course.startDate ? formatDate(course.startDate) : "");
		var date = dateLabel
			? '<span class="cl-course-list__date"><i class="cl-icon-calendar"></i>' + dateLabel + "</span>"
			: "";

		var meta = date || tags
			? '<div class="cl-course-list__meta">' + date + tags + "</div>"
			: "";

		return (
			'<li class="cl-course-list__item">' +
			'<a class="cl-course-list__link" href="' + (course.link || "#") + '">' +
			'<div class="cl-course-list__img">' + img + "</div>" +
			'<div class="cl-course-list__body">' +
			'<p class="cl-course-list__title">' + (course.title || "") + "</p>" +
			meta +
			"</div>" +
			"</a>" +
			"</li>"
		);
	}

	function initCourseList(el) {
		if (isAuthorEditMode()) return;

		var url = el.dataset.coursesUrl;
		if (!url) return;

		var defaultImg = el.dataset.defaultImg || "";

		var list = el.querySelector(".cl-course-list__items");
		if (!list) return;

		fetch(url)
			.then(function (res) {
				if (!res.ok) throw new Error("HTTP " + res.status);
				return res.json();
			})
			.then(function (courses) {
				if (!courses || courses.length === 0) {
					el.style.display = "none";
					return;
				}
				list.innerHTML = courses.map(function (c) { return buildItem(c, defaultImg); }).join("");
			})
			.catch(function (err) {
				console.error("Course list: failed to load", err);
			});
	}

	function init() {
		document.querySelectorAll(".cl-course-list").forEach(initCourseList);
	}

	if (document.readyState === "loading") {
		document.addEventListener("DOMContentLoaded", init);
	} else {
		init();
	}

	document.addEventListener("foundation-contentloaded", function (e) {
		var scope = e && e.target ? e.target : document;
		if (scope.querySelectorAll) {
			scope.querySelectorAll(".cl-course-list").forEach(initCourseList);
		}
	});
})();
