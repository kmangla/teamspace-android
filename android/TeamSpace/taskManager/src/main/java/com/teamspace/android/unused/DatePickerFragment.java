package com.teamspace.android.unused;

import java.util.Calendar;
import java.util.GregorianCalendar;

import com.teamspace.android.common.ui.TaskManagerApplication;
import com.teamspace.android.models.Task;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

public class DatePickerFragment extends DialogFragment implements
		DatePickerDialog.OnDateSetListener {

	private Task task;
	private TaskListFragment fragment;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Use the current date as the default date in the picker
		final Calendar c = Calendar.getInstance();
		int year = c.get(Calendar.YEAR);
		int month = c.get(Calendar.MONTH);
		int day = c.get(Calendar.DAY_OF_MONTH);

		// Create a new instance of DatePickerDialog and return it
		return new DatePickerDialog(getActivity(), this, year, month, day);
	}

	public void onDateSet(DatePicker view, int year, int month, int day) {
		TaskManagerApplication context = (TaskManagerApplication) getActivity()
				.getApplication();
		Calendar calendar = new GregorianCalendar(year, month, day);
		context.updateTaskPause(task, (calendar.getTimeInMillis() / 1000L));
		fragment.notifyDataSetChanged();
	}

	public DatePickerFragment setTask(Task t) {
		task = t;
		return this;
	}

	public DatePickerFragment setTaskFragment(TaskListFragment f) {
		this.fragment = f;
		return this;
	}

}
