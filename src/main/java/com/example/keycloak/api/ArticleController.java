package com.example.keycloak.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    // ✅ 確保角色名稱與 Keycloak 配置一致
    private static final String ROLE_PREMIUM = "ROLE_premium_access";
    private static final String ROLE_BASIC = "ROLE_basic_access";

    private static final Map<Long, Article> articles = createArticles();

    private static Map<Long, Article> createArticles() {
        Map<Long, Article> articlesMap = new HashMap<>();
        articlesMap.put(1L, new Article(1L, "Free Article", "This is a free article.", false));
        articlesMap.put(2L, new Article(2L, "Premium Article", "This is premium article, for premium members only.", true));
        return Collections.unmodifiableMap(articlesMap);
    }
    
    /**
     * ✅ **訪問權限：Basic & Premium**
     * - `@PreAuthorize("hasAnyRole('basic_access', 'premium_access')")`
     * - **🚀 修正點：** Spring Security 會自動加上 `ROLE_` 前綴，這裡 **不需要手動添加**
     */
    @GetMapping("/basic")
    @PreAuthorize("hasAnyRole('basic_access', 'premium_access')")
    public String getBasicArticle(){
        return "Free Article";
    }

    /**
     * ✅ **訪問權限：Premium**
     * - `@PreAuthorize("hasRole('premium_access')")`
     * - **🚀 修正點：** Spring Security 會自動加上 `ROLE_`，這裡 **不需要寫 `ROLE_premium_access`**
     */
    @GetMapping("/premium")
    @PreAuthorize("hasRole('premium_access')")
    public String getPremiumArticle(){
        return "Premium Article";
    }

    /**
     * ✅ **In-Method Authorization（手動權限驗證）**
     * - **🚀 修正點：** `authentication.getAuthorities()` 內的角色已經包含 `ROLE_` 前綴。
     * - **不需要手動添加 `ROLE_`，否則匹配不到角色！**
     */
    @GetMapping("/all/{id}")
    public ResponseEntity<?> getArticleById(@PathVariable Long id, Authentication authentication) {
        if (!articles.containsKey(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("This content may have already been deleted.");
        }

        Article article = articles.get(id);

        // **如果是 Premium 文章，需要額外驗證權限**
        if (article.isPremium()) {
            boolean isPremiumUser = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_premium_access"));
            if (isPremiumUser) {
                return ResponseEntity.ok(article);
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("This content is only available for premium members.");
            }
        } else {
            return ResponseEntity.ok(article);
        }
    }
}
