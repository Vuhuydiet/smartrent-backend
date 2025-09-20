package com.smartrent.service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String uploadImage(MultipartFile file);
    String uploadVideo(MultipartFile file);
}
