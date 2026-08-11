import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Registro() {
  const [form, setForm] = useState({ email: '', password: '', nombre: '', apellido: '' });
  const [error, setError] = useState('');
  const [cargando, setCargando] = useState(false);
  const { registro } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setCargando(true);
    try {
      await registro(form.email, form.password, form.nombre, form.apellido);
      navigate('/perfil');
    } catch (err) {
      setError(err.response?.data || 'No se pudo completar el registro');
    } finally {
      setCargando(false);
    }
  };

  return (
    <div style={{ maxWidth: 360, margin: '80px auto', fontFamily: 'sans-serif' }}>
      <h1>FinNova</h1>
      <h2>Crear cuenta</h2>
      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: 12 }}>
          <input name="nombre" placeholder="Nombre" value={form.nombre} onChange={handleChange} required style={{ width: '100%', padding: 8 }} />
        </div>
        <div style={{ marginBottom: 12 }}>
          <input name="apellido" placeholder="Apellido" value={form.apellido} onChange={handleChange} required style={{ width: '100%', padding: 8 }} />
        </div>
        <div style={{ marginBottom: 12 }}>
          <input name="email" type="email" placeholder="Email" value={form.email} onChange={handleChange} required style={{ width: '100%', padding: 8 }} />
        </div>
        <div style={{ marginBottom: 12 }}>
          <input name="password" type="password" placeholder="Contraseña" value={form.password} onChange={handleChange} required minLength={8} style={{ width: '100%', padding: 8 }} />
        </div>
        {error && <p style={{ color: 'red' }}>{String(error)}</p>}
        <button type="submit" disabled={cargando} style={{ width: '100%', padding: 10 }}>
          {cargando ? 'Creando cuenta...' : 'Registrarme'}
        </button>
      </form>
      <p>¿Ya tenés cuenta? <Link to="/login">Iniciar sesión</Link></p>
    </div>
  );
}