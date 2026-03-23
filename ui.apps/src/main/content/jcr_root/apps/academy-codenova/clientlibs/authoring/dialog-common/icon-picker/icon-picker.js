// (function (document) {
// 	"use strict";

// 	var SELECTOR = "coral-select.icon-picker";

// 	function createPreview(value) {
// 		var preview = document.createElement("span");
// 		preview.className = "icon-picker-preview";
// 		var glyph = document.createElement("span");
// 		glyph.className = "icon-picker-glyph";

// 		if (!value) {
// 			preview.classList.add("is-placeholder");
// 			glyph.textContent = "";
// 			preview.appendChild(glyph);
// 			return preview;
// 		}

// 		glyph.classList.add("cl-icon-" + value);
// 		preview.appendChild(glyph);
// 		return preview;
// 	}

// 	function getLabelNode(item) {
// 		var labelNode = item.querySelector(".icon-picker-label");
// 		if (!labelNode) {
// 			labelNode = document.createElement("span");
// 			labelNode.className = "icon-picker-label";
// 		}
// 		return labelNode;
// 	}

// 	function decorateItem(item) {
// 		if (item.dataset.iconDecorated === "true") {
// 			return;
// 		}

// 		var rawLabel = item.getAttribute("data-icon-label") || item.textContent.trim();
// 		item.setAttribute("data-icon-label", rawLabel);

// 		while (item.firstChild) {
// 			item.removeChild(item.firstChild);
// 		}

// 		item.appendChild(createPreview(item.value));
// 		var labelNode = getLabelNode(item);
// 		labelNode.textContent = rawLabel;
// 		item.appendChild(labelNode);

// 		item.dataset.iconDecorated = "true";
// 	}

// 	function decorateButton(select) {
// 		var button = select.querySelector("button[is='coral-button']");
// 		var selectedItem = select.selectedItem;
// 		if (!button || !selectedItem) {
// 			return;
// 		}

// 		var label = selectedItem.getAttribute("data-icon-label") || selectedItem.textContent.trim();
// 		var labelNode = button.querySelector("[handle='label'], .coral3-Select-label, .icon-picker-selected-label");
// 		var oldPreviews = button.querySelectorAll(".icon-picker-preview");
// 		Array.prototype.forEach.call(oldPreviews, function (node) {
// 			node.remove();
// 		});

// 		if (!labelNode) {
// 			labelNode = document.createElement("span");
// 			labelNode.className = "icon-picker-selected-label";
// 			button.appendChild(labelNode);
// 		}

// 		var existing = button.querySelector(".icon-picker-preview");

// 		if (!existing) {
// 			existing = createPreview(selectedItem.value);
// 			var labelNode = button.querySelector("[handle='label'], .coral3-Select-label");

// 			if (labelNode) {
// 				labelNode.insertAdjacentElement("afterbegin", existing);
// 			}
// 		} else {
// 			existing.replaceWith(createPreview(selectedItem.value));
// 		}
// 		labelNode.textContent = label;
// 	}

// 	function decorateSelect(select) {
// 		var items = [];
// 		if (select.items && select.items.getAll) {
// 			items = select.items.getAll();
// 		}
// 		items = Array.prototype.slice.call(items || []);
// 		if (!items.length) {
// 			items = Array.prototype.slice.call(select.querySelectorAll("coral-select-item, coral-selectlist-item"));
// 		}
// 		try {
// 			items.forEach(decorateItem);
// 			decorateButton(select);
// 		} catch (e) {
// 			// Keep dialog usable even if Coral internals change.
// 		}
// 	}

// 	function scheduleDecorate(select) {
// 		decorateSelect(select);
// 		window.requestAnimationFrame(function () {
// 			decorateSelect(select);
// 		});
// 		window.setTimeout(function () {
// 			decorateSelect(select);
// 		}, 50);
// 	}

// 	function getUniqueSelects(scope) {
// 		var nodes = Array.prototype.slice.call(scope.querySelectorAll(SELECTOR));
// 		var seen = [];
// 		return nodes.filter(function (node) {
// 			if (!node || node.tagName !== "CORAL-SELECT") {
// 				return false;
// 			}
// 			if (seen.indexOf(node) !== -1) {
// 				return false;
// 			}
// 			seen.push(node);
// 			return true;
// 		});
// 	}

// 	function init(context) {
// 		var scope = context || document;
// 		var selects = getUniqueSelects(scope);
// 		selects.forEach(function (select) {
// 			scheduleDecorate(select);

// 			if (select.dataset.iconPickerBound !== "true") {
// 				select.addEventListener("change", function () {
// 					scheduleDecorate(select);
// 				});
// 				select.addEventListener("click", function () {
// 					scheduleDecorate(select);
// 				});
// 				select.dataset.iconPickerBound = "true";
// 			}
// 		});
// 	}

// 	document.addEventListener("foundation-contentloaded", function (event) {
// 		init(event.target);
// 		window.setTimeout(function () {
// 			init(event.target);
// 		}, 0);
// 		window.setTimeout(function () {
// 			init(event.target);
// 		}, 100);
// 	});

// 	document.addEventListener("click", function (event) {
// 		var select = event.target && event.target.closest ? event.target.closest("coral-select") : null;
// 		if (select && select.matches(SELECTOR)) {
// 			scheduleDecorate(select);
// 		}
// 	});
// })(document);
