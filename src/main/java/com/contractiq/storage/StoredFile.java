package com.contractiq.storage;

public class StoredFile {
    private final String storageKey;
    private final String displayPath;

    public StoredFile(String storageKey, String displayPath) {
        this.storageKey = storageKey;
        this.displayPath = displayPath;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getDisplayPath() {
        return displayPath;
    }
}
