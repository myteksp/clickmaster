package com.clicker.service;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CookieSession implements CookieJar {

    private final Map<String, List<Cookie>> cookieStore = new ConcurrentHashMap<>();

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        String key = url.host();
        List<Cookie> existing = cookieStore.computeIfAbsent(key, k -> new ArrayList<>());

        for (Cookie cookie : cookies) {
            existing.removeIf(c -> c.name().equals(cookie.name()));
            existing.add(cookie);
        }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        String key = url.host();
        List<Cookie> cookies = cookieStore.getOrDefault(key, List.of());
        List<Cookie> valid = new ArrayList<>();
        for (Cookie cookie : cookies) {
            if (cookie.expiresAt() <= 0 || cookie.expiresAt() > System.currentTimeMillis()) {
                valid.add(cookie);
            }
        }
        return valid;
    }

    public void clear() {
        cookieStore.clear();
    }

    public int cookieCount() {
        return cookieStore.values().stream().mapToInt(List::size).sum();
    }
}
