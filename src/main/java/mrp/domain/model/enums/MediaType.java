package mrp.domain.model.enums;

public enum MediaType {
    MOVIE,
    SERIES,
    GAME;

    public MediaType from(String s) {
        if (s == null) return null;
        return MediaType.valueOf(s.trim().toUpperCase());
    }
}
