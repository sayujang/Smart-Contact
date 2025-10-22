package com.smartcontact.scm.services;

import org.springframework.web.multipart.MultipartFile;

public interface ImageService {

    String uploadImage(MultipartFile contactPic,String filename);
    String getUrlFromPublicId(String publicId);
}
