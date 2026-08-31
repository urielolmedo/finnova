import { useEffect, useState } from 'react';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';
import { Link } from 'react-router-dom';

const MODULOS_DISPONIBLES = [
  { id: 'reportes', label: 'Reportes y Resúmenes' },
  { id: 'alertas', label: 'Alertas y Notificaciones' },
  { id: 'simulaciones', label: 'Simulación de Escenarios' },
  { id: 'asistente', label: 'Asistente Financiero' },
];

export default function Perfil() {
  const { usuario, logout } = useAuth();
  const [perfil, setPerfil] = useState(null);
  const [nombre, setNombre] = useState('');
  const [apellido, setApellido] = useState('');
  const [modulosSeleccionados, setModulosSeleccionados] = useState([]);
  const [mensaje, setMensaje] = useState('');

  useEffect(() => {
    api.get('/usuarios/perfil').then(({ data }) => {
      setPerfil(data);
      setNombre(data.nombre);
      setApellido(data.apellido);
      setModulosSeleccionados(data.modulosActivos ? data.modulosActivos.split(',') : []);
    });
  }, []);

  const guardarPerfil = async (e) => {
    e.preventDefault();
    const { data } = await api.put('/usuarios/perfil', { nombre, apellido });
    setPerfil(data);
    setMensaje('Perfil actualizado');
    setTimeout(() => setMensaje(''), 2000);
  };

  const toggleModulo = (id) => {
    setModulosSeleccionados((prev) =>
      prev.includes(id) ? prev.filter((m) => m !== id) : [...prev, id]
    );
  };

  const guardarModulos = async () => {
    const { data } = await api.put('/usuarios/modulos', { modulos: modulosSeleccionados });
    setPerfil(data);
    setMensaje('Módulos actualizados');
    setTimeout(() => setMensaje(''), 2000);
  };

  if (!perfil) return <p>Cargando...</p>;

  return (
    <div style={{ maxWidth: 480, margin: '60px auto', fontFamily: 'sans-serif' }}>
      <h1>FinNova</h1>
      <p>Hola, {usuario?.nombre} ({usuario?.email})</p>
      <button onClick={logout}>Cerrar sesión</button>
      <p><Link to="/transacciones">Ver mis transacciones</Link></p>
      <h2 style={{ marginTop: 32 }}>Editar perfil</h2>
      <form onSubmit={guardarPerfil}>
        <div style={{ marginBottom: 12 }}>
          <label>Nombre</label>
          <input value={nombre} onChange={(e) => setNombre(e.target.value)} style={{ width: '100%', padding: 8 }} />
        </div>
        <div style={{ marginBottom: 12 }}>
          <label>Apellido</label>
          <input value={apellido} onChange={(e) => setApellido(e.target.value)} style={{ width: '100%', padding: 8 }} />
        </div>
        <button type="submit">Guardar perfil</button>
      </form>

      <h2 style={{ marginTop: 32 }}>Módulos activos</h2>
      {MODULOS_DISPONIBLES.map((m) => (
        <div key={m.id}>
          <label>
            <input
              type="checkbox"
              checked={modulosSeleccionados.includes(m.id)}
              onChange={() => toggleModulo(m.id)}
            />
            {' '}{m.label}
          </label>
        </div>
      ))}
      <button onClick={guardarModulos} style={{ marginTop: 12 }}>Guardar módulos</button>

      {mensaje && <p style={{ color: 'green', marginTop: 16 }}>{mensaje}</p>}
    </div>
  );
}