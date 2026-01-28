package com.availelabs.availe.model

enum class ConversationType {
    OneToOne,
    Group,
    System
}

enum class MessageType {
    Text,
    Image,
    System
}

enum class MessageStatus {
    Pending,
    Sent,
    Delivered,
    Read,
    Failed
}