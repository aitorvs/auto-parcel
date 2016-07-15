package com.aitorvs.android.autoparcel;

import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class PersonActivity extends AppCompatActivity {

    private static final String EXTRA_PERSON = "EXTRA_PERSON";

    @Nullable
    public static Intent createIntent(@NonNull Context context, Person person) {
        //noinspection ConstantConditions
        if (context == null) {
            return null;
        }
        Intent intent = new Intent(context, PersonActivity.class);
        // we need to cast it to Parcelable because Person does not itself implement parcelable
        intent.putExtra(EXTRA_PERSON, (Parcelable) person);

        return intent;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person);

        TextView mFullName = (TextView) findViewById(R.id.fullName);
        TextView mDate = (TextView) findViewById(R.id.dateOfBirth);
        TextView mAge = (TextView) findViewById(R.id.age);

        // get the passed intent
        Intent intent = getIntent();
        if (intent != null) {
            Person person = intent.getParcelableExtra(EXTRA_PERSON);
            mFullName.setText(getString(R.string.formatName, person.name));
            mDate.setText(getString(R.string.format_date, person.birthday.toString()));
            mAge.setText(getString(R.string.format_age, person.age));
        }
    }
}
