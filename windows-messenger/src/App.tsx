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

type MessengerTab = 'all' | 'groups' | 'channels';

const initialStatus = 'Offline';
const sessionKey = 'wm_windows_session';

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
  const [activeTab, setActiveTab] = useState<MessengerTab>('all');
  const [search, setSearch] = useState('');

  useEffect(() => {
    const saved = localStorage.getItem(sessionKey);
    if (!saved) {
      return;
    }

    try {
      const parsed = JSON.parse(saved) as Session;
      if (parsed?.token && parsed?.userId && parsed?.username) {
        setSession(parsed);
      }
    } catch {
      localStorage.removeItem(sessionKey);
    }
  }, []);

  const selectedChat = useMemo(
    () => chats.find((chat) => chat.user_id === selectedChatId) ?? null,
    [chats, selectedChatId]
  );

  const filteredChats = useMemo(() => {
    const byName = chats.filter((chat) => chat.name.toLowerCase().includes(search.toLowerCase()));

    if (activeTab === 'all') {
      return byName;
    }

    return byName.filter((chat) => {
      if (activeTab === 'groups') {
        return chat.name.toLowerCase().includes('group');
      }
      return chat.name.toLowerCase().includes('channel');
    });
  }, [activeTab, chats, search]);

  async function handleLogin(event: FormEvent) {
    event.preventDefault();
    setError('');
    const response = await login(username.trim(), password);

    if (response.api_status === '200' && response.access_token && response.user_id) {
      const nextSession = { token: response.access_token, userId: response.user_id, username: username.trim() };
      setSession(nextSession);
      localStorage.setItem(sessionKey, JSON.stringify(nextSession));
      return;
    }

    setError(response.message ?? 'Login failed. Please check credentials or API compatibility.');
  }

  function handleLogout() {
    localStorage.removeItem(sessionKey);
    setSession(null);
    setChats([]);
    setMessages([]);
    setSelectedChatId(null);
    setNewMessage('');
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
          <h1>WorldMates Desktop</h1>
          <p>Вход с тем же аккаунтом, что и в Android приложении.</p>
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
      <nav className="rail">
        <button className="rail-btn active">💬</button>
        <button className="rail-btn">📞</button>
        <button className="rail-btn">⚙️</button>
      </nav>

      <aside className="sidebar">
        <header className="sidebar-header">
          <div>
            <h2>WorldMates</h2>
            <small>{session.username}</small>
          </div>
          <button className="logout-btn" onClick={handleLogout}>Exit</button>
        </header>

        <div className="tabs">
          <button className={activeTab === 'all' ? 'tab active' : 'tab'} onClick={() => setActiveTab('all')}>All</button>
          <button className={activeTab === 'groups' ? 'tab active' : 'tab'} onClick={() => setActiveTab('groups')}>Groups</button>
          <button className={activeTab === 'channels' ? 'tab active' : 'tab'} onClick={() => setActiveTab('channels')}>Channels</button>
        </div>

        <input
          className="search"
          placeholder="Search"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />

        <div className="status">{status}</div>

        <div className="chat-list">
          {filteredChats.map((chat) => (
            <button
              key={chat.user_id}
              className={chat.user_id === selectedChatId ? 'chat-item active' : 'chat-item'}
              onClick={() => setSelectedChatId(chat.user_id)}
            >
              <strong>{chat.name}</strong>
              <span>{chat.last_message ?? 'No messages yet'}</span>
            </button>
          ))}
        </div>
      </aside>

      <main className="chat-view">
        <header className="chat-header">
          <h3>{selectedChat?.name ?? 'Select chat'}</h3>
          <p>Online status synced with API</p>
        </header>
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
            placeholder="Message"
          />
          <button type="submit">Send</button>
        </form>
      </main>
    </div>
  );
}
