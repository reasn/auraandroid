package io.auraapp.auraandroid.ui;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.SparseArray;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.ui.common.ColorHelper;
import io.auraapp.auraandroid.ui.common.ColorPicker;

import static io.auraapp.auraandroid.common.FormattedLog.v;

public class DialogManager {

    @FunctionalInterface
    public interface MyNameEditedCallback {
        public void onNameEdited(String name);
    }

    @FunctionalInterface
    public interface MyTextEditedCallback {
        public void onTextEdited(String name);
    }

    @FunctionalInterface
    interface AdoptCallback {
        void onAdoptSlogan(Slogan sloganToReplace);
    }

    @FunctionalInterface
    public interface DropCallback {
        public void onDropSlogan(Slogan slogan);
    }

    @FunctionalInterface
    interface BtBrokenDismissHandler {
        void onDismiss(boolean neverShowAgain);
    }

    public interface OnSloganEditConfirm {
        public void onConfirm(String text);
    }

    class DialogState {
        boolean open = false;
    }

    private static final String TAG = "@aura/ui/" + DialogManager.class.getSimpleName();
    private final static Pattern mLinebreakPattern = Pattern.compile("\n");
    private final DialogState mDialogState = new DialogState();
    private Context mContext;
    private ColorPicker.SelectedColor mPickedColor = null;

    public DialogManager(Context context) {
        mContext = context;
    }

    private String getString(@StringRes int resource) {
        return EmojiHelper.replaceShortCode(mContext.getString(resource));
    }

