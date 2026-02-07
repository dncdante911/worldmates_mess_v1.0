export const API_BASE_URL = 'https://worldmates.club/api/v2/';
export const SOCKET_URL = 'https://worldmates.club:449/';
export const REGISTER_PATH = 'https://worldmates.club/phone/register_user.php?type=user_registration';

export const ENDPOINTS = {
  auth: '?type=auth',
  chats: '?type=get_chats',
  messages: '?type=get_user_messages',
  sendMessage: '?type=send-message',
  sendMessageLegacy: '?type=send_message',
  uploadMedia: '?type=upload_media',
  groups: '/api/v2/group_chat_v2.php',
  channels: '/api/v2/channels.php',
  stories: '/api/v2/endpoints/get-stories.php',
  createStory: '/api/v2/endpoints/create-story.php',
  iceServers: '/api/ice-servers/'
};

export const TURN_FALLBACK = [
  { urls: 'stun:stun.l.google.com:19302' },
  { urls: 'stun:stun1.l.google.com:19302' },
  {
    urls: [
      'turn:worldmates.club:3478?transport=udp',
      'turn:worldmates.club:3478?transport=tcp',
      'turns:worldmates.club:5349?transport=tcp'
    ]
  }
];
