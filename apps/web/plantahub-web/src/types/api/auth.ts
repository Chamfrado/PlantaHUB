export type LoginRequest = {
  email: string;
  password: string;
};

export type AuthResponse = {
  accessToken: string;
  tokenType: string;
  fullName?: string;
};

export type RegisterRequest = {
  email: string;
  password: string;
  fullName: string;
};