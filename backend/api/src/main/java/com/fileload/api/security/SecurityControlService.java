package com.fileload.api.security;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class SecurityControlService {

    private final Set<String> blockedIps = ConcurrentHashMap.newKeySet();
    private final Map<String, Boolean> featureFlags = new ConcurrentHashMap<>();

    public void blockIp(String ipAddress) {
        blockedIps.add(normalizeIp(ipAddress));
    }

    public void unblockIp(String ipAddress) {
        blockedIps.remove(normalizeIp(ipAddress));
    }

    public boolean isBlocked(String ipAddress) {
        return blockedIps.contains(normalizeIp(ipAddress));
    }

    public Set<String> getBlockedIps() {
        return new TreeSet<>(blockedIps);
    }

    public void setFeatureFlag(String flagKey, boolean enabled) {
        featureFlags.put(normalizeFlag(flagKey), enabled);
    }

    public Map<String, Boolean> getFeatureFlags() {
        return Map.copyOf(featureFlags);
    }

    private String normalizeIp(String ipAddress) {
        if (ipAddress == null) {
            throw new IllegalArgumentException("IP address is required");
        }
        String normalized = ipAddress.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("IP address is required");
        }
        return normalized;
    }

    private String normalizeFlag(String flagKey) {
        if (flagKey == null || flagKey.isBlank()) {
            throw new IllegalArgumentException("Feature flag key is required");
        }
        return flagKey.trim().toUpperCase();
    }
}

