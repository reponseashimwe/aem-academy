// /**
//  * Slideshow Component — prev/next navigation and dot indicators.
//  */
// (function () {
//     'use strict';

//     function initSlideshow(el) {
//         var track   = el.querySelector('.cl-slideshow__track');
//         var slides  = el.querySelectorAll('.cl-slideshow__slide');
//         var dots    = el.querySelectorAll('[data-slideshow-dot]');
//         var prevBtn = el.querySelector('[data-slideshow-prev]');
//         var nextBtn = el.querySelector('[data-slideshow-next]');
//         var total   = slides.length;
//         var current = 0;

//         if (!track || total <= 1) return;

//         function goTo(index) {
//             current = (index + total) % total;
//             track.style.transform = 'translateX(-' + (current * 100) + '%)';
//             dots.forEach(function (dot, i) {
//                 dot.classList.toggle('is-active', i === current);
//             });
//         }

//         if (prevBtn) prevBtn.addEventListener('click', function () { goTo(current - 1); });
//         if (nextBtn) nextBtn.addEventListener('click', function () { goTo(current + 1); });

//         dots.forEach(function (dot) {
//             dot.addEventListener('click', function () {
//                 goTo(parseInt(dot.getAttribute('data-slideshow-dot'), 10));
//             });
//         });

//         // Keyboard navigation when focused inside the slideshow
//         el.addEventListener('keydown', function (e) {
//             if (e.key === 'ArrowLeft')  goTo(current - 1);
//             if (e.key === 'ArrowRight') goTo(current + 1);
//         });

//         goTo(0);
//     }

//     function init() {
//         document.querySelectorAll('[data-slideshow]').forEach(initSlideshow);
//     }

//     if (document.readyState === 'loading') {
//         document.addEventListener('DOMContentLoaded', init);
//     } else {
//         init();
//     }
// })();
