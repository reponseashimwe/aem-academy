(function (document) {
  "use strict";

  var TOGGLE_SELECTOR = "coral-checkbox.cl-logo-mode-toggle";
  var DEFAULT_ICON_FIELDS_SELECTOR = ".cl-logo-default-icon-fields";
  var ASSET_FIELDS_SELECTOR = ".cl-logo-asset-fields";

  function isChecked(checkbox) {
    return checkbox && checkbox.checked;
  }

  function setVisible(element, visible) {
    if (!element) {
      return;
    }
    element.style.display = visible ? "" : "none";
  }

  function updateDialogState(checkbox) {
    var form = checkbox.closest("form");
    var scope = form || document;
    var defaultIconFields = scope.querySelector(DEFAULT_ICON_FIELDS_SELECTOR);
    var assetFields = scope.querySelector(ASSET_FIELDS_SELECTOR);
    var useDefaultIcon = isChecked(checkbox);

    setVisible(defaultIconFields, useDefaultIcon);
    setVisible(assetFields, !useDefaultIcon);
  }

  function bindToggle(checkbox) {
    if (!checkbox || checkbox.dataset.logoToggleBound === "true") {
      return;
    }

    updateDialogState(checkbox);
    checkbox.addEventListener("change", function () {
      updateDialogState(checkbox);
    });
    checkbox.dataset.logoToggleBound = "true";
  }

  function init(context) {
    var scope = context || document;
    var checkboxes = scope.querySelectorAll(TOGGLE_SELECTOR);
    checkboxes.forEach(bindToggle);
  }

  document.addEventListener("foundation-contentloaded", function (event) {
    init(event.target);
  });
})(document);
