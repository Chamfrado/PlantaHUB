import { http } from '../lib/http';
import type { AuthResponse, LoginRequest, RegisterRequest } from '../types/api/auth';

export async function login(payload: LoginRequest): Promise<AuthResponse> {
  return http<AuthResponse>('/v1/auth/login', {
    method: 'POST',
    body: payload,
  });
}

export async function register(payload: RegisterRequest): Promise<void> {
  await http<void>('/v1/auth/register', {
    method: 'POST',
    body: payload,
  });
}