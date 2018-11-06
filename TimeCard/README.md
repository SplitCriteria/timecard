# Time Card

## Purpose
Time Card allows users to quickly (at the press of a button from the app, widget, or notification) clock in and out of projects in order to record time spent on an activity.

By exporting this data, the user is then able to use a third party program (e.g. Excel, Google Sheets) to analyze the data.
 
Examples:
* Record your time spent working out by clocking in and out at the start and end or your workouts

## Planned Updates
- [x] Inform using trying to create a project which already exists
- [x] Create single use data tracking (i.e. start time and end time are the same)
- [x] Add additional data to be associated with time data (e.g. billing rate, weight)
- [x] Add project settings in ProjectActivity
- [x] Add extra data to be added via a "Clock Out" notification
- [x] Create a persistent backup (e.g. Service which automatically backs up database)
- [x] Create a backup activity which allows removal of backup,
- [x] Add summary of data on project list screen and options in project preferences (e.g. total duration, average)
- [x] Add confirmation dialog when restoring backup
- [x] Add ability to suppress the "clock out" notification (i.e. no indication that project is clocked in)
- [x] Group project settings into logical bins
- [x] Add new data summary options (i.e. count, frequency, projected next event)
- [ ] Allow a widget to get Extra Data from a user if they are clicking into a project which uses extra data but doesn't have default data specified
- [ ] Allow recording/showing of backup/restore history, etc.
- [ ] Add location tracking
- [ ] Create Shortcut to replace the clock in/out widget
- [x] Add ability to rename projects
- [x] Add ability to import backed-up project data
- [ ] Add ability to sort project list (e.g. most used, favorite, custom order)
- [x] Edit database using Date Picker
- [x] Add Extra Data Title use for export (could include commas in title and data to add virtually unlimited extra columns)
- [ ] Add option to automatically capture broadcast events (e.g. phone up/down with Contact name as extra data)
- [ ] Use LocalBroadcastManager and/or Intent.setPackage() for clock in/out and ProjectReceiver
- [ ] Use Voice actions to clock in/out, etc

## Errors to Fix
- [ ] If data deleted (in Edit mode) the remaining rows in the adapter don't account for the missing data (if others are edited, their row number is off by one)
- [ ] Clocking out of a non-instant timecard (with extra data) asks for extra data again
- [x] New projects start show the name "auto" instead of the actual name
- [x] Project rows are sorted according to display name instead of project name
- [x] Frequency & Next Occurrence summary incorrectly calculates the frequency