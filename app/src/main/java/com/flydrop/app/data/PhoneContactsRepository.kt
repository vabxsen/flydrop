package com.flydrop.app.data

import android.Manifest
import android.content.ContentResolver
import android.provider.ContactsContract
import androidx.annotation.RequiresPermission
import com.flydrop.app.data.model.FlyUser

/**
 * Reads contact IDs, display names and one phone number each.
 *
 * The number is read so "Invite to app" can open the messaging app already
 * addressed to that person. It never leaves the device by itself: nothing is
 * uploaded, and the invite is handed to the SMS app for the user to send.
 * Contacts with no number are still listed - they simply cannot be invited by
 * text.
 */
class PhoneContactsRepository(
    private val contentResolver: ContentResolver,
) {

    @RequiresPermission(Manifest.permission.READ_CONTACTS)
    fun loadContacts(): List<FlyUser> {
        val numbers = loadPhoneNumbers()
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
        )

        return contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            )
            buildList {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex).toString()
                    val name = cursor.getString(nameIndex)?.trim().orEmpty()
                    if (name.isNotEmpty()) {
                        add(
                            FlyUser(
                                id = "contact:$id",
                                name = name,
                                flyId = "Phone contact",
                                avatarSeed = id.hashCode().ushr(1),
                                phoneNumber = numbers[id],
                            ),
                        )
                    }
                }
            }
        }.orEmpty()
    }

    /**
     * One number per contact, keyed by contact id.
     *
     * A contact can hold several. The one flagged primary wins; otherwise the
     * first seen is kept, so the choice is at least stable between loads rather
     * than depending on cursor order changing under us.
     */
    @RequiresPermission(Manifest.permission.READ_CONTACTS)
    private fun loadPhoneNumbers(): Map<String, String> {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.IS_PRIMARY,
        )

        return contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            null,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            )
            val numberIndex = cursor.getColumnIndexOrThrow(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            )
            val primaryIndex = cursor.getColumnIndexOrThrow(
                ContactsContract.CommonDataKinds.Phone.IS_PRIMARY,
            )

            val numbers = mutableMapOf<String, String>()
            val primaries = mutableSetOf<String>()
            while (cursor.moveToNext()) {
                val contactId = cursor.getLong(idIndex).toString()
                val number = cursor.getString(numberIndex)?.trim()
                if (number.isNullOrEmpty()) continue

                val isPrimary = cursor.getInt(primaryIndex) != 0
                if (isPrimary) {
                    numbers[contactId] = number
                    primaries += contactId
                } else if (contactId !in primaries) {
                    numbers.putIfAbsent(contactId, number)
                }
            }
            numbers
        }.orEmpty()
    }
}
