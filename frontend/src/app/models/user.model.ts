export interface User {
  id: string | number;
  username?: string;
  name?: string;
  email: string;
  role?: string;
  token?: string;
  profileImage?: string;
  adminPermissions?: string;
}