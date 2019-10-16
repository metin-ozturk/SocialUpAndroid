package com.jora.socialup.models

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Parcel
import android.os.Parcelable
import android.provider.Settings
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.asDeferred
import kotlinx.coroutines.tasks.await

// FieldValue is not parcelable - way to fix it?


class Event(parcel: Parcel? = null) : Parcelable {

    var timeStamp : FieldValue? = null

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
        dest?.writeList(date?.toList())
        dest?.writeList(dateVote?.toList())

        dest?.writeString(locationAddress)
        dest?.writeString(locationDescription)
        dest?.writeString(locationLatitude)
        dest?.writeString(locationLongitude)
        dest?.writeString(locationName)

        dest?.writeList(eventWithWhomID?.toList())
        dest?.writeList(eventWithWhomMayCome?.toList())
        dest?.writeList(eventWithWhomNames?.toList())
        dest?.writeList(eventWithWhomWillCome?.toList())
        dest?.writeList(eventWithWhomWontCome?.toList())

        dest?.writeInt(if (hasImage == true) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun copy() : Event {
        val event = Event()
        event.iD = this.iD
        event.name = this.name
        event.description = this.description
        event.isPrivate = this.isPrivate
        event.image = this.image
        event.isFavorite = this.isFavorite

        event.founderID = this.founderID
        event.founderName = this.founderName
        event.founderImage = this.founderImage

        event.status = this.status
        event.date = this.date
        event.dateVote = this.dateVote

        event.locationName = this.locationName
        event.locationDescription = this.locationDescription
        event.locationLatitude = this.locationLatitude
        event.locationLongitude = this.locationLongitude
        event.locationAddress = this.locationAddress

        event.eventWithWhomID = this.eventWithWhomID
        event.eventWithWhomNames = this.eventWithWhomNames
        event.eventWithWhomMayCome = this.eventWithWhomMayCome
        event.eventWithWhomWillCome = this.eventWithWhomWillCome
        event.eventWithWhomWontCome = this.eventWithWhomWontCome

        event.hasImage = this.hasImage

        return event
    }

    fun returnEventAsMap() : Map<String, Any> {
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
            "WithWhomInvited" to (eventWithWhomID ?: ArrayList()),
            "WithWhomInvitedNames" to (eventWithWhomNames ?:  ArrayList()),
            "WithWhomWillCome" to (eventWithWhomWillCome ?:  ArrayList()),
            "WithWhomMayCome" to (eventWithWhomMayCome ?: ArrayList()),
            "WithWhomWontCome" to (eventWithWhomWontCome ?:  ArrayList()),
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
            val userID = FirebaseAuth.getInstance().currentUser?.uid ?: return
            var eventIDsArray : ArrayList<String>

            FirebaseFirestore.getInstance().collection("users").document(userID).collection("events").get()
                .addOnSuccessListener { snap ->
                    eventIDsArray = snap.documents.map { it.id } as ArrayList<String>

                    completion(eventIDsArray)
                }
        }

        fun downloadEventInformation(downloadedEventID: String, completion: (Event) -> Unit) {
            FirebaseFirestore.getInstance().collection("events").document(downloadedEventID).get()
                .addOnSuccessListener { snap ->

                    val eventData = snap.data

                    val event = Event()

                    val eventID = eventData?.get("EventID") as String
                    val founderID = eventData["EventFounder"] as String
                    val hasImage = eventData["HasImage"] as Boolean

                    event.apply {
                        iD = eventID
                        name = eventData["EventName"] as String
                        description = eventData["EventDescription"] as String
                        isPrivate = eventData["EventIsPrivate"] as Boolean

                        this.founderID = founderID
                        founderName = eventData["EventFounderName"] as String
                        status = eventData["EventStatus"] as Long

                        locationName = eventData["LocationName"] as String
                        locationDescription = eventData["LocationDescription"] as String
                        locationAddress = eventData["LocationAddress"] as String
                        locationLongitude = eventData["LocationLongitude"] as String
                        locationLatitude = eventData["LocationLatitude"] as String

                        eventWithWhomID = eventData["WithWhomInvited"] as? ArrayList<String> ?: ArrayList()
                        eventWithWhomNames = eventData["WithWhomInvitedNames"] as? ArrayList<String> ?: ArrayList()
                        eventWithWhomWillCome = eventData["WithWhomWillCome"] as? ArrayList<String> ?: ArrayList()
                        eventWithWhomMayCome = eventData["WithWhomMayCome"] as? ArrayList<String> ?: ArrayList()
                        eventWithWhomWontCome = eventData["WithWhomWontCome"] as? ArrayList<String> ?: ArrayList()
                        timeStamp = eventData["timestamp"] as? FieldValue

                        this.hasImage = hasImage


                        val downloadedEventDateData = eventData["When"] as? ArrayList<String> ?: ArrayList()

                        date = ArrayList(downloadedEventDateData.map { it.substring(0, 16) })
                        dateVote = ArrayList(downloadedEventDateData.map { it.substring(16) })
                    }

                    val coroutineScope = CoroutineScope(Dispatchers.Main)

                    coroutineScope.launch {
                        downloadEventImages(event) {
                            completion(it)
                            coroutineScope.cancel()
                        }
                    }


                }.addOnFailureListener { exception ->
                    Log.d("EVENT", "DATA DOWNLOAD FAILED WITH: ", exception)
                }
        }

        private suspend fun downloadEventImages(event: Event, onDownloadComplete: (Event) -> Unit) {
            val storageReference = FirebaseStorage.getInstance().reference

            var downloadEventImage: Deferred<ByteArray>? = null

            if (event.hasImage == true) {
                downloadEventImage = storageReference.child("Images/Events/${event.iD}/eventPhoto.jpeg")
                    .getBytes(1024 * 1024).asDeferred()
            }

            val downloadFounderImage =
                storageReference.child("Images/Users/${event.founderID}/profilePhoto.jpeg")
                    .getBytes(1024 * 1024).asDeferred()

            var eventImage: ByteArray?
            var founderImage: ByteArray?

            try {
                if (event.hasImage == true) {

                    val downloadedImages = mutableListOf<ByteArray>(
                        downloadEventImage!!.await(),
                        downloadFounderImage.await()
                    )
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


            event.image = if (eventImage == null) null else BitmapFactory.decodeByteArray(
                eventImage,
                0,
                eventImage.size
            )

            event.founderImage = if (founderImage == null) null else BitmapFactory.decodeByteArray(
                founderImage,
                0,
                founderImage.size
            )

            onDownloadComplete(event)
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