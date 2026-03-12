(function () {
	function initLoadMore(scope) {
		var lists = scope.querySelectorAll(".cn-loadmore-list");

		lists.forEach(function (listEl) {
			if (listEl.dataset.initialized === "true") {
				return;
			}

			listEl.dataset.initialized = "true";

			var grid = listEl.querySelector(".cn-loadmore-grid");
			var button = listEl.querySelector(".cn-loadmore-btn");

			if (!grid || !button) {
				return;
			}

			var limitAttr = listEl.getAttribute("data-limit");
			var limit = parseInt(limitAttr, 10);

			if (isNaN(limit) || limit <= 0) {
				limit = 8;
			}

			var offset = 0;
			var loading = false;

			var endpoint = listEl.getAttribute("data-endpoint");

			function loadMore() {
				if (loading) return;

				loading = true;

				var url = endpoint + "?offset=" + offset;

				fetch(url)
					.then(function (response) {
						if (!response.ok) {
							throw new Error("Network error");
						}

						return response.json();
					})
					.then(function (data) {
						if (!data || !Array.isArray(data.items)) {
							return;
						}

						data.items.forEach(function (item) {
							var card = document.createElement("article");
							card.className = "cn-pages-grid__card";

							var link = document.createElement("a");
							link.href = item.url;
							link.className = "cn-pages-grid__link";

							var title = document.createElement("h3");
							title.className = "cn-pages-grid__title";
							title.textContent = item.title || "";

							link.appendChild(title);

							if (item.description) {
								var desc = document.createElement("p");
								desc.className = "cn-pages-grid__description";
								desc.textContent = item.description;
								link.appendChild(desc);
							}

							card.appendChild(link);
							grid.appendChild(card);
						});

						offset += data.items.length;

						if (!data.pagination || data.pagination.hasMore === false || data.items.length === 0) {
							button.style.display = "none";
						}
					})
					.finally(function () {
						loading = false;
					});
			}

			button.addEventListener("click", function (event) {
				event.preventDefault();
				loadMore();
			});

			// Load first batch automatically
			loadMore();
		});
	}

	// Initial page load
	document.addEventListener("DOMContentLoaded", function () {
		initLoadMore(document);
	});

	// AEM author mode component refresh
	document.addEventListener("foundation-contentloaded", function (event) {
		initLoadMore(event.target);
	});
})();
