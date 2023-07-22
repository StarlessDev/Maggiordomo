package dev.starless.maggiordomo.data.filter;

public record FilterResult(boolean flagged, String message) {

    public FilterResult() {
        this(false,  null);
    }
}
