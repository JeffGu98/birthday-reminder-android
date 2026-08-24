package com.shukun.birthdayreminder.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.shukun.birthdayreminder.R;

public final class Views {
    private Views() {}

    public static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static TextView text(Context context, String value, int sp, int color, int style) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        return view;
    }

    public static Button compactButton(Context context, String label) {
        Button button = new Button(context);
        button.setText(label);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(context, 12), dp(context, 5), dp(context, 12), dp(context, 5));
        return button;
    }

    public static TextView actionButton(Context context, String label) {
        TextView button = text(context, label, 14, context.getColor(R.color.primary), Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setBackgroundResource(R.drawable.bg_action_button);
        button.setMinWidth(dp(context, 96));
        button.setMinHeight(dp(context, 40));
        button.setPadding(dp(context, 18), dp(context, 8), dp(context, 18), dp(context, 8));
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    public static LinearLayout.LayoutParams marginParams(Context context, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(context, left), dp(context, top), dp(context, right), dp(context, bottom));
        return params;
    }

    public static LinearLayout.LayoutParams wrapParams(int gravity) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = gravity;
        return params;
    }
}
