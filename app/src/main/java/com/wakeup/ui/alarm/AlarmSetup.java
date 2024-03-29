package com.wakeup.ui.alarm;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.wakeup.MainActivity;
import com.wakeup.R;
import com.wakeup.alarm.AlarmUtils;
import com.wakeup.database.DatabaseManager;
import com.wakeup.fragment.MissionChoosingFragment;
import com.wakeup.fragment.RepeatDateFragment;
import com.wakeup.fragment.SoundChoosingFragment;
import com.wakeup.fragment.SoundRepeatFragment;
import com.wakeup.model.AlarmModel;
import com.wakeup.model.Mission;
import com.wakeup.shareData.MissonViewModel;
import com.wakeup.shareData.RepeatDateViewModel;
import com.wakeup.shareData.SoundRepeatViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class AlarmSetup extends AppCompatActivity implements View.OnClickListener {
    private List<Mission> missions;
    private NumberPicker hour;
    private NumberPicker minute;
    private NumberPicker inDay;
    private String[] inDayData;
    private ImageView missionOne, missionTwo;
    private MissonViewModel missonViewModel;
    private RepeatDateViewModel repeatDateViewModel;
    private SoundRepeatViewModel soundRepeatViewModel;
    private SoundRepeatFragment soundRepeatFragment;
    private TextView repeatDate, repeatMinute, soundChoosing;
    private MissionChoosingFragment missionChoosingFragment;
    private RepeatDateFragment repeatDateFragment;
    private SoundChoosingFragment soundChoosingFragment;
    private Button saveButton;
    private AlarmModel alarmModel;
    private DatabaseManager databaseManager;
    private String allMission = "";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alarm);
        initView();
        catchEvent();
        bindData();

    }


    private void missionChoosing(Mission mission, int icon) {
        mission.getMissionIcon().setImageResource(icon);
        mission.setStatus("done");
        exit();
    }

    private void catchEvent() {
        missionOne.setOnClickListener(this::onClick);
        missionTwo.setOnClickListener(this::onClick);
        repeatDate.setOnClickListener(this::onClick);
        repeatMinute.setOnClickListener(this::onClick);
        soundChoosing.setOnClickListener(this::onClick);
        inDay.setOnValueChangedListener((numberPicker, i, i1) -> {
            if (i1 == 0) {
                alarmModel.setPmAm("AM");
            } else {
                alarmModel.setPmAm("PM");
            }
        });
        saveButton.setOnClickListener(this::saveToDatabase);
        repeatDateViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                repeatDate.setText(s);
            }
        });
        missonViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                for (Mission mission : missions) {
                    if (mission.getStatus().equals("pending")) {
                        if (s.equalsIgnoreCase("Memory")) {
                            missionChoosing(mission, R.drawable.memmory_icon);
                            s = "";
                            allMission += "Memory ";
                        } else if (s.equalsIgnoreCase("Math")) {
                            missionChoosing(mission, R.drawable.math_icon);
                            s = "";
                            allMission += "Math ";
                        } else if (s.equalsIgnoreCase("Typing")) {
                            missionChoosing(mission, R.drawable.typing_icon);
                            s = "";
                            allMission += "Typing ";
                        }
                    }
                }
            }
        });
    }

    public int[] repeatDateProcess() {
        int[] repeatDates = {0, 0, 0, 0, 0, 0, 0} ;
        if (repeatDate.getText() != "Không báo lại. >") {

            String[] repeatDateText = repeatDate.getText().toString().split(", ");
            for (String s : repeatDateText) {
                if (s.equalsIgnoreCase("Chủ nhật")) {
                    repeatDates[0] = 1;
                } else if (s.equalsIgnoreCase("Thứ 2")) {
                    repeatDates[1] = 1;
                } else if (s.equalsIgnoreCase("Thứ 3")) {
                    repeatDates[2] = 1;
                } else if (s.equalsIgnoreCase("Thứ 4")) {
                    repeatDates[3] = 1;
                } else if (s.equalsIgnoreCase("Thứ 5")) {
                    repeatDates[4] = 1;
                } else if (s.equalsIgnoreCase("Thứ 6")) {
                    repeatDates[5] = 1;
                } else if (s.equalsIgnoreCase("Thứ 7")) {
                    repeatDates[6] = 1;
                }
            }
        }
        return repeatDates;
    }

    public int soundProcess() {
        String sound = soundChoosing.getText().toString();
        switch (sound) {
            case "Báo thức dậy đi em ơi không ai cứu được em đâu.":
                return 1;
            case "Báo thức quân đội.":
                return 2;
            case "Báo thức dậy đi ông cháu ơi.":
                return 3;
        }
        return 0;
    }

    private void saveToDatabase(View view) {
        String[] mission = new String[]{"", ""};
        if (!allMission.equals("")) {
            mission = allMission.split(" ");
        }
        String[] repeatTime;
        if (repeatMinute.getText().equals("Không báo lại. >")) {
            alarmModel.setRepeatTime(0);
        } else {
            repeatTime = repeatMinute.getText().toString().split(" ");
            alarmModel.setRepeatTime(Integer.parseInt(repeatTime[0]));
        }
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour.getValue());
        calendar.set(Calendar.MINUTE, minute.getValue());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if(alarmModel.getPmAm().equals("AM")){
            calendar.set(Calendar.AM_PM, Calendar.AM);
        } else {
            calendar.set(Calendar.AM_PM, Calendar.PM);
        }
        alarmModel.setTime(calendar.getTimeInMillis());
        validateTime(alarmModel);
        alarmModel.setHours(hour.getValue()+"");
        alarmModel.setMinutes(minute.getValue()+"");
        alarmModel.setMission(mission);
        alarmModel.setOn(1);
        alarmModel.setRepeatDate(repeatDateProcess());
        alarmModel.setSound(soundProcess());
        try {
            databaseManager.addAlarm(alarmModel);
            AlarmUtils.create(this);
            Toast.makeText(this, "Báo thức đã được đặt thành công!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
        } catch (NumberFormatException numberFormatException){
            databaseManager.deleteAllAlarm();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void validateTime(AlarmModel alarmModel) {
        if (alarmModel.getTime() < System.currentTimeMillis()) {
            alarmModel.setTime(alarmModel.getTime() + 24 * 60 * 60 * 1000);
        } else {
            alarmModel.setTime(alarmModel.getTime());
        }
    }

    private void bindData() {
        setUpNumberPicker(hour, 1, 12);
        setUpNumberPicker(minute, 0, 59);
        Calendar calendar = Calendar.getInstance();
        hour.setValue(calendar.get(Calendar.HOUR));
        minute.setValue(calendar.get(Calendar.MINUTE));
        inDay.setMinValue(0);
        inDay.setMaxValue(1);
        inDay.setValue(calendar.get(Calendar.AM_PM) == 0 ? 0 : 1);
        alarmModel.setPmAm(inDayData[inDay.getValue()]);
//        Toast.makeText(this, "AM_PM"+alarmModel.getPmAm(), Toast.LENGTH_SHORT).show();
        inDay.setDisplayedValues(inDayData);
        inDay.setValue(calendar.get(Calendar.AM_PM));
    }

    private void initView() {
        hour = findViewById(R.id.hour);
        minute = findViewById(R.id.minute);
        inDay = findViewById(R.id.inDay);
        inDayData = getResources().getStringArray(R.array.inDays);
        missionOne = findViewById(R.id.mission_one);
        missionTwo = findViewById(R.id.mission_two);
        missonViewModel = ViewModelProviders.of(this).get(MissonViewModel.class);
        missions = new ArrayList<>();
        missions.add(new Mission(missionOne, "pending"));
        missions.add(new Mission(missionTwo, "pending"));
        repeatDate = findViewById(R.id.repeat_date);
        repeatDateViewModel = ViewModelProviders.of(this).get(RepeatDateViewModel.class);
        missionChoosingFragment = new MissionChoosingFragment();
        repeatDateFragment = new RepeatDateFragment();
        repeatMinute = findViewById(R.id.tab3_layout2);
        soundRepeatFragment = new SoundRepeatFragment();
        soundRepeatViewModel = ViewModelProviders.of(this).get(SoundRepeatViewModel.class);
        soundChoosingFragment = new SoundChoosingFragment();
        soundChoosing = findViewById(R.id.soundChoosing);
        saveButton = findViewById(R.id.save_alarm_button);
        alarmModel = new AlarmModel();
        databaseManager = new DatabaseManager(this);
    }

    private void setUpNumberPicker(NumberPicker numberPicker, int start, int end) {
        numberPicker.setMinValue(start);
        numberPicker.setMaxValue(end);
    }

    private void exit() {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        fragmentTransaction.remove(missionChoosingFragment);
        fragmentTransaction.commit();
    }

    private void openFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        fragmentTransaction.add(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.mission_one || v.getId() == R.id.mission_two) {
            openFragment(missionChoosingFragment);
        } else if (v.getId() == R.id.repeat_date) {
            openFragment(repeatDateFragment);
        } else if (v.getId() == R.id.tab3_layout2) {
            openFragment(soundRepeatFragment);
        } else if (v.getId() == R.id.soundChoosing) {
            openFragment(soundChoosingFragment);
        }
    }

    @Override
    public void onBackPressed() {
        // Start the MainActivity when back button is pressed
        super.onBackPressed();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

}
