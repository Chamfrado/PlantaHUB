import { http } from '../lib/http';
import type { LibraryProductDTO } from '../types/api/library';

export async function getMyLibrary() {
  return http<LibraryProductDTO[]>('/v1/me/library');
}