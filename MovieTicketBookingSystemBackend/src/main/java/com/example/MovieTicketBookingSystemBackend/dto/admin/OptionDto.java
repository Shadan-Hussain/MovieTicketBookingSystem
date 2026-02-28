package com.example.MovieTicketBookingSystemBackend.dto.admin;

/**
 * For admin dropdowns: id and display name. Hall uses label as "theatreName / hallName".
 * durationMins is set only for movie options (for computing show end time).
 */
public class OptionDto {
    private Long id;
    private String label;
    private Integer durationMins;

    public OptionDto(Long id, String label) {
        this.id = id;
        this.label = label;
        this.durationMins = null;
    }

    public OptionDto(Long id, String label, Integer durationMins) {
        this.id = id;
        this.label = label;
        this.durationMins = durationMins;
    }

    public Long getId() { return id; }
    public String getLabel() { return label; }
    public Integer getDurationMins() { return durationMins; }
}
