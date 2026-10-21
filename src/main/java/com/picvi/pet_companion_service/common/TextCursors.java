package com.picvi.pet_companion_service.common;

import com.picvi.pet_companion_service.exception.petlog.InvalidCursor;

/** Opaque offset cursors for lists whose order is a ranking rather than a sortable key. */
public final class TextCursors {
    private TextCursors() {}

    public static int parseOffset(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        try {
            int offset = Integer.parseInt(raw);
            if (offset < 0) {
                throw new InvalidCursor();
            }
            return offset;
        } catch (NumberFormatException e) {
            throw new InvalidCursor();
        }
    }

    public static String formatOffset(int offset) {
        return Integer.toString(offset);
    }
}
