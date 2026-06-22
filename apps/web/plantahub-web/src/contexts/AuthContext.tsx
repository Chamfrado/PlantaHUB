/* eslint-disable react-hooks/set-state-in-effect */
/* eslint-disable react-refresh/only-export-components */
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { useNavigate } from 'react-router-dom';
import { AUTH_SESSION_EXPIRED_EVENT, resetSessionExpiredFlag } from '../lib/auth-events';
import { loginRequest, registerRequest } from '../services/auth.service';

type AuthUser = {
  fullName: string | null;
  firstName: string | null;
  email: string | null;
};

type LoginPayload = {
  email: string;
  password: string;
};

type RegisterPayload = {
  fullName: string;
  email: string;
  password: string;
};

type AuthContextValue = {
  isAuthenticated: boolean;
  isLoading: boolean;
  user: AuthUser | null;
  token: string | null;
  login: (payload: LoginPayload) => Promise<void>;
  register: (payload: RegisterPayload) => Promise<void>;
  logout: () => void;
};

const TOKEN_KEY = 'token';
const TOKEN_TYPE_KEY = 'tokenType';
const USER_FULL_NAME_KEY = 'userFullName';
const USER_EMAIL_KEY = 'userEmail';

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function getFirstName(fullName: string | null) {
  if (!fullName) return null;
  return fullName.trim().split(' ')[0] ?? null;
}

function getStoredAuth() {
  const token = localStorage.getItem(TOKEN_KEY);
  const tokenType = localStorage.getItem(TOKEN_TYPE_KEY) ?? 'Bearer';
  const fullName = localStorage.getItem(USER_FULL_NAME_KEY);
  const email = localStorage.getItem(USER_EMAIL_KEY);

  return {
    token,
    tokenType,
    fullName,
    email,
  };
}

function persistAuth({
  token,
  tokenType,
  fullName,
  email,
}: {
  token: string;
  tokenType: string;
  fullName?: string | null;
  email?: string | null;
}) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(TOKEN_TYPE_KEY, tokenType);

  if (fullName) {
    localStorage.setItem(USER_FULL_NAME_KEY, fullName);
  } else {
    localStorage.removeItem(USER_FULL_NAME_KEY);
  }

  if (email) {
    localStorage.setItem(USER_EMAIL_KEY, email);
  } else {
    localStorage.removeItem(USER_EMAIL_KEY);
  }
}

function clearStoredAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(TOKEN_TYPE_KEY);
  localStorage.removeItem(USER_FULL_NAME_KEY);
  localStorage.removeItem(USER_EMAIL_KEY);
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate();
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const stored = getStoredAuth();

    if (stored.token) {
      setToken(stored.token);
      setUser({
        fullName: stored.fullName,
        firstName: getFirstName(stored.fullName),
        email: stored.email,
      });
    }

    setIsLoading(false);
  }, []);

  useEffect(() => {
    function handleSessionExpired() {
      const currentPath = window.location.pathname + window.location.search;
      const existingParams = new URLSearchParams(window.location.search);
      const redirect =
        window.location.pathname === '/login'
          ? existingParams.get('redirect') || '/'
          : currentPath;

      clearStoredAuth();
      setToken(null);
      setUser(null);

      navigate(
        `/login?expired=1&redirect=${encodeURIComponent(redirect)}`,
        { replace: true }
      );
    }

    window.addEventListener(AUTH_SESSION_EXPIRED_EVENT, handleSessionExpired);

    return () => {
      window.removeEventListener(AUTH_SESSION_EXPIRED_EVENT, handleSessionExpired);
    };
  }, [navigate]);

  const login = useCallback(async ({ email, password }: LoginPayload) => {
    const response = await loginRequest({ email, password });

    const fullName = response.fullName ?? localStorage.getItem(USER_FULL_NAME_KEY);

    persistAuth({
      token: response.accessToken,
      tokenType: response.tokenType,
      fullName,
      email,
    });

    setToken(response.accessToken);
    resetSessionExpiredFlag();
    setUser({
      fullName: fullName ?? null,
      firstName: getFirstName(fullName ?? null),
      email,
    });
  }, []);

  const register = useCallback(async ({ fullName, email, password }: RegisterPayload) => {
    await registerRequest({ fullName, email, password });

    const response = await loginRequest({ email, password });

    persistAuth({
      token: response.accessToken,
      tokenType: response.tokenType,
      fullName,
      email,
    });

    setToken(response.accessToken);
    resetSessionExpiredFlag();
    setUser({
      fullName,
      firstName: getFirstName(fullName),
      email,
    });
  }, []);

  const logout = useCallback(() => {
    clearStoredAuth();
    resetSessionExpiredFlag();
    setToken(null);
    setUser(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      isAuthenticated: !!token,
      isLoading,
      user,
      token,
      login,
      register,
      logout,
    }),
    [token, isLoading, user, login, register, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }

  return context;
}
