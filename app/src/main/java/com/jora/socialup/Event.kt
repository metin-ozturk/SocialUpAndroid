package com.jora.socialup

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.android.synthetic.main.fragment_event.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.asDeferred
import java.io.ByteArrayOutputStream


class Event(parcel: Parcel? = null) : Parcelable {

    var timeStamp : Timestamp? = null

    var iD : String? = null
    var name : String? = null
    var description : String? = null
    var isPrivate : Boolean? = null
    var image : Bitmap? = null
    var isFavorite : Boolean? = null

    var founderID : String? = null
    var founderName : String? = null
    var founderImage : Bitmap? = null

    var status : Long? = null
    var date : ArrayList<String>? = null
    var dateVote : ArrayList<String>? = null

    var locationName : String? = null
    var locationLatitude : String? = null
    var locationLongitude : String? = null
    var locationDescription : String? = null
    var locationAddress : String? = null

    var eventWithWhomID: ArrayList<String>? = null
    var eventWithWhomNames: ArrayList<String>? = null
    var eventWithWhomWillCome: ArrayList<String>? = null
    var eventWithWhomWontCome: ArrayList<String>? = null
    var eventWithWhomMayCome: ArrayList<String>? = null

    var hasImage : Boolean? = null

    override fun toString(): String {
        return returnEventAsMap().toString()
    }

