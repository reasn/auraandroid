package io.auraapp.auraandroid.ui;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.auraapp.auraandroid.R;


public class FullWidthDialog extends Dialog {

    private final LinearLayout mRootView;
    private TextView mTitleView;
    private DialogManager.DialogState mDialogState;
    private Button mCancelButton;
    private Button mConfirmButton;

    public static FullWidthDialog create(@NonNull Context context,
                                         String title,
                                         boolean keyboard,
                                         DialogManager.DialogState dialogState,
                                         Runnable onConfirm) {

        LinearLayout rootView = (LinearLayout) View.inflate(context, R.layout.common_full_width_dialog, null);

        return new FullWidthDialog(context, rootView, title, keyboard, dialogState, onConfirm);
    }

    private FullWidthDialog(@NonNull Context context,
                            LinearLayout rootView,
                            String title,
                            boolean keyboard,
                            DialogManager.DialogState dialogState,
                            Runnable onConfirm) {

        super(context, R.style.FullWidthDialog);
        mDialogState = dialogState;
        mRootView = rootView;

        init(title, keyboard, onConfirm);
    }

    private void init(String title,
                      boolean keyboard,
                      Runnable onConfirm) {

        mTitleView = mRootView.findViewById(R.id.common_dialog_title);
        mTitleView.setText(title);

        setContentView(mRootView);
        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//            getWindow().setBackgroundDrawableResource(R.color.dialogBackground);

            if (keyboard) {
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            }
//            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
//            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        mCancelButton = mRootView.findViewById(R.id.common_dialog_cancel);
        mCancelButton.setOnClickListener($ -> dismiss());
        mConfirmButton = mRootView.findViewById(R.id.common_dialog_confirm);
        mConfirmButton.setOnClickListener($ -> {
            dismiss();
            if (onConfirm != null) {
                onConfirm.run();
            }
        });

        super.setOnDismissListener($ -> mDialogState.open = false);

        setCanceledOnTouchOutside(true);
    }

    public void setContent(@NonNull View view) {
        ((LinearLayout) mRootView.findViewById(R.id.common_dialog_view_wrapper)).addView(view);
    }

    public void setMessage(@StringRes int message) {
        ((TextView) mRootView.findViewById(R.id.common_dialog_message)).setText(message);
        mRootView.findViewById(R.id.common_dialog_message).setVisibility(View.VISIBLE);
    }

    public void setMessage(String message) {
        ((TextView) mRootView.findViewById(R.id.common_dialog_message)).setText(message);
        mRootView.findViewById(R.id.common_dialog_message).setVisibility(View.VISIBLE);
    }

    public TextView getTitleView() {
        return mTitleView;
    }

    public Button getConfirmButton() {
        return mConfirmButton;
    }

    public Button getCancelButton() {
        return mCancelButton;
    }

    @Override
    public void setOnDismissListener(@Nullable OnDismissListener listener) {
        if (listener != null) {
            super.setOnDismissListener(dialog -> {
                mDialogState.open = false;
                listener.onDismiss(dialog);
            });
        } else {
            super.setOnDismissListener($ -> mDialogState.open = false);
        }
    }

    @Override
    public void show() {
        if (!mDialogState.open) {
            mDialogState.open = true;
            super.show();
        }
    }
}
