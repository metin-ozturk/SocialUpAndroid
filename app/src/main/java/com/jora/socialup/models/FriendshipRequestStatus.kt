package com.jora.socialup.models

enum class FriendshipRequestStatus(val value: Int) {
    NoFriendshipRequest(0),
    ReceivedFriendshipRequest(1),
    SentFriendshipRequest(2),
    AlreadyFriends(3);

    companion object {
        fun getFriendshipRequestStatusByValue(value: Int) : FriendshipRequestStatus {
            return when (value) {
                0 -> NoFriendshipRequest
                1 -> ReceivedFriendshipRequest
                2 -> SentFriendshipRequest
                3 -> AlreadyFriends
                else -> NoFriendshipRequest
            }
        }
    }
}