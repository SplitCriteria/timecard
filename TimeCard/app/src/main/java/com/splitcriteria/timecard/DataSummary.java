package com.splitcriteria.timecard;

import android.content.Context;
import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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
 * Contains static methods for summarizing rows of data from ProjectData
 */

class DataSummary {

    private static final int SEC_PER_YEAR = 31536000;
    private static final int SEC_PER_DAY = 86400;
    private static final int SEC_PER_HOUR = 3600;
    private static final int SEC_PER_MIN = 60;

    private static String getTimeText(Context context, int seconds) {
        int years = seconds / SEC_PER_YEAR;
        int days = (seconds % SEC_PER_YEAR) / SEC_PER_DAY;
        int hours = (seconds % SEC_PER_DAY) / SEC_PER_HOUR;
        int minutes = (seconds % SEC_PER_HOUR) / SEC_PER_MIN;
        int sec = seconds % SEC_PER_MIN;
        if (years > 0) {
            return context.getResources().getString(
                    R.string.time_years, years, days, hours, minutes, sec);
        } else if (days > 0) {
            return context.getResources().getString(
                    R.string.time_days, days, hours, minutes, sec);
        } else if (hours > 0) {
            return context.getResources().getString(
                    R.string.time_hours, hours, minutes, sec);
        } else if (minutes > 0) {
            return context.getResources().getString(
                    R.string.time_minutes, minutes, sec);
        } else {
            return context.getResources().getString(
                    R.string.time_seconds, sec);
        }
    }

    static String getSummary(Context context, ProjectData.ExtendedMetadata extendedMetadata) {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        return getTimeText(context, (int)(extendedMetadata.totalTime +
                (calendar.getTimeInMillis()/1000 - extendedMetadata.metadataTime)));
    }

    static String getSummary(Context context, String summaryMethod, List<ProjectData.Row> data) {

        if (TextUtils.isEmpty(summaryMethod)) {
            summaryMethod = context.getString(R.string.preferences_summary_type_default_value);
        }

        // Reverse the data, since it is given in reverse chronological order
        Collections.reverse(data);

        // Calculate the total time (in seconds)
        int count = 0;
        int totalTime = 0;
        double extraData = 0.0d;
        int extraDataCount = 0;
        List<Integer> periods = new ArrayList<>();
        Calendar firstStartTime = data.size() > 0 ? data.get(0).startTime : null;
        Calendar lastStartTime = null;
        Calendar lastEndTime = null;
        for (ProjectData.Row datum : data) {
            // Total the time
            Calendar endTime = datum.endTime;
            if (datum.endTime == null) {
                endTime = Calendar.getInstance(Locale.getDefault());
                endTime.setTime(new Date());
            }
            totalTime += (endTime.getTimeInMillis() - datum.startTime.getTimeInMillis()) / 1000;
            // Total the extra data (if it's a number)
            try {
                if (datum.extraData != null) {
                    extraData += Double.parseDouble(datum.extraData);
                    extraDataCount += 1;
                }
            } catch (NumberFormatException error) {
                // No operation needed -- it's expected that some of the extra data
                // won't be a number. We don't want to count those
            }
            // Record the period between the last end time and the current start time
            if (lastEndTime != null) {
                periods.add((int)((datum.startTime.getTimeInMillis() -
                                   lastEndTime.getTimeInMillis()) / 1000));
            }
            // Record the last start and end times
            lastStartTime = datum.startTime;
            lastEndTime = endTime;
            // Increase the row count
            count++;
        }

        if (count == 0) {
            return context.getString(R.string.data_summary_unknown);
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat(
                context.getString(R.string.data_summary_simple_date_format), Locale.getDefault());

        if (summaryMethod.equals(
                context.getString(R.string.preferences_summary_type_auto))) {
            if (count > 0) {
                summaryMethod = context.getString(R.string.preferences_summary_type_total_time);
            } else if (extraDataCount > 0) {
                summaryMethod = context.getString(
                        R.string.preferences_summary_type_average_extra_data);
            } else {
                return context.getString(R.string.data_summary_unknown);
            }
        }

        if (summaryMethod.equals(
                context.getString(R.string.preferences_summary_type_total_time))) {
            return getTimeText(context, totalTime);
        } else if (summaryMethod.equals(
                context.getString(R.string.preferences_summary_type_average_time))) {
            return context.getString(
                    R.string.time_average, getTimeText(context, totalTime / count));
        } else if (summaryMethod.equals(
                context.getString(R.string.preferences_summary_type_average_extra_data))) {
            if (extraDataCount == 0) {
                return context.getString(R.string.average_extra_data_none);
            } else {
                return context.getString(R.string.average_extra_data, extraData / extraDataCount);
            }
        } else if (summaryMethod.equals(
                context.getString(R.string.preferences_summary_type_count))) {
            return context.getString(R.string.data_summary_count, count,
                                     dateFormat.format(firstStartTime.getTime()));
        } else if (summaryMethod.equals(
                context.getString(R.string.preferences_summary_type_frequency))) {
            // Calculate the average frequency
            int totalPeriod = 0;
            for (Integer period : periods) {
                totalPeriod += period;
            }
            int averagePeriod = totalPeriod / count;
            lastEndTime.add(Calendar.SECOND, averagePeriod);
            return context.getString(R.string.data_summary_frequency_and_next_occurrence,
                                     getTimeText(context, averagePeriod),
                                     dateFormat.format(lastEndTime.getTime()));
        } else {
            return context.getString(R.string.data_summary_unknown);
        }

    }
}
