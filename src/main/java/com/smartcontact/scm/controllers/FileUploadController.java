package com.smartcontact.scm.controllers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class FileUploadController {

    
    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/"; //static means spring boot can serve these files directly like http://localhost:8000/uploads/filename.jpg


    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // 1. Create unique filename
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path path = Paths.get(UPLOAD_DIR + fileName);
            
            // 2. Ensure directory exists
            Files.createDirectories(path.getParent());
            
            // 3. Save file
            Files.write(path, file.getBytes());
            
            // 4. Return the URL (assuming you serve static files from /uploads/)
            Map<String, String> response = new HashMap<>();
            response.put("url", "/uploads/" + fileName); // The link the frontend needs
            
            return ResponseEntity.ok(response); //return http status 200ok with body having json "url": /uploads/filename.png
        } catch (IOException e) {
            return ResponseEntity.status(500).build(); //returns status 500 ie internal server error
        }
    }
}