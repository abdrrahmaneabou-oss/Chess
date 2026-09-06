package com.chess.overlay;

import android.graphics.Color;

public enum ShapeType {
    PLUS(1, "الشكل 1", Color.rgb(255, 82, 82)),
    X(2, "الشكل 2", Color.rgb(0, 230, 118)),
    KNIGHT(3, "الشكل 3", Color.rgb(68, 138, 255)),
    PLUS_X(4, "الشكل 4", Color.rgb(255, 214, 0)),
    PAWN(5, "الشكل 5", Color.rgb(224, 64, 251)),
    KING(6, "الشكل 6", Color.rgb(0, 229, 255));

    public final int id;
    public final String arabicName;
    public final int color;

    ShapeType(int id, String arabicName, int color) {
        this.id = id;
        this.arabicName = arabicName;
        this.color = color;
    }

    public static ShapeType fromId(int id) {
        for (ShapeType type : values()) {
            if (type.id == id) return type;
        }
        return PLUS;
    }
}
