package io.auraapp.auraandroid.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.skydoves.colorpickerpreference.ColorPickerView;

import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.Slogan;

public class DialogManager {

    private final String COLOR_PICKER_INTERNAL_PREF_NAME = "auraColor";

    @FunctionalInterface
    public interface ColorPickedCallback {
        public void onColorPicked(String color);
    }

    public void showColorPickerDialog(ColorPickedCallback colorPickedCallback) {

        if (mDialogOpen) {
            return;
        }
        mDialogOpen = true;

        @SuppressLint("InflateParams")
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = inflater.inflate(R.layout.profile_dialog_pick_color, null);

        ColorPickerView colorPickerView = dialogView.findViewById(R.id.color_picker);
        colorPickerView.setPreferenceName(COLOR_PICKER_INTERNAL_PREF_NAME);
        colorPickerView.setColorListener(colorEnvelope -> {
            colorPickerView.saveData();
            colorPickedCallback.onColorPicked("#" + colorEnvelope.getColorHtml().toLowerCase());
        });

        AlertDialog alert = new AlertDialog.Builder(mContext, R.style.Dialog)
//                .setTitle(getString())
                .setIcon(R.mipmap.ic_memo)
//                .setMessage(getString(message))
                .setView(dialogView)
//                .setPositiveButton(getString(confirm), ($$, $$$) -> onConfirm.onConfirm(editText.getText().toString()))
//                .setNegativeButton(getString(cancel), ($$, $$$) -> {
//                })
                .setOnDismissListener($ -> mDialogOpen = false)
                .create();
        alert.show();
    }

    @FunctionalInterface
    interface AdoptCallback {
        void onAdoptSlogan(Slogan sloganToReplace);
    }

    @FunctionalInterface
    public interface DropCallback {
        public void onDropSlogan(Slogan slogan);
    }

    private boolean mDialogOpen = false;
    private Context mContext;

    private final Pattern mLinebreakPattern = Pattern.compile("\n");

    public DialogManager(Context context) {
        mContext = context;
    }

    private String getString(@StringRes int resource) {
        return EmojiHelper.replaceShortCode(mContext.getString(resource));
    }

    public void showDrop(Slogan slogan, DropCallback dropCallback) {
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
        View dialogView = inflater.inflate(R.layout.profile_dialog_replace_slogan, null);

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
                .setMessage(getString(R.string.ui_replace_dialog_message).replaceAll("##maxSlogans##", Integer.toString(Config.COMMON_SLOGAN_MAX_SLOGANS)))
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

    public interface OnSloganEditConfirm {
        public void onConfirm(String text);
    }

    public void showParametrizedSloganEdit(@StringRes int title,
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
        View dialogView = inflater.inflate(R.layout.profile_dialog_edit_slogan, null);

        EditText editText = dialogView.findViewById(R.id.dialog_edit_slogan_slogan_text);

        editText.setHint(EmojiHelper.replaceShortCode(getString(R.string.ui_dialog_edit_slogan_edit_hint)));

        if (slogan != null) {
            editText.setText(slogan.getText());
        }

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
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(Config.COMMON_SLOGAN_MAX_LENGTH)});


        // Some phones have problems with filtering newline characters using an InputFilter.
        // Therefore this very ugly solution that does the replacement manually.
        // First trial (didn't work) InputFilter:
        // (source, start, end, dest, dstart, dend) -> source.toString().replaceAll("\n", ""),
        // Second trial (didn't work) InputFilter:
        // (source, start, end, dest, dstart, dend) -> {
        //     for (int i = start; i < end; i++) {
        //         if (Config.COMMON_SLOGAN_BLOCKED_CHARACTERS.contains(source.subSequence(i, 1))) {
        //             return "";
        //         }
        //     }
        //     return null;
        // }
        // Thanks to https://stackoverflow.com/questions/15653664/replace-character-inside-textwatcher-in-android

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String string = editable.toString();
                if (!string.contains("\n")) {
                    return;
                }
                Matcher m = mLinebreakPattern.matcher(string);
                int count = 0;
                while (m.find()) {
                    count += 1;
                }
                if (count > Config.COMMON_SLOGAN_MAX_LINE_BREAKS) {
                    editable.replace(0, editable.length(), new SpannableStringBuilder(replaceLineBreaks(string)));
                }
            }
        });
    }

    /**
     * Thanks to https://stackoverflow.com/questions/767759/occurrences-of-substring-in-a-string
     * Thanks to https://stackoverflow.com/questions/3448330/in-matcher-replace-method-how-to-limit-replace-times
     */
    private String replaceLineBreaks(String content) {
        Matcher m = mLinebreakPattern.matcher(content);
        StringBuffer sb = new StringBuffer();
        boolean filtered = false;
        int index = 0;
        while (m.find()) {
            index++;
            if (index > Config.COMMON_SLOGAN_MAX_LINE_BREAKS) {
                filtered = true;
                m.appendReplacement(sb, "");
            }
        }
        // Show no toast if line breaks are disabled altogether
        //noinspection ConstantConditions
        if (filtered && Config.COMMON_SLOGAN_MAX_LINE_BREAKS > 0) {
            String text = mContext.getResources().getQuantityString(
                    R.plurals.ui_dialog_edit_slogan_line_break_limit,
                    Config.COMMON_SLOGAN_MAX_LINE_BREAKS,
                    Config.COMMON_SLOGAN_MAX_LINE_BREAKS);
            Toast.makeText(mContext, EmojiHelper.replaceShortCode(text), Toast.LENGTH_SHORT).show();
        }
        m.appendTail(sb);
        return sb.toString();
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
        View dialogView = inflater.inflate(R.layout.common_dialog_bt_stack_broken, null);
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
