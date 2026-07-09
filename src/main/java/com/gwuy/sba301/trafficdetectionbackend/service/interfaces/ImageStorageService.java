package com.gwuy.sba301.trafficdetectionbackend.service.interfaces;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {

    String upload(MultipartFile file);
}
