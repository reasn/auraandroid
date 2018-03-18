package io.auraapp.auraandroid.main;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.InputFilter;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.TreeSet;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.Slogan;

class DialogManager {

    @FunctionalInterface
    interface AdoptCallback {
        void onAdoptSlogan(Slogan sloganToReplace);
    }

    @FunctionalInterface
    interface DropCallback {
        void onDropSlogan(Slogan slogan);
    }

    private boolean mDialogOpen = false;
    private Context mContext;

    DialogManager(Context context) {
        mContext = context;
    }

    private String getString(@StringRes int resource) {
        return EmojiHelper.replaceShortCode(mContext.getString(resource));
    }

    void showDrop(Slogan slogan, DropCallback dropCallback) {
        if (mDialogOpen) {
            return;
        }
        mDialogOpen = true;
        new AlertDialog.Builder(mContext)
                .setTitle(EmojiHelper.replaceShortCode(getString(R.string.ui_drop_dialog_title)))
                .setIcon(R.mipmap.ic_wastebasket)
                .setMessage(getString(R.string.ui_drop_dialog_message))
                .setPositiveButton(getString(R.string.ui_drop_dialog_confirm), (DialogInterface $, int $$) -> {
                    dropCallback.onDropSlogan(slogan);
                })
                .setNegativeButton(getString(R.string.ui_drop_dialog_cancel), (DialogInterface $, int $$) -> {
                })
                .setOnDismissListener($ -> mDialogOpen = false)
                .create()
                .show();
    }

    void showReplace(TreeSet<Slogan> mySlogans, AdoptCallback adoptCallback) {
        if (mDialogOpen) {
            return;
        }
        mDialogOpen = true;
        @SuppressLint("InflateParams")
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = inflater.inflate(R.layout.dialog_replace_slogan, null);

        RadioGroup radioGroup = dialogView.findViewById(R.id.radio_group);

        SparseArray<Slogan> map = new SparseArray<>();

        for (Slogan slogan : mySlogans) {
            RadioButton button = new RadioButton(mContext);
            // TODO emoji support
            String text = slogan.getText().length() < 20
                    ? slogan.getText()
                    : slogan.getText().substring(0, 20) + "...";
            button.setText(text);
            int id = View.generateViewId();
            button.setId(id);
            map.put(id, slogan);
            radioGroup.addView(button);
        }

        AlertDialog alert = new AlertDialog.Builder(mContext)
                .setTitle(getString(R.string.ui_replace_dialog_title))
                .setIcon(R.mipmap.ic_fire)
                // TODO getPluralizedString
                .setMessage(getString(R.string.ui_replace_dialog_message).replaceAll("##maxSlogans##", Integer.toString(MySloganManager.MAX_SLOGANS)))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.ui_replace_dialog_confirm),
                        (DialogInterface $$, int $$$) -> adoptCallback.onAdoptSlogan(map.get(radioGroup.getCheckedRadioButtonId()))
                )
                .setNegativeButton(getString(R.string.ui_replace_dialog_cancel), (DialogInterface $$, int $$$) -> {
                })
                .setOnDismissListener($ -> mDialogOpen = false)
                .create();

        alert.show();

        alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        radioGroup.setOnCheckedChangeListener(
                (RadioGroup $, int checkedId) -> alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true)
        );
    }

    interface OnSloganEditConfirm {
        void onConfirm(String text);
    }

    void showParametrizedSloganEdit(@StringRes int title,
                                    @StringRes int message,
                                    @StringRes int confirm,
                                    @StringRes int cancel,
                                    @Nullable Slogan slogan,
                                    OnSloganEditConfirm onConfirm) {
        if (mDialogOpen) {
            return;
        }
        mDialogOpen = true;

        @SuppressLint("InflateParams")
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = inflater.inflate(R.layout.dialog_edit_slogan, null);

        EditText editText = dialogView.findViewById(R.id.dialog_edit_slogan_slogan_text);
        if (slogan != null) {
            editText.setText(slogan.getText());
        }

        // Some phones have problems with filtering newline characters using an InputFilter.
        // Therefore this very ugly solution that does the replacement manually.
        // Original InputFilter:
//                (source, start, end, dest, dstart, dend) -> source.toString().replaceAll("\n", ""),


//        Timer timer = new Timer();
//
//        timer.setSerializedInterval("line-breaks", () -> {
//            if (editText.getText().)
//        });

        AlertDialog alert = new AlertDialog.Builder(mContext, R.style.Dialog)
                .setTitle(getString(title))
                .setIcon(R.mipmap.ic_memo)
                .setMessage(getString(message))
                .setView(dialogView)
                .setPositiveButton(getString(confirm), ($$, $$$) -> onConfirm.onConfirm(editText.getText().toString()))
                .setNegativeButton(getString(cancel), ($$, $$$) -> {
                })
                .setOnDismissListener($ -> mDialogOpen = false)
                .create();
        if (alert.getWindow() != null) {
            alert.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        alert.show();
        editText.requestFocus();
        editText.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(Config.COMMON_SLOGAN_MAX_LENGTH),
                (source, start, end, dest, dstart, dend) -> {
                    if (source != null && Config.COMMON_SLOGAN_BLOCKED_CHARACTERS.contains(("" + source))) {
                        return "";
                    }
                    return null;
                }
        });
    }

    @FunctionalInterface
    interface BtBrokenDismissHandler {
        void onDismiss(boolean neverShowAgain);
    }

    void showBtBroken(BtBrokenDismissHandler btBrokenDismissHandler) {
        if (mDialogOpen) {
            return;
        }
        mDialogOpen = true;

        @SuppressLint("InflateParams")
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = inflater.inflate(R.layout.dialog_bt_stack_broken, null);
        CheckBox checkBox = dialogView.findViewById(R.id.dont_show_again);
        new AlertDialog.Builder(mContext, R.style.Dialog)
                .setTitle(R.string.ui_dialog_bt_broken_title)
                .setMessage(R.string.ui_dialog_bt_broken_text)
                .setView(dialogView)
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton(R.string.ui_dialog_bt_broken_confirm, ($$, $$$) -> {
                    btBrokenDismissHandler.onDismiss(checkBox.isChecked());
                    mDialogOpen = false;
                })
                .setOnDismissListener($ -> {
                    btBrokenDismissHandler.onDismiss(false);
                    mDialogOpen = false;
                })
                .create()
                .show();
    }
}
