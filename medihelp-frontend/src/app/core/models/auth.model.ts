export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName?: string;
  phone?: string;
  // Cultural / chatbot context — collected at registration so the chatbot can
  // give personalised diet, severity (age-aware), and regional food advice
  dateOfBirth?: string;     // ISO yyyy-MM-dd
  gender?: string;          // Male | Female | Other
  state?: string;           // Indian state / UT
  dietPreference?: string;  // Vegetarian | Non-vegetarian | Vegan | Eggetarian
}

export interface OtpVerifyRequest {
  email: string;
  otp: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  userId: string;
  email: string;
  role: string;
  expiresIn: number;
}
