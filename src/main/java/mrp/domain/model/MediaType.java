package mrp.domain.model;

//Art des Mediums --> Movie, Series, Game

public enum MediaType {
    MOVIE, SERIES, GAME;

    public static MediaType fromString(String s){
        if(s == null){
            throw new IllegalArgumentException("MediaType cannot be null");
        }
        return MediaType.valueOf(s.trim().toUpperCase());
    }
}
