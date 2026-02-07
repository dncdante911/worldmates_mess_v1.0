import { API_BASE_URL, ENDPOINTS } from './config';
import type { AuthResponse, ChatListResponse, MessagesResponse } from './types';

function createFormBody(payload: Record<string, string | number>) {
  const body = new URLSearchParams();
  Object.entries(payload).forEach(([key, value]) => {
    body.append(key, String(value));
  });
  return body;
}

export async function login(username: string, password: string): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE_URL}${ENDPOINTS.auth}`, {
    method: 'POST',
    body: createFormBody({ username, password, device_type: 'windows' })
  });
  return response.json();
}

export async function loadChats(token: string): Promise<ChatListResponse> {
  const response = await fetch(`${API_BASE_URL}${ENDPOINTS.chats}&access_token=${encodeURIComponent(token)}`, {
    method: 'POST',
    body: createFormBody({ user_limit: 80, data_type: 'all', SetOnline: 1, offset: 0 })
  });
  return response.json();
}

export async function loadMessages(token: string, recipientId: number): Promise<MessagesResponse> {
  const response = await fetch(`${API_BASE_URL}${ENDPOINTS.messages}&access_token=${encodeURIComponent(token)}`, {
    method: 'POST',
    body: createFormBody({ recipient_id: recipientId, limit: 40, before_message_id: 0 })
  });
  return response.json();
}

export async function sendMessage(token: string, recipientId: number, text: string): Promise<void> {
  await fetch(`${API_BASE_URL}${ENDPOINTS.sendMessage}&access_token=${encodeURIComponent(token)}`, {
    method: 'POST',
    body: createFormBody({ recipient_id: recipientId, text })
  });
}
