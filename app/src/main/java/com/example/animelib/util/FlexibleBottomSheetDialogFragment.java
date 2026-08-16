package com.example.animelib.util;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class FlexibleBottomSheetDialogFragment extends BottomSheetDialogFragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = new FlexibleBottomSheetDialog(requireContext(), getTheme());
        FlexibleBottomSheetUtils.setupFlexibleStyle(dialog);
        return dialog;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Dialog dialog = getDialog();
        if (dialog instanceof BottomSheetDialog) {
            FlexibleBottomSheetUtils.applyFlexibleToView((BottomSheetDialog) dialog);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog instanceof BottomSheetDialog) {
            FlexibleBottomSheetUtils.applyFlexibleToView((BottomSheetDialog) dialog);
        }
    }
}
