"use strict";
use(function () {
	var res = this.resource;
	return {
		imagePosition: res ? (res.getValueMap().get("imagePosition", "right") || "right") : "right"
	};
});
