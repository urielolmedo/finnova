import { useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/axios';

export default function RecuperarPassword() {
  const [email, setEmail] = useState('');
  const [mensaje, setMensaje] = useState('');
  const [cargando, setCargando] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setCargando(true);
    try {
      const { data } = await api.post('/auth/recuperar-password', { email });
      setMensaje(data);
    } catch (err) {
      setMensaje('Ocurrió un error. Intentá de nuevo.');
    } finally {
      setCargando(false);
    }
  };

  return (
    <div style={{ maxWidth: 360, margin: '80px auto', fontFamily: 'sans-serif' }}>
      <h1>FinNova</h1>
      <h2>Recuperar contraseña</h2>
      {!mensaje ? (
        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: 12 }}>
            <input
              type="email"
              placeholder="Tu email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              style={{ width: '100%', padding: 8 }}
            />
          </div>
          <button type="submit" disabled={cargando} style={{ width: '100%', padding: 10 }}>
            {cargando ? 'Enviando...' : 'Enviar instrucciones'}
          </button>
        </form>
      ) : (
        <p>{mensaje}</p>
      )}
      <p><Link to="/login">Volver a iniciar sesión</Link></p>
    </div>
  );
}