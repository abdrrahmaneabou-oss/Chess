package com.chess.overlay;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class ShapeStore {
    private static final String PREFS = "chess_overlay_state";
    private static final String KEY_LAYOUT = "startup_layout_v1";
    private final SharedPreferences prefs;

    public ShapeStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public int getDefaultLength(ShapeType type) {
        return prefs.getInt("length_type_" + type.id, 45);
    }

    public void setDefaultLength(ShapeType type, int value) {
        prefs.edit().putInt("length_type_" + type.id, ShapeModel.clampLength(value)).apply();
    }

    public void saveStartupLayout(List<ShapeModel> shapes) {
        JSONArray array = new JSONArray();
        for (ShapeModel shape : shapes) {
            try {
                JSONObject o = new JSONObject();
                o.put("id", shape.id);
                o.put("type", shape.type.id);
                o.put("x", shape.centerX);
                o.put("y", shape.centerY);
                o.put("length", shape.length);
                array.put(o);
            } catch (Exception ignored) {
            }
        }
        prefs.edit().putString(KEY_LAYOUT, array.toString()).apply();
    }

    public List<ShapeModel> loadStartupLayout() {
        ArrayList<ShapeModel> result = new ArrayList<>();
        String raw = prefs.getString(KEY_LAYOUT, null);
        if (raw == null || raw.isEmpty()) return result;

        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                int typeId = o.optInt("type", 1);
                if (typeId < 1 || typeId > 5) continue;

                ShapeType type = ShapeType.fromId(typeId);
                result.add(new ShapeModel(
                        o.optString("id"),
                        type,
                        (float) o.optDouble("x", 250),
                        (float) o.optDouble("y", 350),
                        o.optInt("length", getDefaultLength(type))));
            }
        } catch (Exception ignored) {
            result.clear();
        }
        return result;
    }
}
