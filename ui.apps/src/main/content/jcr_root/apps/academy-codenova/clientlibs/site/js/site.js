/**
 * Academy CodeNova site scripts. Cookie alert: jQuery for dismiss (slideUp), fallback to vanilla.
 */
(function () {
  function initCookieAlerts() {
    if (typeof window.jQuery !== "undefined") {
      var $ = window.jQuery;
      $(".cn-cookie-alert").each(function () {
        $(this).find(".cn-cookie-alert__accept, .cn-cookie-alert__reject").on("click", function () {
          $(this).closest(".cn-cookie-alert").slideUp(300);
        });
      });
      return;
    }
    document.querySelectorAll(".cn-cookie-alert").forEach(function (alertEl) {
      alertEl.querySelectorAll(".cn-cookie-alert__accept, .cn-cookie-alert__reject").forEach(function (btn) {
        btn.addEventListener("click", function () {
          alertEl.style.display = "none";
        });
      });
    });
  }

  document.addEventListener("DOMContentLoaded", function () {
    console.log("academy-codenova site loaded");
    initCookieAlerts();
  });
})();
