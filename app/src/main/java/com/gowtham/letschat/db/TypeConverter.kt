package com.gowtham.letschat.db

import androidx.room.TypeConverter
import com.gowtham.letschat.db.data.*
import com.gowtham.letschat.models.UserProfile
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class TypeConverter {

    @TypeConverter
    fun fromProfileToString(userProfile: UserProfile): String {
        return Json.encodeToString(userProfile)
    }

    @TypeConverter
    fun fromStringToProfile(userProfile: String): UserProfile {
        return Json.decodeFromString(userProfile)
    }

    @TypeConverter
    fun fromTextMessageToString(textMessage: TextMessage?): String {
        return Json.encodeToString(textMessage ?: TextMessage())
    }

    @TypeConverter
    fun fromStringToTextMessage(messageData: String): TextMessage {
        return Json.decodeFromString(messageData)
    }

    @TypeConverter
    fun fromImageMessageToString(imageMessage: ImageMessage?): String {
        return Json.encodeToString(imageMessage ?: ImageMessage())
    }


    @TypeConverter
    fun fromStringToImageMessage(messageData: String): ImageMessage {
        return Json.decodeFromString(messageData)
    }


    @TypeConverter
    fun fromAudioMessageToString(audioMessage: AudioMessage?): String {
        return Json.encodeToString(audioMessage ?: AudioMessage())
    }

    @TypeConverter
    fun fromStringToAudioMessage(messageData: String): AudioMessage {
        return Json.decodeFromString(messageData)
    }

    @TypeConverter
    fun fromVideoMessageToString(videoMessage: VideoMessage?): String {
        return Json.encodeToString(videoMessage ?: VideoMessage())
    }

    @TypeConverter
    fun fromStringToVideoMessage(messageData: String): VideoMessage {
        return Json.decodeFromString(messageData)
    }

    @TypeConverter
    fun fromFileMessageToString(fileMessage: FileMessage?): String {
        return Json.encodeToString(fileMessage ?: FileMessage())
    }

    @TypeConverter
    fun fromStringToFileMessage(messageData: String): FileMessage {
        return Json.decodeFromString(messageData)
    }

    @TypeConverter
    fun fromChatUserToString(chatUser: ChatUser): String {
        return Json.encodeToString(chatUser)
    }

    @TypeConverter
    fun fromStringToChatUser(chatUser: String): ChatUser {
        return Json.decodeFromString(chatUser)
    }

    @TypeConverter
    fun fromGroupToString(group: Group): String {
        return Json.encodeToString(group)
    }

    @TypeConverter
    fun fromStringToGroup(group: String): Group {
        return Json.decodeFromString(group)
    }

    @TypeConverter
    fun fromGroupMessageToString(groupMessage: GroupMessage): String {
        return Json.encodeToString(groupMessage)
    }

    @TypeConverter
    fun fromStringToGroupMessage(groupMessage: String): GroupMessage {
        return Json.decodeFromString(groupMessage)
    }

    @TypeConverter
    fun fromToMembersToString(to: ArrayList<String>): String {
        return Json.encodeToString(to)
    }

    @TypeConverter
    fun fromStringToMembers(to: String): ArrayList<String> {
        return Json.decodeFromString(to)
    }

    @TypeConverter
    fun fromProfilesToString(profiles: ArrayList<UserProfile>): String {
        return Json.encodeToString(profiles)
    }

    @TypeConverter
    fun fromStringToProfiles(profilesString: String): ArrayList<UserProfile> {
        return Json.decodeFromString(profilesString)
    }

    @TypeConverter
    fun fromGroupMembersToString(members: ArrayList<ChatUser>): String {
        return Json.encodeToString(members)
    }

    @TypeConverter
    fun fromStringToGroupMembers(members: String): ArrayList<ChatUser> {
        return Json.decodeFromString(members)
    }

    @TypeConverter
    fun fromGroupMsgStatusToString(status: ArrayList<Int>): String {
        return Json.encodeToString(status)
    }

    @TypeConverter
    fun fromStringToGroupMsgStatus(status: String): ArrayList<Int> {
        return Json.decodeFromString(status)
    }

    @TypeConverter
    fun fromSeenStatusListToString(status: ArrayList<Long>): String {
        return Json.encodeToString(status)
    }

    @TypeConverter
    fun fromStringToSeenStatusList(status: String): ArrayList<Long> {
        return Json.decodeFromString(status)
    }

}