/**
 * Header Component — Mobile menu toggle
 */

(function () {
	'use strict';

	const SELECTORS = {
		header: '.cl-header',
		toggle: '[data-header-toggle]',
		menu: '[data-header-menu]',
		body: 'body',
	};

	const CLASSES = {
		open: 'is-open',
		visible: 'is-open',
		noScroll: 'no-scroll',
	};

	function initHeader() {
		const header = document.querySelector(SELECTORS.header);
		if (!header) {
			return;
		}

		const toggleButtons = header.querySelectorAll(SELECTORS.toggle);
		const menu = header.querySelector(SELECTORS.menu);
		const body = document.body;

		if (!menu || !toggleButtons.length) {
			return;
		}

		// Toggle menu visibility
		function toggleMenu() {
			const isOpen = menu.classList.contains(CLASSES.visible);

			if (isOpen) {
				// Close menu
				menu.classList.remove(CLASSES.visible);
				header.classList.remove(CLASSES.open);
				body.classList.remove(CLASSES.noScroll);
			} else {
				// Open menu
				menu.classList.add(CLASSES.visible);
				header.classList.add(CLASSES.open);
				body.classList.add(CLASSES.noScroll);
			}
		}

		// Attach click handlers
		toggleButtons.forEach((button) => {
			button.addEventListener('click', toggleMenu);
		});

		// Close menu on escape key
		document.addEventListener('keydown', (event) => {
			if (event.key === 'Escape' && menu.classList.contains(CLASSES.visible)) {
				toggleMenu();
			}
		});

		// Close menu when clicking links inside mobile menu
		const mobileLinks = menu.querySelectorAll('.cl-header__mobile-link');
		mobileLinks.forEach((link) => {
			link.addEventListener('click', () => {
				if (menu.classList.contains(CLASSES.visible)) {
					toggleMenu();
				}
			});
		});

		// Close menu on window resize if desktop
		let resizeTimer;
		window.addEventListener('resize', () => {
			clearTimeout(resizeTimer);
			resizeTimer = setTimeout(() => {
				if (window.innerWidth > 768 && menu.classList.contains(CLASSES.visible)) {
					toggleMenu();
				}
			}, 150);
		});
	}

	// Initialize on DOM ready
	if (document.readyState === 'loading') {
		document.addEventListener('DOMContentLoaded', initHeader);
	} else {
		initHeader();
	}
})();
