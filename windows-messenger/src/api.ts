import { API_BASE_URL, ENDPOINTS, REGISTER_PATH, SERVER_KEY, SITE_ENCRYPT_KEY } from './config';
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
  body.append('server_key', SERVER_KEY);
  Object.entries(payload).forEach(([key, value]) => body.append(key, String(value)));
  return body;
}

function withSecurityParams(url: string, accessToken?: string): string {
  const full = new URL(url);
  full.searchParams.set('s', SITE_ENCRYPT_KEY);
  if (accessToken) {
    full.searchParams.set('access_token', accessToken);
  }
  return full.toString();
}

function v2Url(endpoint: string, accessToken?: string): string {
  return withSecurityParams(`${API_BASE_URL}${endpoint}`, accessToken);
}

function absoluteUrl(path: string, accessToken?: string): string {
  return withSecurityParams(`https://worldmates.club${path}`, accessToken);
}

async function parseJson<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const body = await response.text().catch(() => '');
    throw new Error(`HTTP ${response.status}: ${body.slice(0, 300)}`);
  }
  return response.json() as Promise<T>;
}

export async function login(username: string, password: string): Promise<AuthResponse> {
  const response = await fetch(v2Url(ENDPOINTS.auth), {
    method: 'POST',
    body: createFormBody({ username, password, device_type: 'windows' })
  });
  return parseJson<AuthResponse>(response);
}

export async function loginByPhone(phone: string, password: string): Promise<AuthResponse> {
  const response = await fetch(v2Url(ENDPOINTS.auth), {
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
  const response = await fetch(withSecurityParams(REGISTER_PATH), {
    method: 'POST',
    body: createFormBody({
      username: input.username,
      email: input.email ?? '',
      phone_number: input.phoneNumber ?? '',
      password: input.password,
      confirm_password: input.password,
      s: SITE_ENCRYPT_KEY,
      device_type: 'windows',
      gender: 'male'
    })
  });

  return parseJson<AuthResponse>(response);
}

export async function loadChats(token: string): Promise<ChatListResponse> {
  const response = await fetch(v2Url(ENDPOINTS.chats, token), {
    method: 'POST',
    body: createFormBody({ user_limit: 80, data_type: 'all', SetOnline: 1, offset: 0 })
  });
  return parseJson<ChatListResponse>(response);
}

export async function loadMessages(token: string, recipientId: number): Promise<MessagesResponse> {
  const response = await fetch(v2Url(ENDPOINTS.messages, token), {
    method: 'POST',
    body: createFormBody({ recipient_id: recipientId, limit: 40, before_message_id: 0 })
  });
  return parseJson<MessagesResponse>(response);
}

export async function sendMessage(token: string, recipientId: number, text: string): Promise<void> {
  const payload = {
    user_id: recipientId,
    recipient_id: recipientId,
    text,
    message_hash_id: `${Date.now()}-${Math.random().toString(16).slice(2, 10)}`
  };

  await fetch(v2Url(ENDPOINTS.sendMessage, token), { method: 'POST', body: createFormBody(payload) });
  await fetch(v2Url(ENDPOINTS.sendMessageLegacy, token), {
    method: 'POST',
    body: createFormBody({ recipient_id: recipientId, text })
  });
}

export async function uploadMedia(token: string, file: File): Promise<MediaUploadResponse> {
  const form = new FormData();
  form.append('server_key', SERVER_KEY);
  form.append('file', file);

  const response = await fetch(v2Url(ENDPOINTS.uploadMedia, token), { method: 'POST', body: form });
  return parseJson<MediaUploadResponse>(response);
}

export async function sendMessageWithMedia(token: string, recipientId: number, text: string, file: File): Promise<void> {
  const form = new FormData();
  form.append('server_key', SERVER_KEY);
  form.append('user_id', String(recipientId));
  form.append('text', text);
  form.append('message_hash_id', `${Date.now()}-${Math.random().toString(16).slice(2, 10)}`);
  form.append('file', file);

  await fetch(v2Url(ENDPOINTS.sendMessage, token), { method: 'POST', body: form });
}

export async function loadGroups(token: string): Promise<GenericListResponse<GroupItem>> {
  const response = await fetch(absoluteUrl(ENDPOINTS.groups, token), {
    method: 'POST',
    body: createFormBody({ type: 'get_list', limit: 50, offset: 0 })
  });
  return parseJson<GenericListResponse<GroupItem>>(response);
}

export async function createGroup(token: string, groupName: string): Promise<void> {
  await fetch(absoluteUrl(ENDPOINTS.groups, token), {
    method: 'POST',
    body: createFormBody({ type: 'create', group_name: groupName, parts: '', group_type: 'group' })
  });
}

export async function loadChannels(token: string): Promise<GenericListResponse<ChannelItem>> {
  const response = await fetch(absoluteUrl(ENDPOINTS.channels, token), {
    method: 'POST',
    body: createFormBody({ type: 'get_list', limit: 50, offset: 0 })
  });
  return parseJson<GenericListResponse<ChannelItem>>(response);
}

export async function createChannel(token: string, channelName: string, description: string): Promise<void> {
  await fetch(absoluteUrl(ENDPOINTS.channels, token), {
    method: 'POST',
    body: createFormBody({ action: 'create_channel', name: channelName, description })
  });
}

export async function loadStories(token: string): Promise<GenericListResponse<StoryItem>> {
  const response = await fetch(absoluteUrl(ENDPOINTS.stories, token), {
    method: 'POST',
    body: createFormBody({ limit: 35 })
  });
  return parseJson<GenericListResponse<StoryItem>>(response);
}

export async function createStory(token: string, file: File, fileType: 'image' | 'video'): Promise<void> {
  const form = new FormData();
  form.append('server_key', SERVER_KEY);
  form.append('file', file);
  form.append('file_type', fileType);

  await fetch(absoluteUrl(ENDPOINTS.createStory, token), { method: 'POST', body: form });
}

export async function getIceServers(userId: number): Promise<RTCIceServer[]> {
  const response = await fetch(absoluteUrl(`${ENDPOINTS.iceServers}${userId}`));
  if (!response.ok) {
    return [];
  }
  const payload = await response.json();
  return payload.iceServers ?? payload ?? [];
}
