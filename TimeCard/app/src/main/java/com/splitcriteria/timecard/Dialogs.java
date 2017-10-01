package com.splitcriteria.timecard;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
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
import java.util.Locale;

/**
 * Timecard - Allows users to easily track time-based data for analysis.
 * Copyright (C) 2017  Nicholas Johnson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Timecard - Copyright (C) 2017  Nicholas Johnson
 *
 * This program comes with ABSOLUTELY NO WARRANTY; for details type `show w'.
 * This is free software, and you are welcome to redistribute it
 * under certain conditions; type `show c' for details.
 *
 * Provides template DialogFragment's which inherit from ResultFragment which
 * provides common use cases.
 */

public class Dialogs {

    /**
     * Dialog Fragment designed to display a message to a user
     */
    public static class SimpleMessageDialogFragment extends ResultFragment {

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
    public static class UserInputDialogFragment extends ResultFragment {

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
    public static class PickDateDialogFragment extends ResultFragment implements
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
    public static class PickTimeDialogFragment extends ResultFragment implements
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
