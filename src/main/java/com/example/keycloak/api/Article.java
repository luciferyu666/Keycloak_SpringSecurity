package com.example.keycloak.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@AllArgsConstructor
@NoArgsConstructor // ✅ 為 JSON 反序列化提供無參構造函數
@ToString
public class Article {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("content")
    private String content;

    @JsonProperty("is_premium") // ✅ 確保 JSON 轉換時鍵名為 "is_premium"
    private boolean isPremium;
}
