import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { UserRole } from '../types';

interface AuthState {
  token: string | null;
  userId: number | null;
  username: string | null;
  role: UserRole | null;
  isAuthenticated: boolean;
  _hasHydrated: boolean;
  login: (token: string, userId: number, username: string, role: UserRole) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      userId: null,
      username: null,
      role: null,
      isAuthenticated: false,
      _hasHydrated: false,
      login: (token, userId, username, role) =>
        set({ token, userId, username, role, isAuthenticated: true }),
      logout: () =>
        set({ token: null, userId: null, username: null, role: null, isAuthenticated: false }),
    }),
    {
      name: 'egov-auth',
      onRehydrateStorage: () => () => {
        useAuthStore.setState({ _hasHydrated: true });
      },
    }
  )
);
