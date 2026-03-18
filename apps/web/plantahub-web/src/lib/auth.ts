const TOKEN_KEY = 'token';
const TOKEN_TYPE_KEY = 'tokenType';
const USER_NAME_KEY = 'userFullName';

export function saveAuth(accessToken: string, tokenType: string, fullName?: string) {
  localStorage.setItem(TOKEN_KEY, accessToken);
  localStorage.setItem(TOKEN_TYPE_KEY, tokenType);

  if (fullName) {
    localStorage.setItem(USER_NAME_KEY, fullName);
  }
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function getTokenType() {
  return localStorage.getItem(TOKEN_TYPE_KEY) ?? 'Bearer';
}

export function getUserFullName() {
  return localStorage.getItem(USER_NAME_KEY);
}

export function getUserFirstName() {
  const fullName = getUserFullName();

  if (!fullName) return null;

  return fullName.trim().split(' ')[0] ?? null;
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(TOKEN_TYPE_KEY);
  localStorage.removeItem(USER_NAME_KEY);
}

export function isAuthenticated() {
  return !!getToken();
}