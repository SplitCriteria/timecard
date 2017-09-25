package com.splitcriteria.timecard;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Deuce on 9/23/17.
 */

public class Dialogs {

    static final String EXTRA_BUNDLE = "com.splitcriteria.timecard.extra_bundle";

    interface OnDialogResultListener {
        void onDialogResult(int requestCode, int resultCode, Intent intent);
    }

    /**
     * A DialogResultFragment allows the client to attach an OnDialogResultListener
     * which child fragments should call when a result if available. A result is
     * returned with a set request code and a bundle of extras which can be retrieved
     * by calling Intent.getBundleExtra(EXTRA_BUNDLE)
     */
    public static abstract class DialogResultFragment extends DialogFragment {
        private int mRequestCode = -1;
        private Bundle mExtras;
        private OnDialogResultListener mListener;

        DialogResultFragment setOnDialogResultListener(OnDialogResultListener listener) {
            mListener = listener;
            return this;
        }

        DialogResultFragment putExtras(Bundle extras) {
            mExtras = extras;
            // In case setArguments has already been called, go ahead and re-set the extras
            Bundle args = getArguments();
            if (args != null) {
                args.putBundle(EXTRA_BUNDLE, mExtras);
            }
            return this;
        }

        DialogResultFragment setRequestCode(int requestCode) {
            mRequestCode = requestCode;
            return this;
        }

        @Override
        public void setArguments(Bundle args) {
            // Add the extra bundle to arguments to save it across destroy/creation cycles
            args.putBundle(EXTRA_BUNDLE, mExtras);
            super.setArguments(args);
        }

        /**
         * Returns a result to a OnDialogResultListener which includes a result
         *
         * @param resultCode    the result code to return
         * @param intent        an Intent to return, or null
         * @return  true, if the listener existed and was called; false otherwise
         */
        protected boolean returnResult(int resultCode, Intent intent) {
            if (intent == null) {
                intent = new Intent();
            }
            Bundle extras = getArguments().getBundle(EXTRA_BUNDLE);
            intent.putExtra(EXTRA_BUNDLE, extras);
            if (mListener == null) {
                return false;
            } else {
                mListener.onDialogResult(mRequestCode, resultCode, intent);
                return true;
            }
        }
    }

    /**
     * Dialog Fragment designed to display a message to a user
     */
    public static class SimpleMessageDialogFragment extends DialogResultFragment {

        static final String TITLE = "title";
        static final String MESSAGE = "message";

        public SimpleMessageDialogFragment() {}

