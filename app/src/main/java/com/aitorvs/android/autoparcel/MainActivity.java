package com.aitorvs.android.autoparcel;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.aitorvs.android.autoparcel.model.Address;
import com.aitorvs.android.autoparcel.model.Person;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private EditText mAgeEditText;
    private EditText mNameEditText;
    private EditText mBdayEditText;
    private EditText mStreetEditText;
    private EditText mPostcodeEditText;
    private EditText mCityEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mNameEditText = (EditText) findViewById(R.id.fullName);
        mBdayEditText = (EditText) findViewById(R.id.dateOfBirth);
        mAgeEditText = (EditText) findViewById(R.id.age);
        mStreetEditText = (EditText) findViewById(R.id.street);
        mPostcodeEditText = (EditText) findViewById(R.id.postCode);
        mCityEditText = (EditText) findViewById(R.id.city);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
                // Create the parcelable object using the creator
                try {
                    int age = TextUtils.isEmpty(mAgeEditText.getText()) ? 0 : Integer.parseInt(mAgeEditText.getText().toString());
                    Date date = TextUtils.isEmpty(mBdayEditText.getText()) ? new Date(System.currentTimeMillis()) : df.parse(mBdayEditText.getText().toString());
                    Address address = Address.create(
                            mStreetEditText.getText().toString(),
                            mPostcodeEditText.getText().toString(),
                            mCityEditText.getText().toString(),
                            /* Country */ null);
                    Person person = Person.create(
                            mNameEditText.getText().toString(),
                            date, age, address);

                    Intent activityIntent = PersonActivity.createIntent(MainActivity.this, person);

                    if (activityIntent != null) {
                        MainActivity.this.startActivity(activityIntent);
                    }
                } catch (ParseException e) {
                    Log.e(TAG, "onClick: Error parsing date", e);
                }
            }
        });
    }

}
