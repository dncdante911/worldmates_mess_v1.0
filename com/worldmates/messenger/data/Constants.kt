package com.worldmates.messenger.data

object Constants {
    // !! КОНСТАНТЫ API И КЛЮЧИ (ВНЕСЕНЫ ИЗ ВАШИХ ДАННЫХ)
    const val BASE_URL = "https://worldmates.club/api/v2/" 
    // URL для Node.js Socket.IO (ПОРТ 4001 - пример. Требует проверки на вашем сервере!)
    const val SOCKET_URL = "https://worldmates.club:4001/" 
    
    // Ваш siteEncryptKey
    const val SITE_ENCRYPT_KEY = "2ad9c757daccdfff436dc226779e20b719f6d6f8" 
    // !!

    // API ENDPOINTS (GET/POST параметр type)
    const val AUTH_ENDPOINT = "?type=auth"
    const val GET_CHATS_ENDPOINT = "?type=get_chats"
    const val GET_MESSAGES_ENDPOINT = "?type=get_user_messages"

    // SOCKET.IO EVENTS (Проверьте, что эти имена соответствуют вашему Node.js серверу)
    const val SOCKET_EVENT_AUTH = "register_socket"
    const val SOCKET_EVENT_NEW_MESSAGE = "new_message"
    const val SOCKET_EVENT_SEND_MESSAGE = "send_message"
}