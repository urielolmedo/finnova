import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';
import TransaccionForm from '../components/TransaccionForm';

export default function Transacciones() {
  const { usuario, logout } = useAuth();
  const [transacciones, setTransacciones] = useState([]);
  const [categorias, setCategorias] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [mostrarForm, setMostrarForm] = useState(false);
  const [editando, setEditando] = useState(null);

  // Filtros
  const [filtroCategoria, setFiltroCategoria] = useState('');
  const [filtroDesde, setFiltroDesde] = useState('');
  const [filtroHasta, setFiltroHasta] = useState('');

  const cargarCategorias = async () => {
    const { data } = await api.get('/categorias');
    setCategorias(data);
  };

  const cargarTransacciones = async () => {
    setCargando(true);
    let url = '/transacciones';
    if (filtroCategoria) {
      url = `/transacciones/filtrar/categoria/${filtroCategoria}`;
    } else if (filtroDesde && filtroHasta) {
      url = `/transacciones/filtrar/fecha?desde=${filtroDesde}&hasta=${filtroHasta}`;
    }
    const { data } = await api.get(url);
    setTransacciones(data);
    setCargando(false);
  };

  useEffect(() => {
    cargarCategorias();
    cargarTransacciones();
  }, []);

  const aplicarFiltros = (e) => {
    e.preventDefault();
    cargarTransacciones();
  };

  const limpiarFiltros = () => {
    setFiltroCategoria('');
    setFiltroDesde('');
    setFiltroHasta('');
    setTimeout(cargarTransacciones, 0);
  };

  const eliminar = async (id) => {
    if (!confirm('¿Eliminar esta transacción?')) return;
    await api.delete(`/transacciones/${id}`);
    cargarTransacciones();
  };

  const editar = (t) => {
    setEditando(t);
    setMostrarForm(true);
  };

  const nuevaTransaccion = () => {
    setEditando(null);
    setMostrarForm(true);
  };

  const alGuardar = () => {
    setMostrarForm(false);
    setEditando(null);
    cargarTransacciones();
  };

  const totalIngresos = transacciones.filter(t => t.tipo === 'INGRESO').reduce((s, t) => s + t.monto, 0);
  const totalEgresos = transacciones.filter(t => t.tipo === 'EGRESO').reduce((s, t) => s + t.monto, 0);

  return (
    <div style={{ maxWidth: 900, margin: '40px auto', fontFamily: 'sans-serif', padding: '0 16px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1>FinNova</h1>
        <div>
          <Link to="/perfil" style={{ marginRight: 16 }}>Mi perfil</Link>
          <button onClick={logout}>Cerrar sesión</button>
        </div>
      </div>
      <p>Hola, {usuario?.nombre}</p>

      <div style={{ display: 'flex', gap: 24, margin: '16px 0' }}>
        <div style={{ padding: 12, background: '#e8f5e9', borderRadius: 6 }}>
          <strong>Ingresos:</strong> ${totalIngresos.toFixed(2)}
        </div>
        <div style={{ padding: 12, background: '#ffebee', borderRadius: 6 }}>
          <strong>Egresos:</strong> ${totalEgresos.toFixed(2)}
        </div>
        <div style={{ padding: 12, background: '#e3f2fd', borderRadius: 6 }}>
          <strong>Balance:</strong> ${(totalIngresos - totalEgresos).toFixed(2)}
        </div>
      </div>

      <button onClick={nuevaTransaccion} style={{ padding: '8px 16px', marginBottom: 16 }}>
        + Nueva transacción
      </button>

      {mostrarForm && (
        <TransaccionForm
          categorias={categorias}
          transaccion={editando}
          onGuardado={alGuardar}
          onCancelar={() => setMostrarForm(false)}
        />
      )}

      <h3>Filtros</h3>
      <form onSubmit={aplicarFiltros} style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap', marginBottom: 16 }}>
        <select value={filtroCategoria} onChange={(e) => setFiltroCategoria(e.target.value)}>
          <option value="">Todas las categorías</option>
          {categorias.map((c) => (
            <option key={c.id} value={c.id}>{c.nombre}</option>
          ))}
        </select>
        <span>o rango de fechas:</span>
        <input type="date" value={filtroDesde} onChange={(e) => setFiltroDesde(e.target.value)} />
        <input type="date" value={filtroHasta} onChange={(e) => setFiltroHasta(e.target.value)} />
        <button type="submit">Filtrar</button>
        <button type="button" onClick={limpiarFiltros}>Limpiar</button>
      </form>

      <h3>Historial</h3>
      {cargando ? (
        <p>Cargando...</p>
      ) : transacciones.length === 0 ? (
        <p>No hay transacciones registradas.</p>
      ) : (
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ textAlign: 'left', borderBottom: '2px solid #ccc' }}>
              <th style={{ padding: 8 }}>Fecha</th>
              <th style={{ padding: 8 }}>Tipo</th>
              <th style={{ padding: 8 }}>Categoría</th>
              <th style={{ padding: 8 }}>Descripción</th>
              <th style={{ padding: 8 }}>Monto</th>
              <th style={{ padding: 8 }}>Comprobante</th>
              <th style={{ padding: 8 }}>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {transacciones.map((t) => (
              <tr key={t.id} style={{ borderBottom: '1px solid #eee' }}>
                <td style={{ padding: 8 }}>{t.fecha}{t.esRecurrente && ' 🔁'}</td>
                <td style={{ padding: 8, color: t.tipo === 'INGRESO' ? 'green' : 'red' }}>{t.tipo}</td>
                <td style={{ padding: 8 }}>{t.categoriaNombre}</td>
                <td style={{ padding: 8 }}>{t.descripcion}</td>
                <td style={{ padding: 8 }}>${t.monto.toFixed(2)}</td>
                <td style={{ padding: 8 }}>
                  {t.tieneComprobante ? (
                    <a
                      href={`http://localhost:8080/uploads/comprobantes/${t.comprobanteUrl}`}
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      📎 Ver
                    </a>
                  ) : '—'}
                </td>
                <td style={{ padding: 8 }}>
                  <button onClick={() => editar(t)}>Editar</button>{' '}
                  <button onClick={() => eliminar(t.id)}>Eliminar</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
