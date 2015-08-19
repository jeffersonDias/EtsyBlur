package com.ms.square.android.etsyblur;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.support.v4.app.DialogFragment;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * BlurDialogFragmentHelper.java
 *
 * @author Manabu-GT on 6/12/14.
 */
public class BlurDialogFragmentHelper {

    private final DialogFragment mFragment;

    private ViewGroup mRoot;

    private ViewGroup mBlurContainer;

    private View mBlurBgView;

    private ImageView mBlurImgView;

    private int mAnimDuration;

    private int mWindowAnimStyle;

    private int mBgColorResId;

    public BlurDialogFragmentHelper(@NonNull DialogFragment fragment) {
        mFragment = fragment;
        mAnimDuration = fragment.getActivity().getResources().getInteger(android.R.integer.config_mediumAnimTime);
        mWindowAnimStyle = R.style.DialogSlideAnimation;
        mBgColorResId = R.color.bg_glass;
    }

    /**
     * Duration of the alpha animation.
     * @param animDuration The length of ensuing property animations, in milliseconds.
     *                     The value cannot be negative.
     */
    public void setAnimDuration(int animDuration) {
        mAnimDuration = animDuration;
    }

    public void setWindowAnimStyle(@StyleRes int windowAnimStyle) {
        mWindowAnimStyle = windowAnimStyle;
    }

    public void setBgColorResId(@ColorRes int bgColorResId) {
        mBgColorResId = bgColorResId;
    }

    public void onCreate() {
        mFragment.setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Translucent_NoTitleBar);
    }

    public void onActivityCreated() {
        Window window = mFragment.getDialog().getWindow();
        window.setWindowAnimations(mWindowAnimStyle);

        mRoot = (ViewGroup) mFragment.getActivity().getWindow().getDecorView();
        Rect visibleFrame = new Rect();
        mRoot.getWindowVisibleDisplayFrame(visibleFrame);

        mBlurContainer = new FrameLayout(mFragment.getActivity());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        params.topMargin = visibleFrame.top;
        params.bottomMargin = mRoot.getHeight() - visibleFrame.bottom;
        mBlurContainer.setLayoutParams(params);

        mBlurBgView = new View(mFragment.getActivity());
        mBlurBgView.setBackgroundColor(mFragment.getResources().getColor(mBgColorResId));
        Util.setAlpha(mBlurBgView, 0f);

        mBlurImgView = new ImageView(mFragment.getActivity());
        Util.setAlpha(mBlurImgView, 0f);

        mBlurContainer.addView(mBlurImgView);
        mBlurContainer.addView(mBlurBgView);

        mRoot.addView(mBlurContainer);

        final Rect visibleFrameCopy = new Rect(visibleFrame);
        mRoot.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Bitmap bitmap = Util.drawViewToBitmap(mRoot, mRoot.getWidth(),
                        visibleFrameCopy.bottom, 0, visibleFrameCopy.top, 3);
                Bitmap blurred = Blur.apply(mFragment.getActivity(), bitmap);
                mBlurImgView.setImageBitmap(blurred);
                bitmap.recycle();

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    BlurDialogFragmentHelper.this.mRoot.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    BlurDialogFragmentHelper.this.mRoot.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            }
        });

        View view = mFragment.getView();
        if (view != null) {
            view.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    mFragment.dismiss();
                    return true;
                }
            });
        }
    }

    public void onStart() {
        startEnterAnimation();
    }

    public void onDismiss() {
        startExitAnimation();
    }

    private void startEnterAnimation() {
        Util.animateAlpha(mBlurBgView, 0f, 1f, mAnimDuration, null);
        Util.animateAlpha(mBlurImgView, 0f, 1f, mAnimDuration, null);
    }

    private void startExitAnimation() {
        Util.animateAlpha(mBlurBgView, 1f, 0f, mAnimDuration, null);
        Util.animateAlpha(mBlurImgView, 1f, 0f, mAnimDuration, new Runnable() {
            @Override
            public void run() {
                mRoot.removeView(mBlurContainer);
            }
        });
    }
}