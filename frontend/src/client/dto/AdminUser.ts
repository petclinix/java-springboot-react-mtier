export interface AdminUser {
  id: number;
  username: string;
  role: string;
  active: boolean;
  lastLogin?: string | null;
}