        public static SimpleMessageDialogFragment createSimpleMessageDialog(
                String title, String message) {
            SimpleMessageDialogFragment dialogFragment = new SimpleMessageDialogFragment();
            Bundle args = new Bundle();
            args.putString(TITLE, title);
            args.putString(MESSAGE, message);
            dialogFragment.setArguments(args);
            return dialogFragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            Bundle arguments = getArguments();
            String title = arguments.getString(TITLE);
            String message = arguments.getString(MESSAGE);
            builder.setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            returnResult(Activity.RESULT_OK, null);
                            dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                                       new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            returnResult(Activity.RESULT_CANCELED, null);
                            dismiss();
                        }
                    });
            return builder.create();
        }
    }

    /**
     * Dialog Fragment designed to collect extra data from the user
     */
    public static class UserInputDialogFragment extends DialogResultFragment {

        public static final String KEY_USER_INPUT = "user_input";

        private static final String KEY_TITLE = "title";
        private static final String KEY_MESSAGE = "message";
        private static final String KEY_POSITIVE_BUTTON = "positive_button";

        public UserInputDialogFragment() {}

        public static UserInputDialogFragment createUserInputDialog(
                String title, String message, int positiveButtonText, String defaultInput) {
            UserInputDialogFragment dialogFragment = new UserInputDialogFragment();
            Bundle args = new Bundle();
            args.putString(KEY_TITLE, title);
            args.putString(KEY_MESSAGE, message);
            args.putString(KEY_USER_INPUT, defaultInput);
            args.putInt(KEY_POSITIVE_BUTTON, positiveButtonText);
            dialogFragment.setArguments(args);
            return dialogFragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            View view = LayoutInflater.from(getActivity())
                    .inflate(R.layout.dialog_user_input, null);
            final EditText editText = view.findViewById(R.id.text);
            Bundle args = getArguments();
            String title = args.getString(KEY_TITLE);
            String message = args.getString(KEY_MESSAGE);
            String defaultInput = args.getString(KEY_USER_INPUT);
            editText.setText(defaultInput);
            builder.setView(view)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(args.getInt(KEY_POSITIVE_BUTTON),
                                       new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    // Get the user input from the user
                                    String userInput = editText.getText() == null ? null :
                                            editText.getText().toString();
                                    Intent intent = new Intent();
                                    intent.putExtra(KEY_USER_INPUT, userInput);
                                    returnResult(Activity.RESULT_OK, intent);
                                    dismiss();
                                }
                            })
                    .setNegativeButton(android.R.string.cancel,
                                       new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            returnResult(Activity.RESULT_CANCELED, null);
                            dismiss();
                        }
                    });
            return builder.create();
        }
    }
    /**
     * Dialog Fragment designed to collect the initial project information from the user
     */
    public static class PickDateDialogFragment extends DialogResultFragment implements
            DatePickerDialog.OnDateSetListener {

        public static final String KEY_YEAR = "year";
        public static final String KEY_MONTH = "month";
        public static final String KEY_DAY_OF_MONTH = "day";

        public PickDateDialogFragment() {}

        public static PickDateDialogFragment createPickDateDialogFragment(Calendar date) {
            PickDateDialogFragment dialogFragment = new PickDateDialogFragment();
            if (date == null) {
                date = Calendar.getInstance(Locale.getDefault());
            }
            Bundle args = new Bundle();
            args.putInt(KEY_YEAR, date.get(Calendar.YEAR));
            args.putInt(KEY_MONTH, date.get(Calendar.MONTH));
            args.putInt(KEY_DAY_OF_MONTH, date.get(Calendar.DAY_OF_MONTH));
            dialogFragment.setArguments(args);
            return dialogFragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Bundle args = getArguments();
            return new DatePickerDialog(getActivity(), this, args.getInt(KEY_YEAR),
                                                             args.getInt(KEY_MONTH),
                                                             args.getInt(KEY_DAY_OF_MONTH));
        }

        @Override
        public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
            Intent intent = new Intent();
            intent.putExtra(KEY_YEAR, year);
            intent.putExtra(KEY_MONTH, month);
            intent.putExtra(KEY_DAY_OF_MONTH, dayOfMonth);
            returnResult(Activity.RESULT_OK, intent);
        }
    }

    /**
     * Dialog Fragment designed to collect the initial project information from the user
     */
    public static class PickTimeDialogFragment extends DialogResultFragment implements
            TimePickerDialog.OnTimeSetListener {

        public static final String KEY_HOUR_OF_DAY = "hour_of_day";
        public static final String KEY_MINUTE = "minute";

        public PickTimeDialogFragment() {}

        public static PickTimeDialogFragment createPickTimeDialogFragment(Calendar date) {
            PickTimeDialogFragment dialogFragment = new PickTimeDialogFragment();
            if (date == null) {
                date = Calendar.getInstance(Locale.getDefault());
            }
            Bundle args = new Bundle();
            args.putInt(KEY_HOUR_OF_DAY, date.get(Calendar.HOUR_OF_DAY));
            args.putInt(KEY_MINUTE, date.get(Calendar.MINUTE));
            dialogFragment.setArguments(args);
            return dialogFragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Bundle args = getArguments();
            return new TimePickerDialog(getActivity(), this,
                    args.getInt(KEY_HOUR_OF_DAY), args.getInt(KEY_MINUTE),
                    DateFormat.is24HourFormat(getActivity()));
        }

        @Override
        public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {
            Intent intent = new Intent();
            intent.putExtra(KEY_HOUR_OF_DAY, hourOfDay);
            intent.putExtra(KEY_MINUTE, minute);
            returnResult(Activity.RESULT_OK, intent);
        }
    }
}
