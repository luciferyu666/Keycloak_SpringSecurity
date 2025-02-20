package com.example.keycloak.security;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter =
            new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        Collection<GrantedAuthority> authorities = Stream.concat(
            jwtGrantedAuthoritiesConverter.convert(jwt).stream(),
            Stream.concat(extractApiRoles(jwt).stream(), extractRealmRoles(jwt).stream()) // ✅ 加入 `realm_access`
        ).collect(Collectors.toSet());

        return new JwtAuthenticationToken(jwt, authorities);
    }

    /**
     * 🚀 **解析 `news-api` 內的角色**
     * 這裡會從 `resource_access` 內找到 `news-api` 的 `roles`
     */
    @SuppressWarnings("unchecked")
    private Collection<? extends GrantedAuthority> extractApiRoles(Jwt jwt) {
        Map<String, Object> resourceAccess;
        Map<String, Object> apiRolesMap;
        Collection<String> apiRoles;

        // 🔍 取得 `resource_access`
        Object rawResourceAccess = jwt.getClaim("resource_access");
        if (!(rawResourceAccess instanceof Map)) {
            return Set.of();
        }
        resourceAccess = (Map<String, Object>) rawResourceAccess;

        // 🚀 **確保讀取 `news-api` 內的角色**
        Object rawApiRoles = resourceAccess.get("news-api"); // ✅ 使用 `news-api`
        if (!(rawApiRoles instanceof Map)) {
            return Set.of();
        }
        apiRolesMap = (Map<String, Object>) rawApiRoles;

        // 取得 `roles` 欄位
        Object rawRoles = apiRolesMap.get("roles");
        if (!(rawRoles instanceof Collection)) {
            return Set.of();
        }
        apiRoles = (Collection<String>) rawRoles;

        return apiRoles
                .stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())) // ✅ 確保加上 `ROLE_` 並轉為大寫
                .collect(Collectors.toSet());
    }

    /**
     * 🚀 **解析 `realm_access` 內的角色**
     * 這些是全域角色，可能會影響 API 權限
     */
    @SuppressWarnings("unchecked")
    private Collection<? extends GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess;
        Collection<String> realmRoles;

        // 🔍 取得 `realm_access`
        Object rawRealmAccess = jwt.getClaim("realm_access");
        if (!(rawRealmAccess instanceof Map)) {
            return Set.of();
        }
        realmAccess = (Map<String, Object>) rawRealmAccess;

        // 取得 `roles`
        Object rawRoles = realmAccess.get("roles");
        if (!(rawRoles instanceof Collection)) {
            return Set.of();
        }
        realmRoles = (Collection<String>) rawRoles;

        return realmRoles
                .stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())) // ✅ 確保加上 `ROLE_`
                .collect(Collectors.toSet());
    }
}
