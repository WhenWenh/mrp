package mrp.domain.ports;

public class MediaSearch {
    private String title;
    private String mediaType;
    private String genre;
    private Integer releaseYear;
    private Integer ageRestriction;
    private Double rating;
    private String sortBy;
    private String sortDir;
    private int limit;
    private int offset;

    public MediaSearch(
            String title,
            String mediaType,
            String genre,
            Integer releaseYear,
            Integer ageRestriction,
            Double rating,
            String sortBy,
            String sortDir,
            int limit,
            int offset
    ) {
        this.title = title;
        this.mediaType = mediaType;
        this.genre = genre;
        this.releaseYear = releaseYear;
        this.ageRestriction = ageRestriction;
        this.rating = rating;
        this.sortBy = sortBy;
        this.sortDir = sortDir;
        this.limit = limit;
        this.offset = offset;
    }

    public String getTitle() { return title; }
    public String getMediaType() { return mediaType; }
    public String getGenre() { return genre; }
    public Integer getReleaseYear() { return releaseYear; }
    public Integer getAgeRestriction() { return ageRestriction; }
    public Double getRating() { return rating; }
    public String getSortBy() { return sortBy; }
    public String getSortDir() { return sortDir; }
    public int getLimit() { return limit; }
    public int getOffset() { return offset; }
}
