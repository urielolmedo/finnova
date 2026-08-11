import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
});

// Interceptor: agrega el token JWT a todas las peticiones automaticamente
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('finnova_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Interceptor: si el backend responde 401/403, el token ya no sirve -> deslogueamos
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      localStorage.removeItem('finnova_token');
      localStorage.removeItem('finnova_usuario');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default api;