    init {
        if (parcel != null) {
            iD = parcel.readString()
            name = parcel.readString()
            description = parcel.readString()
            isPrivate = parcel.readInt() == 1

            founderID = parcel.readString()
            founderName = parcel.readString()

            status = parcel.readLong()
            date = parcel.readArrayList(ArrayList::class.java.classLoader) as ArrayList<String>
            dateVote = parcel.readArrayList(ArrayList::class.java.classLoader) as ArrayList<String>

            locationAddress = parcel.readString()
            locationDescription = parcel.readString()
            locationLatitude = parcel.readString()
            locationLongitude = parcel.readString()
            locationName = parcel.readString()

            eventWithWhomID = parcel.readArrayList(ArrayList::class.java.classLoader) as ArrayList<String>
            eventWithWhomMayCome = parcel.readArrayList(ArrayList::class.java.classLoader) as ArrayList<String>
            eventWithWhomNames = parcel.readArrayList(ArrayList::class.java.classLoader) as ArrayList<String>
            eventWithWhomWillCome = parcel.readArrayList(ArrayList::class.java.classLoader) as ArrayList<String>
            eventWithWhomWontCome = parcel.readArrayList(ArrayList::class.java.classLoader) as ArrayList<String>

            hasImage = parcel.readInt() == 1
            timeStamp = parcel.readParcelable(Timestamp::class.java.classLoader)
        }
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeString(iD)
        dest?.writeString(name)
        dest?.writeString(description)
        dest?.writeInt(if (isPrivate == true) 1 else 0)


        dest?.writeString(founderID)
        dest?.writeString(founderName)

        dest?.writeLong(status ?: 0)
        dest?.writeList(date)
        dest?.writeList(dateVote)

        dest?.writeString(locationAddress)
        dest?.writeString(locationDescription)
        dest?.writeString(locationLatitude)
        dest?.writeString(locationLongitude)
        dest?.writeString(locationName)

        dest?.writeList(eventWithWhomID)
        dest?.writeList(eventWithWhomMayCome)
        dest?.writeList(eventWithWhomNames)
        dest?.writeList(eventWithWhomWillCome)
        dest?.writeList(eventWithWhomWontCome)

        dest?.writeInt(if (hasImage == true) 1 else 0)
        dest?.writeParcelable(timeStamp, 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    private fun returnEventAsMap() : Map<String, Any> {
        return mapOf("EventID" to (iD ?: "ERROR"),
            "EventName" to (name ?: "ERROR"),
            "EventDescription" to (description ?: "ERROR"),
            "EventIsPrivate" to (isPrivate ?: "ERROR"),
            "EventFounder" to (founderID ?: "ERROR"),
            "EventFounderName" to (founderName ?: "ERROR"),
            "EventStatus" to (status ?: "ERROR"),
            "LocationName" to (locationName ?: "ERROR"),
            "LocationDescription" to (locationDescription ?: "ERROR"),
            "LocationAddress" to (locationAddress ?: "ERROR"),
            "LocationLatitude" to (locationLatitude ?: "ERROR"),
            "LocationLongitude" to (locationLongitude ?: "ERROR"),
            "WithWhomInvited" to (eventWithWhomID ?: "ERROR"),
            "WithWhomInvitedNames" to (eventWithWhomNames ?: "ERROR"),
            "WithWhomWillCome" to (eventWithWhomWillCome ?: "ERROR"),
            "WithWhomMayCome" to (eventWithWhomMayCome ?: "ERROR"),
            "WithWhomWontCome" to (eventWithWhomWontCome ?: "ERROR"),
            "When" to ((date ?: ArrayList()).zip(dateVote ?: ArrayList()) { dateElement, dateVoteElement -> "$dateElement$dateVoteElement" }),
            "HasImage" to (hasImage ?: "ERROR"),
            "timestamp" to (timeStamp ?: "ERROR") )
    }


    companion object CREATOR: Parcelable.Creator<Event> {
        override fun createFromParcel(parcel: Parcel): Event {
            return Event(parcel)
        }

        override fun newArray(size: Int): Array<Event?> {
            return arrayOfNulls(size)
        }

        fun downloadEventIDs(completion: (ArrayList<String>) -> Unit) {

            var eventIDsArray : ArrayList<String>

            FirebaseFirestore.getInstance().collection("users").document("MKbCN5M1gnZ9Yi427rPf2SzyvqM2").collection("events").get()
                .addOnSuccessListener { snap ->
                    eventIDsArray = snap.documents.map { it.id } as ArrayList<String>

                    completion(eventIDsArray)
                }
        }

        fun downloadEventInformation(downloadedEventID: String, completion: (Event) -> Unit)  {
            FirebaseFirestore.getInstance().collection("events").document(downloadedEventID).get().addOnSuccessListener { snap ->
                val eventData = snap.data

                val event = Event()

                val eventID = eventData?.get("EventID") as String
                val founderID = eventData["EventFounder"] as String
                val hasImage = eventData["HasImage"] as Boolean

                event.iD = eventID
                event.name = eventData["EventName"] as String
                event.description = eventData["EventDescription"] as String
                event.isPrivate = eventData["EventIsPrivate"] as Boolean

                event.founderID = founderID
                event.founderName = eventData["EventFounderName"] as String
                event.status = eventData["EventStatus"] as Long

                event.locationName = eventData["LocationName"] as String
                event.locationDescription = eventData["LocationDescription"] as String
                event.locationAddress = eventData["LocationAddress"] as String
                event.locationLongitude = eventData["LocationLongitude"] as String
                event.locationLatitude = eventData["LocationLatitude"] as String

                event.eventWithWhomID = eventData["WithWhomInvited"] as? ArrayList<String> ?: ArrayList()
                event.eventWithWhomNames = eventData["WithWhomInvitedNames"] as? ArrayList<String> ?: ArrayList()
                event.eventWithWhomWillCome = eventData["WithWhomWillCome"] as? ArrayList<String> ?: ArrayList()
                event.eventWithWhomMayCome = eventData["WithWhomMayCome"] as? ArrayList<String> ?: ArrayList()
                event.eventWithWhomWontCome = eventData["WithWhomWontCome"] as? ArrayList<String> ?: ArrayList()
                event.timeStamp = eventData["timestamp"] as Timestamp

                event.hasImage = hasImage


                val downloadedEventDateData = eventData["When"] as? ArrayList<String> ?: ArrayList()

                event.date  = ArrayList(downloadedEventDateData.map { it.substring(0, 16) })
                event.dateVote = ArrayList(downloadedEventDateData.map { it.substring(16) })

                val storageReference = FirebaseStorage.getInstance().reference

                GlobalScope.launch(Dispatchers.Main) {
                    var downloadEventImage : Deferred<ByteArray>? = null

                    if (hasImage) {
                        downloadEventImage = storageReference.child("Images/Events/$eventID/eventPhoto.jpeg").getBytes(1024 * 1024).asDeferred()
                    }

                    val downloadFounderImage = storageReference.child("Images/Users/$founderID/profilePhoto.jpeg").getBytes(1024 * 1024).asDeferred()

                    var eventImage : ByteArray?
                    var founderImage : ByteArray?

                    try {
                        if (hasImage) {
                            val downloadedImages = mutableListOf<ByteArray>(downloadEventImage!!.await(), downloadFounderImage.await())
                            eventImage = downloadedImages[0]
                            founderImage = downloadedImages[1]
                        } else {
                            founderImage = downloadFounderImage.await()
                            eventImage = null
                        }

                    } catch (e: StorageException) {
                        founderImage = null
                        eventImage = null
                        Log.d("EVENT", "IMAGE DOWNLOAD FAILED WITH: ", e)
                    }



                    event.image = if (eventImage == null) null else BitmapFactory.decodeByteArray(eventImage, 0, eventImage.size)
                    event.founderImage = if (founderImage == null) null else BitmapFactory.decodeByteArray(founderImage, 0, founderImage.size)

                    completion(event)
                }

            }.addOnFailureListener { exception ->
                Log.d("EVENT", "DATA DOWNLOAD FAILED WITH: ", exception)
            }
        }

        fun convertDateToReadableFormat(date: String) : String {
            val day = date.substring(0, 2)
            val month = date.substring(2, 4)
            val year = date.substring(4,8)
            val initialHour = date.substring(8, 10)
            val initialMinutes = date.substring(10,12)
            val finalHour = date.substring(12, 14)
            val finalMinutes = date.substring(14, 16)

            return "$day/$month/$year $initialHour:$initialMinutes $finalHour:$finalMinutes"

        }


    }

}