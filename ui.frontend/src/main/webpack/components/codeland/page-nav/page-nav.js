const initPageNav = (root) => {
	if (!root) return;

	const list = root.querySelector(".cl-page-nav__list");
	if (!list) return;

	const anchors = Array.from(document.querySelectorAll(".cl-section-anchor[id]"));
	if (!anchors.length) return;

	list.innerHTML = "";

	const makeLabel = (anchorEl) => {
		const section = anchorEl.closest(".cl-section");
		const titleEl = section ? section.querySelector(".cmp-title__text") : null;
		const titleText = titleEl ? titleEl.textContent.trim() : "";
		if (titleText) return titleText;
		return anchorEl.id.replace(/[-_]+/g, " ").replace(/\s+/g, " ").trim();
	};

	anchors.forEach((anchorEl, index) => {
		const id = anchorEl.getAttribute("id");
		if (!id) return;

		const item = document.createElement("li");
		item.className = "cl-page-nav__item";

		const link = document.createElement("a");
		link.className = "cl-page-nav__link";
		link.href = `#${id}`;
		link.textContent = makeLabel(anchorEl);

		if (index === 0) {
			link.classList.add("cl-page-nav__link--active");
		}

		item.appendChild(link);
		list.appendChild(item);
	});

	const links = Array.from(list.querySelectorAll(".cl-page-nav__link"));

	const setActive = (href) => {
		links.forEach((link) => {
			link.classList.toggle("cl-page-nav__link--active", link.getAttribute("href") === href);
		});
	};

	list.addEventListener("click", (event) => {
		const link = event.target.closest(".cl-page-nav__link");
		if (!link) return;
		setActive(link.getAttribute("href"));
	});

	if ("IntersectionObserver" in window) {
		const observer = new IntersectionObserver(
			(entries) => {
				entries.forEach((entry) => {
					if (!entry.isIntersecting) return;
					const id = entry.target.getAttribute("id");
					if (!id) return;
					setActive(`#${id}`);
				});
			},
			{ rootMargin: "-40% 0px -45% 0px", threshold: 0.01 }
		);

		anchors.forEach((anchor) => observer.observe(anchor));
	}
};

document.addEventListener("DOMContentLoaded", () => {
	document.querySelectorAll(".cl-page-nav").forEach((root) => initPageNav(root));
});
