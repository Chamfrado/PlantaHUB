import { http } from '../lib/http';
import type { DeleteProfileRequest, DeleteProfileResponse, ProfileResponse, UpdateProfileRequest } from '../types/api/profile';

export async function getMyProfile() {
  return http<ProfileResponse>('/v1/me/profile');
}

export async function updateMyProfile(payload: UpdateProfileRequest) {
  return http<ProfileResponse>('/v1/me/profile', {
    method: 'PUT',
    body: payload,
  });
}

export async function deleteMyProfile(payload: DeleteProfileRequest) {
  return http<DeleteProfileResponse>('/v1/me/profile/delete', {
    method: 'POST',
    body: payload,
  });
}