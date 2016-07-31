package com.aitorvs.android.autoparcel;

import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import com.aitorvs.android.autoparcel.model.Person;

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

        TextView fullName = (TextView) findViewById(R.id.fullName);
        TextView date = (TextView) findViewById(R.id.dateOfBirth);
        TextView age = (TextView) findViewById(R.id.age);
        TextView fullAddress = (TextView) findViewById(R.id.fullAddress);

        // get the passed intent
        Intent intent = getIntent();
        if (intent != null) {
            Person person = intent.getParcelableExtra(EXTRA_PERSON);
            fullName.setText(getString(R.string.formatName, person.name));
            date.setText(getString(R.string.format_date, person.birthday.toString()));
            age.setText(getString(R.string.format_age, person.age));
            fullAddress.setText(getString(R.string.full_address,
                    TextUtils.isEmpty(person.address.street) ? "<street>" : person.address.street,
                    TextUtils.isEmpty(person.address.postCode) ? "<PC>" : person.address.postCode,
                    TextUtils.isEmpty(person.address.city) ? "<city>" : person.address.city,
                    TextUtils.isEmpty(person.address.country) ? "<country>" : person.address.country));
        }
    }
}
