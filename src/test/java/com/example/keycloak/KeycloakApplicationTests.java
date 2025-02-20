package com.example.keycloak;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc // ✅ 啟用 MockMvc 測試
class KeycloakApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    /**
     * ✅ 測試 Spring Boot 應用是否能成功加載
     */
    @Test
    void contextLoads() {
    }

    /**
     * ✅ 測試公開 API 是否可訪問（無需授權）
     */
    @Test
    void shouldAllowAccessToPublicEndpoint() throws Exception {
        mockMvc.perform(get("/api/public"))
                .andExpect(status().isOk())
                .andExpect(result -> containsString("Public API"));
    }

    /**
     * ✅ 測試 /api/user 需要 USER 角色
     */
    @Test
    @WithMockUser(username = "testuser", roles = "USER") // 模擬 USER 角色
    void shouldAllowUserAccess() throws Exception {
        mockMvc.perform(get("/api/user"))
                .andExpect(status().isOk());
    }

    /**
     * ✅ 測試 /api/admin 需要 ADMIN 角色
     */
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN") // 模擬 ADMIN 角色
    void shouldAllowAdminAccess() throws Exception {
        mockMvc.perform(get("/api/admin"))
                .andExpect(status().isOk());
    }

    /**
     * ❌ 測試 /api/admin 若無 ADMIN 角色應返回 403 Forbidden
     */
    @Test
    @WithMockUser(username = "testuser", roles = "USER") // 模擬非 ADMIN 角色
    void shouldDenyAccessToAdminForUserRole() throws Exception {
        mockMvc.perform(get("/api/admin"))
                .andExpect(status().isForbidden());
    }
}
