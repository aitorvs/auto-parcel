package com.aitorvs.android.autoparcel.model;

/*
 * Copyright (C) 14/07/16 aitorvs
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

import android.os.Parcel;

import com.aitorvs.autoparcel.ParcelTypeAdapter;

import java.util.Date;

class DateTypeAdapter implements ParcelTypeAdapter<Date> {
    @Override
    public Date fromParcel(Parcel in) {
        return new Date(in.readLong());
    }

    @Override
    public void toParcel(Date value, Parcel dest) {
        dest.writeLong(value.getTime());
    }
}
