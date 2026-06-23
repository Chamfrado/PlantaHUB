import { http } from '../lib/http';
import type { AuthResponse, LoginRequest, RegisterRequest } from '../types/api/auth';

export async function loginRequest(payload: LoginRequest): Promise<AuthResponse> {
  return http<AuthResponse>('/v1/auth/login', {
    method: 'POST',
    body: payload,
  });
}

export async function getCurrentAuthUser(): Promise<AuthResponse> {
  return http<AuthResponse>('/v1/auth/me');
}

export async function logoutRequest(): Promise<void> {
  return http<void>('/v1/auth/logout', {
    method: 'POST',
  });
}

export async function registerRequest(payload: RegisterRequest): Promise<void> {
  await http<void>('/v1/auth/register', {
    method: 'POST',
    body: payload,
  });
}
