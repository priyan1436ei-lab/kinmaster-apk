package dev.priyan.cut;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.AbsoluteSizeSpan;
import androidx.media3.common.*;
import androidx.media3.effect.*;
import androidx.media3.transformer.*;
import java.util.*;

/** One effect definition shared by preview and export. */
@android.annotation.SuppressLint("UnsafeOptInUsageError")
final class EditPipeline {
    static MediaItem media(Clip c) {
        return new MediaItem.Builder().setUri(c.uri).setClippingConfiguration(new MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(c.inMs).setEndPositionMs(c.outMs).build()).build();
    }
    static List<Effect> effects(Clip c, int height) {
        List<Effect> effects = new ArrayList<>();
        if (c.rotation != 0) effects.add(new ScaleAndRotateTransformation.Builder().setRotationDegrees(c.rotation).build());
        if (c.grayscale) effects.add(RgbFilter.createGrayscaleFilter());
        effects.add(Presentation.createForWidthAndHeight(height * 16 / 9, height, Presentation.LAYOUT_SCALE_TO_FIT));
        if (!c.text.trim().isEmpty()) {
            SpannableString text = new SpannableString(c.text);
            text.setSpan(new ForegroundColorSpan(Color.WHITE), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setSpan(new AbsoluteSizeSpan(42), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            effects.add(new OverlayEffect(com.google.common.collect.ImmutableList.of(TextOverlay.createStaticTextOverlay(text))));
        }
        return effects;
    }
    static Composition composition(List<Clip> clips, int height) {
        List<EditedMediaItem> items = new ArrayList<>();
        for (Clip c : clips) items.add(new EditedMediaItem.Builder(media(c)).setRemoveAudio(c.muted)
            .setEffects(new Effects(Collections.emptyList(), effects(c, height))).build());
        return new Composition.Builder(new EditedMediaItemSequence(items)).build();
    }
}
