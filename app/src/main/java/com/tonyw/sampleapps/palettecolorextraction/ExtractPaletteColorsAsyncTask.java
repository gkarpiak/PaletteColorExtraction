package com.tonyw.sampleapps.palettecolorextraction;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.graphics.Palette;
import android.support.v7.graphics.Target;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * An AsyncTask dedicated to extracting prominent colors from bitmaps and updating them on a card.
 */
public class ExtractPaletteColorsAsyncTask extends AsyncTask<Bitmap, Void, Palette> implements View.OnClickListener {
    private Context mContext;
    private View mCardView;

    public ExtractPaletteColorsAsyncTask(Context context, View cardView) {
        mContext = context;
        mCardView = cardView;
    }

    @Override
    protected Palette doInBackground(Bitmap... bitmaps) {
        return PaletteHelper.generate(bitmaps[0]);
    }

    @Override
    protected void onPostExecute(Palette palette) {
        ViewGroup swatches1 = (ViewGroup) mCardView.findViewById(R.id.swatches1);
        ViewGroup swatches2 = (ViewGroup) mCardView.findViewById(R.id.swatches2);
        swatches1.removeAllViews();
        swatches2.removeAllViews();
        ViewGroup layout = swatches1;
        for (Target target : palette.getTargets()) {
            addSwatch(layout, palette, target);
            if (layout == swatches1) {
                layout = swatches2;
            } else {
                layout = swatches1;
            }
        }
        addSwatch(layout, palette, palette.getDominantSwatch(), "DOMINANT");

        Palette.Swatch best = PaletteHelper.findBestSwatch(palette);
        if (null != best) {
            setShapeColor(best, mCardView);
        }
    }

    private void addSwatch(ViewGroup layout, Palette palette, Target target) {
        Palette.Swatch swatch = palette.getSwatchForTarget(target);
        String title = getTargetAndSwatchTitle(target);

        addSwatch(layout, palette, swatch, title);
    }

    private void addSwatch(ViewGroup layout, Palette palette, Palette.Swatch swatch, String title) {
        TextView textView = new TextView(layout.getContext());
        textView.setGravity(Gravity.CENTER);
        textView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, layout.getResources().getDisplayMetrics())));
        if (null == swatch) {
            textView.setBackgroundColor(0xffffffff);
            textView.setTextColor(0xff000000);
            textView.setText(title + " (null)");
        } else {
            textView.setBackgroundColor(swatch.getRgb());
            textView.setTextColor(swatch.getBodyTextColor());
            textView.setText(title + " " + swatch.getPopulation() + " " + (palette.getDominantSwatch() == swatch ? " (dominant)" : ""));
            textView.setTag(swatch);
            textView.setOnClickListener(this);
        }

        layout.addView(textView);
    }

    protected String getTargetAndSwatchTitle(Target target) {
        if (Target.VIBRANT == target) {
            return "VIBRANT";
        } else if (Target.DARK_MUTED == target) {
            return "DARK_MUTED";
        } else if (Target.DARK_VIBRANT == target) {
            return "DARK_VIBRANT";
        } else if (Target.LIGHT_MUTED == target) {
            return "LIGHT_MUTED";
        } else if (Target.LIGHT_VIBRANT == target) {
            return "LIGHT_VIBRANT";
        } else if (Target.MUTED == target) {
            return "MUTED";
        } else {
            return "unknown";
        }
    }

    private GradientDrawable getGradientDrawable(View colorShape) {
        return (GradientDrawable) colorShape.getBackground();
    }

    private View.OnLongClickListener mOnLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if (v.getTag() == null) {
                Toast.makeText(mContext, "No color available.", Toast.LENGTH_SHORT).show();
            } else {
                Palette.Swatch swatch = (Palette.Swatch) v.getTag();
                String colorHex = String.format("%06X", (0xFFFFFF & swatch.getRgb()));
                ClipboardManager clipboard = (ClipboardManager)
                        mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Color Hex", colorHex);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(mContext, "Copied color '" + colorHex + "' to clipboard.",
                        Toast.LENGTH_SHORT).show();
            }
            return true;
        }
    };

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Palette.Swatch swatch = (Palette.Swatch) v.getTag();
            if (swatch == null) return;
            int backgroundColor = swatch.getRgb();
            int titleTextColor = swatch.getTitleTextColor();
            int bodyTextColor = swatch.getBodyTextColor();
            Intent colorWithTextIntent = new Intent(mContext, ColorWithTextActivity.class);
            colorWithTextIntent.putExtra(ColorWithTextActivity.EXTRA_BACKGROUND_COLOR,
                    backgroundColor);
            colorWithTextIntent.putExtra(ColorWithTextActivity.EXTRA_TITLE_TEXT_COLOR,
                    titleTextColor);
            colorWithTextIntent.putExtra(ColorWithTextActivity.EXTRA_BODY_TEXT_COLOR,
                    bodyTextColor);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                CircularRevealTransition.setStartPosition((Activity) mContext, v);
                mContext.startActivity(colorWithTextIntent,
                        ActivityOptions.makeSceneTransitionAnimation(
                                (Activity) mContext).toBundle());
            } else {
                mContext.startActivity(colorWithTextIntent);
            }
        }
    };

    @Override
    public void onClick(View view) {
        setShapeColor((Palette.Swatch) view.getTag(), ((ViewGroup) view.getParent().getParent().getParent()));
    }

    public void setShapeColor(Palette.Swatch swatch, View container) {
        TextView textView = (TextView) container.findViewById(R.id.color_preview);
        textView.setBackgroundColor(ColorUtils.setAlphaComponent(swatch.getRgb(), (int) (0.9f * 255)));
        textView.setTextColor(PaletteHelper.getObscureTextColor(swatch));
    }
}
