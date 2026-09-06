package com.chess.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class OverlayService extends Service {
    private static final String CHANNEL_ID = "chess_overlay_active";
    private static final int NOTIFICATION_ID = 1001;
    private static final int BASE_FLAGS = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<ShapeModel> shapes = new ArrayList<>();

    private WindowManager windowManager;
    private ShapeStore store;
    private ShapeCanvasView canvas;
    private WindowManager.LayoutParams canvasParams;
    private View floatingButton;
    private View menuView;
    private View lengthPanel;
    private TextView allShapesCard;
    private TextView editModeCard;
    private TextView lengthLabel;
    private SeekBar lengthSeek;

    private boolean shapesVisible;
    private boolean editMode;
    private boolean menuVisible;
    private ShapeModel selectedShape;
    private long lastFloatingTap;
    private Runnable pendingSingleTap;

    @Override
    public void onCreate() {
        super.onCreate();
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        startAsForegroundService();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        store = new ShapeStore(this);
        shapes.addAll(store.loadStartupLayout());

        createCanvasOverlay();
        createFloatingButton();
        createMenuOverlay();
        createLengthPanel();
        refreshUiState();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startAsForegroundService() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Chess Overlay",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Keeps the user-controlled overlay active");
            manager.createNotificationChannel(channel);
        }

        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                openIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        Notification notification = builder
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Chess Overlay يعمل")
                .setContentText("اضغط لفتح التطبيق")
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private void createCanvasOverlay() {
        canvas = new ShapeCanvasView(this);
        canvas.setShapes(shapes);
        canvas.setVisibility(View.INVISIBLE);
        canvas.setListener(new ShapeCanvasView.Listener() {
            @Override
            public void onShapeSelected(ShapeModel shape) {
                selectShape(shape);
            }

            @Override
            public void onShapeDeleted(ShapeModel shape) {
                shapes.remove(shape);
                if (shape == selectedShape) {
                    selectedShape = null;
                    canvas.setSelected(null);
                    lengthPanel.setVisibility(View.GONE);
                }
                canvas.invalidate();
                toast("تم حذف " + shape.type.arabicName);
            }

            @Override
            public void onShapeMoved(ShapeModel shape) {
                selectedShape = shape;
            }
        });

        canvasParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                BASE_FLAGS | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        canvasParams.gravity = Gravity.TOP | Gravity.START;
        windowManager.addView(canvas, canvasParams);
    }

    private void createFloatingButton() {
        TextView button = new TextView(this);
        button.setText("♟");
        button.setTextColor(Color.WHITE);
        button.setTextSize(25f);
        button.setGravity(Gravity.CENTER);
        button.setBackground(roundRect(Color.rgb(12, 12, 12), dp(30), Color.rgb(65, 65, 65), dp(1)));
        button.setElevation(dp(8));
        button.setOnClickListener(v -> handleFloatingTap());
        floatingButton = button;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                dp(54),
                dp(54),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                BASE_FLAGS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = dp(14);
        params.y = dp(135);
        windowManager.addView(floatingButton, params);
    }

    private void handleFloatingTap() {
        long now = System.currentTimeMillis();
        if (now - lastFloatingTap <= 300) {
            if (pendingSingleTap != null) handler.removeCallbacks(pendingSingleTap);
            pendingSingleTap = null;
            lastFloatingTap = 0;
            setEditMode(!editMode);
            return;
        }

        lastFloatingTap = now;
        pendingSingleTap = () -> {
            if (System.currentTimeMillis() - lastFloatingTap >= 260) {
                toggleMenu();
                lastFloatingTap = 0;
            }
        };
        handler.postDelayed(pendingSingleTap, 280);
    }

    private void createMenuOverlay() {
        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(dp(8), dp(8), dp(8), dp(8));
        menu.setBackground(roundRect(Color.rgb(13, 13, 13), dp(12), Color.rgb(55, 55, 55), dp(1)));
        menu.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title = new TextView(this);
        title.setText("Chess Overlay");
        title.setTextColor(Color.WHITE);
        title.setTextSize(15f);
        title.setGravity(Gravity.CENTER);
        title.setPadding(dp(4), dp(6), dp(4), dp(8));
        menu.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        allShapesCard = createCard("تفعيل جميع الأشكال", v -> toggleAllShapes());
        menu.addView(allShapesCard, cardParams());

        editModeCard = createCard("وضع التعديل: إيقاف", v -> setEditMode(!editMode));
        menu.addView(editModeCard, cardParams());

        TextView editLength = createCard("تعديل طول الخطوط", v -> {
            if (!editMode) setEditMode(true);
            if (selectedShape == null) {
                toast("اضغط على مربع لاختياره، ثم عدّل المقياس من 1 إلى 100");
            } else {
                showLengthFor(selectedShape);
            }
        });
        menu.addView(editLength, cardParams());

        for (ShapeType type : ShapeType.values()) {
            TextView add = createCard("+ " + type.arabicName, v -> addShape(type));
            menu.addView(add, cardParams());
        }

        TextView save = createCard("حفظ وضع البدء", v -> saveStartupLayout());
        menu.addView(save, cardParams());

        TextView close = createCard("إغلاق القائمة", v -> toggleMenu());
        menu.addView(close, cardParams());

        menuView = menu;
        menuView.setVisibility(View.GONE);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                dp(260),
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                BASE_FLAGS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = dp(76);
        params.y = dp(88);
        windowManager.addView(menuView, params);
    }

    private void createLengthPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(12), dp(8), dp(12), dp(10));
        panel.setBackground(roundRect(Color.rgb(11, 11, 11), dp(13), Color.rgb(60, 60, 60), dp(1)));
        panel.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        lengthLabel = new TextView(this);
        lengthLabel.setTextColor(Color.WHITE);
        lengthLabel.setTextSize(14f);
        lengthLabel.setGravity(Gravity.CENTER);
        panel.addView(lengthLabel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        lengthSeek = new SeekBar(this);
        lengthSeek.setMax(99);
        lengthSeek.setProgress(44);
        lengthSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || selectedShape == null) return;
                selectedShape.length = progress + 1;
                store.setDefaultLength(selectedShape.type, selectedShape.length);
                updateLengthLabel();
                canvas.invalidate();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        panel.addView(lengthSeek, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));

        lengthPanel = panel;
        lengthPanel.setVisibility(View.GONE);

        int width = getResources().getDisplayMetrics().widthPixels - dp(30);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                BASE_FLAGS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.y = dp(28);
        windowManager.addView(lengthPanel, params);
    }

    private void toggleAllShapes() {
        if (shapesVisible) {
            shapesVisible = false;
            canvas.setVisibility(View.INVISIBLE);
            setEditMode(false);
        } else {
            if (shapes.isEmpty()) createInitialTwelve();
            shapesVisible = true;
            canvas.setVisibility(View.VISIBLE);
        }
        refreshUiState();
    }

    private void createInitialTwelve() {
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        int index = 0;

        for (ShapeType type : ShapeType.values()) {
            for (int copy = 0; copy < 2; copy++) {
                int col = index % 3;
                int row = index / 3;
                float x = width * (col + 1f) / 4f;
                float y = height * (row + 1f) / 5f;
                shapes.add(ShapeModel.create(type, x, y, store.getDefaultLength(type)));
                index++;
            }
        }
        canvas.invalidate();
    }

    private void addShape(ShapeType type) {
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        float offset = dp((shapes.size() % 7) * 8f);
        ShapeModel shape = ShapeModel.create(
                type,
                width / 2f + offset,
                height / 2f + offset,
                store.getDefaultLength(type));
        shapes.add(shape);
        shapesVisible = true;
        canvas.setVisibility(View.VISIBLE);
        canvas.invalidate();
        selectShape(shape);
        refreshUiState();
    }

    private void setEditMode(boolean enabled) {
        editMode = enabled;
        if (canvasParams != null) {
            canvasParams.flags = BASE_FLAGS | (enabled ? 0 : WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            try {
                windowManager.updateViewLayout(canvas, canvasParams);
            } catch (Exception ignored) {
            }
        }

        if (!enabled) {
            selectedShape = null;
            canvas.setSelected(null);
            if (lengthPanel != null) lengthPanel.setVisibility(View.GONE);
        } else {
            toast("وضع التعديل: اضغط للاختيار، اضغط مطولًا 0.3 ثانية ثم اسحب للتحريك، واضغط مرتين للحذف");
        }
        refreshUiState();
    }

    private void selectShape(ShapeModel shape) {
        selectedShape = shape;
        canvas.setSelected(shape);
        if (editMode) showLengthFor(shape);
    }

    private void showLengthFor(ShapeModel shape) {
        selectedShape = shape;
        lengthSeek.setProgress(shape.length - 1);
        updateLengthLabel();
        lengthPanel.setVisibility(View.VISIBLE);
    }

    private void updateLengthLabel() {
        if (selectedShape == null) {
            lengthLabel.setText("الطول");
        } else {
            lengthLabel.setText(selectedShape.type.arabicName + "  —  الطول: " + selectedShape.length + " / 100");
        }
    }

    private void saveStartupLayout() {
        store.saveStartupLayout(shapes);
        for (ShapeModel shape : shapes) {
            store.setDefaultLength(shape.type, shape.length);
        }
        toast("تم حفظ عدد الأشكال ومواضعها وأطوالها كوضع البدء");
    }

    private void toggleMenu() {
        menuVisible = !menuVisible;
        menuView.setVisibility(menuVisible ? View.VISIBLE : View.GONE);
    }

    private void refreshUiState() {
        if (allShapesCard != null) {
            allShapesCard.setText(shapesVisible ? "إيقاف جميع الأشكال" : "تفعيل جميع الأشكال");
        }
        if (editModeCard != null) {
            editModeCard.setText(editMode ? "وضع التعديل: تشغيل" : "وضع التعديل: إيقاف");
        }
    }

    private TextView createCard(String text, View.OnClickListener listener) {
        TextView card = new TextView(this);
        card.setText(text);
        card.setTextColor(Color.WHITE);
        card.setTextSize(13.5f);
        card.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        card.setPadding(dp(12), 0, dp(12), 0);
        card.setBackground(roundRect(Color.rgb(30, 30, 30), dp(8), Color.rgb(52, 52, 52), dp(1)));
        card.setOnClickListener(listener);
        return card;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(43));
        lp.setMargins(0, dp(3), 0, dp(3));
        return lp;
    }

    private GradientDrawable roundRect(int fill, float radius, int strokeColor, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) drawable.setStroke(strokeWidth, strokeColor);
        return drawable;
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        removeOverlay(canvas);
        removeOverlay(floatingButton);
        removeOverlay(menuView);
        removeOverlay(lengthPanel);
        super.onDestroy();
    }

    private void removeOverlay(View view) {
        if (view == null || windowManager == null) return;
        try {
            windowManager.removeView(view);
        } catch (Exception ignored) {
        }
    }
}
