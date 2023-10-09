package dev.starless.maggiordomo.data.filter;

public record FilterResult(boolean flagged, String data) {

    public FilterResult() {
        this(false,  null);
    }
}
