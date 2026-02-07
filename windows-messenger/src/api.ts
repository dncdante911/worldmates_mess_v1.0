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
  if (accessToken) full.searchParams.set('access_token', accessToken);
  return full.toString();
}

function v2Url(endpoint: string, accessToken?: string): string {
  return withSecurityParams(`${API_BASE_URL}${endpoint}`, accessToken);
}

function absoluteUrl(path: string, accessToken?: string): string {
  return withSecurityParams(`https://worldmates.club${path}`, accessToken);
}

async function parseTextAsJson<T>(status: number, text: string, ok: boolean): Promise<T> {
  let payload: T;
  try {
    payload = JSON.parse(text) as T;
  } catch {
    throw new Error(`HTTP ${status}: ${text.slice(0, 300) || 'Non-JSON response'}`);
  }

  if (!ok) {
    throw new Error(`HTTP ${status}: ${text.slice(0, 300)}`);
  }

  return payload;
}

async function postForm<T>(url: string, body: URLSearchParams): Promise<T> {
  const desktopRequest = window.desktopApp?.request;
  if (desktopRequest) {
    const result = await desktopRequest({
      url,
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
      },
      body: body.toString()
    });

    return parseTextAsJson<T>(result.status, result.text, result.ok);
  }

  const response = await fetch(url, { method: 'POST', body });
  const text = await response.text();
  return parseTextAsJson<T>(response.status, text, response.ok);
}

async function postMultipart<T>(url: string, body: FormData): Promise<T> {
  const response = await fetch(url, { method: 'POST', body });
  const text = await response.text();
  return parseTextAsJson<T>(response.status, text, response.ok);
}

async function getJson<T>(url: string): Promise<T> {
  const desktopRequest = window.desktopApp?.request;
  if (desktopRequest) {
    const result = await desktopRequest({ url, method: 'GET' });
    return parseTextAsJson<T>(result.status, result.text, result.ok);
  }

  const response = await fetch(url);
  const text = await response.text();
  return parseTextAsJson<T>(response.status, text, response.ok);
}

export function login(username: string, password: string): Promise<AuthResponse> {
  return postForm<AuthResponse>(v2Url(ENDPOINTS.auth), createFormBody({ username, password, device_type: 'windows' }));
}

export function loginByPhone(phone: string, password: string): Promise<AuthResponse> {
  return postForm<AuthResponse>(
    v2Url(ENDPOINTS.auth),
    createFormBody({ username: phone, phone_number: phone, password, device_type: 'windows' })
  );
}

export function registerAccount(input: {
  username: string;
  email?: string;
  phoneNumber?: string;
  password: string;
}): Promise<AuthResponse> {
  return postForm<AuthResponse>(
    withSecurityParams(REGISTER_PATH),
    createFormBody({
      username: input.username,
      email: input.email ?? '',
      phone_number: input.phoneNumber ?? '',
      password: input.password,
      confirm_password: input.password,
      s: SITE_ENCRYPT_KEY,
      device_type: 'windows',
      gender: 'male'
    })
  );
}

export function loadChats(token: string): Promise<ChatListResponse> {
  return postForm<ChatListResponse>(
    v2Url(ENDPOINTS.chats, token),
    createFormBody({ user_limit: 80, data_type: 'all', SetOnline: 1, offset: 0 })
  );
}

export function loadMessages(token: string, recipientId: number): Promise<MessagesResponse> {
  return postForm<MessagesResponse>(
    v2Url(ENDPOINTS.messages, token),
    createFormBody({ recipient_id: recipientId, limit: 40, before_message_id: 0 })
  );
}

export async function sendMessage(token: string, recipientId: number, text: string): Promise<void> {
  const payload = {
    user_id: recipientId,
    recipient_id: recipientId,
    text,
    message_hash_id: `${Date.now()}-${Math.random().toString(16).slice(2, 10)}`
  };

  await postForm(v2Url(ENDPOINTS.sendMessage, token), createFormBody(payload));
  await postForm(v2Url(ENDPOINTS.sendMessageLegacy, token), createFormBody({ recipient_id: recipientId, text }));
}

export function uploadMedia(token: string, file: File): Promise<MediaUploadResponse> {
  const form = new FormData();
  form.append('server_key', SERVER_KEY);
  form.append('file', file);
  return postMultipart<MediaUploadResponse>(v2Url(ENDPOINTS.uploadMedia, token), form);
}

export async function sendMessageWithMedia(token: string, recipientId: number, text: string, file: File): Promise<void> {
  const form = new FormData();
  form.append('server_key', SERVER_KEY);
  form.append('user_id', String(recipientId));
  form.append('text', text);
  form.append('message_hash_id', `${Date.now()}-${Math.random().toString(16).slice(2, 10)}`);
  form.append('file', file);

  await postMultipart(v2Url(ENDPOINTS.sendMessage, token), form);
}

export function loadGroups(token: string): Promise<GenericListResponse<GroupItem>> {
  return postForm<GenericListResponse<GroupItem>>(
    absoluteUrl(ENDPOINTS.groups, token),
    createFormBody({ type: 'get_list', limit: 50, offset: 0 })
  );
}

export function createGroup(token: string, groupName: string): Promise<unknown> {
  return postForm(absoluteUrl(ENDPOINTS.groups, token), createFormBody({ type: 'create', group_name: groupName, parts: '', group_type: 'group' }));
}

export function loadChannels(token: string): Promise<GenericListResponse<ChannelItem>> {
  return postForm<GenericListResponse<ChannelItem>>(
    absoluteUrl(ENDPOINTS.channels, token),
    createFormBody({ type: 'get_list', limit: 50, offset: 0 })
  );
}

export function createChannel(token: string, channelName: string, description: string): Promise<unknown> {
  return postForm(absoluteUrl(ENDPOINTS.channels, token), createFormBody({ action: 'create_channel', name: channelName, description }));
}

export function loadStories(token: string): Promise<GenericListResponse<StoryItem>> {
  return postForm<GenericListResponse<StoryItem>>(absoluteUrl(ENDPOINTS.stories, token), createFormBody({ limit: 35 }));
}

export async function createStory(token: string, file: File, fileType: 'image' | 'video'): Promise<void> {
  const form = new FormData();
  form.append('server_key', SERVER_KEY);
  form.append('file', file);
  form.append('file_type', fileType);

  await postMultipart(absoluteUrl(ENDPOINTS.createStory, token), form);
}

export async function getIceServers(userId: number): Promise<RTCIceServer[]> {
  try {
    const payload = await getJson<{ iceServers?: RTCIceServer[] } | RTCIceServer[]>(absoluteUrl(`${ENDPOINTS.iceServers}${userId}`));
    return Array.isArray(payload) ? payload : (payload.iceServers ?? []);
  } catch {
    return [];
  }
}
