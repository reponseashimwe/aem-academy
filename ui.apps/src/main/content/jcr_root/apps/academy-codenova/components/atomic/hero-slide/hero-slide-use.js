"use strict";
use(function () {
	var res = this.resource;
	var imageRes = res ? res.getChild("image") : null;
	var vm = imageRes ? imageRes.getValueMap() : null;

	function getExt(path) {
		if (!path) return "jpg";
		var clean = String(path).split("?")[0];
		var dot = clean.lastIndexOf(".");
		return dot > -1 && dot < clean.length - 1 ? clean.substring(dot + 1) : "jpg";
	}

	function buildTransform(path, transformName) {
		if (!path) return "";
		var ext = getExt(path);
		return path + ".transform/" + transformName + "/image." + ext;
	}

	var desktopRef = vm ? (vm.get("fileReference", "") || "") : "";

	// If asset references are not set (direct binary upload), use component binaries.
	if (!desktopRef && imageRes) {
		desktopRef = imageRes.getPath() + "/file";
	}

	var heroDesktopSm = buildTransform(desktopRef, "image-hero-sm");
	var heroDesktopMd = buildTransform(desktopRef, "image-hero-md");
	var heroDesktopLg = buildTransform(desktopRef, "image-hero-lg");

	return {
		imagePosition: res ? (res.getValueMap().get("imagePosition", "right") || "right") : "right",
		heroDesktopSrc: heroDesktopMd,
		heroDesktopSrcSet: heroDesktopSm && heroDesktopMd && heroDesktopLg
			? (heroDesktopSm + " 640w, " + heroDesktopMd + " 1024w, " + heroDesktopLg + " 1600w")
			: ""
	};
});
