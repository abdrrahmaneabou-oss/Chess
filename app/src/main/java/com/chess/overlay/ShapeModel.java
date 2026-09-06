package com.chess.overlay;

import java.util.UUID;

public final class ShapeModel {
    public final String id;
    public final ShapeType type;
    public float centerX;
    public float centerY;
    public int length;

    public ShapeModel(String id, ShapeType type, float centerX, float centerY, int length) {
        this.id = id;
        this.type = type;
        this.centerX = centerX;
        this.centerY = centerY;
        this.length = clampLength(length);
    }

    public static ShapeModel create(ShapeType type, float x, float y, int length) {
        return new ShapeModel(UUID.randomUUID().toString(), type, x, y, length);
    }

    public static int clampLength(int value) {
        return Math.max(1, Math.min(100, value));
    }
}
