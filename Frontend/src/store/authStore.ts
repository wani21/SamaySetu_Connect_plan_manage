import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';

interface User {
  email: string;
  role: string;
  token: string;
  name?: string;
}

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  rememberMe: boolean;
  _hasHydrated: boolean;
  setHasHydrated: (state: boolean) => void;
  login: (user: User, rememberMe?: boolean) => void;
  logout: () => void;
  setRememberMe: (remember: boolean) => void;
}

// Dynamic storage — decides localStorage vs sessionStorage based on the
// rememberMe flag INSIDE the persisted value (not a captured closure).
const createDynamicStorage = () => ({
  getItem: (name: string) => {
    // Check localStorage first (persistent), then sessionStorage (tab-scoped)
    return localStorage.getItem(name) || sessionStorage.getItem(name);
  },
  setItem: (name: string, value: string) => {
    // Parse the persisted state to read the rememberMe flag
    try {
      const parsed = JSON.parse(value);
      const remember = parsed?.state?.rememberMe ?? false;
      if (remember) {
        localStorage.setItem(name, value);
        sessionStorage.removeItem(name);
      } else {
        sessionStorage.setItem(name, value);
        localStorage.removeItem(name);
      }
    } catch {
      // Fallback to sessionStorage if parsing fails
      sessionStorage.setItem(name, value);
    }
  },
  removeItem: (name: string) => {
    localStorage.removeItem(name);
    sessionStorage.removeItem(name);
  },
});

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      isAuthenticated: false,
      rememberMe: false,
      _hasHydrated: false,
      setHasHydrated: (state) => {
        set({ _hasHydrated: state });
      },
      login: (user, rememberMe = false) => {
        if (rememberMe) {
          localStorage.setItem('jwt_token', user.token);
          sessionStorage.removeItem('jwt_token');
        } else {
          sessionStorage.setItem('jwt_token', user.token);
          localStorage.removeItem('jwt_token');
        }
        set({ user, isAuthenticated: true, rememberMe });
      },
      logout: () => {
        localStorage.removeItem('jwt_token');
        sessionStorage.removeItem('jwt_token');
        localStorage.removeItem('auth-storage');
        sessionStorage.removeItem('auth-storage');
        set({ user: null, isAuthenticated: false, rememberMe: false });
      },
      setRememberMe: (remember) => {
        set({ rememberMe: remember });
      },
    }),
    {
      name: 'auth-storage',
      storage: createJSONStorage(() => createDynamicStorage()),
      onRehydrateStorage: () => (state) => {
        if (state?.user?.token) {
          if (state.rememberMe) {
            localStorage.setItem('jwt_token', state.user.token);
          } else {
            sessionStorage.setItem('jwt_token', state.user.token);
          }
        }
        state?.setHasHydrated(true);
      },
    }
  )
);
