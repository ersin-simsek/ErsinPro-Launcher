package org.ersinpro.launcher;

final class AppEntry {
    final String id;
    final String label;

    final int category;

    AppEntry(String id, String label) { this(id, label, -1); }

    AppEntry(String id, String label, int category) {
        this.category = category;
        this.id = id;
        this.label = label;
    }
}
