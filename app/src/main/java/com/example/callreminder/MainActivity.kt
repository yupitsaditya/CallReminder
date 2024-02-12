package com.example.callreminder

import android.os.Bundle
import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.callreminder.ui.theme.CalLReminderTheme
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var selectContactButton: Button
    private lateinit var contactRecyclerView: RecyclerView
    private val contactList =  mutableListOf<Contact>()
    private lateinit var adapter: ContactAdapter
    private val REQUEST_CONTACT_PERMISSION = 123

    companion object {
        private const val CONTACT_PICKER_RESULT = 1001
    }
    fun onDateTimeSelected(dateTime: String, contact: Contact) {
        // Handle the selected date and time value here
        // Update the Contact object with the selected date and time
        contact.timeToCall = dateTime

        // Add the contact to the list or update it in your data structure
        contactList.add(contact)
        adapter.notifyItemInserted(contactList.size - 1)
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

//        pickDateTimeButton = findViewById(R.id.pickDateTimeButton)
//        pickDateTimeButton.setOnClickListener {
//            showDateTimePickerDialog()
//        }




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
                    val contact = Contact(contactName, contactId, contactPhoneNumber, "")
                    // Pass the contact object to the showDateTimePickerDialog method
                    showDateTimePickerDialog(contact, this)
//                    contactList.add(contact)
//                    adapter.notifyItemInserted(contactList.size - 1)
                }
            }

        }
    }
    @SuppressLint("Range")
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
    @SuppressLint("Range")
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
    private fun showDateTimePickerDialog(contact: Contact, dateTimeSelectedListener: MainActivity)  {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                val selectedDateTime = Calendar.getInstance()
                selectedDateTime.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute)

                // Convert the selected date and time to a string format
                val timeToCall = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(selectedDateTime.time)

                // Pass the selected date and time back to the caller
                dateTimeSelectedListener.onDateTimeSelected(timeToCall, contact)
            }, hour, minute, true)


            timePickerDialog.show()
        }, year, month, day)

        datePickerDialog.show()
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
            private val contactTimeToCall: TextView = itemView.findViewById(R.id.contactIdTimeToCallView)

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
                contactTimeToCall.text = contact.timeToCall


            }

        }
    }
}

data class Contact(val name: String, val id: String, val phoneNumber: String, var timeToCall: String)


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