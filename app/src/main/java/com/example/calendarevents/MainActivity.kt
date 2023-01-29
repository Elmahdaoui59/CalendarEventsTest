package com.example.calendarevents

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.calendarevents.ui.theme.CalendarEventsTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.*
import java.util.*

class MainActivity : ComponentActivity() {
    private var eventUris: MutableList<Uri> = mutableListOf()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            CalendarEventsTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {

                    val emailAddress = remember {
                        mutableStateOf("")
                    }
                    val calendarPermissionsState = rememberMultiplePermissionsState(
                        permissions = listOf(
                            Manifest.permission.READ_CALENDAR,
                            Manifest.permission.WRITE_CALENDAR
                        )
                    ) { isGranted ->
                        val per = isGranted.values
                        if (per.all { it == true }) {
                            for (i in 1..3) {
                                addEvent(emailAddress.value, i)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {

                            TextField(
                                value = emailAddress.value,
                                onValueChange = { emailAddress.value = it })
                            Spacer(modifier = Modifier.height(30.dp))
                            Button(onClick = {
                                calendarPermissionsState.launchMultiplePermissionRequest()
                            }) {
                                Text(text = "Add Events")
                            }
                            Spacer(modifier = Modifier.height(30.dp))
                            Button(onClick = {
                                eventUris.forEach {
                                    removeEvent(it)
                                }
                            }) {
                                Text(text = "Delete Events")
                            }
                        }
                    }
                }
            }
        }
    }



    private fun addEvent(
        email: String,
        eventNumber: Int
    ) {
        var cursor: Cursor? = null
        try {

            if (email.isNotBlank()) {

                val trimedEmail = email.trim()
                val calendar = Calendar.getInstance()
                val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR)
                val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
                calendar.set(Calendar.WEEK_OF_YEAR, currentWeek)
                calendar.set(Calendar.DAY_OF_WEEK, Random().nextInt(7) - currentDay + 1)
                calendar.set(Calendar.HOUR_OF_DAY, Random().nextInt(9) + 9)
                val startMillis: Long = calendar.timeInMillis

                Log.i("date", "start date is $startMillis")
                val endMillis: Long = startMillis + 60 * 60 * 1000

                var calenderId = -1
                val projection = arrayOf(
                    CalendarContract.Calendars._ID,
                    CalendarContract.Calendars.ACCOUNT_NAME
                )
                val cr: ContentResolver = contentResolver
                cursor = cr.query(
                    Uri.parse("content://com.android.calendar/calendars"), projection,
                    CalendarContract.Calendars.ACCOUNT_NAME + "=? and (" +
                            CalendarContract.Calendars.NAME + "=? or " +
                            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME + "=?)", arrayOf(
                        trimedEmail, trimedEmail,
                        trimedEmail
                    ), null
                )

                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        if (cursor.getString(1).equals(trimedEmail)) {
                            calenderId = cursor.getInt(0)
                        }
                    }
                }
                val values = ContentValues().apply {
                    put(CalendarContract.Events.DTSTART, startMillis)
                    put(CalendarContract.Events.DTEND, endMillis)
                    put(CalendarContract.Events.TITLE, "this is test event number $eventNumber")
                    put(CalendarContract.Events.DESCRIPTION, "Group workout")
                    put(CalendarContract.Events.CALENDAR_ID, calenderId)
                    put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                }

                val uri: Uri? =
                    contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)

                if (uri != null) {
                    eventUris.add(uri)
                }

                Log.i("event", "event saved succefully ${uri.toString()}")
            } else {
                throw Exception("please enter your email address")
            }
        } catch (e: Exception) {
            Log.i("event", e.message.toString())
            e.printStackTrace()
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        } finally {
            cursor?.close()
        }
    }
    private fun removeEvent(uri: Uri) {
        try {
            contentResolver.delete(uri, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
