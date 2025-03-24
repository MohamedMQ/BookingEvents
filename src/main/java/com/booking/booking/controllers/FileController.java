package com.booking.booking.controllers;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static java.io.File.separator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/uploads")
public class FileController {
    @GetMapping("/users/events/{filename}")
    public ResponseEntity<Resource> getDefaultEventImage(@PathVariable String filename) throws Exception {
        Path path = Paths.get("uploads" + separator + "users" + separator + "events" + separator + filename);
        Resource resource = new UrlResource(path.toUri());
        
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }
        
        String contentType = Files.probeContentType(path);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        
        // System.err.println(contentType);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    @GetMapping("/users/events/{userId}/{filename}")
    public ResponseEntity<Resource> getEventImage(@PathVariable String userId, @PathVariable String filename) throws Exception {
        Path path = Paths.get("uploads" + separator + "users" + separator + "events" + separator + userId + separator + filename);
        Resource resource = new UrlResource(path.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(path);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }
}