    public void showEditMyNameDialog(@Nullable String name, MyNameEditedCallback callback) {

        EditText editText = (EditText) View.inflate(mContext, R.layout.profile_dialog_edit_name, null);
        editText.setText(name != null ? name : "");
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(Config.PROFILE_NAME_MAX_LENGTH)});
        editText.requestFocus();
        editText.selectAll();

        new DialogBuilder(mContext, mDialogState)
                .setTitle(R.string.ui_profile_dialog_edit_name_title)
                .setView(editText)
                .enableKeyboard()
                .setOnConfirm(() -> callback.onNameEdited(editText.getText().toString()))
                .setCancelText(R.string.ui_profile_dialog_edit_cancel)
                .setConfirmText(R.string.ui_profile_dialog_edit_confirm)
                .build()
                .show();
    }

    public void showEditMyTextDialog(@Nullable String text, MyTextEditedCallback callback) {

        EditText editText = (EditText) View.inflate(mContext, R.layout.profile_dialog_edit_text, null);
        editText.setText(text != null ? text : "");
        editText.requestFocus();
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(Config.PROFILE_TEXT_MAX_LENGTH)});
        editText.setSelection(0);

        AuraDialog dialog = new DialogBuilder(mContext, mDialogState)
                .setTitle(R.string.ui_profile_dialog_edit_text_title)
                .setView(editText)
                .enableKeyboard()
                .setOnConfirm(() -> callback.onTextEdited(editText.getText().toString()))
                .setCancelText(R.string.ui_profile_dialog_edit_cancel)
                .setConfirmText(R.string.ui_profile_dialog_edit_confirm)
                .build();

        // Accidentally cancelling can turn out frustrating, let's disable it
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.show();
    }

    public void showColorPickerDialog(String color,
                                      float selectedPointX,
                                      float selectedPointY,
                                      ColorPicker.ColorListener colorListener) {
        v(TAG, "Showing color picker dialog, x: %f, y: %f", selectedPointX, selectedPointY);

        ColorPicker colorPicker = (ColorPicker) View.inflate(mContext, R.layout.profile_dialog_edit_color, null);

        AuraDialog dialog = new DialogBuilder(mContext, mDialogState)
                .setTitle(R.string.ui_profile_dialog_edit_color_title)
                .setView(colorPicker)
                .setCancelText(R.string.ui_profile_dialog_edit_cancel)
                .setConfirmText(R.string.ui_profile_dialog_edit_confirm)
                .setOnConfirm(() -> {
                    if (mPickedColor != null && !mPickedColor.getColor().equals(color)) {
                        colorListener.onColorSelected(mPickedColor);
                    }
                })
                .build();

        TextView titleView = dialog.getTitleView();

        ColorPicker.ColorListener onChange = selectedColor -> {
            mPickedColor = selectedColor;
            int parsed = Color.parseColor(selectedColor.getColor());
            titleView.setBackgroundColor(parsed);
            titleView.setTextColor(ColorHelper.getTextColor(parsed));
        };

        colorPicker.init(selectedPointX, selectedPointY);
        colorPicker.setColorListener(onChange);
        onChange.onColorSelected(new ColorPicker.SelectedColor(color, 0, 0));

        dialog.show();
    }

    public void showDrop(Slogan slogan, DropCallback dropCallback) {
        new DialogBuilder(mContext, mDialogState)
                .setTitle(EmojiHelper.replaceShortCode(getString(R.string.ui_drop_dialog_title)))
                .setMessage(getString(R.string.ui_drop_dialog_message))
                .setOnConfirm(() -> dropCallback.onDropSlogan(slogan))
                .setConfirmText(R.string.ui_drop_dialog_confirm)
                .setCancelText(R.string.ui_drop_dialog_cancel)
                .build()
                .show();
    }

    void showReplace(TreeSet<Slogan> mySlogans, AdoptCallback adoptCallback) {

        RadioGroup radioGroup = (RadioGroup) View.inflate(mContext, R.layout.profile_dialog_replace_slogan, null);
        SparseArray<Slogan> map = new SparseArray<>();

        for (Slogan slogan : mySlogans) {
            RadioButton button = new RadioButton(mContext);
            button.setEllipsize(TextUtils.TruncateAt.END);
            button.setText(slogan.getText());
            button.setId(View.generateViewId());
            map.put(button.getId(), slogan);
            radioGroup.addView(button);
        }

        AuraDialog dialog = new DialogBuilder(mContext, mDialogState)
                .setTitle(R.string.ui_replace_dialog_title)
                // TODO pluralize
                .setMessage(getString(R.string.ui_replace_dialog_message)
                        .replaceAll("##maxSlogans##", Integer.toString(Config.PROFILE_SLOGANS_MAX_SLOGANS))
                )
                .setView(radioGroup)
                .setOnConfirm(() -> adoptCallback.onAdoptSlogan(map.get(radioGroup.getCheckedRadioButtonId())))
                .build();

        radioGroup.setOnCheckedChangeListener(($, $$) -> dialog.getConfirmButton().setEnabled(true));

        dialog.getConfirmButton().setEnabled(false);
        dialog.show();
    }


    public void showParametrizedSloganEdit(@StringRes int title,
                                           @Nullable Slogan slogan,
                                           OnSloganEditConfirm onConfirm) {

        EditText editText = (EditText) View.inflate(mContext, R.layout.profile_dialog_edit_slogan, null);
        editText.setHint(EmojiHelper.replaceShortCode(getString(R.string.ui_profile_dialog_edit_slogan_hint)));
        editText.setText(slogan != null ? slogan.getText() : "");

        new DialogBuilder(mContext, mDialogState)
                .setTitle(title)
                .setView(editText)
                .enableKeyboard()
                .setOnConfirm(() -> onConfirm.onConfirm(editText.getText().toString()))
                .build()
                .show();

        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(Config.PROFILE_NAME_MAX_LENGTH)});
        editText.requestFocus();

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
                if (count > Config.PROFILE_SLOGANS_MAX_LINE_BREAKS) {
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
            if (index > Config.PROFILE_SLOGANS_MAX_LINE_BREAKS) {
                filtered = true;
                m.appendReplacement(sb, "");
            }
        }
        // Show no toast if line breaks are disabled altogether
        //noinspection ConstantConditions
        if (filtered && Config.PROFILE_SLOGANS_MAX_LINE_BREAKS > 0) {
            String text = mContext.getResources().getQuantityString(
                    R.plurals.ui_dialog_edit_slogan_line_break_limit,
                    Config.PROFILE_SLOGANS_MAX_LINE_BREAKS,
                    Config.PROFILE_SLOGANS_MAX_LINE_BREAKS);
            Toast.makeText(mContext, EmojiHelper.replaceShortCode(text), Toast.LENGTH_SHORT).show();
        }
        m.appendTail(sb);
        return sb.toString();
    }

    void showBtBroken(BtBrokenDismissHandler btBrokenDismissHandler) {

        View dialogView = View.inflate(mContext, R.layout.common_dialog_bt_stack_broken, null);
        CheckBox checkBox = dialogView.findViewById(R.id.dont_show_again);

        new DialogBuilder(mContext, mDialogState)
                .setTitle(R.string.ui_dialog_bt_broken_title)
                .setMessage(R.string.ui_dialog_bt_broken_text)
                .setView(dialogView)
                .setOnConfirm(() -> btBrokenDismissHandler.onDismiss(checkBox.isChecked()))
                .build()
                .show();
    }
}
