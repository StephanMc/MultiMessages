package com.stephanmc.multimessages.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.style.DynamicDrawableSpan;

import com.stephanmc.multimessages.R;


public class BubbleSpan extends DynamicDrawableSpan {

    private Context mContext;

    public BubbleSpan(Context context) {
        super();
        mContext = context;
    }

    @Override
    public Drawable getDrawable() {
        Resources resources = mContext.getResources();
        Drawable drawable = resources.getDrawable(R.drawable.background_oval);
        drawable.setBounds(0, 0, 100, 20);
        return drawable;
    }
}
