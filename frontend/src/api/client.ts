import axios from 'axios';

const client = axios.create({
  baseURL: '', // Proxied via Vite config
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to attach JWT token
client.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('access_token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor to handle token refresh and 401 redirects
client.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // Prevent infinite loop if refresh token request itself fails with 401
    if (originalRequest.url?.includes('/api/v1/auth/refresh')) {
      localStorage.removeItem('access_token');
      window.dispatchEvent(new Event('auth_session_expired'));
      return Promise.reject(error);
    }

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        // Attempt to call refresh endpoint (HttpOnly cookie will be included automatically)
        const response = await axios.post<{ accessToken: string }>('/api/v1/auth/refresh', {}, {
          headers: { 'Content-Type': 'application/json' },
          withCredentials: true,
        });

        const newAccessToken = response.data.accessToken;
        localStorage.setItem('access_token', newAccessToken);

        // Retry the original request with the new token
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return client(originalRequest);
      } catch (refreshError) {
        localStorage.removeItem('access_token');
        window.dispatchEvent(new Event('auth_session_expired'));
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default client;
