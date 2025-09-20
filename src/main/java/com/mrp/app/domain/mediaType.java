package com.mrp.app.domain;

import java.awt.*;

//Art des Mediums --> Movie, Series, Game

public enum mediaType {
    MOVIE, SERIES, GAME;

    public static mediaType fromString(String s){
        if(s == null){
            throw new IllegalArgumentException("MediaType cannot be null");
        }
        return mediaType.valueOf(s.trim().toUpperCase());
    }
}
