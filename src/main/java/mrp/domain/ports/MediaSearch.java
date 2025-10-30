package mrp.domain.ports;

public class MediaSearch {
    private String query;
    private String mediaType;
    private Integer yearFrom;
    private Integer yearTo;
    private Integer ageMax;
    private String sortBy;  // "title" | "year" | "created"
    private String sortDir; // "asc" | "desc"
    private int limit;
    private int offset;

    public MediaSearch(String query, String mediaType, Integer yearFrom, Integer yearTo, Integer ageMax,
                       String sortBy, String sortDir, int limit, int offset) {
        this.query = query;
        this.mediaType = mediaType;
        this.yearFrom = yearFrom;
        this.yearTo = yearTo;
        this.ageMax = ageMax;
        this.sortBy = sortBy;
        this.sortDir = sortDir;
        this.limit = limit;
        this.offset = offset;
    }

    public String getQuery() { return query; }
    public String getMediaType() { return mediaType; }
    public Integer getYearFrom() { return yearFrom; }
    public Integer getYearTo() { return yearTo; }
    public Integer getAgeMax() { return ageMax; }
    public String getSortBy() { return sortBy; }
    public String getSortDir() { return sortDir; }
    public int getLimit() { return limit; }
    public int getOffset() { return offset; }
}
