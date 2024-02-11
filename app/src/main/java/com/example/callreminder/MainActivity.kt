package com.example.callreminder

import android.os.Bundle
import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.callreminder.ui.theme.CalLReminderTheme
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : ComponentActivity() {

    private lateinit var selectContactButton: Button
    private lateinit var contactRecyclerView: RecyclerView
    private val contactList =  mutableListOf<Contact>()
    private lateinit var adapter: ContactAdapter
    private val REQUEST_CONTACT_PERMISSION = 123

    companion object {
        private const val CONTACT_PICKER_RESULT = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        // Check contact permission
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
        {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.READ_CONTACTS),REQUEST_CONTACT_PERMISSION)
        } 
        selectContactButton = findViewById(R.id.selectContactButton)
        contactRecyclerView = findViewById(R.id.contactRecyclerView)
        adapter = ContactAdapter(contactList)
        contactRecyclerView.adapter = adapter
        contactRecyclerView.layoutManager = LinearLayoutManager(this)
        // After setting up RecyclerView adapter and layout manager
        val dividerItemDecoration = SimpleDividerItemDecoration(this)
        contactRecyclerView.addItemDecoration(dividerItemDecoration)




        selectContactButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            startActivityForResult(intent, CONTACT_PICKER_RESULT)
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CONTACT_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED))
                {
                    // Permission granted, proceed
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                } else {
                    // Permission denied, handle accordingly
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CONTACT_PICKER_RESULT && resultCode == Activity.RESULT_OK) {
            val contactUri = data?.data
            val contactName = getContactName(contactUri)
            val contactId = getContactId(contactUri)
            val contactPhoneNumber = getContactPhoneNumber(contactId)
            if (contactPhoneNumber != null) {
                if (contactName.isNotEmpty() && contactId.isNotEmpty() && contactPhoneNumber.isNotEmpty()) {
                    val contact = Contact(contactName, contactId, contactPhoneNumber)
                    contactList.add(contact)
                    adapter.notifyItemInserted(contactList.size - 1)
                }
            }
        }
    }
    private fun getContactId(contactUri: Uri?): String {
        val projection = arrayOf(ContactsContract.Contacts._ID)
        val cursor = contactUri?.let { uri ->
            contentResolver.query(uri, projection, null, null, null)
        }
        var contactId = ""
        cursor?.use {
            if (it.moveToFirst()) {
                contactId = it.getString(it.getColumnIndex(ContactsContract.Contacts._ID))
            }
        }
        cursor?.close()
        return contactId
    }
    private fun getContactName(contactUri: Uri?): String {
        var contactName = ""
        contactUri?.let { uri ->
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    contactName =
                        cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                }
            }
        }
        return contactName
    }
    private fun getContactPhoneNumber(contactId: String?): String? {
        contactId?.let { id ->
            val phoneCursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                arrayOf(id),
                null
            )

            phoneCursor?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val phoneNumberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (phoneNumberIndex != -1) {
                        return cursor.getString(phoneNumberIndex)
                    }
                }
            }
        }
        return null
    }

    private class ContactAdapter(private val contacts: List<Contact>):
        RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.contact_item, parent, false)
            return ContactViewHolder(view, contacts)
        }

        override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
            holder.bind(contacts[position])
        }

        override fun getItemCount(): Int {
            return contacts.size
        }

        class ContactViewHolder(itemView: View, private val contacts: List<Contact>) : RecyclerView.ViewHolder(itemView) {
            private val contactNameTextView: TextView = itemView.findViewById(R.id.contactNameTextView)
            private val contactIdTextView: TextView = itemView.findViewById(R.id.contactIdTextView)
            private lateinit var phoneNumber: String


            init {
                itemView.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val contact = contacts[position]
                        val context = itemView.context
                        val phoneNumber = contact.phoneNumber
                        val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
                        context.startActivity(callIntent)
                    }
                }
            }


            fun bind(contact: Contact) {
                contactNameTextView.text = contact.name
                contactIdTextView.text = contact.id
                phoneNumber = contact.phoneNumber
            }

        }
    }
}
data class Contact(val name: String, val id: String, val phoneNumber: String)


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CalLReminderTheme {
        Greeting("Android")
    }
}