import { FormEvent, useEffect, useMemo, useState } from 'react';
import type { Socket } from 'socket.io-client';
import { loadChats, loadMessages, login, sendMessage } from './api';
import { createChatSocket } from './socket';
import type { ChatItem, MessageItem } from './types';

type Session = {
  token: string;
  userId: number;
  username: string;
};

const initialStatus = 'Offline';

export default function App() {
  const [session, setSession] = useState<Session | null>(null);
  const [status, setStatus] = useState(initialStatus);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const [chats, setChats] = useState<ChatItem[]>([]);
  const [selectedChatId, setSelectedChatId] = useState<number | null>(null);
  const [messages, setMessages] = useState<MessageItem[]>([]);
  const [newMessage, setNewMessage] = useState('');
  const [socket, setSocket] = useState<Socket | null>(null);

  const selectedChat = useMemo(
    () => chats.find((chat) => chat.user_id === selectedChatId) ?? null,
    [chats, selectedChatId]
  );

  async function handleLogin(event: FormEvent) {
    event.preventDefault();
    setError('');
    const response = await login(username.trim(), password);

    if (response.api_status === '200' && response.access_token && response.user_id) {
      setSession({ token: response.access_token, userId: response.user_id, username: username.trim() });
      return;
    }

    setError(response.message ?? 'Login failed. Please check credentials or API compatibility.');
  }

  useEffect(() => {
    if (!session) {
      return;
    }

    loadChats(session.token)
      .then((response) => {
        setChats(response.data ?? []);
        if ((response.data ?? []).length > 0) {
          setSelectedChatId(response.data?.[0].user_id ?? null);
        }
      })
      .catch(() => setError('Failed to load chats from API.'));

    const liveSocket = createChatSocket(session.token, {
      onStatus: setStatus,
      onMessage: (message) => {
        if (message.from_id === selectedChatId || message.to_id === selectedChatId) {
          setMessages((current) => [...current, message]);
        }
      }
    });

    setSocket(liveSocket);

    return () => {
      liveSocket.disconnect();
      setSocket(null);
      setStatus(initialStatus);
    };
  }, [session, selectedChatId]);

  useEffect(() => {
    if (!session || !selectedChatId) {
      return;
    }

    loadMessages(session.token, selectedChatId)
      .then((response) => setMessages(response.messages ?? []))
      .catch(() => setError('Failed to load messages for selected chat.'));
  }, [selectedChatId, session]);

  async function handleSend(event: FormEvent) {
    event.preventDefault();
    if (!session || !selectedChatId || !newMessage.trim()) {
      return;
    }

    const text = newMessage.trim();
    setNewMessage('');

    await sendMessage(session.token, selectedChatId, text);
    setMessages((current) => [
      ...current,
      {
        id: Date.now(),
        from_id: session.userId,
        to_id: selectedChatId,
        text,
        time_text: 'now'
      }
    ]);

    socket?.emit('private_message', {
      recipient_id: selectedChatId,
      text,
      access_token: session.token
    });
  }

  if (!session) {
    return (
      <div className="auth-page">
        <form className="auth-card" onSubmit={handleLogin}>
          <h1>WorldMates Windows</h1>
          <p>Вход в аккаунт Android API (beta client).</p>
          <label>
            Username
            <input value={username} onChange={(event) => setUsername(event.target.value)} required />
          </label>
          <label>
            Password
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
            />
          </label>
          {error ? <div className="error">{error}</div> : null}
          <button type="submit">Sign In</button>
        </form>
      </div>
    );
  }

  return (
    <div className="layout">
      <aside className="sidebar">
        <header>
          <h2>Chats</h2>
          <small>{session.username}</small>
        </header>
        <div className="status">{status}</div>
        {chats.map((chat) => (
          <button
            key={chat.user_id}
            className={chat.user_id === selectedChatId ? 'chat-item active' : 'chat-item'}
            onClick={() => setSelectedChatId(chat.user_id)}
          >
            <strong>{chat.name}</strong>
            <span>{chat.last_message ?? 'No messages yet'}</span>
          </button>
        ))}
      </aside>

      <main className="chat-view">
        <header className="chat-header">{selectedChat?.name ?? 'Select chat'}</header>
        <section className="messages">
          {messages.map((message) => (
            <article
              key={message.id}
              className={message.from_id === session.userId ? 'bubble own' : 'bubble'}
            >
              <p>{message.text}</p>
              <time>{message.time_text ?? ''}</time>
            </article>
          ))}
        </section>

        <form className="composer" onSubmit={handleSend}>
          <input
            value={newMessage}
            onChange={(event) => setNewMessage(event.target.value)}
            placeholder="Type a message"
          />
          <button type="submit">Send</button>
        </form>
      </main>
    </div>
  );
}
