package com.aitorvs.android.autoparcel.model;

/*
 * Copyright (C) 13/07/16 aitorvs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.aitorvs.autoparcel.AutoParcel;
import com.aitorvs.autoparcel.ParcelAdapter;

import java.util.Date;

@AutoParcel
public abstract class Person {
    @Nullable
    public String name;
    @ParcelAdapter(DateTypeAdapter.class)
    public Date birthday;
    public int age;

    public static Person create(@NonNull String name, @NonNull Date birthday, int age) {
        return new AutoParcel_Person(name, birthday, age);
    }
}
