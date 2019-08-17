package com.linkme.fido.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;

import com.linkme.fido.R;
import com.linkme.fido.databinding.DialogTextBinding;

/**
 *
 */
public class TextDialog extends DialogFragment {

    DialogTextBinding binding;

    private String title;
    private String text;

    public static TextDialog newInstance(final String title, final String text) {
        TextDialog dialog = new TextDialog();
        dialog.title = title;
        dialog.text = text;
        dialog.setCancelable(false);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_text, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.tvTitle.setText(title);
        binding.tvText.setText(text);

    }
}
