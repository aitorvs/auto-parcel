# Auto-Parcel

A fast annotation processor to make your objects `Parcelable` without
writing any of the boilerplate.

The project is inspired in [auto-value-parcel](https://github.com/rharter/auto-value-parcel/) 
extension and some of the code and utils are borrowed from it.

## Background

[Parcelable](https://developer.android.com/reference/android/os/Parcelable.html) classes
are great to put your objects into a [Bundle](https://developer.android.com/reference/android/os/Bundle.html) 
and/or send them across an [Intent](https://developer.android.com/reference/android/content/Intent.html).
But there is a non-negligible boilerplate one has to write to make a class
parcelable and, let's be clear, nobody should ever need to write this code.
It is uninteresting code and highly prone to error.

There are a number of libraries out there to make your life simpler when
using parcelables. Some create [wrappers](https://github.com/johncarl81/parceler) around your object to parcel/unparcel,
some others generate code.
This is yet-another-parcelable-library that uses Android Studio
annotation processors to generate your parcelable classes at compile time.

## Installation
[![](https://jitpack.io/v/aitorvs/auto-parcel.svg)](https://jitpack.io/#aitorvs/auto-parcel)

### Repository

Add the following repo to your `app/build.gradle`

```gradle
repositories {
    maven { url "https://jitpack.io" }
}
```

### Dependencies

Add the following to gradle dependencies:

```gradle
dependencies {
    
    //... other dependencies here
    
    provided 'com.github.aitorvs.auto-parcel:library:0.1.0-rc1'
    apt 'com.github.aitorvs.auto-parcel:compiler:0.1.0-rc1'
}
```

The annotation processor requires [android-apt](https://bitbucket.org/hvisser/android-apt) plugin.

## Usage

The use of the library is very simple. 
Just create an abstract `Parcelable`-to-be class, annotate it with `@AutoParcel` and 
it will do the rest.

```java
import com.aitorvs.autoparcel.AutoParcel;

@AutoParcel
public abstract class Person {
    @Nullable
    public String name;
    public int age;

    public static Person create(@NonNull String name, int age) {
        return new AutoParcel_Person(name, age);
    }
}
```

AutoParcel will generate a parcelable class that extends from the abstract
class you created. The generated class name follows the convention `AutoParcel_<YouClassName>`.

You will need to add a convenience builder method (e.g. `creator`) that 
calls the generated class constructor and that is all there is to it. 

You are ready now to e.g. send an instance of `Person` across an intent

```
intent.putExtra(EXTRA_PERSON, (Parcelable) person);
```

To avoid the need to cast `Person` to `(Parcelable)` just add `implements Parcelable` 
to your abstract class definition. AutoParcel will detect it and do the rest anyway.

```
@AutoParcel
public abstract class Person implements Parcelable {...}
```

It is important to note that AutoParcel errors out when **private** fields are found, because
they are not accessible from the generated class. Use either `protected` or `public` instead.

## Parcel Adapters

AutoParcel supports all types supported by [Parcel](https://developer.android.com/reference/android/os/Parcel.html)
with the exception of `Map` -- why? read [here](https://developer.android.com/reference/android/os/Parcel.html). 

At times you will also need to parcel more complex types. For that, use `ParcelAdapter`s.
Let's see an example for a `Date` parcel adapter.

```java
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
```

Now you can use your adapter into your classes.

```java
import com.aitorvs.autoparcel.AutoParcel;

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
```

Parcel adapters are optional and the require the `ParcelTypeAdapter` runtime component.
To use them just add to your gradle the following dependency.

```
compile 'com.github.aitorvs.auto-parcel:adapter:0.1.0-rc1'
```

## Pitfalls

- Bootstrap is somehow annoying because when typing your first `AutoParcel_Foo` 
it will turn red in the IDE until the first build is performed.
- Order of the constructor `AutoParcel_Foo` parameters is important and 
according to their appearance in the source file.

## License

```
Copyright 2016 Aitor Viana Sánchez.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```