export type LoginRequest = {
  email: string;
  password: string;
};

export type AuthResponse = {
  accessToken?: string | null;
  tokenType?: string | null;
  fullName?: string | null;
  email?: string | null;
};

export type RegisterRequest = {
  email: string;
  password: string;
  fullName: string;
};
