package com.aitorvs.autoparcel;

import android.os.Parcel;

/**
 * Converts Java objects to and from Parcels.
 *
 * <p>By default AutoParcel can parcel and unparcel data types known by
 * the {@link Parcel} class.  To support other types, like custom classes or other objects,
 * like {@link java.util.Date} objects or booleans, you can create a custom ParcelTypeAdapter to
 * tell the Parcel extension how to parcel the object.
 *
 * <p>Here's an example ParcelTypeAdapter for a Date object:
 *
 * <pre>
 * <code>
 * public class DateTypeAdapter implements ParcelTypeAdapter<Date> {
 *   public Date fromParcel(Parcel in) {
 *     return new Date(in.readLong());
 *   }
 *
 *   public void toParcel(Date value, Parcel dest) {
 *     dest.writeLong(value.getTime());
 *   }
 * }
 * </code>
 * </pre>
 *
 * You can tell the Parcel Extension to use this ParcelTypeAdapter by using the {@link ParcelAdapter}
 * annotation on any Date properties.
 *
 * <pre>
 * <code>
 * {@literal @}AutoParcel public abstract class Foo {
 *   {@literal @}ParcelAdapter(DateTypeAdapter.class) public abstract Date date;
 * }
 * </code>
 * </pre>
 */
public interface ParcelTypeAdapter<T> {

    /**
     * Creates a new object based on the values in the provided {@link Parcel}.
     * @param in The {@link Parcel} which contains the values of {@code T}.
     * @return A new object based on the values in {@code in}.
     */
    T fromParcel(Parcel in);

    /**
     * Writes {@code value} into {@code dest}.
     * @param value The object to be written.
     * @param dest The {@link Parcel} in which to write {@code value}.
     */
    void toParcel(T value, Parcel dest);

}