import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/axios';

export default function Categorias() {
  const [categorias, setCategorias] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [mostrarForm, setMostrarForm] = useState(false);
  const [editando, setEditando] = useState(null);
  const [form, setForm] = useState({ nombre: '', tipo: 'EGRESO', color: '#6c757d' });
  const [error, setError] = useState('');
  const [mensaje, setMensaje] = useState('');

  const cargar = async () => {
    setCargando(true);
    const { data } = await api.get('/categorias');
    setCategorias(data);
    setCargando(false);
  };

  useEffect(() => {
    cargar();
  }, []);

  const nuevaCategoria = () => {
    setEditando(null);
    setForm({ nombre: '', tipo: 'EGRESO', color: '#6c757d' });
    setError('');
    setMostrarForm(true);
  };

  const editar = (c) => {
    setEditando(c);
    setForm({ nombre: c.nombre, tipo: c.tipo, color: c.color });
    setError('');
    setMostrarForm(true);
  };

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const guardar = async (e) => {
    e.preventDefault();
    setError('');
    try {
      if (editando) {
        await api.put(`/categorias/${editando.id}`, form);
        setMensaje('Categoría actualizada');
      } else {
        await api.post('/categorias', form);
        setMensaje('Categoría creada');
      }
      setMostrarForm(false);
      cargar();
      setTimeout(() => setMensaje(''), 2000);
    } catch (err) {
      setError(err.response?.data || 'No se pudo guardar la categoría');
    }
  };

  const eliminar = async (c) => {
    if (!confirm(`¿Eliminar la categoría "${c.nombre}"? Esta acción es permanente.`)) return;
    try {
      await api.delete(`/categorias/${c.id}`);
      setMensaje('Categoría eliminada');
      cargar();
      setTimeout(() => setMensaje(''), 2000);
    } catch (err) {
      alert(err.response?.data || 'No se pudo eliminar la categoría');
    }
  };

  const predefinidas = categorias.filter((c) => c.predefinida);
  const personalizadas = categorias.filter((c) => !c.predefinida);

  return (
    <div style={{ maxWidth: 700, margin: '40px auto', fontFamily: 'sans-serif', padding: '0 16px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1>FinNova</h1>
        <Link to="/transacciones">← Volver a Transacciones</Link>
      </div>
      <h2>Categorías</h2>

      <button onClick={nuevaCategoria} style={{ padding: '8px 16px', marginBottom: 16 }}>
        + Nueva categoría personalizada
      </button>

      {mostrarForm && (
        <div style={{ border: '1px solid #ccc', borderRadius: 8, padding: 16, marginBottom: 16 }}>
          <h3>{editando ? `Editando: ${editando.nombre}` : 'Nueva categoría'}</h3>
          <form onSubmit={guardar}>
            <div style={{ marginBottom: 8 }}>
              <label>Nombre: </label>
              <input
                type="text"
                name="nombre"
                value={form.nombre}
                onChange={handleChange}
                maxLength={50}
                required
              />
            </div>
            <div style={{ marginBottom: 8 }}>
              <label>Tipo: </label>
              <select name="tipo" value={form.tipo} onChange={handleChange}>
                <option value="INGRESO">Ingreso</option>
                <option value="EGRESO">Egreso</option>
                <option value="AMBOS">Ambos</option>
              </select>
            </div>
            <div style={{ marginBottom: 8 }}>
              <label>Color: </label>
              <input type="color" name="color" value={form.color} onChange={handleChange} />
            </div>
            {error && <p style={{ color: 'red' }}>{String(error)}</p>}
            <button type="submit">Guardar</button>{' '}
            <button type="button" onClick={() => setMostrarForm(false)}>Cancelar</button>
          </form>
        </div>
      )}

      {mensaje && <p style={{ color: 'green' }}>{mensaje}</p>}

      {cargando ? (
        <p>Cargando...</p>
      ) : (
        <>
          <h3>Predefinidas del sistema</h3>
          <ul style={{ listStyle: 'none', padding: 0 }}>
            {predefinidas.map((c) => (
              <li key={c.id} style={{ display: 'flex', alignItems: 'center', gap: 8, padding: 6 }}>
                <span style={{ width: 14, height: 14, borderRadius: '50%', background: c.color, display: 'inline-block' }} />
                {c.nombre} <small style={{ color: '#888' }}>({c.tipo})</small>
              </li>
            ))}
          </ul>

          <h3>Mis categorías personalizadas</h3>
          {personalizadas.length === 0 ? (
            <p>No tenés categorías personalizadas todavía.</p>
          ) : (
            <ul style={{ listStyle: 'none', padding: 0 }}>
              {personalizadas.map((c) => (
                <li key={c.id} style={{ display: 'flex', alignItems: 'center', gap: 8, padding: 6 }}>
                  <span style={{ width: 14, height: 14, borderRadius: '50%', background: c.color, display: 'inline-block' }} />
                  {c.nombre} <small style={{ color: '#888' }}>({c.tipo})</small>
                  <button onClick={() => editar(c)}>Editar</button>
                  <button onClick={() => eliminar(c)}>Eliminar</button>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </div>
  );
}