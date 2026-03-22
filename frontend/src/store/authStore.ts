import { create } from 'zustand';
import type { UserRole } from '../types';

interface AuthState {
  token: string | null;
  userId: number | null;
  username: string | null;
  role: UserRole | null;
  isAuthenticated: boolean;
  login: (token: string, userId: number, username: string, role: UserRole) => void;
  logout: () => void;
}

function loadFromStorage(): Partial<AuthState> {
  try {
    const raw = localStorage.getItem('egov-auth');
    if (raw) {
      const parsed = JSON.parse(raw);
      const s = parsed.state || parsed;
      if (s.token && s.isAuthenticated) {
        return { token: s.token, userId: s.userId, username: s.username, role: s.role, isAuthenticated: true };
      }
    }
  } catch { /* ignore */ }
  return {};
}

const saved = loadFromStorage();

export const useAuthStore = create<AuthState>()((set) => ({
  token: null,
  userId: null,
  username: null,
  role: null,
  isAuthenticated: false,
  ...saved,
  login: (token, userId, username, role) => {
    const state = { token, userId, username, role, isAuthenticated: true };
    localStorage.setItem('egov-auth', JSON.stringify({ state }));
    set(state);
  },
  logout: () => {
    localStorage.removeItem('egov-auth');
    set({ token: null, userId: null, username: null, role: null, isAuthenticated: false });
  },
}));
