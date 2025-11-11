package com.agendalo.domain;

public enum ImageType {
    USER_IMAGE("Imagen de usuario"),
    GALLERY("Imagen de galería"),
    LOGO("Logo de compañía");

    private final String description;

    ImageType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
} 