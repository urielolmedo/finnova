import { createContext, useContext, useState } from 'react';
import api from '../api/axios';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(() => {
    const guardado = localStorage.getItem('finnova_usuario');
    return guardado ? JSON.parse(guardado) : null;
  });

  const login = async (email, password) => {
    const { data } = await api.post('/auth/login', { email, password });
    localStorage.setItem('finnova_token', data.token);
    localStorage.setItem('finnova_usuario', JSON.stringify({ email: data.email, nombre: data.nombre }));
    setUsuario({ email: data.email, nombre: data.nombre });
    return data;
  };

  const registro = async (email, password, nombre, apellido) => {
    const { data } = await api.post('/auth/registro', { email, password, nombre, apellido });
    localStorage.setItem('finnova_token', data.token);
    localStorage.setItem('finnova_usuario', JSON.stringify({ email: data.email, nombre: data.nombre }));
    setUsuario({ email: data.email, nombre: data.nombre });
    return data;
  };

  const logout = async () => {
    try {
      await api.post('/auth/logout');
    } catch (e) {
      // aunque falle el logout en el server, igual limpiamos el lado del cliente
    }
    localStorage.removeItem('finnova_token');
    localStorage.removeItem('finnova_usuario');
    setUsuario(null);
  };

  return (
    <AuthContext.Provider value={{ usuario, login, registro, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}