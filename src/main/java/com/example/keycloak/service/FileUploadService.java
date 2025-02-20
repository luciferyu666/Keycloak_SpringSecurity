package com.example.keycloak.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

/**
 * 提供檔案上傳至本地檔案系統的服務類別。
 */
@Service
public class FileUploadService {

    // 從 application.yml 或 application.properties 中取得上傳目錄路徑
    @Value("${app.upload.directory:./uploads}")
    private String uploadDir;

    /**
     * 上傳單一檔案至本地檔案系統，並回傳最終存放的檔名（或可改為全URL）。
     *
     * @param file 前端/客戶端傳來的檔案
     * @return 儲存在本地的檔名
     * @throws IOException 若寫檔失敗或 IO 錯誤時拋出
     */
    public String uploadFile(MultipartFile file) throws IOException {
        // 1. 取得原始檔名並分離副檔名
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 2. 生成新的檔名（避免衝突）
        String newFilename = UUID.randomUUID().toString() + fileExtension;

        // 3. 建立路徑物件
        Path targetPath = Paths.get(uploadDir).resolve(newFilename).normalize();

        // 若目錄不存在就先建立
        Files.createDirectories(targetPath.getParent());

        // 4. 寫入檔案
        file.transferTo(targetPath.toFile());

        // 5. 回傳最終檔名（實務可換成 http://domain/uploads/xxx 供前端顯示）
        return newFilename;
    }

}
