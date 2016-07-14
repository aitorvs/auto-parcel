package com.aitorvs.autoparcel;

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

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * An annotation that indicates the auto-parcel {@link ParcelTypeAdapter} to use to
 * parcel and unparcel the field.  The value must be set to a valid {@link ParcelTypeAdapter}
 * class.
 *
 * <pre>
 * <code>
 * {@literal @}AutoParcel public abstract class Foo {
 *   {@literal @}ParcelAdapter(DateTypeAdapter.class) public abstract Date date;
 * }
 * </code>
 * </pre>
 *
 * The generated code will instantiate and use the {@code DateTypeAdapter} class to parcel and
 * unparcel the {@code date()} property. In order for the generated code to instantiate the
 * {@link ParcelTypeAdapter}, it needs a public, no-arg constructor.
 */
@Target(FIELD)
@Retention(SOURCE)
@Documented
public @interface ParcelAdapter {
    Class<? extends ParcelTypeAdapter<?>> value();
}