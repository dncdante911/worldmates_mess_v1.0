import { API_BASE_URL, ENDPOINTS, REGISTER_PATH } from './config';
import type {
  AuthResponse,
  ChannelItem,
  ChatListResponse,
  GenericListResponse,
  GroupItem,
  MediaUploadResponse,
  MessagesResponse,
  StoryItem
} from './types';

function createFormBody(payload: Record<string, string | number>) {
  const body = new URLSearchParams();
  Object.entries(payload).forEach(([key, value]) => {
    body.append(key, String(value));
  });
  return body;
}

async function parseJson<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  return response.json() as Promise<T>;
}

export async function login(username: string, password: string): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE_URL}${ENDPOINTS.auth}`, {
    method: 'POST',
    body: createFormBody({ username, password, device_type: 'windows' })
  });
  return parseJson<AuthResponse>(response);
}

export async function loginByPhone(phone: string, password: string): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE_URL}${ENDPOINTS.auth}`, {
    method: 'POST',
    body: createFormBody({ username: phone, phone_number: phone, password, device_type: 'windows' })
  });
  return parseJson<AuthResponse>(response);
}

export async function registerAccount(input: {
  username: string;
  email?: string;
  phoneNumber?: string;
  password: string;
}): Promise<AuthResponse> {
  const response = await fetch(REGISTER_PATH, {
    method: 'POST',
    body: createFormBody({
      username: input.username,
      email: input.email ?? '',
      phone_number: input.phoneNumber ?? '',
      password: input.password,
      confirm_password: input.password,
      s: Date.now(),
      device_type: 'windows',
      gender: 'male'
    })
  });

  return parseJson<AuthResponse>(response);
}

export async function loadChats(token: string): Promise<ChatListResponse> {
  const response = await fetch(`${API_BASE_URL}${ENDPOINTS.chats}&access_token=${encodeURIComponent(token)}`, {
    method: 'POST',
    body: createFormBody({ user_limit: 80, data_type: 'all', SetOnline: 1, offset: 0 })
  });
  return parseJson<ChatListResponse>(response);
}

export async function loadMessages(token: string, recipientId: number): Promise<MessagesResponse> {
  const response = await fetch(`${API_BASE_URL}${ENDPOINTS.messages}&access_token=${encodeURIComponent(token)}`, {
    method: 'POST',
    body: createFormBody({ recipient_id: recipientId, limit: 40, before_message_id: 0 })
  });
  return parseJson<MessagesResponse>(response);
}

export async function sendMessage(token: string, recipientId: number, text: string): Promise<void> {
  const basePayload = {
    user_id: recipientId,
    recipient_id: recipientId,
    text,
    message_hash_id: `${Date.now()}-${Math.random().toString(16).slice(2, 10)}`
  };

  await fetch(`${API_BASE_URL}${ENDPOINTS.sendMessage}&access_token=${encodeURIComponent(token)}`, {
    method: 'POST',
    body: createFormBody(basePayload)
  });

  await fetch(`${API_BASE_URL}${ENDPOINTS.sendMessageLegacy}&access_token=${encodeURIComponent(token)}`, {
    method: 'POST',
    body: createFormBody({ recipient_id: recipientId, text })
  });
}

export async function uploadMedia(token: string, file: File): Promise<MediaUploadResponse> {
  const form = new FormData();
  form.append('file', file);

  const response = await fetch(`${API_BASE_URL}${ENDPOINTS.uploadMedia}&access_token=${encodeURIComponent(token)}`, {
    method: 'POST',
    body: form
  });

  return parseJson<MediaUploadResponse>(response);
}

export async function sendMessageWithMedia(
  token: string,
  recipientId: number,
  text: string,
  file: File
): Promise<void> {
  const form = new FormData();
  form.append('user_id', String(recipientId));
  form.append('text', text);
  form.append('message_hash_id', `${Date.now()}-${Math.random().toString(16).slice(2, 10)}`);
  form.append('file', file);

  await fetch(`${API_BASE_URL}${ENDPOINTS.sendMessage}&access_token=${encodeURIComponent(token)}`, {
    method: 'POST',
    body: form
  });
}

export async function loadGroups(token: string): Promise<GenericListResponse<GroupItem>> {
  const response = await fetch(`${API_BASE_URL.replace('/api/v2/', '')}${ENDPOINTS.groups}?access_token=${encodeURIComponent(token)}`, {
    method: 'POST',
    body: createFormBody({ type: 'get_list', limit: 50, offset: 0 })
  });
  return parseJson<GenericListResponse<GroupItem>>(response);
}

export async function createGroup(token: string, groupName: string): Promise<void> {
  await fetch(`${API_BASE_URL.replace('/api/v2/', '')}${ENDPOINTS.groups}?access_token=${encodeURIComponent(token)}`, {
    method: 'POST',
    body: createFormBody({ type: 'create', group_name: groupName, parts: '', group_type: 'group' })
  });
}

export async function loadChannels(token: string): Promise<GenericListResponse<ChannelItem>> {
  const response = await fetch(`${API_BASE_URL.replace('/api/v2/', '')}${ENDPOINTS.channels}?access_token=${encodeURIComponent(token)}`, {
    method: 'POST',
    body: createFormBody({ type: 'get_list', limit: 50, offset: 0 })
  });
  return parseJson<GenericListResponse<ChannelItem>>(response);
}

export async function createChannel(token: string, channelName: string, description: string): Promise<void> {
  await fetch(`${API_BASE_URL.replace('/api/v2/', '')}${ENDPOINTS.channels}?access_token=${encodeURIComponent(token)}`, {
    method: 'POST',
    body: createFormBody({ action: 'create_channel', name: channelName, description })
  });
}

export async function loadStories(token: string): Promise<GenericListResponse<StoryItem>> {
  const response = await fetch(`${API_BASE_URL.replace('/api/v2/', '')}${ENDPOINTS.stories}?access_token=${encodeURIComponent(token)}`, {
    method: 'POST',
    body: createFormBody({ limit: 35 })
  });
  return parseJson<GenericListResponse<StoryItem>>(response);
}

export async function createStory(token: string, file: File, fileType: 'image' | 'video'): Promise<void> {
  const form = new FormData();
  form.append('file', file);
  form.append('file_type', fileType);

  await fetch(`${API_BASE_URL.replace('/api/v2/', '')}${ENDPOINTS.createStory}?access_token=${encodeURIComponent(token)}`, {
    method: 'POST',
    body: form
  });
}

export async function getIceServers(userId: number): Promise<RTCIceServer[]> {
  const response = await fetch(`${API_BASE_URL.replace('/api/v2/', '')}${ENDPOINTS.iceServers}${userId}`);
  if (!response.ok) {
    return [];
  }
  const payload = await response.json();
  return payload.iceServers ?? payload ?? [];
}
