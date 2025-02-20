package com.example.keycloak.api;

import com.example.keycloak.service.FileUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    // ✅ 確保角色名稱與 Keycloak 配置一致
    private static final String ROLE_PREMIUM = "ROLE_premium_access";
    private static final String ROLE_BASIC = "ROLE_basic_access";

    // 假設有一個檔案上傳服務（本地儲存或 S3），用於上傳文章附件
    private final FileUploadService fileUploadService;

    // 模擬文章儲存
    private static final Map<Long, Article> articles = createArticles();

    public ArticleController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    private static Map<Long, Article> createArticles() {
        Map<Long, Article> articlesMap = new HashMap<>();
        articlesMap.put(1L, new Article(1L, "Free Article", "This is a free article.", false));
        articlesMap.put(2L, new Article(2L, "Premium Article", "This is a premium article, for premium members only.", true));
        return Collections.unmodifiableMap(articlesMap);
    }

    /**
     * ✅ 訪問權限：Basic & Premium
     *   - `@PreAuthorize("hasAnyRole('basic_access', 'premium_access')")`
     *   - Spring Security 會自動加上 `ROLE_` 前綴，這裡不用手動添加
     */
    @GetMapping("/basic")
    @PreAuthorize("hasAnyRole('basic_access', 'premium_access')")
    public String getBasicArticle() {
        return "Free Article";
    }

    /**
     * ✅ 訪問權限：Premium
     *   - `@PreAuthorize("hasRole('premium_access')")`
     *   - Spring Security 會自動加上 `ROLE_`
     */
    @GetMapping("/premium")
    @PreAuthorize("hasRole('premium_access')")
    public String getPremiumArticle() {
        return "Premium Article";
    }

    /**
     * ✅ In-Method Authorization（手動權限驗證）
     *   - `authentication.getAuthorities()` 內角色已包含 `ROLE_` 前綴
     */
    @GetMapping("/all/{id}")
    public ResponseEntity<?> getArticleById(@PathVariable Long id, Authentication authentication) {
        if (!articles.containsKey(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body("This content may have already been deleted.");
        }
        Article article = articles.get(id);

        // 如果是 Premium 文章，需要檢查角色
        if (article.isPremium()) {
            boolean isPremiumUser = authentication.getAuthorities()
                    .contains(new SimpleGrantedAuthority("ROLE_premium_access"));
            if (isPremiumUser) {
                return ResponseEntity.ok(article);
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                     .body("This content is only available for premium members.");
            }
        } else {
            return ResponseEntity.ok(article);
        }
    }

    /**
     * ✅ 上傳文章附件（圖片/影片檔）
     *   - 透過 multipart/form-data 以 `file` 欄位提交
     *   - 綁定至指定的 articleId
     */
    @PostMapping("/{id}/upload")
    @PreAuthorize("hasRole('editor_access') or hasRole('admin_access')") 
    // e.g. 只有 Editor/Admin 才能上傳附件
    public ResponseEntity<?> uploadArticleAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        if (!articles.containsKey(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body("Article not found.");
        }

        Article article = articles.get(id);

        try {
            // 1. 呼叫 FileUploadService 上傳檔案，取得存放後的檔名或URL
            String savedFilename = fileUploadService.uploadFile(file);

            // 2. 將附件資訊綁定到此 Article (可改為 article.setMediaUrl(...) 等欄位)
            article.setAttachment(savedFilename);

            // 這裡我們只是替換 map 中的 article；若是真實環境，可能要存入資料庫
            // articles.put(id, article); // createArticles() 目前是 unmodifiable，實務需用 repository 進行更新

            // 3. 回傳成功
            return ResponseEntity.ok("Attachment uploaded for article #" + id
                    + " => " + savedFilename);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("檔案上傳失敗: " + e.getMessage());
        }
    }
}
