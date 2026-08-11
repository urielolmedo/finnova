import { useState } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import api from '../api/axios';

export default function ResetearPassword() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const [nuevaPassword, setNuevaPassword] = useState('');
  const [mensaje, setMensaje] = useState('');
  const [error, setError] = useState('');
  const [cargando, setCargando] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setCargando(true);
    try {
      await api.post('/auth/resetear-password', { token, nuevaPassword });
      setMensaje('Contraseña actualizada. Ya podés iniciar sesión.');
      setTimeout(() => navigate('/login'), 2000);
    } catch (err) {
      setError(err.response?.data || 'El link es inválido o expiró. Pedí uno nuevo.');
    } finally {
      setCargando(false);
    }
  };

  if (!token) {
    return (
      <div style={{ maxWidth: 360, margin: '80px auto', fontFamily: 'sans-serif' }}>
        <p>Link inválido. <Link to="/recuperar-password">Pedí uno nuevo</Link>.</p>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: 360, margin: '80px auto', fontFamily: 'sans-serif' }}>
      <h1>FinNova</h1>
      <h2>Elegí tu nueva contraseña</h2>
      {!mensaje ? (
        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: 12 }}>
            <input
              type="password"
              placeholder="Nueva contraseña"
              value={nuevaPassword}
              onChange={(e) => setNuevaPassword(e.target.value)}
              required
              minLength={8}
              style={{ width: '100%', padding: 8 }}
            />
          </div>
          {error && <p style={{ color: 'red' }}>{String(error)}</p>}
          <button type="submit" disabled={cargando} style={{ width: '100%', padding: 10 }}>
            {cargando ? 'Guardando...' : 'Cambiar contraseña'}
          </button>
        </form>
      ) : (
        <p style={{ color: 'green' }}>{mensaje}</p>
      )}
    </div>
  );
}