package com.flydrop.app.data

import android.Manifest
import android.content.ContentResolver
import android.provider.ContactsContract
import androidx.annotation.RequiresPermission
import com.flydrop.app.data.model.FlyUser

/** Reads only contact IDs and display names; phone numbers are never requested. */
class PhoneContactsRepository(
    private val contentResolver: ContentResolver,
) {

    @RequiresPermission(Manifest.permission.READ_CONTACTS)
    fun loadContacts(): List<FlyUser> {
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
                            ),
                        )
                    }
                }
            }
        }.orEmpty()
    }
}
