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

// Create a dynamic storage based on rememberMe preference
const createDynamicStorage = (rememberMe: boolean = false) => {
  return {
    getItem: (name: string) => {
      // Check localStorage first for persistent auth, then sessionStorage
      const localItem = localStorage.getItem(name);
      if (localItem) return localItem;
      return sessionStorage.getItem(name);
    },
    setItem: (name: string, value: string) => {
      // Use localStorage for remember me, sessionStorage otherwise
      if (rememberMe) {
        localStorage.setItem(name, value);
        sessionStorage.removeItem(name);
      } else {
        sessionStorage.setItem(name, value);
        localStorage.removeItem(name);
      }
    },
    removeItem: (name: string) => {
      localStorage.removeItem(name);
      sessionStorage.removeItem(name);
    },
  };
};

export const useAuthStore = create<AuthState>()(
  persist(
    (set, _get) => ({
      user: null,
      isAuthenticated: false,
      rememberMe: false, // Default to false for better security
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
        // Clear token from both storage locations
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
        // Restore authentication state and JWT token on page load
        if (state?.user?.token) {
          // Token is already in state, it will be used by the request interceptor
          if (state.rememberMe) {
            localStorage.setItem('jwt_token', state.user.token);
          } else {
            sessionStorage.setItem('jwt_token', state.user.token);
          }
        }
        // Mark as hydrated
        state?.setHasHydrated(true);
      },
    }
  )
);
