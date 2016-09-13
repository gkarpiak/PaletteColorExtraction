package com.tonyw.sampleapps.palettecolorextraction;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.support.v7.graphics.Palette;
import android.support.v7.graphics.Target;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Palette helper.
 */
public class PaletteHelper {
    final static Target TARGET_PRIORITY_ONE = Target.LIGHT_VIBRANT;
    final static Target TARGET_PRIORITY_TWO = Target.LIGHT_MUTED;
    final static Target TARGET_PRIORITY_THREE = Target.VIBRANT;

    public static Palette generate(Bitmap bitmap) {
        return Palette.from(bitmap)
                .maximumColorCount(75)
                .addTarget(TARGET_PRIORITY_ONE)
                .addTarget(TARGET_PRIORITY_TWO)
                .addTarget(TARGET_PRIORITY_THREE)
                .clearFilters()
                .generate();
    }

    public static Palette.Swatch findBestSwatch(Palette palette) {
        Palette.Swatch swatch = palette.getSwatchForTarget(TARGET_PRIORITY_ONE);
        if (null == swatch) {
            swatch = palette.getSwatchForTarget(TARGET_PRIORITY_TWO);
        }
        if (null == swatch) {
            swatch = palette.getSwatchForTarget(TARGET_PRIORITY_THREE);
        }
        if (null == swatch) {
            swatch = findBestSwatchFromDefaults(palette);
        }

        if (null == swatch) {
            swatch = new Palette.Swatch(Color.WHITE, 1);
        }

        return swatch;
    }

    public static int getObscureTextColor(Palette.Swatch swatch) {
        Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(swatch.getRgb());
        canvas.drawColor(swatch.getBodyTextColor());
        return bitmap.getPixel(0, 0);
    }


    private static Palette.Swatch findBestSwatchFromDefaults(Palette palette) {
        Palette.Swatch vibrant = findBestSwatch(palette, palette.getVibrantSwatch(), palette.getLightVibrantSwatch(), palette.getDarkVibrantSwatch());
        Palette.Swatch muted = findBestSwatch(palette, palette.getMutedSwatch(), palette.getLightMutedSwatch(), palette.getDarkMutedSwatch());
        Palette.Swatch best = null;
        if (null != vibrant && null != muted) {
            best = muted.getPopulation() > vibrant.getPopulation() ? muted : vibrant;
        } else if (null != vibrant) {
            best = vibrant;
        } else if (null != muted) {
            best = muted;
        }
        return best;
    }

    private static Palette.Swatch findBestSwatch(final Palette palette, Palette.Swatch... swatch) {
        List<Palette.Swatch> result = new ArrayList<>();
        for (Palette.Swatch s : swatch) {
            if (null != s) {
                result.add(s);
            }
        }

        return result.isEmpty() ? null : Collections.max(result, new Comparator<Palette.Swatch>() {
            @Override
            public int compare(Palette.Swatch s1, Palette.Swatch s2) {
                int result = -1 * (s1.getPopulation() - s2.getPopulation());
                if (0 == result) {
                    Palette.Swatch lightVibrant = palette.getSwatchForTarget(Target.LIGHT_VIBRANT);
                    Palette.Swatch vibrant = palette.getSwatchForTarget(Target.VIBRANT);
                    Palette.Swatch lightMuted = palette.getSwatchForTarget(Target.LIGHT_MUTED);
                    Palette.Swatch muted = palette.getSwatchForTarget(Target.MUTED);
                    if (s1 == lightVibrant) {
                        result = 1;
                    } else if (s2 == lightVibrant) {
                        result = -1;
                    } else if (s1 == vibrant) {
                        result = 1;
                    } else if (s2 == vibrant) {
                        result = -1;
                    } else if (s1 == lightMuted) {
                        result = 1;
                    } else if (s2 == lightMuted) {
                        result = -1;
                    } else if (s1 == muted) {
                        result = 1;
                    } else if (s2 == muted) {
                        result = -1;
                    }
                }
                return result;
            }
        });
    }
}
