package com.cinema.movies.command.controller;

import com.cinema.commonservice.annotation.ApiMessage;
import com.cinema.movies.service.MinioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/upload")
@Slf4j
public class FileUploadController {

    private final MinioService minioService;

    public FileUploadController(MinioService minioService) {
        this.minioService = minioService;
    }

    /**
     * Upload movie poster image
     * 
     * @param file Image file
     * @return URL of uploaded image
     */
    @PostMapping(value = "/poster", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiMessage("Upload poster thành công")
    public Map<String, String> uploadPoster(@RequestParam("file") MultipartFile file) throws Exception {
        log.info("Uploading poster: {}", file.getOriginalFilename());

        String fileUrl = minioService.uploadFile(file);

        Map<String, String> response = new HashMap<>();
        response.put("url", fileUrl);
        response.put("filename", file.getOriginalFilename());

        return response;
    }

    /**
     * Delete image by URL
     * 
     * @param url URL of image to delete
     */
    @DeleteMapping("/poster")
    @ApiMessage("Xóa poster thành công")
    public ResponseEntity<Void> deletePoster(@RequestParam("url") String url) throws Exception {
        log.info("Deleting poster: {}", url);
        minioService.deleteFileByUrl(url);
        return ResponseEntity.ok().build();
    }
}
