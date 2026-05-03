package de.tomsblog.blogcontent.adapter.inbound.web;

import jakarta.validation.constraints.NotBlank;

/** @req SWR-027 @req SWR-035 @req SWR-040 @req SWR-041 */
public class PostFormData {

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    @NotBlank
    private String contentType;

    @NotBlank
    private String locale;

    private String socialMediaTitle;

    private String socialMediaSummary;

    private String seriesPreviousPostId;

    private String seriesNextPostId;

    private String featuredFrom;

    private String featuredUntil;

    public PostFormData() {
        this.contentType = "HTML";
    }

    public PostFormData(
            String title,
            String content,
            String contentType,
            String locale,
            String socialMediaTitle,
            String socialMediaSummary,
            String seriesPreviousPostId,
            String seriesNextPostId,
            String featuredFrom,
            String featuredUntil) {
        this.title = title;
        this.content = content;
        this.contentType = contentType;
        this.locale = locale;
        this.socialMediaTitle = socialMediaTitle;
        this.socialMediaSummary = socialMediaSummary;
        this.seriesPreviousPostId = seriesPreviousPostId;
        this.seriesNextPostId = seriesNextPostId;
        this.featuredFrom = featuredFrom;
        this.featuredUntil = featuredUntil;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getSocialMediaTitle() {
        return socialMediaTitle;
    }

    public void setSocialMediaTitle(String socialMediaTitle) {
        this.socialMediaTitle = socialMediaTitle;
    }

    public String getSocialMediaSummary() {
        return socialMediaSummary;
    }

    public void setSocialMediaSummary(String socialMediaSummary) {
        this.socialMediaSummary = socialMediaSummary;
    }

    public String getSeriesPreviousPostId() {
        return seriesPreviousPostId;
    }

    public void setSeriesPreviousPostId(String seriesPreviousPostId) {
        this.seriesPreviousPostId = seriesPreviousPostId;
    }

    public String getSeriesNextPostId() {
        return seriesNextPostId;
    }

    public void setSeriesNextPostId(String seriesNextPostId) {
        this.seriesNextPostId = seriesNextPostId;
    }

    public String getFeaturedFrom() {
        return featuredFrom;
    }

    public void setFeaturedFrom(String featuredFrom) {
        this.featuredFrom = featuredFrom;
    }

    public String getFeaturedUntil() {
        return featuredUntil;
    }

    public void setFeaturedUntil(String featuredUntil) {
        this.featuredUntil = featuredUntil;
    }
}
