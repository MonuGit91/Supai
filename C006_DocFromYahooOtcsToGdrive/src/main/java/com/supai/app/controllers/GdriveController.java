package com.supai.app.controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.supai.app.services.gdrive.GdriveUploaderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class GdriveController {
	private final GdriveUploaderService gdriveUploaderService;

	@PostMapping("upload")
	public void upload() {
		gdriveUploaderService.upload();
	}
}
