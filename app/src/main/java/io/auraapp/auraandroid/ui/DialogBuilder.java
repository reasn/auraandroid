package io.auraapp.auraandroid.ui;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.StringRes;
import android.view.View;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.EmojiHelper;

public class DialogBuilder {

    private Context mContext;
    private String mTitle;
    private View mView = null;
    private boolean keyboard;
    private DialogManager.DialogState mDialogState;
    private Runnable mOnConfirm;
    private String mCancelText;
    private String mConfirmText;
    private String mMessage;

    public DialogBuilder(Context context, DialogManager.DialogState dialogState) {
        mContext = context;
        mDialogState = dialogState;
        mCancelText = context.getString(R.string.common_dialog_cancel);
        mConfirmText = context.getString(R.string.common_dialog_confirm);
    }

    public DialogBuilder setTitle(@StringRes int title) {
        this.mTitle = mContext.getString(title);
        return this;
    }

    public DialogBuilder setTitle(String title) {
        this.mTitle = title;
        return this;
    }

    public DialogBuilder setMessage(@StringRes int message) {
        mMessage = mContext.getString(message);
        return this;
    }

    public DialogBuilder setMessage(String message) {
        mMessage = message;
        return this;
    }

    public DialogBuilder setView(@LayoutRes int layout) {
        mView = View.inflate(mContext, layout, null);
        return this;
    }

    public DialogBuilder setView(View view) {
        mView = view;
        return this;
    }

    public DialogBuilder enableKeyboard() {
        keyboard = true;
        return this;
    }

    public DialogBuilder setOnConfirm(Runnable onConfirm) {
        mOnConfirm = onConfirm;
        return this;
    }

    public DialogBuilder setCancelText(String cancelText) {
        this.mCancelText = cancelText;
        return this;
    }

    public DialogBuilder setConfirmText(String confirmText) {
        this.mConfirmText = confirmText;
        return this;
    }

    public DialogBuilder setCancelText(@StringRes int cancelText) {
        this.mCancelText = mContext.getString(cancelText);
        return this;
    }

    public DialogBuilder setConfirmText(@StringRes int confirmText) {
        this.mConfirmText = mContext.getString(confirmText);
        return this;
    }

    public FullWidthDialog build() {
        FullWidthDialog dialog = FullWidthDialog.create(
                mContext,
                EmojiHelper.replaceShortCode(mTitle),
                keyboard,
                mDialogState,
                mOnConfirm);

        if (mView != null) {
            dialog.setContent(mView);
        }

        dialog.getCancelButton().setText(mCancelText);
        dialog.getConfirmButton().setText(mConfirmText);

        if (mMessage != null) {
            dialog.setMessage(mMessage);
        }

        return dialog;
    }
}
