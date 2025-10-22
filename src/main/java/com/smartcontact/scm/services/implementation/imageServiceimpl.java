package com.smartcontact.scm.services.implementation;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import com.smartcontact.scm.Helpers.AppConstants;
import com.smartcontact.scm.services.ImageService;
@Service
public class imageServiceimpl implements ImageService {
    @Autowired
    private Cloudinary cloudinary;

    @Override
    public String uploadImage(MultipartFile contactPic) {
        byte[] data;
        String filename=UUID.randomUUID().toString();
        try {
            data = new byte[contactPic.getInputStream().available()];
            contactPic.getInputStream().read(data);
            //upload to cloudinary
            cloudinary.uploader().upload(data,ObjectUtils.asMap(
                "public_id",filename
            ));
            return this.getUrlFromPublicId(filename);
        } catch (IOException e) {
           
            e.printStackTrace();
            return null;
        }
        
    }

    @Override
    public String getUrlFromPublicId(String publicId) {
        return cloudinary
        .url()
        .transformation(
            new Transformation<>()
            .width(AppConstants.CONTACT_PIC_WIDTH)
            .height(AppConstants.CONTACT_PIC_HEIGHT)
            .crop(AppConstants.CONTACT_PIC_CROP)
        )
        .generate(publicId);
    }
    
}
