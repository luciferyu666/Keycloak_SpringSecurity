package com.example.keycloak.api;

import com.example.keycloak.service.FileUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 提供上傳檔案的 REST API (本地檔案系統版本).
 */
@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    /**
     * 上傳單一檔案
     *
     * @param file 前端/客戶端以 multipart/form-data 提交的檔案欄位
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadSingleFile(@RequestParam("file") MultipartFile file) {
        try {
            // 呼叫服務層上傳，回傳檔名或 URL
            String savedFilename = fileUploadService.uploadFile(file);

            // 回傳成功訊息
            return ResponseEntity.ok("上傳成功, 檔名: " + savedFilename);

        } catch (IOException e) {
            // 若有 I/O 錯誤，回傳 500
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("檔案上傳失敗: " + e.getMessage());
        }
    }

    /**
     * 一次上傳多個檔案
     *
     * @param files 以 multipart/form-data 上傳多檔
     */
    @PostMapping("/upload/multiple")
    public ResponseEntity<?> uploadMultipleFiles(@RequestParam("files") List<MultipartFile> files) {
        List<String> savedFilenames = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                String filename = fileUploadService.uploadFile(file);
                savedFilenames.add(filename);
            } catch (IOException e) {
                // 若任何一個檔案失敗，可視需求：全部回滾 or 部分成功
                // 這裡簡化只要一有錯就返回 500
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                     .body("部分檔案上傳失敗: " + e.getMessage());
            }
        }
        return ResponseEntity.ok("全部檔案上傳成功: " + savedFilenames);
    }

}
