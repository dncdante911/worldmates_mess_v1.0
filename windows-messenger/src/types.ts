export type AuthResponse = {
  api_status: string;
  access_token?: string;
  user_id?: number;
  message?: string;
};

export type ChatItem = {
  user_id: number;
  name: string;
  avatar?: string;
  last_message?: string;
  time?: string;
};

export type ChatListResponse = {
  api_status: string;
  data?: ChatItem[];
};

export type MessageItem = {
  id: number;
  from_id: number;
  to_id: number;
  text: string;
  time_text?: string;
};

export type MessagesResponse = {
  api_status: string;
  messages?: MessageItem[];
};
